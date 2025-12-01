package com.ghiloufi.aicode.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EnrichedDiffAnalysisBundleTest {

  private RepositoryIdentifier testRepo;

  @BeforeEach
  void setUp() {
    testRepo = RepositoryIdentifier.create(SourceProvider.GITLAB, "test/repo");
  }

  private GitDiffDocument createTestDiff() {
    return new GitDiffDocument(List.of());
  }

  private ContextRetrievalResult createTestContext() {
    final List<ContextMatch> matches =
        List.of(new ContextMatch("File1.java", MatchReason.DIRECT_IMPORT, 0.95, "evidence"));

    final ContextRetrievalMetadata metadata =
        new ContextRetrievalMetadata("metadata", Duration.ofMillis(200), 1, 1, Map.of());

    return new ContextRetrievalResult(matches, metadata);
  }

  @Test
  void should_create_enriched_bundle_from_basic_bundle() {
    final DiffAnalysisBundle basicBundle =
        new DiffAnalysisBundle(testRepo, createTestDiff(), "diff content", null);

    final EnrichedDiffAnalysisBundle enriched = new EnrichedDiffAnalysisBundle(basicBundle);

    assertThat(enriched.structuredDiff()).isEqualTo(basicBundle.structuredDiff());
    assertThat(enriched.rawDiffText()).isEqualTo(basicBundle.rawDiffText());
    assertThat(enriched.contextResult().isEmpty()).isTrue();
    assertThat(enriched.hasContext()).isFalse();
  }

  @Test
  void should_create_enriched_bundle_with_context() {
    final GitDiffDocument diff = createTestDiff();
    final ContextRetrievalResult context = createTestContext();

    final EnrichedDiffAnalysisBundle enriched =
        new EnrichedDiffAnalysisBundle(testRepo, diff, "diff content", context, null);

    assertThat(enriched.hasContext()).isTrue();
    assertThat(enriched.contextResult()).isEqualTo(context);
  }

  @Test
  void should_add_context_to_enriched_bundle() {
    final DiffAnalysisBundle basicBundle =
        new DiffAnalysisBundle(testRepo, createTestDiff(), "diff content", null);
    final ContextRetrievalResult context = createTestContext();

    final EnrichedDiffAnalysisBundle enriched =
        new EnrichedDiffAnalysisBundle(basicBundle).withContext(context);

    assertThat(enriched.hasContext()).isTrue();
    assertThat(enriched.contextResult()).isEqualTo(context);
    assertThat(enriched.getContextMatchCount()).isEqualTo(1);
  }

  @Test
  void should_convert_to_basic_bundle() {
    final DiffAnalysisBundle basicBundle =
        new DiffAnalysisBundle(testRepo, createTestDiff(), "diff content", null);
    final EnrichedDiffAnalysisBundle enriched = new EnrichedDiffAnalysisBundle(basicBundle);

    final DiffAnalysisBundle converted = enriched.toBasicBundle();

    assertThat(converted.structuredDiff()).isEqualTo(basicBundle.structuredDiff());
    assertThat(converted.rawDiffText()).isEqualTo(basicBundle.rawDiffText());
  }

  @Test
  void should_return_zero_context_count_when_no_context() {
    final EnrichedDiffAnalysisBundle enriched =
        new EnrichedDiffAnalysisBundle(
            testRepo, createTestDiff(), "diff content", ContextRetrievalResult.empty(), null);

    assertThat(enriched.getContextMatchCount()).isZero();
  }

  @Test
  void should_return_context_count_when_context_present() {
    final ContextRetrievalResult context = createTestContext();
    final EnrichedDiffAnalysisBundle enriched =
        new EnrichedDiffAnalysisBundle(testRepo, createTestDiff(), "diff content", context, null);

    assertThat(enriched.getContextMatchCount()).isEqualTo(1);
  }

  @Test
  void should_generate_summary_without_context() {
    final EnrichedDiffAnalysisBundle enriched =
        new EnrichedDiffAnalysisBundle(
            testRepo, createTestDiff(), "diff content", ContextRetrievalResult.empty(), null);

    final String summary = enriched.getSummary();

    assertThat(summary).contains("0 file(s) modified");
    assertThat(summary).doesNotContain("context file(s)");
  }

  @Test
  void should_generate_summary_with_context() {
    final ContextRetrievalResult context = createTestContext();
    final EnrichedDiffAnalysisBundle enriched =
        new EnrichedDiffAnalysisBundle(testRepo, createTestDiff(), "diff content", context, null);

    final String summary = enriched.getSummary();

    assertThat(summary).contains("0 file(s) modified");
    assertThat(summary).contains("1 context file(s)");
  }

  @Test
  void should_throw_when_repository_identifier_is_null() {
    assertThatThrownBy(
            () ->
                new EnrichedDiffAnalysisBundle(
                    null, createTestDiff(), "diff content", ContextRetrievalResult.empty(), null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Repository identifier cannot be null");
  }

  @Test
  void should_throw_when_structured_diff_is_null() {
    assertThatThrownBy(
            () ->
                new EnrichedDiffAnalysisBundle(
                    testRepo, null, "diff content", ContextRetrievalResult.empty(), null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Structured diff cannot be null");
  }

  @Test
  void should_throw_when_raw_diff_text_is_null() {
    assertThatThrownBy(
            () ->
                new EnrichedDiffAnalysisBundle(
                    testRepo, createTestDiff(), null, ContextRetrievalResult.empty(), null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Raw diff text cannot be null");
  }

  @Test
  void should_throw_when_raw_diff_text_is_empty() {
    assertThatThrownBy(
            () ->
                new EnrichedDiffAnalysisBundle(
                    testRepo, createTestDiff(), "", ContextRetrievalResult.empty(), null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Raw diff text cannot be empty");
  }

  @Test
  void should_throw_when_context_result_is_null() {
    assertThatThrownBy(
            () ->
                new EnrichedDiffAnalysisBundle(
                    testRepo, createTestDiff(), "diff content", null, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Context result cannot be null");
  }

  @Test
  void should_throw_when_adding_null_context() {
    final EnrichedDiffAnalysisBundle enriched =
        new EnrichedDiffAnalysisBundle(
            testRepo, createTestDiff(), "diff content", ContextRetrievalResult.empty(), null);

    assertThatThrownBy(() -> enriched.withContext(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Context cannot be null");
  }
}
