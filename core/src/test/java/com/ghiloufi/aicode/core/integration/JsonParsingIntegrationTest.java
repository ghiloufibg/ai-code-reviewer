package com.ghiloufi.aicode.core.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghiloufi.aicode.core.domain.model.ReviewChunk;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.service.ReviewSummaryFormatter;
import com.ghiloufi.aicode.core.exception.JsonValidationException;
import com.ghiloufi.aicode.core.service.accumulator.ReviewChunkAccumulator;
import com.ghiloufi.aicode.core.service.prompt.JsonReviewResultParser;
import com.ghiloufi.aicode.core.service.validation.ReviewResultValidator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("JSON Parsing Integration Tests")
class JsonParsingIntegrationTest {

  private ReviewChunkAccumulator chunkAccumulator;
  private JsonReviewResultParser jsonParser;
  private ReviewResultValidator validator;
  private ReviewSummaryFormatter summaryFormatter;

  @BeforeEach
  void setUp() {
    final ObjectMapper objectMapper = new ObjectMapper();
    validator = new ReviewResultValidator(objectMapper);
    summaryFormatter = new ReviewSummaryFormatter();
    jsonParser = new JsonReviewResultParser(validator, objectMapper);
    final com.ghiloufi.aicode.core.service.filter.ConfidenceFilter confidenceFilter =
        new com.ghiloufi.aicode.core.service.filter.ConfidenceFilter();
    chunkAccumulator = new ReviewChunkAccumulator(summaryFormatter, jsonParser, confidenceFilter);
  }

  @Nested
  @DisplayName("Valid JSON Response Formats")
  class ValidJsonFormats {

    @Test
    @DisplayName("should parse complete JSON with all fields")
    void should_parse_complete_json_with_all_fields() {
      final String completeJson =
          """
          {
            "summary": "Code review completed successfully. Found 2 critical issues.",
            "issues": [
              {
                "severity": "critical",
                "title": "SQL Injection vulnerability",
                "file": "UserController.java",
                "start_line": 45,
                "suggestion": "Use parameterized queries instead of string concatenation"
              },
              {
                "severity": "major",
                "title": "Missing error handling",
                "file": "DataService.java",
                "start_line": 78,
                "suggestion": "Add try-catch block for database operations"
              }
            ],
            "non_blocking_notes": [
              {
                "file": "Utils.java",
                "line": 12,
                "note": "Consider extracting this method to improve testability"
              }
            ]
          }
          """;

      final List<ReviewChunk> chunks = createChunksFromJson(completeJson);

      final ReviewResult result = chunkAccumulator.accumulateChunks(chunks);

      assertThat(result).isNotNull();
      assertThat(result.getSummary()).contains("Code review completed successfully");
      assertThat(result.getIssues()).hasSize(2);
      assertThat(result.getNonBlockingNotes()).hasSize(1);

      assertThat(result.getIssues().get(0).getSeverity()).isEqualTo("critical");
      assertThat(result.getIssues().get(0).getTitle()).isEqualTo("SQL Injection vulnerability");
      assertThat(result.getIssues().get(0).getFile()).isEqualTo("UserController.java");
      assertThat(result.getIssues().get(0).getStartLine()).isEqualTo(45);

      assertThat(result.getNonBlockingNotes().get(0).getFile()).isEqualTo("Utils.java");
      assertThat(result.getNonBlockingNotes().get(0).getLine()).isEqualTo(12);

      assertThat(result.getRawLlmResponse()).isNotNull();
      assertThat(result.getRawLlmResponse()).contains("Code review completed successfully");
    }

    @Test
    @DisplayName("should parse JSON with empty issues array")
    void should_parse_json_with_empty_issues() {
      final String jsonWithEmptyIssues =
          """
          {
            "summary": "No issues found. Code looks good!",
            "issues": [],
            "non_blocking_notes": [
              {
                "file": "Config.java",
                "line": 5,
                "note": "Consider using environment variables for configuration"
              }
            ]
          }
          """;

      final List<ReviewChunk> chunks = createChunksFromJson(jsonWithEmptyIssues);

      final ReviewResult result = chunkAccumulator.accumulateChunks(chunks);

      assertThat(result).isNotNull();
      assertThat(result.getSummary()).isEqualTo("No issues found. Code looks good!");
      assertThat(result.getIssues()).isEmpty();
      assertThat(result.getNonBlockingNotes()).hasSize(1);
    }

    @Test
    @DisplayName("should parse JSON with empty notes array")
    void should_parse_json_with_empty_notes() {
      final String jsonWithEmptyNotes =
          """
          {
            "summary": "Found security issues that need immediate attention",
            "issues": [
              {
                "severity": "critical",
                "title": "Hardcoded credentials",
                "file": "DatabaseConfig.java",
                "start_line": 23,
                "suggestion": "Move credentials to secure vault or environment variables"
              }
            ],
            "non_blocking_notes": []
          }
          """;

      final List<ReviewChunk> chunks = createChunksFromJson(jsonWithEmptyNotes);

      final ReviewResult result = chunkAccumulator.accumulateChunks(chunks);

      assertThat(result).isNotNull();
      assertThat(result.getIssues()).hasSize(1);
      assertThat(result.getNonBlockingNotes()).isEmpty();
    }

    @Test
    @DisplayName("should parse JSON with minimal required fields")
    void should_parse_json_with_minimal_fields() {
      final String minimalJson =
          """
          {
            "summary": "Minimal review",
            "issues": [],
            "non_blocking_notes": []
          }
          """;

      final List<ReviewChunk> chunks = createChunksFromJson(minimalJson);

      final ReviewResult result = chunkAccumulator.accumulateChunks(chunks);

      assertThat(result).isNotNull();
      assertThat(result.getSummary()).isEqualTo("Minimal review");
      assertThat(result.getIssues()).isEmpty();
      assertThat(result.getNonBlockingNotes()).isEmpty();
    }
  }

  @Nested
  @DisplayName("JSON Format Edge Cases")
  class JsonFormatEdgeCases {

    @Test
    @DisplayName("should parse JSON wrapped in markdown code block")
    void should_parse_json_wrapped_in_markdown() {
      final String markdownWrappedJson =
          """
          ```json
          {
            "summary": "Review complete",
            "issues": [
              {
                "severity": "minor",
                "title": "Code style issue",
                "file": "Style.java",
                "start_line": 10,
                "suggestion": "Follow Java naming conventions"
              }
            ],
            "non_blocking_notes": []
          }
          ```
          """;

      final List<ReviewChunk> chunks = createChunksFromJson(markdownWrappedJson);

      final ReviewResult result = chunkAccumulator.accumulateChunks(chunks);

      assertThat(result).isNotNull();
      assertThat(result.getIssues()).hasSize(1);
    }

    @Test
    @DisplayName("should parse JSON with extra whitespace")
    void should_parse_json_with_extra_whitespace() {
      final String jsonWithWhitespace =
          """


          {
            "summary": "Review with whitespace",
            "issues": [],
            "non_blocking_notes": []
          }


          """;

      final List<ReviewChunk> chunks = createChunksFromJson(jsonWithWhitespace);

      final ReviewResult result = chunkAccumulator.accumulateChunks(chunks);

      assertThat(result).isNotNull();
      assertThat(result.getSummary()).isEqualTo("Review with whitespace");
    }

    @Test
    @DisplayName("should parse JSON split across multiple chunks")
    void should_parse_json_split_across_chunks() {
      final List<ReviewChunk> chunks =
          List.of(
              ReviewChunk.of(ReviewChunk.ChunkType.ANALYSIS, "{\"summary\": \"Split "),
              ReviewChunk.of(ReviewChunk.ChunkType.ANALYSIS, "JSON review\","),
              ReviewChunk.of(ReviewChunk.ChunkType.ANALYSIS, "\"issues\": [],"),
              ReviewChunk.of(ReviewChunk.ChunkType.ANALYSIS, "\"non_blocking_notes\": []}"));

      final ReviewResult result = chunkAccumulator.accumulateChunks(chunks);

      assertThat(result).isNotNull();
      assertThat(result.getSummary()).isEqualTo("Split JSON review");
    }

    @Test
    @DisplayName("should parse JSON with unicode characters")
    void should_parse_json_with_unicode_characters() {
      final String jsonWithUnicode =
          """
          {
            "summary": "Révision du code complétée avec succès ✅",
            "issues": [
              {
                "severity": "info",
                "title": "Amélioration possible",
                "file": "Données.java",
                "start_line": 5,
                "suggestion": "Utiliser des constantes pour les chaînes répétées"
              }
            ],
            "non_blocking_notes": []
          }
          """;

      final List<ReviewChunk> chunks = createChunksFromJson(jsonWithUnicode);

      final ReviewResult result = chunkAccumulator.accumulateChunks(chunks);

      assertThat(result).isNotNull();
      assertThat(result.getSummary()).contains("✅");
      assertThat(result.getIssues().get(0).getTitle()).contains("Amélioration");
    }

    @Test
    @DisplayName("should parse JSON with preamble text from LLM")
    void should_parse_json_with_llm_preamble_text() {
      final String jsonWithPreamble =
          """
          Here is your code review as requested in JSON format:

          {
            "summary": "Code review completed successfully",
            "issues": [
              {
                "severity": "minor",
                "title": "Variable naming",
                "file": "UserService.java",
                "start_line": 42,
                "suggestion": "Use more descriptive variable names"
              }
            ],
            "non_blocking_notes": []
          }
          """;

      final List<ReviewChunk> chunks = createChunksFromJson(jsonWithPreamble);

      final ReviewResult result = chunkAccumulator.accumulateChunks(chunks);

      assertThat(result).isNotNull();
      assertThat(result.getSummary()).isEqualTo("Code review completed successfully");
      assertThat(result.getIssues()).hasSize(1);
      assertThat(result.getIssues().get(0).getSeverity()).isEqualTo("minor");
      assertThat(result.getIssues().get(0).getTitle()).isEqualTo("Variable naming");
    }

    @Test
    @DisplayName("should parse JSON with preamble and postamble text from LLM")
    void should_parse_json_with_preamble_and_postamble_text() {
      final String jsonWithPreambleAndPostamble =
          """
          Here is your code review as requested in JSON format:

          {
            "summary": "Review complete with suggestions",
            "issues": [
              {
                "severity": "major",
                "title": "Memory leak",
                "file": "CacheManager.java",
                "start_line": 67,
                "suggestion": "Close resources properly"
              }
            ],
            "non_blocking_notes": []
          }

          Do not hesitate if you have any questions about these findings!
          """;

      final List<ReviewChunk> chunks = createChunksFromJson(jsonWithPreambleAndPostamble);

      final ReviewResult result = chunkAccumulator.accumulateChunks(chunks);

      assertThat(result).isNotNull();
      assertThat(result.getSummary()).isEqualTo("Review complete with suggestions");
      assertThat(result.getIssues()).hasSize(1);
      assertThat(result.getIssues().get(0).getSeverity()).isEqualTo("major");
      assertThat(result.getIssues().get(0).getTitle()).isEqualTo("Memory leak");
    }
  }

  @Nested
  @DisplayName("Invalid JSON Scenarios")
  class InvalidJsonScenarios {

    @Test
    @DisplayName("should throw exception for non-JSON text response")
    void should_throw_exception_for_non_json_text() {
      final String plainText =
          "This is a plain text review without JSON structure. Security issue found in auth.java at line 45.";

      final List<ReviewChunk> chunks = createChunksFromJson(plainText);

      assertThatThrownBy(() -> chunkAccumulator.accumulateChunks(chunks))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Expected JSON response from LLM")
          .hasMessageContaining("Ensure the LLM is configured to return structured JSON format");
    }

    @Test
    @DisplayName("should throw exception for malformed JSON")
    void should_throw_exception_for_malformed_json() {
      final String malformedJson =
          """
          {
            "summary": "Malformed JSON",
            "issues": [
              {
                "severity": "critical"
                "title": "Missing comma"
              }
            ]
          """;

      final List<ReviewChunk> chunks = createChunksFromJson(malformedJson);

      assertThatThrownBy(() -> chunkAccumulator.accumulateChunks(chunks))
          .isInstanceOf(JsonValidationException.class)
          .hasMessageContaining("JSON validation failed");
    }

    @Test
    @DisplayName("should throw exception for JSON missing required fields")
    void should_throw_exception_for_missing_required_fields() {
      final String jsonMissingFields =
          """
          {
            "summary": "Missing required fields"
          }
          """;

      final List<ReviewChunk> chunks = createChunksFromJson(jsonMissingFields);

      assertThatThrownBy(() -> chunkAccumulator.accumulateChunks(chunks))
          .isInstanceOf(JsonValidationException.class)
          .hasMessageContaining("issues")
          .hasMessageContaining("non_blocking_notes");
    }

    @Test
    @DisplayName("should throw exception for invalid severity value")
    void should_throw_exception_for_invalid_severity() {
      final String jsonWithInvalidSeverity =
          """
          {
            "summary": "Invalid severity",
            "issues": [
              {
                "severity": "super-critical",
                "title": "Invalid severity level",
                "file": "Test.java",
                "start_line": 1,
                "suggestion": "This should fail validation"
              }
            ],
            "non_blocking_notes": []
          }
          """;

      final List<ReviewChunk> chunks = createChunksFromJson(jsonWithInvalidSeverity);

      assertThatThrownBy(() -> chunkAccumulator.accumulateChunks(chunks))
          .isInstanceOf(JsonValidationException.class)
          .hasMessageContaining("severity");
    }

    @Test
    @DisplayName("should throw exception for JSON array instead of object")
    void should_throw_exception_for_json_array() {
      final String jsonArray =
          """
          [
            {
              "summary": "This is an array, not an object",
              "issues": [],
              "non_blocking_notes": []
            }
          ]
          """;

      final List<ReviewChunk> chunks = createChunksFromJson(jsonArray);

      assertThatThrownBy(() -> chunkAccumulator.accumulateChunks(chunks))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Expected JSON response from LLM");
    }

    @Test
    @DisplayName("should throw exception for empty string")
    void should_throw_exception_for_empty_string() {
      final List<ReviewChunk> chunks = createChunksFromJson("");

      assertThatThrownBy(() -> chunkAccumulator.accumulateChunks(chunks))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Expected JSON response from LLM");
    }

    @Test
    @DisplayName("should throw exception for null chunks list")
    void should_throw_exception_for_null_chunks() {
      assertThatThrownBy(() -> chunkAccumulator.accumulateChunks(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Chunks list cannot be null");
    }
  }

  @Nested
  @DisplayName("Additional JSON Parsing Scenarios")
  class AdditionalScenarios {

    @Test
    @DisplayName("should handle multiple severity levels correctly")
    void should_handle_multiple_severity_levels() {
      final String jsonWithAllSeverities =
          """
          {
            "summary": "Comprehensive review with all severity levels",
            "issues": [
              {
                "severity": "critical",
                "title": "Security vulnerability",
                "file": "Auth.java",
                "start_line": 10,
                "suggestion": "Fix immediately"
              },
              {
                "severity": "major",
                "title": "Major bug",
                "file": "Logic.java",
                "start_line": 20,
                "suggestion": "Fix soon"
              },
              {
                "severity": "minor",
                "title": "Code smell",
                "file": "Style.java",
                "start_line": 30,
                "suggestion": "Refactor when possible"
              },
              {
                "severity": "info",
                "title": "Informational note",
                "file": "Docs.java",
                "start_line": 40,
                "suggestion": "For your information"
              }
            ],
            "non_blocking_notes": []
          }
          """;

      final List<ReviewChunk> chunks = createChunksFromJson(jsonWithAllSeverities);

      final ReviewResult result = chunkAccumulator.accumulateChunks(chunks);

      assertThat(result.getIssues()).hasSize(4);
      assertThat(result.getIssues())
          .extracting(ReviewResult.Issue::getSeverity)
          .containsExactly("critical", "major", "minor", "info");
    }

    @Test
    @DisplayName("should handle large JSON responses")
    void should_handle_large_json_responses() {
      final StringBuilder largeJson = new StringBuilder();
      largeJson.append("{\"summary\": \"Large review\", \"issues\": [");

      for (int i = 0; i < 50; i++) {
        if (i > 0) {
          largeJson.append(",");
        }
        largeJson.append(
            String.format(
                """
                {
                  "severity": "minor",
                  "title": "Issue %d",
                  "file": "File%d.java",
                  "start_line": %d,
                  "suggestion": "Fix issue number %d"
                }
                """,
                i, i, (i * 10) + 1, i));
      }

      largeJson.append("], \"non_blocking_notes\": []}");

      final List<ReviewChunk> chunks = createChunksFromJson(largeJson.toString());

      final ReviewResult result = chunkAccumulator.accumulateChunks(chunks);

      assertThat(result.getIssues()).hasSize(50);
    }
  }

  private List<ReviewChunk> createChunksFromJson(final String json) {
    return List.of(ReviewChunk.of(ReviewChunk.ChunkType.ANALYSIS, json));
  }
}
