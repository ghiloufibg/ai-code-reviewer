package com.ghiloufi.aicode.core.service.prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ghiloufi.aicode.core.config.FeaturesConfiguration;
import com.ghiloufi.aicode.core.config.PromptPropertiesFactory;
import com.ghiloufi.aicode.core.domain.model.ContextMatch;
import com.ghiloufi.aicode.core.domain.model.ContextRetrievalMetadata;
import com.ghiloufi.aicode.core.domain.model.ContextRetrievalResult;
import com.ghiloufi.aicode.core.domain.model.DiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.DiffHunkBlock;
import com.ghiloufi.aicode.core.domain.model.EnrichedDiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.GitDiffDocument;
import com.ghiloufi.aicode.core.domain.model.GitFileModification;
import com.ghiloufi.aicode.core.domain.model.MatchReason;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.model.ReviewConfiguration;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import com.ghiloufi.aicode.core.domain.model.TicketBusinessContext;
import com.ghiloufi.aicode.core.domain.service.DiffFormatter;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@DisplayName("PromptBuilder Tests")
@SpringBootTest(
    classes = {
      FeaturesConfiguration.class,
      PromptTemplateService.class,
      PromptPropertiesFactory.class
    })
class PromptBuilderTest {

  private PromptBuilder promptBuilder;
  private DiffFormatter diffFormatter;
  @Autowired private PromptTemplateService promptTemplateService;
  private RepositoryIdentifier testRepo;

  @BeforeEach
  void setUp() {
    diffFormatter = new DiffFormatter();
    promptBuilder = new PromptBuilder(diffFormatter, promptTemplateService);
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

    final DiffAnalysisBundle bundle = new DiffAnalysisBundle(testRepo, diff, rawDiff, null, null);
    final EnrichedDiffAnalysisBundle enrichedBundle = new EnrichedDiffAnalysisBundle(bundle);
    final ReviewConfiguration config = ReviewConfiguration.defaults();

    final String prompt =
        promptBuilder.buildReviewPrompt(enrichedBundle, config, TicketBusinessContext.empty());

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
    final DiffAnalysisBundle bundle =
        new DiffAnalysisBundle(testRepo, diff, "raw diff text", null, null);
    final EnrichedDiffAnalysisBundle enrichedBundle = new EnrichedDiffAnalysisBundle(bundle);
    final ReviewConfiguration config = ReviewConfiguration.defaults();

    final String prompt =
        promptBuilder.buildReviewPrompt(enrichedBundle, config, TicketBusinessContext.empty());

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
    final DiffAnalysisBundle bundle = new DiffAnalysisBundle(testRepo, diff, "raw", null, null);
    final EnrichedDiffAnalysisBundle enrichedBundle = new EnrichedDiffAnalysisBundle(bundle);
    final ReviewConfiguration config = ReviewConfiguration.defaults();

    final String prompt =
        promptBuilder.buildReviewPrompt(enrichedBundle, config, TicketBusinessContext.empty());

    assertThat(prompt).containsAnyOf("code review assistant", "Senior software engineer");
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

    assertThatThrownBy(() -> promptBuilder.buildReviewPrompt(null, config, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("EnrichedDiffAnalysisBundle cannot be null");
  }

  @Test
  @DisplayName("should_throw_exception_when_config_is_null")
  void should_throw_exception_when_config_is_null() {
    final GitFileModification file = new GitFileModification("Test.java", "Test.java");
    final DiffHunkBlock hunk = new DiffHunkBlock(1, 1, 1, 1);
    hunk.lines = List.of(" test");
    file.diffHunkBlocks = List.of(hunk);

    final GitDiffDocument diff = new GitDiffDocument(List.of(file));
    final DiffAnalysisBundle bundle = new DiffAnalysisBundle(testRepo, diff, "raw", null, null);
    final EnrichedDiffAnalysisBundle enrichedBundle = new EnrichedDiffAnalysisBundle(bundle);

    assertThatThrownBy(() -> promptBuilder.buildReviewPrompt(enrichedBundle, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ReviewConfiguration cannot be null");
  }

  @Nested
  @DisplayName("Context Formatting")
  final class ContextFormatting {

    @Test
    @DisplayName("should_not_include_context_section_when_no_context_available")
    final void should_not_include_context_section_when_no_context_available() {
      final GitFileModification file = new GitFileModification("Test.java", "Test.java");
      final DiffHunkBlock hunk = new DiffHunkBlock(1, 1, 1, 1);
      hunk.lines = List.of(" test");
      file.diffHunkBlocks = List.of(hunk);

      final GitDiffDocument diff = new GitDiffDocument(List.of(file));
      final DiffAnalysisBundle bundle = new DiffAnalysisBundle(testRepo, diff, "raw", null, null);
      final EnrichedDiffAnalysisBundle enrichedBundle = new EnrichedDiffAnalysisBundle(bundle);
      final ReviewConfiguration config = ReviewConfiguration.defaults();

      final String prompt =
          promptBuilder.buildReviewPrompt(enrichedBundle, config, TicketBusinessContext.empty());

      assertThat(prompt).doesNotContain("[CONTEXT]");
      assertThat(prompt).doesNotContain("[/CONTEXT]");
      assertThat(prompt).doesNotContain("Relevant files");
    }

    @Test
    @DisplayName("should_include_formatted_context_matches_with_confidence_and_reason")
    final void should_include_formatted_context_matches_with_confidence_and_reason() {
      final GitFileModification file = new GitFileModification("Test.java", "Test.java");
      final DiffHunkBlock hunk = new DiffHunkBlock(1, 1, 1, 1);
      hunk.lines = List.of(" test");
      file.diffHunkBlocks = List.of(hunk);

      final GitDiffDocument diff = new GitDiffDocument(List.of(file));
      final DiffAnalysisBundle bundle = new DiffAnalysisBundle(testRepo, diff, "raw", null, null);

      final List<ContextMatch> matches =
          List.of(
              new ContextMatch(
                  "src/UserService.java", MatchReason.DIRECT_IMPORT, 0.95, "imported in Test.java"),
              new ContextMatch(
                  "src/UserRepository.java", MatchReason.TYPE_REFERENCE, 0.85, "used as type"));

      final ContextRetrievalMetadata metadata =
          new ContextRetrievalMetadata("metadata-based", Duration.ofMillis(100), 2, 2, Map.of());

      final ContextRetrievalResult contextResult = new ContextRetrievalResult(matches, metadata);

      final EnrichedDiffAnalysisBundle enrichedBundle =
          new EnrichedDiffAnalysisBundle(bundle).withContext(contextResult);
      final ReviewConfiguration config = ReviewConfiguration.defaults();

      final String prompt =
          promptBuilder.buildReviewPrompt(enrichedBundle, config, TicketBusinessContext.empty());

      assertThat(prompt).contains("[CONTEXT]");
      assertThat(prompt).contains("[/CONTEXT]");
      assertThat(prompt).contains("Relevant files identified by context analysis");
      assertThat(prompt).contains("metadata-based");
      assertThat(prompt).contains("src/UserService.java");
      assertThat(prompt).contains("confidence: 0.95");
      assertThat(prompt).contains("reason: Direct import");
      assertThat(prompt).contains("Evidence: imported in Test.java");
      assertThat(prompt).contains("src/UserRepository.java");
      assertThat(prompt).contains("confidence: 0.85");
      assertThat(prompt).contains("reason: Type reference");
    }

    @Test
    @DisplayName("should_format_multiple_context_matches_sorted_by_confidence")
    final void should_format_multiple_context_matches_sorted_by_confidence() {
      final GitFileModification file = new GitFileModification("Test.java", "Test.java");
      final DiffHunkBlock hunk = new DiffHunkBlock(1, 1, 1, 1);
      hunk.lines = List.of(" test");
      file.diffHunkBlocks = List.of(hunk);

      final GitDiffDocument diff = new GitDiffDocument(List.of(file));
      final DiffAnalysisBundle bundle = new DiffAnalysisBundle(testRepo, diff, "raw", null, null);

      final List<ContextMatch> matches =
          List.of(
              new ContextMatch(
                  "FileA.java", MatchReason.GIT_COCHANGE_HIGH, 0.9, "frequently changed together"),
              new ContextMatch("FileB.java", MatchReason.SIBLING_FILE, 0.7, "same package"),
              new ContextMatch("FileC.java", MatchReason.DIRECT_IMPORT, 0.95, "imported in Test"));

      final ContextRetrievalMetadata metadata =
          new ContextRetrievalMetadata(
              "metadata-based+git-history", Duration.ofMillis(200), 3, 3, Map.of());

      final ContextRetrievalResult contextResult = new ContextRetrievalResult(matches, metadata);
      final EnrichedDiffAnalysisBundle enrichedBundle =
          new EnrichedDiffAnalysisBundle(bundle).withContext(contextResult);
      final ReviewConfiguration config = ReviewConfiguration.defaults();

      final String prompt =
          promptBuilder.buildReviewPrompt(enrichedBundle, config, TicketBusinessContext.empty());

      assertThat(prompt).contains("metadata-based+git-history");
      assertThat(prompt).contains("FileA.java");
      assertThat(prompt).contains("FileB.java");
      assertThat(prompt).contains("FileC.java");
      assertThat(prompt).contains("These files may provide important context");
    }
  }
}
