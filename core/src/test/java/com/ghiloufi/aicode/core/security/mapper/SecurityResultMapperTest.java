package com.ghiloufi.aicode.core.security.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.security.detector.SensitiveFileDetector;
import com.ghiloufi.aicode.core.security.model.SecurityIssue;
import com.ghiloufi.aicode.core.security.model.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SecurityResultMapper Tests")
final class SecurityResultMapperTest {

  private SecurityResultMapper mapper;
  private SensitiveFileDetector fileDetector;

  @BeforeEach
  final void setUp() {
    fileDetector = new SensitiveFileDetector();
    mapper = new SecurityResultMapper(fileDetector);
  }

  @Test
  @DisplayName("should_map_critical_security_issue_to_review_issue")
  final void should_map_critical_issue() {
    final SecurityIssue securityIssue =
        SecurityIssue.builder()
            .severity(Severity.CRITICAL)
            .category("SQL_INJECTION")
            .description("SQL injection vulnerability detected")
            .recommendation("Use PreparedStatement")
            .build();

    final ReviewResult.Issue reviewIssue = mapper.toReviewIssue(securityIssue, "Test.java", 42);

    assertThat(reviewIssue.file).isEqualTo("Test.java");
    assertThat(reviewIssue.start_line).isEqualTo(42);
    assertThat(reviewIssue.severity).isEqualTo("critical");
    assertThat(reviewIssue.title).isEqualTo("[Security] SQL_INJECTION");
    assertThat(reviewIssue.suggestion).contains("SQL injection vulnerability detected");
    assertThat(reviewIssue.suggestion).contains("Use PreparedStatement");
    assertThat(reviewIssue.confidenceScore).isEqualTo(0.95);
    assertThat(reviewIssue.suggestedFix).isEqualTo("Use PreparedStatement");
  }

  @Test
  @DisplayName("should_map_high_severity_to_high_string")
  final void should_map_high_severity() {
    final SecurityIssue securityIssue =
        SecurityIssue.builder()
            .severity(Severity.HIGH)
            .category("REFLECTION_ABUSE")
            .description("Reflection abuse detected")
            .recommendation("Avoid reflection")
            .build();

    final ReviewResult.Issue reviewIssue = mapper.toReviewIssue(securityIssue, "Test.java");

    assertThat(reviewIssue.severity).isEqualTo("high");
    assertThat(reviewIssue.confidenceScore).isEqualTo(0.85);
  }

  @Test
  @DisplayName("should_map_medium_severity_to_medium_string")
  final void should_map_medium_severity() {
    final SecurityIssue securityIssue =
        SecurityIssue.builder()
            .severity(Severity.MEDIUM)
            .category("INSECURE_RANDOM")
            .description("Insecure random detected")
            .recommendation("Use SecureRandom")
            .build();

    final ReviewResult.Issue reviewIssue = mapper.toReviewIssue(securityIssue, "Test.java");

    assertThat(reviewIssue.severity).isEqualTo("medium");
    assertThat(reviewIssue.confidenceScore).isEqualTo(0.75);
  }

  @Test
  @DisplayName("should_map_low_severity_to_low_string")
  final void should_map_low_severity() {
    final SecurityIssue securityIssue =
        SecurityIssue.builder()
            .severity(Severity.LOW)
            .category("CODE_QUALITY")
            .description("Code quality issue")
            .recommendation("Improve code")
            .build();

    final ReviewResult.Issue reviewIssue = mapper.toReviewIssue(securityIssue, "Test.java");

    assertThat(reviewIssue.severity).isEqualTo("low");
    assertThat(reviewIssue.confidenceScore).isEqualTo(0.65);
  }

  @Test
  @DisplayName("should_map_info_severity_to_info_string")
  final void should_map_info_severity() {
    final SecurityIssue securityIssue =
        SecurityIssue.builder()
            .severity(Severity.INFO)
            .category("INFORMATION")
            .description("Informational message")
            .recommendation("Consider this")
            .build();

    final ReviewResult.Issue reviewIssue = mapper.toReviewIssue(securityIssue, "Test.java");

    assertThat(reviewIssue.severity).isEqualTo("info");
    assertThat(reviewIssue.confidenceScore).isEqualTo(0.50);
  }

  @Test
  @DisplayName("should_increase_confidence_for_sensitive_files")
  final void should_increase_confidence_for_sensitive_files() {
    final SecurityIssue securityIssue =
        SecurityIssue.builder()
            .severity(Severity.MEDIUM)
            .category("HARDCODED_CREDENTIALS")
            .description("Hardcoded password detected")
            .recommendation("Use environment variables")
            .build();

    final ReviewResult.Issue reviewIssue =
        mapper.toReviewIssue(securityIssue, "application.properties");

    assertThat(reviewIssue.confidenceScore).isEqualTo(0.95);
    assertThat(reviewIssue.confidenceExplanation).contains("sensitive");
  }

  @Test
  @DisplayName("should_format_title_with_security_prefix")
  final void should_format_title_correctly() {
    final SecurityIssue securityIssue =
        SecurityIssue.builder()
            .severity(Severity.CRITICAL)
            .category("COMMAND_INJECTION")
            .description("Command injection detected")
            .recommendation("Sanitize input")
            .build();

    final ReviewResult.Issue reviewIssue = mapper.toReviewIssue(securityIssue, "Test.java");

    assertThat(reviewIssue.title).isEqualTo("[Security] COMMAND_INJECTION");
  }

  @Test
  @DisplayName("should_format_suggestion_with_description_and_recommendation")
  final void should_format_suggestion_correctly() {
    final SecurityIssue securityIssue =
        SecurityIssue.builder()
            .severity(Severity.HIGH)
            .category("PATH_TRAVERSAL")
            .description("Path traversal vulnerability")
            .recommendation("Validate and normalize paths")
            .build();

    final ReviewResult.Issue reviewIssue = mapper.toReviewIssue(securityIssue, "Test.java");

    assertThat(reviewIssue.suggestion).contains("**Security Issue Detected**");
    assertThat(reviewIssue.suggestion).contains("Path traversal vulnerability");
    assertThat(reviewIssue.suggestion).contains("**Recommendation:**");
    assertThat(reviewIssue.suggestion).contains("Validate and normalize paths");
  }

  @Test
  @DisplayName("should_handle_missing_recommendation_gracefully")
  final void should_handle_missing_recommendation() {
    final SecurityIssue securityIssue =
        SecurityIssue.builder()
            .severity(Severity.HIGH)
            .category("SECURITY_ISSUE")
            .description("Security issue detected")
            .build();

    final ReviewResult.Issue reviewIssue = mapper.toReviewIssue(securityIssue, "Test.java");

    assertThat(reviewIssue.suggestion).contains("Security issue detected");
    assertThat(reviewIssue.suggestion).doesNotContain("**Recommendation:**");
  }

  @Test
  @DisplayName("should_set_confidence_explanation_for_regular_files")
  final void should_set_confidence_explanation_for_regular_files() {
    final SecurityIssue securityIssue =
        SecurityIssue.builder()
            .severity(Severity.HIGH)
            .category("SECURITY_ISSUE")
            .description("Security issue detected")
            .recommendation("Fix it")
            .build();

    final ReviewResult.Issue reviewIssue = mapper.toReviewIssue(securityIssue, "Test.java");

    assertThat(reviewIssue.confidenceExplanation)
        .isEqualTo("Security issue detected with high severity.");
  }

  @Test
  @DisplayName("should_set_line_number_to_zero_when_not_provided")
  final void should_default_line_number_to_zero() {
    final SecurityIssue securityIssue =
        SecurityIssue.builder()
            .severity(Severity.CRITICAL)
            .category("SQL_INJECTION")
            .description("SQL injection vulnerability")
            .recommendation("Use PreparedStatement")
            .build();

    final ReviewResult.Issue reviewIssue = mapper.toReviewIssue(securityIssue, "Test.java");

    assertThat(reviewIssue.start_line).isEqualTo(0);
  }

  @Test
  @DisplayName("should_detect_sensitive_file_by_extension")
  final void should_detect_sensitive_file_by_extension() {
    final SecurityIssue securityIssue =
        SecurityIssue.builder()
            .severity(Severity.MEDIUM)
            .category("HARDCODED_CREDENTIALS")
            .description("Hardcoded credentials")
            .recommendation("Use secure storage")
            .build();

    final ReviewResult.Issue reviewIssue = mapper.toReviewIssue(securityIssue, "config.yml");

    assertThat(reviewIssue.confidenceScore).isEqualTo(0.95);
    assertThat(reviewIssue.confidenceExplanation)
        .contains("File has sensitive extension (config, credentials, keys)");
  }

  @Test
  @DisplayName("should_detect_sensitive_file_by_path")
  final void should_detect_sensitive_file_by_path() {
    final SecurityIssue securityIssue =
        SecurityIssue.builder()
            .severity(Severity.LOW)
            .category("HARDCODED_CREDENTIALS")
            .description("Hardcoded credentials")
            .recommendation("Use secure storage")
            .build();

    final ReviewResult.Issue reviewIssue =
        mapper.toReviewIssue(securityIssue, "/security/auth.txt");

    assertThat(reviewIssue.confidenceScore).isEqualTo(0.95);
    assertThat(reviewIssue.confidenceExplanation)
        .contains("File is in a sensitive directory (config, security, auth, credentials)");
  }
}
