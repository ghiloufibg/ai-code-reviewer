package com.ghiloufi.aicode.core.service.prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ghiloufi.aicode.core.domain.model.DiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.DiffHunkBlock;
import com.ghiloufi.aicode.core.domain.model.GitDiffDocument;
import com.ghiloufi.aicode.core.domain.model.GitFileModification;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.model.ReviewConfiguration;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import com.ghiloufi.aicode.core.domain.service.DiffFormatter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PromptBuilder Tests")
class PromptBuilderTest {

  private PromptBuilder promptBuilder;
  private DiffFormatter diffFormatter;
  private RepositoryIdentifier testRepo;

  @BeforeEach
  void setUp() {
    diffFormatter = new DiffFormatter();
    promptBuilder = new PromptBuilder(diffFormatter);
    testRepo = RepositoryIdentifier.create(SourceProvider.GITLAB, "test/repo");
  }

  @Test
  @DisplayName("should_use_enriched_diff_instead_of_raw_diff")
  void should_use_enriched_diff_instead_of_raw_diff() {
    final GitFileModification file = new GitFileModification("src/Test.java", "src/Test.java");
    final DiffHunkBlock hunk = new DiffHunkBlock(10, 2, 10, 3);
    hunk.lines = List.of(" context", "+added line", " context");
    file.diffHunkBlocks = List.of(hunk);

    final GitDiffDocument diff = new GitDiffDocument(List.of(file));
    final String rawDiff = "--- a/src/Test.java\n+++ b/src/Test.java\n@@ -10,2 +10,3 @@";

    final DiffAnalysisBundle bundle = new DiffAnalysisBundle(testRepo, diff, rawDiff);
    final ReviewConfiguration config = ReviewConfiguration.defaults();

    final String prompt = promptBuilder.buildReviewPrompt(bundle, config);

    assertThat(prompt).contains("FILE: src/Test.java");
    assertThat(prompt).contains("10   │   context");
    assertThat(prompt).contains("11   │ + added line");
    assertThat(prompt).doesNotContain("--- a/src/Test.java");
    assertThat(prompt).doesNotContain("+++ b/src/Test.java");
  }

  @Test
  @DisplayName("should_include_enriched_diff_with_explicit_line_numbers")
  void should_include_enriched_diff_with_explicit_line_numbers() {
    final GitFileModification file = new GitFileModification("Example.java", "Example.java");
    final DiffHunkBlock hunk = new DiffHunkBlock(50, 3, 50, 4);
    hunk.lines = List.of(" line 50", "+added line 51", " line 52", "+added line 53");
    file.diffHunkBlocks = List.of(hunk);

    final GitDiffDocument diff = new GitDiffDocument(List.of(file));
    final DiffAnalysisBundle bundle = new DiffAnalysisBundle(testRepo, diff, "raw diff text");
    final ReviewConfiguration config = ReviewConfiguration.defaults();

    final String prompt = promptBuilder.buildReviewPrompt(bundle, config);

    assertThat(prompt).contains("50   │   line 50");
    assertThat(prompt).contains("51   │ + added line 51");
    assertThat(prompt).contains("52   │   line 52");
    assertThat(prompt).contains("53   │ + added line 53");
  }

  @Test
  @DisplayName("should_include_system_prompt_and_configuration")
  void should_include_system_prompt_and_configuration() {
    final GitFileModification file = new GitFileModification("Test.java", "Test.java");
    final DiffHunkBlock hunk = new DiffHunkBlock(1, 1, 1, 1);
    hunk.lines = List.of(" test");
    file.diffHunkBlocks = List.of(hunk);

    final GitDiffDocument diff = new GitDiffDocument(List.of(file));
    final DiffAnalysisBundle bundle = new DiffAnalysisBundle(testRepo, diff, "raw");
    final ReviewConfiguration config = ReviewConfiguration.defaults();

    final String prompt = promptBuilder.buildReviewPrompt(bundle, config);

    assertThat(prompt).contains("code review assistant");
    assertThat(prompt).contains("[REPO]");
    assertThat(prompt).contains("language: Java");
    assertThat(prompt).contains("focus: COMPREHENSIVE");
    assertThat(prompt).contains("[/REPO]");
    assertThat(prompt).contains("[DIFF]");
    assertThat(prompt).contains("[/DIFF]");
  }

  @Test
  @DisplayName("should_throw_exception_when_diff_bundle_is_null")
  void should_throw_exception_when_diff_bundle_is_null() {
    final ReviewConfiguration config = ReviewConfiguration.defaults();

    assertThatThrownBy(() -> promptBuilder.buildReviewPrompt(null, config))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("DiffAnalysisBundle cannot be null");
  }

  @Test
  @DisplayName("should_throw_exception_when_config_is_null")
  void should_throw_exception_when_config_is_null() {
    final GitFileModification file = new GitFileModification("Test.java", "Test.java");
    final DiffHunkBlock hunk = new DiffHunkBlock(1, 1, 1, 1);
    hunk.lines = List.of(" test");
    file.diffHunkBlocks = List.of(hunk);

    final GitDiffDocument diff = new GitDiffDocument(List.of(file));
    final DiffAnalysisBundle bundle = new DiffAnalysisBundle(testRepo, diff, "raw");

    assertThatThrownBy(() -> promptBuilder.buildReviewPrompt(bundle, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ReviewConfiguration cannot be null");
  }
}
