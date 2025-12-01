package com.ghiloufi.aicode.core.service.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.service.validation.ReviewResultValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JsonReviewResultParser Confidence Handling Tests")
class JsonReviewResultParserConfidenceTest {

  private JsonReviewResultParser parser;

  @BeforeEach
  void setUp() {
    final ObjectMapper objectMapper = new ObjectMapper();
    final ReviewResultValidator validator = new ReviewResultValidator(objectMapper);
    parser = new JsonReviewResultParser(validator, objectMapper);
  }

  @Test
  @DisplayName("should_parse_confidence_score_and_explanation_when_provided")
  void should_parse_confidence_score_and_explanation_when_provided() {
    final String json =
        """
        {
          "summary": "Code review summary",
          "issues": [{
            "file": "Test.java",
            "start_line": 10,
            "severity": "major",
            "title": "Security issue",
            "suggestion": "Fix the vulnerability",
            "confidenceScore": 0.85,
            "confidenceExplanation": "Clear SQL injection pattern with user input"
          }],
          "non_blocking_notes": []
        }
        """;

    final ReviewResult result = parser.parse(json);

    assertThat(result.getIssues()).hasSize(1);
    final ReviewResult.Issue issue = result.getIssues().get(0);
    assertThat(issue.getConfidenceScore()).isEqualTo(0.85);
    assertThat(issue.getConfidenceExplanation())
        .isEqualTo("Clear SQL injection pattern with user input");
  }

  @Test
  @DisplayName("should_use_default_explanation_when_blank")
  void should_use_default_explanation_when_blank() {
    final String json =
        """
        {
          "summary": "Code review summary",
          "issues": [{
            "file": "Test.java",
            "start_line": 10,
            "severity": "major",
            "title": "Issue with blank explanation",
            "suggestion": "Fix it",
            "confidenceScore": 0.7,
            "confidenceExplanation": "   "
          }],
          "non_blocking_notes": []
        }
        """;

    final ReviewResult result = parser.parse(json);

    assertThat(result.getIssues()).hasSize(1);
    assertThat(result.getIssues().get(0).getConfidenceExplanation())
        .isEqualTo("No explanation provided");
  }

  @Test
  @DisplayName("should_handle_boundary_confidence_values")
  void should_handle_boundary_confidence_values() {
    final String json =
        """
        {
          "summary": "Code review summary",
          "issues": [
            {
              "file": "Test1.java",
              "start_line": 10,
              "severity": "major",
              "title": "Issue with min confidence",
              "suggestion": "Fix it",
              "confidenceScore": 0.0,
              "confidenceExplanation": "Minimum confidence"
            },
            {
              "file": "Test2.java",
              "start_line": 20,
              "severity": "critical",
              "title": "Issue with max confidence",
              "suggestion": "Fix it",
              "confidenceScore": 1.0,
              "confidenceExplanation": "Maximum confidence"
            }
          ],
          "non_blocking_notes": []
        }
        """;

    final ReviewResult result = parser.parse(json);

    assertThat(result.getIssues()).hasSize(2);
    assertThat(result.getIssues().get(0).getConfidenceScore()).isEqualTo(0.0);
    assertThat(result.getIssues().get(1).getConfidenceScore()).isEqualTo(1.0);
  }

  @Test
  @DisplayName("should_handle_multiple_issues_with_different_confidence_levels")
  void should_handle_multiple_issues_with_different_confidence_levels() {
    final String json =
        """
        {
          "summary": "Code review summary",
          "issues": [
            {
              "file": "Test1.java",
              "start_line": 10,
              "severity": "critical",
              "title": "High confidence issue",
              "suggestion": "Fix immediately",
              "confidenceScore": 0.95,
              "confidenceExplanation": "Clear security vulnerability"
            },
            {
              "file": "Test2.java",
              "start_line": 20,
              "severity": "minor",
              "title": "Medium confidence issue",
              "suggestion": "Consider refactoring",
              "confidenceScore": 0.6,
              "confidenceExplanation": "Possible code smell"
            },
            {
              "file": "Test3.java",
              "start_line": 30,
              "severity": "info",
              "title": "Low confidence issue",
              "suggestion": "Optional improvement",
              "confidenceScore": 0.3,
              "confidenceExplanation": "Stylistic preference"
            }
          ],
          "non_blocking_notes": []
        }
        """;

    final ReviewResult result = parser.parse(json);

    assertThat(result.getIssues()).hasSize(3);
    assertThat(result.getIssues().get(0).getConfidenceScore()).isEqualTo(0.95);
    assertThat(result.getIssues().get(1).getConfidenceScore()).isEqualTo(0.6);
    assertThat(result.getIssues().get(2).getConfidenceScore()).isEqualTo(0.3);
  }
}
