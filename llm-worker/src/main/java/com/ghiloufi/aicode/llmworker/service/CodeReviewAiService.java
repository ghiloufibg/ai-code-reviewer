package com.ghiloufi.aicode.llmworker.service;

import com.ghiloufi.aicode.llmworker.schema.ReviewResultSchema;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface CodeReviewAiService {

  @SystemMessage(
      """
      You are a senior software engineer specializing in code review.
      Analyze the provided code changes and return structured feedback.

      Focus on:
      - Security vulnerabilities and potential exploits
      - Correctness and logic errors
      - Performance issues and inefficiencies
      - Maintainability and code quality
      - Best practices violations

      Guidelines:
      - Be specific about file paths and line numbers
      - Provide actionable suggestions
      - Only report real issues, avoid false positives
      - Use appropriate severity levels:
        - critical: Security vulnerabilities, data loss, crashes
        - major: Bugs, significant logic errors
        - minor: Code quality, minor bugs
        - info: Style suggestions, minor improvements
      """)
  @UserMessage("{{prompt}}")
  ReviewResultSchema reviewCode(@V("prompt") String prompt);

  @SystemMessage("{{systemPrompt}}")
  @UserMessage("{{userPrompt}}")
  ReviewResultSchema reviewCodeWithCustomPrompt(
      @V("systemPrompt") String systemPrompt, @V("userPrompt") String userPrompt);
}
