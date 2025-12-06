package com.ghiloufi.aicode.llmworker.schema;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ReviewResultSchema Tests")
final class ReviewResultSchemaTest {

  @Nested
  @DisplayName("Construction")
  final class Construction {

    @Test
    @DisplayName("should_create_schema_with_all_fields")
    final void should_create_schema_with_all_fields() {
      final IssueSchema issue =
          new IssueSchema(
              "src/Main.java",
              10,
              Severity.major,
              "Null check missing",
              "Add null check",
              0.9,
              "High confidence",
              null);
      final NoteSchema note = new NoteSchema("src/Main.java", 20, "Consider using Optional");

      final ReviewResultSchema schema =
          new ReviewResultSchema("Good code changes", List.of(issue), List.of(note));

      assertThat(schema.summary()).isEqualTo("Good code changes");
      assertThat(schema.issues()).hasSize(1);
      assertThat(schema.nonBlockingNotes()).hasSize(1);
    }

    @Test
    @DisplayName("should_create_schema_with_empty_lists")
    final void should_create_schema_with_empty_lists() {
      final ReviewResultSchema schema =
          new ReviewResultSchema("No issues found", List.of(), List.of());

      assertThat(schema.summary()).isEqualTo("No issues found");
      assertThat(schema.issues()).isEmpty();
      assertThat(schema.nonBlockingNotes()).isEmpty();
    }

    @Test
    @DisplayName("should_create_schema_with_null_lists")
    final void should_create_schema_with_null_lists() {
      final ReviewResultSchema schema = new ReviewResultSchema("Summary", null, null);

      assertThat(schema.summary()).isEqualTo("Summary");
      assertThat(schema.issues()).isNull();
      assertThat(schema.nonBlockingNotes()).isNull();
    }

    @Test
    @DisplayName("should_create_schema_with_multiple_issues")
    final void should_create_schema_with_multiple_issues() {
      final IssueSchema issue1 =
          new IssueSchema(
              "src/Main.java",
              10,
              Severity.critical,
              "Security vulnerability",
              "Fix immediately",
              0.95,
              "Critical issue",
              null);
      final IssueSchema issue2 =
          new IssueSchema(
              "src/Utils.java",
              25,
              Severity.minor,
              "Code style",
              "Follow convention",
              0.7,
              "Style issue",
              null);

      final ReviewResultSchema schema =
          new ReviewResultSchema("Found issues", List.of(issue1, issue2), List.of());

      assertThat(schema.issues()).hasSize(2);
      assertThat(schema.issues().get(0).severity()).isEqualTo(Severity.critical);
      assertThat(schema.issues().get(1).severity()).isEqualTo(Severity.minor);
    }
  }

  @Nested
  @DisplayName("IssueSchema Tests")
  final class IssueSchemaTests {

    @Test
    @DisplayName("should_create_issue_with_all_severity_levels")
    final void should_create_issue_with_all_severity_levels() {
      for (final Severity severity : Severity.values()) {
        final IssueSchema issue =
            new IssueSchema(
                "src/Test.java", 1, severity, "Test issue", "Test suggestion", null, null, null);

        assertThat(issue.severity()).isEqualTo(severity);
      }
    }

    @Test
    @DisplayName("should_create_issue_with_suggested_fix")
    final void should_create_issue_with_suggested_fix() {
      final String base64Fix = "ZGlmZiAtLWdpdCBhL3NyYy9NYWluLmphdmE=";
      final IssueSchema issue =
          new IssueSchema(
              "src/Main.java",
              15,
              Severity.major,
              "Bug fix needed",
              "Apply the fix",
              0.85,
              "Confident",
              base64Fix);

      assertThat(issue.suggestedFix()).isEqualTo(base64Fix);
      assertThat(issue.confidenceScore()).isEqualTo(0.85);
    }

    @Test
    @DisplayName("should_create_issue_without_optional_fields")
    final void should_create_issue_without_optional_fields() {
      final IssueSchema issue =
          new IssueSchema(
              "src/Main.java", 5, Severity.info, "Observation", "Consider this", null, null, null);

      assertThat(issue.confidenceScore()).isNull();
      assertThat(issue.confidenceExplanation()).isNull();
      assertThat(issue.suggestedFix()).isNull();
    }

    @Test
    @DisplayName("should_preserve_file_path_and_line_number")
    final void should_preserve_file_path_and_line_number() {
      final IssueSchema issue =
          new IssueSchema(
              "src/com/example/Service.java",
              142,
              Severity.major,
              "Title",
              "Suggestion",
              null,
              null,
              null);

      assertThat(issue.file()).isEqualTo("src/com/example/Service.java");
      assertThat(issue.startLine()).isEqualTo(142);
    }
  }

  @Nested
  @DisplayName("NoteSchema Tests")
  final class NoteSchemaTests {

    @Test
    @DisplayName("should_create_note_with_all_fields")
    final void should_create_note_with_all_fields() {
      final NoteSchema note =
          new NoteSchema("src/Main.java", 30, "Consider using dependency injection");

      assertThat(note.file()).isEqualTo("src/Main.java");
      assertThat(note.line()).isEqualTo(30);
      assertThat(note.note()).isEqualTo("Consider using dependency injection");
    }

    @Test
    @DisplayName("should_preserve_multiline_note_content")
    final void should_preserve_multiline_note_content() {
      final String multilineNote =
          "This method could be improved by:\n1. Adding caching\n2. Using streams";
      final NoteSchema note = new NoteSchema("src/Service.java", 50, multilineNote);

      assertThat(note.note()).contains("1. Adding caching");
      assertThat(note.note()).contains("2. Using streams");
    }
  }

  @Nested
  @DisplayName("Severity Enum Tests")
  final class SeverityTests {

    @Test
    @DisplayName("should_have_all_expected_severity_levels")
    final void should_have_all_expected_severity_levels() {
      assertThat(Severity.values())
          .containsExactly(Severity.critical, Severity.major, Severity.minor, Severity.info);
    }

    @Test
    @DisplayName("should_convert_severity_to_string")
    final void should_convert_severity_to_string() {
      assertThat(Severity.critical.name()).isEqualTo("critical");
      assertThat(Severity.major.name()).isEqualTo("major");
      assertThat(Severity.minor.name()).isEqualTo("minor");
      assertThat(Severity.info.name()).isEqualTo("info");
    }
  }
}
