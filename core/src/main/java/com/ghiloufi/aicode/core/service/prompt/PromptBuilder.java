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

  private static final String FIX_GENERATION_INSTRUCTIONS =
      """

            AUTOMATED FIX GENERATION (CRITICAL):
            For HIGH-CONFIDENCE issues (score >= 0.7) where the fix is clear and safe, you MUST provide:

            1. suggestedFix: The corrected code snippet that replaces the problematic section
               - CRITICAL: Must be valid JSON - all special characters MUST be escaped
               - Newlines MUST be escaped as \\n (not literal line breaks)
               - Double quotes MUST be escaped as \\"
               - Backslashes MUST be escaped as \\\\
               - Tabs MUST be escaped as \\t
               - Must be complete, compilable code
               - Include necessary imports if adding new dependencies
               - Preserve indentation and formatting style from original code
               - Should be the minimal change needed to fix the issue

            2. fixDiff: A valid unified diff format that can be applied programmatically
               - CRITICAL: Must be valid JSON - all special characters MUST be escaped
               - Newlines MUST be escaped as \\n (not literal line breaks)
               - Use standard unified diff format (--- original, +++ modified, @@ line numbers @@)
               - Show exact line changes with context
               - Must be applicable to the source file without conflicts
               - Include 3 lines of context before and after changes

            WHEN TO PROVIDE AUTOMATED FIXES:
            ✅ PROVIDE fixes for:
            - Clear security vulnerabilities (SQL injection, XSS) with obvious safe alternatives
            - Missing null checks with clear fix locations
            - Resource leaks (unclosed streams, connections) with clear try-with-resources fix
            - Incorrect API usage with documented correct usage
            - Simple logic errors with unambiguous corrections

            ❌ DO NOT provide fixes for:
            - Architectural or design issues requiring broader refactoring
            - Issues where multiple valid approaches exist
            - Changes requiring understanding of broader business logic
            - Stylistic preferences without functional impact
            - Complex performance optimizations requiring profiling

            JSON ESCAPING EXAMPLES (CRITICAL - FOLLOW THESE EXACTLY):

            ✅ CORRECT suggestedFix (properly escaped):
            "suggestedFix": "if (user != null) {\\n  user.getName();\\n}"

            ❌ WRONG suggestedFix (literal newlines - DO NOT DO THIS):
            "suggestedFix": "if (user != null) {
              user.getName();
            }"

            ✅ CORRECT fixDiff (properly escaped):
            "fixDiff": "--- a/User.java\\n+++ b/User.java\\n@@ -10,1 +10,3 @@\\n-user.getName();\\n+if (user != null) {\\n+  user.getName();\\n+}"

            COMPLETE EXAMPLE:
            "suggestedFix": "String query = \\"SELECT * FROM users WHERE id = ?\\";\\nPreparedStatement pstmt = conn.prepareStatement(query);\\npstmt.setInt(1, userId);"

            """;

  private static final String CONFIDENCE_SCORING_INSTRUCTIONS =
      """

            CONFIDENCE SCORING GUIDELINES:
            For each issue identified, you MUST provide a confidence score from 0.0 to 1.0 and a brief explanation.

            Consider these factors when assigning confidence:

            1. Pattern Clarity (How well-established is this issue pattern?)
               - High confidence (0.8-1.0): Clear security vulnerabilities (SQL injection, XSS, buffer overflow),
                 well-known anti-patterns, or violations of language best practices with strong consensus
               - Medium confidence (0.5-0.7): Code smells, potential performance issues, maintainability concerns
                 that depend on broader context, or patterns that are problematic in most but not all cases
               - Low confidence (0.0-0.4): Stylistic preferences, subjective design choices, or issues that are
                 highly context-dependent and might be intentional

            2. Context Completeness (Do you have enough information to be certain?)
               - Full context: Complete function/method visible with clear intent and surrounding code
               - Partial context: Some parts of the logic visible but missing critical dependencies or state
               - Limited context: Only fragments visible, missing key context that could change the assessment

            3. False Positive Risk (Could this be intentional or acceptable?)
               - Low risk: Unambiguous bug or security flaw with no reasonable justification
               - Medium risk: Questionable practice that might be valid in specific scenarios or frameworks
               - High risk: Pattern that looks problematic but could be framework-specific, performance-optimized,
                 or intentionally designed for a specific use case you cannot see

            CONFIDENCE SCALE:
            - 0.9-1.0: Definitive issue (e.g., "SQL injection via string concatenation", "Null pointer dereference")
            - 0.7-0.8: High confidence (e.g., "Resource leak - InputStream not closed", "Thread-safety violation")
            - 0.5-0.6: Moderate confidence (e.g., "Possible N+1 query", "Code duplication suggests refactoring")
            - 0.3-0.4: Low confidence (e.g., "Consider using Optional", "Method could be extracted")
            - 0.0-0.2: Speculative (avoid reporting issues this low unless specifically requested)

            CONFIDENCE EXPLANATION:
            Provide a 1-2 sentence explanation of why you assigned this confidence score. Reference specific factors:
            - "Clear SQL injection pattern with user input directly concatenated into query"
            - "Well-established anti-pattern, but incomplete context about exception handling strategy"
            - "Stylistic preference for builder pattern; existing approach is valid but less maintainable"

            IMPORTANT: Only report issues with confidence >= 0.5 unless specifically instructed otherwise.
            If you identify something but confidence is below 0.5, do not include it in the issues array.
            """;

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
            """
          + FIX_GENERATION_INSTRUCTIONS
          + CONFIDENCE_SCORING_INSTRUCTIONS
          + """

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
