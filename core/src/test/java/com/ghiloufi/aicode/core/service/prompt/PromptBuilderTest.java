package com.ghiloufi.aicode.core.service.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.DiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.GitDiffDocument;
import com.ghiloufi.aicode.core.domain.model.ReviewConfiguration;
import com.ghiloufi.aicode.core.service.validation.ReviewResultSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PromptBuilder")
class PromptBuilderTest {

  private PromptBuilder promptBuilder;

  @BeforeEach
  void setUp() {
    promptBuilder = new PromptBuilder();
  }

  @Test
  @DisplayName("should_include_json_schema_in_prompt")
  void should_include_json_schema_in_prompt() {
    final GitDiffDocument gitDiff = new GitDiffDocument();
    final DiffAnalysisBundle diff = new DiffAnalysisBundle(gitDiff, "test diff");
    final ReviewConfiguration config = ReviewConfiguration.defaults();

    final String prompt = promptBuilder.buildReviewPrompt(diff, config);

    assertThat(prompt).contains(ReviewResultSchema.SCHEMA);
    assertThat(prompt).contains("JSON SCHEMA");
    assertThat(prompt).contains("RESPONSE FORMAT REQUIREMENTS");
  }

  @Test
  @DisplayName("should_instruct_llm_to_return_valid_json_only")
  void should_instruct_llm_to_return_valid_json_only() {
    final GitDiffDocument gitDiff = new GitDiffDocument();
    final DiffAnalysisBundle diff = new DiffAnalysisBundle(gitDiff, "test diff");
    final ReviewConfiguration config = ReviewConfiguration.defaults();

    final String prompt = promptBuilder.buildReviewPrompt(diff, config);

    assertThat(prompt).contains("MUST be valid JSON");
    assertThat(prompt).contains("Return ONLY the raw JSON object");
    assertThat(prompt).contains("NO markdown code blocks");
    assertThat(prompt).contains("NO additional text or explanations");
  }

  @Test
  @DisplayName("should_specify_required_severity_values")
  void should_specify_required_severity_values() {
    final GitDiffDocument gitDiff = new GitDiffDocument();
    final DiffAnalysisBundle diff = new DiffAnalysisBundle(gitDiff, "test diff");
    final ReviewConfiguration config = ReviewConfiguration.defaults();

    final String prompt = promptBuilder.buildReviewPrompt(diff, config);

    assertThat(prompt).contains("critical, major, minor, info");
  }

  @Test
  @DisplayName("should_include_diff_content_in_prompt")
  void should_include_diff_content_in_prompt() {
    final String diffContent = "--- a/Test.java\n+++ b/Test.java\n@@ -1,1 +1,1 @@";
    final GitDiffDocument gitDiff = new GitDiffDocument();
    final DiffAnalysisBundle diff = new DiffAnalysisBundle(gitDiff, diffContent);
    final ReviewConfiguration config = ReviewConfiguration.defaults();

    final String prompt = promptBuilder.buildReviewPrompt(diff, config);

    assertThat(prompt).contains("[DIFF]");
    assertThat(prompt).contains(diffContent);
    assertThat(prompt).contains("[/DIFF]");
  }

  @Test
  @DisplayName("should_include_repository_configuration_in_prompt")
  void should_include_repository_configuration_in_prompt() {
    final GitDiffDocument gitDiff = new GitDiffDocument();
    final DiffAnalysisBundle diff = new DiffAnalysisBundle(gitDiff, "test diff");
    final ReviewConfiguration config =
        new ReviewConfiguration(
            ReviewConfiguration.ReviewFocus.COMPREHENSIVE,
            ReviewConfiguration.SeverityThreshold.LOW,
            true,
            null,
            "Java",
            null,
            null);

    final String prompt = promptBuilder.buildReviewPrompt(diff, config);

    assertThat(prompt).contains("[REPO]");
    assertThat(prompt).contains("language: Java");
    assertThat(prompt).contains("focus: COMPREHENSIVE");
    assertThat(prompt).contains("[/REPO]");
  }

  @Test
  @DisplayName("should_include_custom_instructions_when_provided")
  void should_include_custom_instructions_when_provided() {
    final GitDiffDocument gitDiff = new GitDiffDocument();
    final DiffAnalysisBundle diff = new DiffAnalysisBundle(gitDiff, "test diff");
    final ReviewConfiguration config =
        new ReviewConfiguration(
            ReviewConfiguration.ReviewFocus.COMPREHENSIVE,
            ReviewConfiguration.SeverityThreshold.LOW,
            true,
            "Focus on performance",
            "Java",
            null,
            null);

    final String prompt = promptBuilder.buildReviewPrompt(diff, config);

    assertThat(prompt).contains("[CUSTOM_INSTRUCTIONS]");
    assertThat(prompt).contains("Focus on performance");
    assertThat(prompt).contains("[/CUSTOM_INSTRUCTIONS]");
  }

  @Test
  @DisplayName("should_not_include_custom_instructions_section_when_null")
  void should_not_include_custom_instructions_section_when_null() {
    final GitDiffDocument gitDiff = new GitDiffDocument();
    final DiffAnalysisBundle diff = new DiffAnalysisBundle(gitDiff, "test diff");
    final ReviewConfiguration config = ReviewConfiguration.defaults();

    final String prompt = promptBuilder.buildReviewPrompt(diff, config);

    assertThat(prompt).doesNotContain("[CUSTOM_INSTRUCTIONS]");
  }
}
