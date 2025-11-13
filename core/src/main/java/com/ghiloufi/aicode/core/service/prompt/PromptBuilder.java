package com.ghiloufi.aicode.core.service.prompt;

import com.ghiloufi.aicode.core.domain.model.DiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.ReviewConfiguration;
import com.ghiloufi.aicode.core.domain.service.DiffFormatter;
import com.ghiloufi.aicode.core.service.validation.ReviewResultSchema;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

  private final DiffFormatter diffFormatter;

  public PromptBuilder(final DiffFormatter diffFormatter) {
    this.diffFormatter = Objects.requireNonNull(diffFormatter, "DiffFormatter cannot be null");
  }

  private static final String SYSTEM_PROMPT =
      """
            You are a rigorous code review assistant specialized in Java.
            Review ONLY the changes in this Pull Request.

            CRITICAL: Your response MUST be valid JSON matching the schema below. Do NOT include markdown formatting, code blocks, or explanations.
            Return ONLY the raw JSON object.

            Principles:
            - Comment only on changed lines and ±5 lines of context.
            - Prefer minimal, safe patches; avoid restyling unless it prevents defects.
            - If uncertain, use severity=info and explain briefly.
            - Classify findings: critical, major, minor, info.
            - Categories: correctness, security, concurrency, performance, maintainability, test gaps.

            JSON SCHEMA (strictly follow this structure):
            """
          + ReviewResultSchema.SCHEMA
          + """

            STRICT RESPONSE FORMAT REQUIREMENTS:
            1. Return ONLY valid JSON matching the schema above
            2. Include ONLY these fields: summary, issues, non_blocking_notes
            3. DO NOT add extra fields like $schema, type, version, metadata, etc.
            4. NO markdown code blocks (```json or ```)
            5. NO additional text or explanations outside the JSON
            6. Ensure all required fields are present
            7. Use only allowed severity values: critical, major, minor, info
            8. Line numbers must be positive integers (minimum 1)
            9. The response must start with { and end with }
            10. Do not include any preamble or postamble text

            EXAMPLE VALID RESPONSE (structure only):
            {
              "summary": "Overall review summary here",
              "issues": [...],
              "non_blocking_notes": [...]
            }
            """;

  public String buildReviewPrompt(final DiffAnalysisBundle diff, final ReviewConfiguration config) {
    if (diff == null) {
      throw new IllegalArgumentException("DiffAnalysisBundle cannot be null");
    }
    if (config == null) {
      throw new IllegalArgumentException("ReviewConfiguration cannot be null");
    }

    final String formattedDiff = diffFormatter.formatDiff(diff.structuredDiff());

    final StringBuilder prompt = new StringBuilder();
    prompt.append(SYSTEM_PROMPT).append("\n\n");
    prompt.append("[REPO]\n");
    prompt.append("language: ").append(config.programmingLanguage()).append("\n");
    prompt.append("focus: ").append(config.focus().name()).append("\n");
    prompt.append("[/REPO]\n\n");
    prompt.append("[DIFF]\n");
    prompt.append(formattedDiff);
    prompt.append("\n[/DIFF]\n");

    if (config.customInstructions() != null && !config.customInstructions().isBlank()) {
      prompt.append("\n[CUSTOM_INSTRUCTIONS]\n");
      prompt.append(config.customInstructions());
      prompt.append("\n[/CUSTOM_INSTRUCTIONS]\n");
    }

    return prompt.toString();
  }
}
