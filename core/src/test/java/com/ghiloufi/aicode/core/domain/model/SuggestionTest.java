package com.ghiloufi.aicode.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class SuggestionTest {

  @Nested
  final class Creation {

    @Test
    void should_create_suggestion_with_builder() {
      final var suggestion =
          Suggestion.builder()
              .id("test-id")
              .file("Test.java")
              .startLine(10)
              .endLine(15)
              .type(Suggestion.SuggestionType.FIX)
              .title("Fix null check")
              .description("Add null check before accessing object")
              .suggestedCode("if (obj != null) { obj.method(); }")
              .rationale("Prevents NPE")
              .confidence(0.95)
              .tags(List.of("npe", "null-safety"))
              .source(Suggestion.SuggestionSource.LLM_REVIEW)
              .build();

      assertThat(suggestion.id()).isEqualTo("test-id");
      assertThat(suggestion.file()).isEqualTo("Test.java");
      assertThat(suggestion.startLine()).isEqualTo(10);
      assertThat(suggestion.endLine()).isEqualTo(15);
      assertThat(suggestion.type()).isEqualTo(Suggestion.SuggestionType.FIX);
      assertThat(suggestion.title()).isEqualTo("Fix null check");
      assertThat(suggestion.confidence()).isEqualTo(0.95);
      assertThat(suggestion.tags()).containsExactly("npe", "null-safety");
    }

    @Test
    void should_generate_id_when_not_provided() {
      final var suggestion =
          Suggestion.builder()
              .file("Test.java")
              .title("Test suggestion")
              .source(Suggestion.SuggestionSource.STATIC_ANALYSIS)
              .build();

      assertThat(suggestion.id()).isNotNull().isNotBlank();
    }

    @Test
    void should_set_end_line_to_start_line_when_less_than_start() {
      final var suggestion =
          Suggestion.builder()
              .file("Test.java")
              .startLine(10)
              .endLine(5)
              .title("Test suggestion")
              .source(Suggestion.SuggestionSource.LLM_REVIEW)
              .build();

      assertThat(suggestion.endLine()).isEqualTo(suggestion.startLine());
    }

    @Test
    void should_require_file() {
      assertThatThrownBy(
              () ->
                  Suggestion.builder()
                      .id("test-id")
                      .title("Test")
                      .source(Suggestion.SuggestionSource.LLM_REVIEW)
                      .build())
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("file");
    }

    @Test
    void should_require_title() {
      assertThatThrownBy(
              () ->
                  Suggestion.builder()
                      .id("test-id")
                      .file("Test.java")
                      .source(Suggestion.SuggestionSource.LLM_REVIEW)
                      .build())
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("title");
    }

    @Test
    void should_reject_negative_confidence() {
      assertThatThrownBy(
              () ->
                  Suggestion.builder()
                      .file("Test.java")
                      .title("Test")
                      .confidence(-0.1)
                      .source(Suggestion.SuggestionSource.LLM_REVIEW)
                      .build())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("confidence");
    }

    @Test
    void should_reject_confidence_greater_than_one() {
      assertThatThrownBy(
              () ->
                  Suggestion.builder()
                      .file("Test.java")
                      .title("Test")
                      .confidence(1.1)
                      .source(Suggestion.SuggestionSource.LLM_REVIEW)
                      .build())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("confidence");
    }

    @Test
    void should_reject_negative_start_line() {
      assertThatThrownBy(
              () ->
                  Suggestion.builder()
                      .file("Test.java")
                      .title("Test")
                      .startLine(-1)
                      .source(Suggestion.SuggestionSource.LLM_REVIEW)
                      .build())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("startLine");
    }
  }

  @Nested
  final class FromIssue {

    @Test
    void should_create_suggestion_from_issue() {
      final var issue =
          ReviewResult.Issue.issueBuilder()
              .file("Test.java")
              .startLine(10)
              .severity("error")
              .title("Potential NPE")
              .suggestion("Add null check")
              .confidenceScore(0.9)
              .confidenceExplanation("High probability of null")
              .build();

      final var suggestion = Suggestion.fromIssue(issue, "if (obj != null) { }");

      assertThat(suggestion.file()).isEqualTo("Test.java");
      assertThat(suggestion.startLine()).isEqualTo(10);
      assertThat(suggestion.type()).isEqualTo(Suggestion.SuggestionType.FIX);
      assertThat(suggestion.title()).isEqualTo("Potential NPE");
      assertThat(suggestion.suggestedCode()).isEqualTo("if (obj != null) { }");
      assertThat(suggestion.confidence()).isEqualTo(0.9);
      assertThat(suggestion.source()).isEqualTo(Suggestion.SuggestionSource.LLM_REVIEW);
    }

    @Test
    void should_use_default_confidence_when_issue_has_none() {
      final var issue =
          ReviewResult.Issue.issueBuilder()
              .file("Test.java")
              .startLine(10)
              .severity("warning")
              .title("Style issue")
              .suggestion("Consider refactoring")
              .build();

      final var suggestion = Suggestion.fromIssue(issue, null);

      assertThat(suggestion.confidence()).isEqualTo(0.8);
    }
  }

  @Nested
  final class QueryMethods {

    @Test
    void should_detect_code_change() {
      final var withCode =
          Suggestion.builder()
              .file("Test.java")
              .title("Test")
              .suggestedCode("new code here")
              .source(Suggestion.SuggestionSource.LLM_REVIEW)
              .build();

      final var withoutCode =
          Suggestion.builder()
              .file("Test.java")
              .title("Test")
              .source(Suggestion.SuggestionSource.LLM_REVIEW)
              .build();

      assertThat(withCode.hasCodeChange()).isTrue();
      assertThat(withoutCode.hasCodeChange()).isFalse();
    }

    @Test
    void should_detect_high_confidence() {
      final var highConfidence =
          Suggestion.builder()
              .file("Test.java")
              .title("Test")
              .confidence(0.9)
              .source(Suggestion.SuggestionSource.LLM_REVIEW)
              .build();

      final var lowConfidence =
          Suggestion.builder()
              .file("Test.java")
              .title("Test")
              .confidence(0.5)
              .source(Suggestion.SuggestionSource.LLM_REVIEW)
              .build();

      assertThat(highConfidence.isHighConfidence()).isTrue();
      assertThat(lowConfidence.isHighConfidence()).isFalse();
    }

    @Test
    void should_check_line_affects() {
      final var suggestion =
          Suggestion.builder()
              .file("Test.java")
              .title("Test")
              .startLine(10)
              .endLine(15)
              .source(Suggestion.SuggestionSource.LLM_REVIEW)
              .build();

      assertThat(suggestion.affectsLine(10)).isTrue();
      assertThat(suggestion.affectsLine(12)).isTrue();
      assertThat(suggestion.affectsLine(15)).isTrue();
      assertThat(suggestion.affectsLine(9)).isFalse();
      assertThat(suggestion.affectsLine(16)).isFalse();
    }

    @Test
    void should_calculate_line_count() {
      final var single =
          Suggestion.builder()
              .file("Test.java")
              .title("Test")
              .startLine(10)
              .endLine(10)
              .source(Suggestion.SuggestionSource.LLM_REVIEW)
              .build();

      final var multi =
          Suggestion.builder()
              .file("Test.java")
              .title("Test")
              .startLine(10)
              .endLine(15)
              .source(Suggestion.SuggestionSource.LLM_REVIEW)
              .build();

      assertThat(single.lineCount()).isEqualTo(1);
      assertThat(multi.lineCount()).isEqualTo(6);
    }
  }

  @Nested
  final class SeverityToTypeMapping {

    @Test
    void should_map_critical_to_fix() {
      final var issue =
          ReviewResult.Issue.issueBuilder()
              .file("Test.java")
              .startLine(1)
              .severity("critical")
              .title("Critical issue")
              .suggestion("Fix now")
              .build();

      final var suggestion = Suggestion.fromIssue(issue, null);

      assertThat(suggestion.type()).isEqualTo(Suggestion.SuggestionType.FIX);
    }

    @Test
    void should_map_warning_to_refactor() {
      final var issue =
          ReviewResult.Issue.issueBuilder()
              .file("Test.java")
              .startLine(1)
              .severity("warning")
              .title("Warning")
              .suggestion("Consider this")
              .build();

      final var suggestion = Suggestion.fromIssue(issue, null);

      assertThat(suggestion.type()).isEqualTo(Suggestion.SuggestionType.REFACTOR);
    }

    @Test
    void should_map_info_to_improvement() {
      final var issue =
          ReviewResult.Issue.issueBuilder()
              .file("Test.java")
              .startLine(1)
              .severity("info")
              .title("Info")
              .suggestion("Maybe do this")
              .build();

      final var suggestion = Suggestion.fromIssue(issue, null);

      assertThat(suggestion.type()).isEqualTo(Suggestion.SuggestionType.IMPROVEMENT);
    }

    @Test
    void should_map_low_to_optimization() {
      final var issue =
          ReviewResult.Issue.issueBuilder()
              .file("Test.java")
              .startLine(1)
              .severity("low")
              .title("Low priority")
              .suggestion("Optional")
              .build();

      final var suggestion = Suggestion.fromIssue(issue, null);

      assertThat(suggestion.type()).isEqualTo(Suggestion.SuggestionType.OPTIMIZATION);
    }
  }
}
