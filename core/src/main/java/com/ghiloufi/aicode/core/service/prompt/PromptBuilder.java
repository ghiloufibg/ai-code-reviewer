package com.ghiloufi.aicode.core.service.prompt;

import com.ghiloufi.aicode.core.domain.model.EnrichedDiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.ReviewConfiguration;
import com.ghiloufi.aicode.core.domain.model.TicketBusinessContext;
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

  public String buildReviewPrompt(
      final EnrichedDiffAnalysisBundle enrichedDiff,
      final ReviewConfiguration config,
      final TicketBusinessContext ticketContext) {
    if (enrichedDiff == null) {
      throw new IllegalArgumentException("EnrichedDiffAnalysisBundle cannot be null");
    }
    if (config == null) {
      throw new IllegalArgumentException("ReviewConfiguration cannot be null");
    }
    if (ticketContext == null) {
      throw new IllegalArgumentException("TicketBusinessContext cannot be null");
    }

    final String ticketContextFormatted = ticketContext.formatForPrompt();

    return buildFullPrompt(enrichedDiff, config, ticketContextFormatted);
  }

  private String buildFullPrompt(
      final EnrichedDiffAnalysisBundle enrichedDiff,
      final ReviewConfiguration config,
      final String ticketContext) {

    final String formattedDiff = diffFormatter.formatDiff(enrichedDiff.structuredDiff());

    final StringBuilder prompt = new StringBuilder();

    if (!ticketContext.isBlank()) {
      prompt.append(ticketContext);
      prompt.append("\n");
    }

    prompt.append(buildSystemPrompt()).append("\n\n");
    prompt.append("[REPO]\n");
    prompt.append("language: ").append(config.programmingLanguage()).append("\n");
    prompt.append("focus: ").append(config.focus().name()).append("\n");
    prompt.append("[/REPO]\n\n");
    prompt.append("[DIFF]\n");
    prompt.append(formattedDiff);
    prompt.append("\n[/DIFF]\n");

    if (enrichedDiff.hasContext() && enrichedDiff.getContextMatchCount() > 0) {
      prompt.append("\n[CONTEXT]\n");
      prompt.append(formatContextMatches(enrichedDiff));
      prompt.append("[/CONTEXT]\n");
    }

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
}
