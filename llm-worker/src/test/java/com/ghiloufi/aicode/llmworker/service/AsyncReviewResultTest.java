package com.ghiloufi.aicode.llmworker.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.llmworker.schema.IssueSchema;
import com.ghiloufi.aicode.llmworker.schema.NoteSchema;
import com.ghiloufi.aicode.llmworker.schema.ReviewResultSchema;
import com.ghiloufi.aicode.llmworker.schema.Severity;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AsyncReviewResult Tests")
final class AsyncReviewResultTest {

  @Nested
  @DisplayName("Construction")
  final class Construction {

    @Test
    @DisplayName("should_create_result_with_schema_and_files_analyzed")
    void should_create_result_with_schema_and_files_analyzed() {
      final ReviewResultSchema schema = new ReviewResultSchema("Summary", List.of(), List.of());

      final AsyncReviewResult result = new AsyncReviewResult(schema, 5);

      assertThat(result.schema()).isEqualTo(schema);
      assertThat(result.filesAnalyzed()).isEqualTo(5);
    }

    @Test
    @DisplayName("should_create_result_with_issues")
    void should_create_result_with_issues() {
      final IssueSchema issue =
          new IssueSchema("Test.java", 10, Severity.major, "Title", "Fix", 0.9, "High");
      final ReviewResultSchema schema =
          new ReviewResultSchema("Summary with issues", List.of(issue), List.of());

      final AsyncReviewResult result = new AsyncReviewResult(schema, 3);

      assertThat(result.schema().summary()).isEqualTo("Summary with issues");
      assertThat(result.schema().issues()).hasSize(1);
      assertThat(result.filesAnalyzed()).isEqualTo(3);
    }

    @Test
    @DisplayName("should_create_result_with_notes")
    void should_create_result_with_notes() {
      final NoteSchema note = new NoteSchema("Util.java", 25, "Consider refactoring");
      final ReviewResultSchema schema =
          new ReviewResultSchema("Summary with notes", List.of(), List.of(note));

      final AsyncReviewResult result = new AsyncReviewResult(schema, 2);

      assertThat(result.schema().nonBlockingNotes()).hasSize(1);
      assertThat(result.filesAnalyzed()).isEqualTo(2);
    }

    @Test
    @DisplayName("should_handle_zero_files_analyzed")
    void should_handle_zero_files_analyzed() {
      final ReviewResultSchema schema =
          new ReviewResultSchema("Empty review", List.of(), List.of());

      final AsyncReviewResult result = new AsyncReviewResult(schema, 0);

      assertThat(result.filesAnalyzed()).isZero();
    }

    @Test
    @DisplayName("should_handle_null_schema")
    void should_handle_null_schema() {
      final AsyncReviewResult result = new AsyncReviewResult(null, 1);

      assertThat(result.schema()).isNull();
      assertThat(result.filesAnalyzed()).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("Equality")
  final class Equality {

    @Test
    @DisplayName("should_be_equal_when_same_values")
    void should_be_equal_when_same_values() {
      final ReviewResultSchema schema = new ReviewResultSchema("Summary", List.of(), List.of());

      final AsyncReviewResult result1 = new AsyncReviewResult(schema, 5);
      final AsyncReviewResult result2 = new AsyncReviewResult(schema, 5);

      assertThat(result1).isEqualTo(result2);
      assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
    }

    @Test
    @DisplayName("should_not_be_equal_when_different_files_analyzed")
    void should_not_be_equal_when_different_files_analyzed() {
      final ReviewResultSchema schema = new ReviewResultSchema("Summary", List.of(), List.of());

      final AsyncReviewResult result1 = new AsyncReviewResult(schema, 5);
      final AsyncReviewResult result2 = new AsyncReviewResult(schema, 10);

      assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    @DisplayName("should_not_be_equal_when_different_schema")
    void should_not_be_equal_when_different_schema() {
      final ReviewResultSchema schema1 = new ReviewResultSchema("Summary1", List.of(), List.of());
      final ReviewResultSchema schema2 = new ReviewResultSchema("Summary2", List.of(), List.of());

      final AsyncReviewResult result1 = new AsyncReviewResult(schema1, 5);
      final AsyncReviewResult result2 = new AsyncReviewResult(schema2, 5);

      assertThat(result1).isNotEqualTo(result2);
    }
  }

  @Nested
  @DisplayName("ToString")
  final class ToStringTests {

    @Test
    @DisplayName("should_include_schema_and_files_in_tostring")
    void should_include_schema_and_files_in_tostring() {
      final ReviewResultSchema schema = new ReviewResultSchema("Summary", List.of(), List.of());

      final AsyncReviewResult result = new AsyncReviewResult(schema, 7);

      final String str = result.toString();
      assertThat(str).contains("7");
      assertThat(str).contains("schema");
    }
  }
}
