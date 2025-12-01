package com.ghiloufi.aicode.core.service.prompt;

import com.ghiloufi.aicode.core.domain.model.CommitInfo;
import com.ghiloufi.aicode.core.domain.model.DiffExpansionResult;
import com.ghiloufi.aicode.core.domain.model.EnrichedDiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.ExpandedFileContext;
import com.ghiloufi.aicode.core.domain.model.PolicyDocument;
import com.ghiloufi.aicode.core.domain.model.PrMetadata;
import com.ghiloufi.aicode.core.domain.model.RepositoryPolicies;
import com.ghiloufi.aicode.core.domain.model.ReviewConfiguration;
import com.ghiloufi.aicode.core.domain.model.TicketContext;
import com.ghiloufi.aicode.core.domain.service.DiffFormatter;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PromptBuilder {

  private final DiffFormatter diffFormatter;
  private final PromptTemplateService promptTemplateService;

  private String buildSystemPrompt() {
    final StringBuilder systemPrompt = new StringBuilder();

    systemPrompt.append(promptTemplateService.compileSystemPrompt()).append("\n\n");
    systemPrompt.append(promptTemplateService.compileFixGenerationInstructions()).append("\n\n");
    systemPrompt.append(promptTemplateService.compileConfidenceInstructions()).append("\n\n");

    systemPrompt.append(
        """

            ═══════════════════════════════════════════════════════════════════════
            JSON SCHEMA (MUST MATCH THIS EXACTLY):
            ═══════════════════════════════════════════════════════════════════════
            """);
    systemPrompt.append(promptTemplateService.compileSchema()).append("\n\n");
    systemPrompt.append(promptTemplateService.compileOutputRequirements());

    return systemPrompt.toString();
  }

  public ReviewPromptResult buildStructuredReviewPrompt(
      final EnrichedDiffAnalysisBundle enrichedDiff,
      final ReviewConfiguration config,
      final TicketContext ticketContext) {
    return buildStructuredReviewPrompt(
        enrichedDiff,
        config,
        ticketContext,
        DiffExpansionResult.empty(),
        PrMetadata.empty(),
        RepositoryPolicies.empty());
  }

  public ReviewPromptResult buildStructuredReviewPrompt(
      final EnrichedDiffAnalysisBundle enrichedDiff,
      final ReviewConfiguration config,
      final TicketContext ticketContext,
      final DiffExpansionResult expansionResult,
      final PrMetadata prMetadata,
      final RepositoryPolicies policies) {
    if (enrichedDiff == null) {
      throw new IllegalArgumentException("EnrichedDiffAnalysisBundle cannot be null");
    }
    if (config == null) {
      throw new IllegalArgumentException("ReviewConfiguration cannot be null");
    }
    if (ticketContext == null) {
      throw new IllegalArgumentException("TicketContext cannot be null");
    }

    final String ticketContextFormatted = ticketContext.formatForPrompt();
    final String systemPrompt = buildSystemPrompt();
    final String userPrompt =
        buildUserPrompt(
            enrichedDiff,
            config,
            ticketContextFormatted,
            expansionResult != null ? expansionResult : DiffExpansionResult.empty(),
            prMetadata != null ? prMetadata : PrMetadata.empty(),
            policies != null ? policies : RepositoryPolicies.empty());

    return new ReviewPromptResult(systemPrompt, userPrompt);
  }

  private String buildUserPrompt(
      final EnrichedDiffAnalysisBundle enrichedDiff,
      final ReviewConfiguration config,
      final String ticketContext,
      final DiffExpansionResult expansionResult,
      final PrMetadata prMetadata,
      final RepositoryPolicies policies) {

    final String formattedDiff = diffFormatter.formatDiff(enrichedDiff.structuredDiff());

    final int estimatedSize = 8000 + formattedDiff.length();
    final StringBuilder prompt = new StringBuilder(estimatedSize);

    if (!ticketContext.isBlank()) {
      prompt.append(ticketContext);
      prompt.append("\n");
    }

    prompt.append("[REPO]\n");
    prompt.append("language: ").append(config.programmingLanguage()).append("\n");
    prompt.append("focus: ").append(config.focus().name()).append("\n");
    prompt.append("[/REPO]\n\n");

    appendPrMetadataSection(prompt, prMetadata);

    prompt.append("[DIFF]\n");
    prompt.append(formattedDiff);
    prompt.append("\n[/DIFF]\n");

    if (enrichedDiff.hasContext() && enrichedDiff.getContextMatchCount() > 0) {
      prompt.append("\n[CONTEXT]\n");
      prompt.append(formatContextMatches(enrichedDiff));
      prompt.append("[/CONTEXT]\n");
    }

    appendExpandedFilesSection(prompt, expansionResult);
    appendPoliciesSection(prompt, policies);

    if (config.customInstructions() != null && !config.customInstructions().isBlank()) {
      prompt.append("\n[CUSTOM_INSTRUCTIONS]\n");
      prompt.append(config.customInstructions());
      prompt.append("\n[/CUSTOM_INSTRUCTIONS]\n");
    }

    if (!ticketContext.isBlank()) {
      prompt.append("\n[REVIEW_FOCUS]\n");
      prompt.append("─────────────────────────────────────────────────────────────\n");
      prompt.append("1. Verify code implements business requirements from ticket\n");
      prompt.append("2. Check code against ticket description expectations\n");
      prompt.append("3. Validate security and quality standards\n");
      prompt.append("[/REVIEW_FOCUS]\n");
    }

    return prompt.toString();
  }

  private String formatContextMatches(final EnrichedDiffAnalysisBundle enrichedDiff) {
    final var contextResult = enrichedDiff.contextResult();
    final var matches = contextResult.matches();
    final var metadata = contextResult.metadata();

    final StringBuilder context = new StringBuilder();
    context
        .append("Relevant files identified by context analysis (")
        .append(metadata.strategyName())
        .append("):\n\n");

    for (final var match : matches) {
      context
          .append("- ")
          .append(match.filePath())
          .append(" (confidence: ")
          .append(String.format(Locale.US, "%.2f", match.confidence()))
          .append(", reason: ")
          .append(match.reason().getDescription())
          .append(")\n");

      if (match.evidence() != null && !match.evidence().isBlank()) {
        context.append("  Evidence: ").append(match.evidence()).append("\n");
      }
    }

    context.append("\n");
    context
        .append("These files may provide important context for understanding the changes.\n")
        .append("Consider their relationships when reviewing the diff.\n");

    return context.toString();
  }

  private void appendExpandedFilesSection(
      final StringBuilder prompt, final DiffExpansionResult expansionResult) {
    if (expansionResult == null || !expansionResult.hasExpandedFiles()) {
      return;
    }

    prompt.append("\n[EXPANDED_FILES]\n");
    prompt.append("Full content of modified files for additional context:\n\n");

    for (final ExpandedFileContext file : expansionResult.expandedFiles()) {
      if (!file.hasContent()) {
        continue;
      }
      prompt.append("--- FILE: ").append(file.filePath());
      if (file.truncated()) {
        prompt.append(" (truncated from ").append(file.lineCount()).append(" lines)");
      }
      prompt.append(" ---\n");
      prompt.append(file.content());
      prompt.append("\n--- END FILE ---\n\n");
    }
    prompt.append("[/EXPANDED_FILES]\n");
  }

  private void appendPrMetadataSection(final StringBuilder prompt, final PrMetadata metadata) {
    if (metadata == null || metadata.title() == null) {
      return;
    }

    prompt.append("[PR_METADATA]\n");
    prompt.append("Pull Request: ").append(metadata.title()).append("\n");

    if (metadata.description() != null && !metadata.description().isBlank()) {
      prompt.append("Description: ").append(metadata.description()).append("\n");
    }

    if (metadata.author() != null) {
      prompt.append("Author: ").append(metadata.author()).append("\n");
    }

    if (metadata.baseBranch() != null && metadata.headBranch() != null) {
      prompt
          .append("Branch: ")
          .append(metadata.headBranch())
          .append(" → ")
          .append(metadata.baseBranch())
          .append("\n");
    }

    if (metadata.hasLabels()) {
      prompt.append("Labels: ").append(String.join(", ", metadata.labels())).append("\n");
    }

    if (metadata.hasCommits()) {
      prompt.append("\nRecent Commits:\n");
      for (final CommitInfo commit : metadata.commits()) {
        prompt.append("  - ").append(commit.shortId()).append(": ");
        if (commit.hasMessage()) {
          prompt.append(commit.firstLineOfMessage());
        } else {
          prompt.append("(no message)");
        }
        prompt.append("\n");
      }
    }

    prompt.append("[/PR_METADATA]\n\n");
  }

  private void appendPoliciesSection(
      final StringBuilder prompt, final RepositoryPolicies policies) {
    if (policies == null || !policies.hasPolicies()) {
      return;
    }

    prompt.append("\n[POLICIES]\n");
    prompt.append("Repository guidelines to consider during review:\n\n");

    for (final PolicyDocument policy : policies.allPolicies()) {
      if (!policy.hasContent()) {
        continue;
      }

      prompt.append("--- ").append(policy.name());
      if (policy.truncated()) {
        prompt.append(" (truncated)");
      }
      prompt.append(" ---\n");
      prompt.append(policy.content());
      prompt.append("\n--- END ---\n\n");
    }
    prompt.append("[/POLICIES]\n");
  }
}
