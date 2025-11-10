package com.ghiloufi.aicode.core.service.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghiloufi.aicode.core.exception.JsonValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ReviewResultValidator")
class ReviewResultValidatorTest {

  private ReviewResultValidator validator;

  @BeforeEach
  void setUp() {
    final ObjectMapper objectMapper = new ObjectMapper();
    validator = new ReviewResultValidator(objectMapper);
  }

  @Nested
  @DisplayName("when validating correct JSON")
  class WhenValidatingCorrectJson {

    @Test
    @DisplayName("should_accept_valid_review_result_with_all_fields")
    void should_accept_valid_review_result_with_all_fields() {
      final String validJson =
          """
          {
            "summary": "Code looks good overall",
            "issues": [
              {
                "file": "src/Main.java",
                "start_line": 10,
                "severity": "major",
                "title": "Null pointer risk",
                "suggestion": "Add null check"
              }
            ],
            "non_blocking_notes": [
              {
                "file": "src/Utils.java",
                "line": 5,
                "note": "Consider using StringBuilder"
              }
            ]
          }
          """;

      final ValidationResult result = validator.validate(validJson);

      assertThat(result.isValid()).isTrue();
      assertThat(result.errors()).isEmpty();
    }

    @Test
    @DisplayName("should_accept_valid_review_result_with_empty_arrays")
    void should_accept_valid_review_result_with_empty_arrays() {
      final String validJson =
          """
          {
            "summary": "No issues found",
            "issues": [],
            "non_blocking_notes": []
          }
          """;

      final ValidationResult result = validator.validate(validJson);

      assertThat(result.isValid()).isTrue();
      assertThat(result.errors()).isEmpty();
    }

    @Test
    @DisplayName("should_accept_all_valid_severity_levels")
    void should_accept_all_valid_severity_levels() {
      final String validJson =
          """
          {
            "summary": "Multiple severity levels",
            "issues": [
              {
                "file": "test.java",
                "start_line": 1,
                "severity": "critical",
                "title": "Critical issue",
                "suggestion": "Fix immediately"
              },
              {
                "file": "test.java",
                "start_line": 2,
                "severity": "major",
                "title": "Major issue",
                "suggestion": "Fix soon"
              },
              {
                "file": "test.java",
                "start_line": 3,
                "severity": "minor",
                "title": "Minor issue",
                "suggestion": "Consider fixing"
              },
              {
                "file": "test.java",
                "start_line": 4,
                "severity": "info",
                "title": "Info",
                "suggestion": "FYI"
              }
            ],
            "non_blocking_notes": []
          }
          """;

      final ValidationResult result = validator.validate(validJson);

      assertThat(result.isValid()).isTrue();
    }
  }

  @Nested
  @DisplayName("when validating invalid JSON")
  class WhenValidatingInvalidJson {

    @Test
    @DisplayName("should_reject_missing_required_summary_field")
    void should_reject_missing_required_summary_field() {
      final String invalidJson =
          """
          {
            "issues": [],
            "non_blocking_notes": []
          }
          """;

      final ValidationResult result = validator.validate(invalidJson);

      assertThat(result.isValid()).isFalse();
      assertThat(result.errors()).isNotEmpty();
      assertThat(result.errors()).anyMatch(error -> error.contains("summary"));
    }

    @Test
    @DisplayName("should_reject_missing_required_issues_field")
    void should_reject_missing_required_issues_field() {
      final String invalidJson =
          """
          {
            "summary": "Test",
            "non_blocking_notes": []
          }
          """;

      final ValidationResult result = validator.validate(invalidJson);

      assertThat(result.isValid()).isFalse();
      assertThat(result.errors()).anyMatch(error -> error.contains("issues"));
    }

    @Test
    @DisplayName("should_reject_issue_with_invalid_severity")
    void should_reject_issue_with_invalid_severity() {
      final String invalidJson =
          """
          {
            "summary": "Test",
            "issues": [
              {
                "file": "test.java",
                "start_line": 1,
                "severity": "invalid_severity",
                "title": "Test",
                "suggestion": "Test"
              }
            ],
            "non_blocking_notes": []
          }
          """;

      final ValidationResult result = validator.validate(invalidJson);

      assertThat(result.isValid()).isFalse();
      assertThat(result.errors()).anyMatch(error -> error.contains("severity"));
    }

    @Test
    @DisplayName("should_reject_issue_with_invalid_line_number")
    void should_reject_issue_with_invalid_line_number() {
      final String invalidJson =
          """
          {
            "summary": "Test",
            "issues": [
              {
                "file": "test.java",
                "start_line": 0,
                "severity": "major",
                "title": "Test",
                "suggestion": "Test"
              }
            ],
            "non_blocking_notes": []
          }
          """;

      final ValidationResult result = validator.validate(invalidJson);

      assertThat(result.isValid()).isFalse();
      assertThat(result.errors())
          .anyMatch(error -> error.contains("start_line") || error.contains("minimum"));
    }

    @Test
    @DisplayName("should_reject_issue_missing_required_fields")
    void should_reject_issue_missing_required_fields() {
      final String invalidJson =
          """
          {
            "summary": "Test",
            "issues": [
              {
                "file": "test.java",
                "start_line": 1
              }
            ],
            "non_blocking_notes": []
          }
          """;

      final ValidationResult result = validator.validate(invalidJson);

      assertThat(result.isValid()).isFalse();
      assertThat(result.errors()).isNotEmpty();
    }

    @Test
    @DisplayName("should_reject_malformed_json")
    void should_reject_malformed_json() {
      final String malformedJson = "{ invalid json }";

      assertThatThrownBy(() -> validator.validate(malformedJson))
          .isInstanceOf(JsonValidationException.class)
          .hasMessageContaining("Invalid JSON format");
    }

    @Test
    @DisplayName("should_reject_null_input")
    void should_reject_null_input() {
      assertThatThrownBy(() -> validator.validate(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("JSON string cannot be null");
    }

    @Test
    @DisplayName("should_reject_empty_input")
    void should_reject_empty_input() {
      assertThatThrownBy(() -> validator.validate(""))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("JSON string cannot be null or empty");
    }
  }
}
