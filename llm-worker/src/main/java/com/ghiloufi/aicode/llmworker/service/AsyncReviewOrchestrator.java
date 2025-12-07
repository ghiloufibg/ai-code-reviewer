package com.ghiloufi.aicode.llmworker.service;

import com.ghiloufi.aicode.core.application.service.TicketContextService;
import com.ghiloufi.aicode.core.application.service.context.ContextOrchestrator;
import com.ghiloufi.aicode.core.config.ContextRetrievalConfig;
import com.ghiloufi.aicode.core.domain.model.ChangeRequestIdentifier;
import com.ghiloufi.aicode.core.domain.model.DiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.DiffExpansionResult;
import com.ghiloufi.aicode.core.domain.model.EnrichedDiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.PrMetadata;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.model.RepositoryPolicies;
import com.ghiloufi.aicode.core.domain.model.ReviewConfiguration;
import com.ghiloufi.aicode.core.domain.model.TicketContext;
import com.ghiloufi.aicode.core.domain.model.async.AsyncReviewRequest;
import com.ghiloufi.aicode.core.domain.port.output.SCMPort;
import com.ghiloufi.aicode.core.infrastructure.factory.SCMProviderFactory;
import com.ghiloufi.aicode.core.service.prompt.PromptBuilder;
import com.ghiloufi.aicode.core.service.prompt.ReviewPromptResult;
import com.ghiloufi.aicode.llmworker.processor.ReviewService;
import com.ghiloufi.aicode.llmworker.schema.ReviewResultSchema;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public final class AsyncReviewOrchestrator {

  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

  private final SCMProviderFactory scmProviderFactory;
  private final ContextOrchestrator contextOrchestrator;
  private final PromptBuilder promptBuilder;
  private final TicketContextService ticketContextService;
  private final ContextRetrievalConfig contextRetrievalConfig;
  private final ReviewService reviewService;

  public ReviewResultSchema performAsyncReview(final AsyncReviewRequest request) {
    log.info(
        "Starting async review for {} PR #{} (requestId={})",
        request.provider(),
        request.changeRequestId(),
        request.requestId());

    final RepositoryIdentifier repo =
        RepositoryIdentifier.create(request.provider(), request.repositoryId());
    final ChangeRequestIdentifier cr =
        ChangeRequestIdentifier.create(request.provider(), request.changeRequestId());

    final SCMPort scmPort = scmProviderFactory.getProvider(request.provider());

    final DiffAnalysisBundle diffBundle = fetchDiff(scmPort, repo, cr);
    log.debug(
        "Fetched diff: {} files, {} lines",
        diffBundle.structuredDiff().files.size(),
        diffBundle.getTotalLineCount());

    final EnrichedDiffAnalysisBundle enrichedDiff = enrichDiff(diffBundle);
    log.debug("Context enrichment: {} matches", enrichedDiff.getContextMatchCount());

    final PrMetadata prMetadata = fetchPrMetadata(scmPort, repo, cr, diffBundle);

    final TicketContext ticketContext = extractTicketContext(prMetadata);
    final DiffExpansionResult expansionResult = expandDiff(scmPort, diffBundle);
    final RepositoryPolicies policies = fetchPolicies(scmPort, repo);

    final ReviewConfiguration config = ReviewConfiguration.defaults();
    final ReviewPromptResult prompt =
        promptBuilder.buildStructuredReviewPrompt(
            enrichedDiff, config, ticketContext, expansionResult, prMetadata, policies);

    logPromptDetails(prompt, enrichedDiff, ticketContext, expansionResult, policies);

    return reviewService.performReview(prompt.systemPrompt(), prompt.userPrompt());
  }

  private DiffAnalysisBundle fetchDiff(
      final SCMPort scmPort, final RepositoryIdentifier repo, final ChangeRequestIdentifier cr) {
    return scmPort.getDiff(repo, cr).block(DEFAULT_TIMEOUT);
  }

  private EnrichedDiffAnalysisBundle enrichDiff(final DiffAnalysisBundle diffBundle) {
    return contextOrchestrator.retrieveEnrichedContext(diffBundle).block(DEFAULT_TIMEOUT);
  }

  private PrMetadata fetchPrMetadata(
      final SCMPort scmPort,
      final RepositoryIdentifier repo,
      final ChangeRequestIdentifier cr,
      final DiffAnalysisBundle diffBundle) {
    if (!contextRetrievalConfig.isPrMetadataEnabled()) {
      return diffBundle.prMetadata() != null ? diffBundle.prMetadata() : PrMetadata.empty();
    }

    return scmPort
        .getPullRequestMetadata(repo, cr)
        .onErrorResume(
            error -> {
              log.warn("Failed to fetch PR metadata: {}", error.getMessage());
              return Mono.just(
                  diffBundle.prMetadata() != null ? diffBundle.prMetadata() : PrMetadata.empty());
            })
        .block(DEFAULT_TIMEOUT);
  }

  private TicketContext extractTicketContext(final PrMetadata prMetadata) {
    if (prMetadata == null || prMetadata.title() == null) {
      return TicketContext.empty();
    }

    return ticketContextService
        .extractFromMergeRequest(prMetadata.title(), prMetadata.description())
        .onErrorResume(
            error -> {
              log.warn("Failed to extract ticket context: {}", error.getMessage());
              return Mono.just(TicketContext.empty());
            })
        .block(DEFAULT_TIMEOUT);
  }

  private DiffExpansionResult expandDiff(
      final SCMPort scmPort, final DiffAnalysisBundle diffBundle) {
    if (!contextRetrievalConfig.isDiffExpansionEnabled()) {
      return DiffExpansionResult.disabled();
    }

    final var expansionConfig = contextRetrievalConfig.diffExpansion();
    final var filePaths = extractModifiedFilePaths(diffBundle);

    if (filePaths.isEmpty()) {
      return DiffExpansionResult.empty();
    }

    final var filesToExpand =
        filePaths.stream()
            .filter(path -> shouldIncludeFile(path, expansionConfig))
            .limit(expansionConfig.maxFilesToExpand())
            .toList();

    log.debug("Expanding {} of {} modified files", filesToExpand.size(), filePaths.size());

    final var expandedFiles =
        filesToExpand.stream()
            .map(
                path -> {
                  try {
                    final String content =
                        scmPort
                            .getFileContent(diffBundle.repositoryIdentifier(), path)
                            .block(DEFAULT_TIMEOUT);
                    return createExpandedContext(path, content, expansionConfig.maxLineCount());
                  } catch (final Exception e) {
                    log.warn("Failed to expand file {}: {}", path, e.getMessage());
                    return com.ghiloufi.aicode.core.domain.model.ExpandedFileContext.empty(path);
                  }
                })
            .filter(com.ghiloufi.aicode.core.domain.model.ExpandedFileContext::hasContent)
            .toList();

    return new DiffExpansionResult(
        expandedFiles,
        filePaths.size(),
        expandedFiles.size(),
        filePaths.size() - expandedFiles.size(),
        filePaths.size() > expansionConfig.maxFilesToExpand() ? "max files limit" : null);
  }

  private java.util.List<String> extractModifiedFilePaths(final DiffAnalysisBundle bundle) {
    return bundle.structuredDiff().files.stream()
        .map(file -> file.newPath)
        .filter(java.util.Objects::nonNull)
        .filter(path -> !path.equals("/dev/null"))
        .toList();
  }

  private boolean shouldIncludeFile(
      final String filePath, final ContextRetrievalConfig.DiffExpansionConfig expansionConfig) {
    final int lastDot = filePath.lastIndexOf('.');
    if (lastDot > 0) {
      final String ext = filePath.substring(lastDot);
      if (expansionConfig.excludedExtensions().contains(ext)) {
        log.trace("Excluding file {} due to extension {}", filePath, ext);
        return false;
      }
    }
    return true;
  }

  private com.ghiloufi.aicode.core.domain.model.ExpandedFileContext createExpandedContext(
      final String filePath, final String content, final int maxLineCount) {
    if (content == null) {
      return com.ghiloufi.aicode.core.domain.model.ExpandedFileContext.empty(filePath);
    }

    final long lineCount = content.lines().count();
    if (lineCount > maxLineCount) {
      final String truncatedContent =
          content.lines().limit(maxLineCount).collect(java.util.stream.Collectors.joining("\n"));
      return com.ghiloufi.aicode.core.domain.model.ExpandedFileContext.truncated(
          filePath, truncatedContent, (int) lineCount);
    }

    return com.ghiloufi.aicode.core.domain.model.ExpandedFileContext.of(filePath, content);
  }

  private RepositoryPolicies fetchPolicies(final SCMPort scmPort, final RepositoryIdentifier repo) {
    if (!contextRetrievalConfig.isPoliciesEnabled()) {
      return RepositoryPolicies.empty();
    }

    final var policiesConfig = contextRetrievalConfig.policies();
    final var files = policiesConfig.files();

    if (files.isEmpty()) {
      return RepositoryPolicies.empty();
    }

    final var policyDocuments =
        files.stream()
            .map(
                path -> {
                  try {
                    final String content =
                        scmPort.getFileContent(repo, path).block(DEFAULT_TIMEOUT);
                    return createPolicyDocument(path, content, policiesConfig.maxContentChars());
                  } catch (final Exception e) {
                    log.trace("Policy file not found: {}", path);
                    return null;
                  }
                })
            .filter(java.util.Objects::nonNull)
            .filter(com.ghiloufi.aicode.core.domain.model.PolicyDocument::hasContent)
            .toList();

    log.debug("Loaded {} policy documents for {}", policyDocuments.size(), repo.getDisplayName());
    return new RepositoryPolicies(policyDocuments);
  }

  private com.ghiloufi.aicode.core.domain.model.PolicyDocument createPolicyDocument(
      final String path, final String content, final int maxChars) {
    if (content == null) {
      return null;
    }

    final String name = extractFileName(path);
    final boolean truncated = content.length() > maxChars;
    final String finalContent =
        truncated ? content.substring(0, maxChars) + "\n... (truncated)" : content;

    return new com.ghiloufi.aicode.core.domain.model.PolicyDocument(
        name, path, finalContent, truncated);
  }

  private String extractFileName(final String path) {
    final int lastSlash = path.lastIndexOf('/');
    return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
  }

  private void logPromptDetails(
      final ReviewPromptResult prompt,
      final EnrichedDiffAnalysisBundle enrichedDiff,
      final TicketContext ticketContext,
      final DiffExpansionResult expansionResult,
      final RepositoryPolicies policies) {
    log.info(
        "Built review prompt: {} total chars (system={}, user={})",
        prompt.totalLength(),
        prompt.systemPrompt().length(),
        prompt.userPrompt().length());

    log.debug(
        "Prompt context: {} context matches, {} expanded files, {} policies, ticket={}",
        enrichedDiff.getContextMatchCount(),
        expansionResult.filesExpanded(),
        policies.policyCount(),
        !ticketContext.isEmpty() ? ticketContext.ticketId() : "none");

    if (log.isTraceEnabled()) {
      log.trace("=== SYSTEM PROMPT ===\n{}", prompt.systemPrompt());
      log.trace("=== USER PROMPT ===\n{}", prompt.userPrompt());
    }
  }
}
