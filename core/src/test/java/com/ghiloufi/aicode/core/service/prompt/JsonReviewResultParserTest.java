package com.ghiloufi.aicode.core.service.prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.exception.JsonValidationException;
import com.ghiloufi.aicode.core.service.validation.ReviewResultValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("JsonReviewResultParser")
class JsonReviewResultParserTest {

  private JsonReviewResultParser parser;

  @BeforeEach
  void setUp() {
    final ObjectMapper objectMapper = new ObjectMapper();
    final ReviewResultValidator validator = new ReviewResultValidator(objectMapper);
    parser = new JsonReviewResultParser(validator, objectMapper);
  }

  @Nested
  @DisplayName("when parsing valid JSON response")
  class WhenParsingValidJsonResponse {

    @Test
    @DisplayName("should_parse_complete_review_result_successfully")
    void should_parse_complete_review_result_successfully() {
      final String jsonResponse =
          """
          {
            "summary": "Code review completed. Found 2 issues.",
            "issues": [
              {
                "file": "src/Main.java",
                "start_line": 10,
                "severity": "major",
                "title": "Potential null pointer exception",
                "suggestion": "Add null check before dereferencing"
              },
              {
                "file": "src/Utils.java",
                "start_line": 25,
                "severity": "minor",
                "title": "Inefficient string concatenation",
                "suggestion": "Use StringBuilder instead"
              }
            ],
            "non_blocking_notes": [
              {
                "file": "src/Config.java",
                "line": 5,
                "note": "Consider extracting this constant to a configuration file"
              }
            ]
          }
          """;

      final ReviewResult result = parser.parse(jsonResponse);

      assertThat(result).isNotNull();
      assertThat(result.summary).isEqualTo("Code review completed. Found 2 issues.");
      assertThat(result.issues).hasSize(2);
      assertThat(result.non_blocking_notes).hasSize(1);

      assertThat(result.issues.get(0).file).isEqualTo("src/Main.java");
      assertThat(result.issues.get(0).start_line).isEqualTo(10);
      assertThat(result.issues.get(0).severity).isEqualTo("major");
      assertThat(result.issues.get(0).title).isEqualTo("Potential null pointer exception");

      assertThat(result.non_blocking_notes.get(0).file).isEqualTo("src/Config.java");
      assertThat(result.non_blocking_notes.get(0).line).isEqualTo(5);
    }

    @Test
    @DisplayName("should_parse_review_result_with_empty_arrays")
    void should_parse_review_result_with_empty_arrays() {
      final String jsonResponse =
          """
          {
            "summary": "No issues found. Code looks good!",
            "issues": [],
            "non_blocking_notes": []
          }
          """;

      final ReviewResult result = parser.parse(jsonResponse);

      assertThat(result).isNotNull();
      assertThat(result.summary).isEqualTo("No issues found. Code looks good!");
      assertThat(result.issues).isEmpty();
      assertThat(result.non_blocking_notes).isEmpty();
    }

    @Test
    @DisplayName("should_strip_markdown_code_blocks_before_parsing")
    void should_strip_markdown_code_blocks_before_parsing() {
      final String jsonResponseWithMarkdown =
          """
          ```json
          {
            "summary": "Review complete",
            "issues": [],
            "non_blocking_notes": []
          }
          ```
          """;

      final ReviewResult result = parser.parse(jsonResponseWithMarkdown);

      assertThat(result).isNotNull();
      assertThat(result.summary).isEqualTo("Review complete");
    }

    @Test
    @DisplayName("should_handle_response_with_extra_whitespace")
    void should_handle_response_with_extra_whitespace() {
      final String jsonResponse =
          """

          {
            "summary": "Review complete",
            "issues": [],
            "non_blocking_notes": []
          }

          """;

      final ReviewResult result = parser.parse(jsonResponse);

      assertThat(result).isNotNull();
      assertThat(result.summary).isEqualTo("Review complete");
    }

    @Test
    @DisplayName("should_strip_schema_property_from_llm_response")
    void should_strip_schema_property_from_llm_response() {
      final String jsonResponseWithSchema =
          """
          {
            "$schema": "http://json-schema.org/draft-07/schema#",
            "summary": "The FizzBuzz implementation is functional but contains redundant checks and could be optimized for clarity and performance.",
            "issues": [],
            "non_blocking_notes": [
              {
                "file": "FizzBuzz.java",
                "line": 10,
                "note": "The nested if-else structure can be simplified to improve readability."
              },
              {
                "file": "FizzBuzz.java",
                "line": 30,
                "note": "The second loop has similar logic to the first; consider consolidating the logic to avoid code duplication."
              },
              {
                "file": "FizzBuzz.java",
                "line": 46,
                "note": "The do-while loop is a valid approach, but ensure that the logic remains clear and consistent with the previous loops."
              }
            ]
          }
          """;

      final ReviewResult result = parser.parse(jsonResponseWithSchema);

      assertThat(result).isNotNull();
      assertThat(result.summary)
          .isEqualTo(
              "The FizzBuzz implementation is functional but contains redundant checks and could be optimized for clarity and performance.");
      assertThat(result.issues).isEmpty();
      assertThat(result.non_blocking_notes).hasSize(3);
      assertThat(result.non_blocking_notes.get(0).file).isEqualTo("FizzBuzz.java");
      assertThat(result.non_blocking_notes.get(0).line).isEqualTo(10);
      assertThat(result.non_blocking_notes.get(1).line).isEqualTo(30);
      assertThat(result.non_blocking_notes.get(2).line).isEqualTo(46);
    }
  }

  @Nested
  @DisplayName("when parsing invalid JSON response")
  class WhenParsingInvalidJsonResponse {

    @Test
    @DisplayName("should_reject_json_missing_required_fields")
    void should_reject_json_missing_required_fields() {
      final String invalidJson =
          """
          {
            "summary": "Review complete"
          }
          """;

      assertThatThrownBy(() -> parser.parse(invalidJson))
          .isInstanceOf(JsonValidationException.class)
          .hasMessageContaining("JSON validation failed");
    }

    @Test
    @DisplayName("should_reject_json_with_invalid_severity")
    void should_reject_json_with_invalid_severity() {
      final String invalidJson =
          """
          {
            "summary": "Review complete",
            "issues": [
              {
                "file": "test.java",
                "start_line": 1,
                "severity": "super_critical",
                "title": "Test",
                "suggestion": "Test"
              }
            ],
            "non_blocking_notes": []
          }
          """;

      assertThatThrownBy(() -> parser.parse(invalidJson))
          .isInstanceOf(JsonValidationException.class)
          .hasMessageContaining("JSON validation failed");
    }

    @Test
    @DisplayName("should_reject_malformed_json")
    void should_reject_malformed_json() {
      final String malformedJson = "{ this is not valid json }";

      assertThatThrownBy(() -> parser.parse(malformedJson))
          .isInstanceOf(JsonValidationException.class);
    }

    @Test
    @DisplayName("should_reject_null_input")
    void should_reject_null_input() {
      assertThatThrownBy(() -> parser.parse(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("JSON response cannot be null");
    }

    @Test
    @DisplayName("should_reject_empty_input")
    void should_reject_empty_input() {
      assertThatThrownBy(() -> parser.parse(""))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("JSON response cannot be empty");
    }
  }
}
