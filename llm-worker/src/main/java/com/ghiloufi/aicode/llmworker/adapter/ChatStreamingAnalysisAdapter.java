package com.ghiloufi.aicode.llmworker.adapter;

import com.ghiloufi.aicode.core.application.service.TicketContextService;
import com.ghiloufi.aicode.core.domain.model.DiffExpansionResult;
import com.ghiloufi.aicode.core.domain.model.EnrichedDiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.PrMetadata;
import com.ghiloufi.aicode.core.domain.model.RepositoryPolicies;
import com.ghiloufi.aicode.core.domain.model.ReviewChunk;
import com.ghiloufi.aicode.core.domain.model.ReviewConfiguration;
import com.ghiloufi.aicode.core.domain.model.TicketContext;
import com.ghiloufi.aicode.core.domain.port.output.AIInteractionPort;
import com.ghiloufi.aicode.core.domain.port.output.ReviewAnalysisPort;
import com.ghiloufi.aicode.core.service.expansion.DiffExpansionService;
import com.ghiloufi.aicode.core.service.policy.RepositoryPolicyProvider;
import com.ghiloufi.aicode.core.service.prompt.PromptBuilder;
import com.ghiloufi.aicode.core.service.prompt.ReviewPromptResult;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatStreamingAnalysisAdapter implements ReviewAnalysisPort {

  private static final String ANALYSIS_METHOD = "chat-streaming";

  private final AIInteractionPort aiPort;
  private final PromptBuilder promptBuilder;
  private final TicketContextService ticketContextService;
  private final DiffExpansionService diffExpansionService;
  private final RepositoryPolicyProvider repositoryPolicyProvider;

  @Override
  public Flux<ReviewChunk> analyzeCode(
      final EnrichedDiffAnalysisBundle enrichedDiff, final ReviewConfiguration config) {
    log.info(
        "Starting AI code review for {} files with {} context matches",
        enrichedDiff.getModifiedFileCount(),
        enrichedDiff.getContextMatchCount());

    final ReviewConfiguration configWithLlmMetadata =
        config.withLlmMetadata(aiPort.getProviderName(), aiPort.getModelName());

    final var basicBundle = enrichedDiff.toBasicBundle();
    final var repo = enrichedDiff.repositoryIdentifier();
    final var prMetadata = enrichedDiff.prMetadata();

    return Mono.zip(
            ticketContextService.extractFromMergeRequest(
                prMetadata.title(), prMetadata.description()),
            diffExpansionService.expandDiff(basicBundle),
            repositoryPolicyProvider.getPolicies(repo))
        .map(
            tuple -> {
              final TicketContext ticketContext = tuple.getT1();
              final DiffExpansionResult expansionResult = tuple.getT2();
              final RepositoryPolicies policies = tuple.getT3();

              logContextResults(ticketContext, expansionResult, prMetadata, policies);

              return promptBuilder.buildStructuredReviewPrompt(
                  enrichedDiff,
                  configWithLlmMetadata,
                  ticketContext,
                  expansionResult,
                  prMetadata,
                  policies);
            })
        .doOnNext(
            prompt -> {
              log.debug(
                  "Built review prompt: {} chars (context: {})",
                  prompt.totalLength(),
                  enrichedDiff.hasContext());
              logFullPromptForDebugging(prompt);
            })
        .flatMapMany(
            (final ReviewPromptResult prompt) ->
                aiPort.streamCompletion(prompt.systemPrompt(), prompt.userPrompt()))
        .map(content -> ReviewChunk.of(ReviewChunk.ChunkType.ANALYSIS, content))
        .doOnNext(chunk -> log.debug("Received review chunk: {} chars", chunk.content().length()))
        .doOnComplete(() -> log.info("AI code review completed successfully"))
        .doOnError(e -> log.error("AI code review failed", e))
        .timeout(Duration.ofSeconds(60));
  }

  @Override
  public String getAnalysisMethod() {
    return ANALYSIS_METHOD;
  }

  @Override
  public String getProviderName() {
    return aiPort.getProviderName();
  }

  @Override
  public String getModelName() {
    return aiPort.getModelName();
  }

  @Override
  public boolean supportsStreaming() {
    return true;
  }

  private void logContextResults(
      final TicketContext ticketContext,
      final DiffExpansionResult expansionResult,
      final PrMetadata prMetadata,
      final RepositoryPolicies policies) {
    if (ticketContext.isEmpty()) {
      log.debug("No ticket context found");
    } else {
      log.debug("Ticket context extracted: {}", ticketContext.ticketId());
    }

    if (expansionResult.hasExpandedFiles()) {
      log.debug(
          "Expanded {} files ({} skipped)",
          expansionResult.filesExpanded(),
          expansionResult.filesSkipped());
    }

    if (prMetadata.title() != null) {
      log.debug(
          "PR metadata: {} labels, {} commits",
          prMetadata.labels().size(),
          prMetadata.commits().size());
    }

    if (policies.hasPolicies()) {
      log.debug("Repository policies loaded: {} documents", policies.allPolicies().size());
    }
  }

  private void logFullPromptForDebugging(final ReviewPromptResult prompt) {
    if (!log.isDebugEnabled()) {
      return;
    }

    final String separator = "═".repeat(80);
    final String sectionSeparator = "─".repeat(80);

    log.debug("\n{}", separator);
    log.debug("                    FULL LLM PROMPT FOR CODE REVIEW");
    log.debug("{}", separator);

    log.debug("\n{}  SYSTEM PROMPT ({} chars)  {}", "▶", prompt.systemPrompt().length(), "◀");
    log.debug("{}", sectionSeparator);
    log.debug("\n{}\n", prompt.systemPrompt());

    log.debug("{}", sectionSeparator);
    log.debug("\n{}  USER PROMPT ({} chars)  {}", "▶", prompt.userPrompt().length(), "◀");
    log.debug("{}", sectionSeparator);
    log.debug("\n{}\n", prompt.userPrompt());

    log.debug("{}", separator);
    log.debug("                    END OF FULL LLM PROMPT");
    log.debug("{}\n", separator);
  }
}
