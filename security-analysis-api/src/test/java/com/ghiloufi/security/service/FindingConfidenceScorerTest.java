package com.ghiloufi.security.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.security.model.SecurityFinding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Finding Confidence Scorer Test")
class FindingConfidenceScorerTest {

  private FindingConfidenceScorer scorer;

  @BeforeEach
  void setUp() {
    scorer = new FindingConfidenceScorer();
  }

  @Test
  @DisplayName("should_assign_high_confidence_to_critical_severity_with_high_cvss_cwe")
  void should_assign_high_confidence_to_critical_severity_with_high_cvss_cwe() {
    final SecurityFinding finding =
        new SecurityFinding(
            "SQL_INJECTION_JDBC",
            "CRITICAL",
            10,
            "SQL injection vulnerability",
            "Use PreparedStatement",
            "CWE-89",
            "A03:2021-Injection");

    final String code =
        """
        String query = "SELECT * FROM users WHERE id = '" + userId + "'";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);
        """;

    final SecurityFinding enriched = scorer.enrichWithConfidenceScore(finding, code);

    assertThat(enriched.confidence()).isNotNull();
    assertThat(enriched.confidence()).isGreaterThanOrEqualTo(0.8);
    assertThat(enriched.type()).isEqualTo("SQL_INJECTION_JDBC");
    assertThat(enriched.severity()).isEqualTo("CRITICAL");
  }

  @Test
  @DisplayName("should_assign_medium_confidence_to_low_severity_finding")
  void should_assign_medium_confidence_to_low_severity_finding() {
    final SecurityFinding finding =
        new SecurityFinding(
            "POTENTIAL_CODE_SMELL",
            "LOW",
            5,
            "Potential code smell detected",
            "Review code quality",
            "CWE-1234",
            "A01:2021-Broken Access Control");

    final String code = "public void doSomething() { /* code */ }";

    final SecurityFinding enriched = scorer.enrichWithConfidenceScore(finding, code);

    assertThat(enriched.confidence()).isNotNull();
    assertThat(enriched.confidence()).isBetween(0.4, 0.6);
  }

  @Test
  @DisplayName("should_increase_confidence_for_high_cvss_cwe")
  void should_increase_confidence_for_high_cvss_cwe() {
    final SecurityFinding findingWithHighCvssCwe =
        new SecurityFinding(
            "OS_COMMAND_INJECTION",
            "HIGH",
            15,
            "OS command injection detected",
            "Sanitize user input",
            "CWE-78",
            "A03:2021-Injection");

    final SecurityFinding findingWithLowCvssCwe =
        new SecurityFinding(
            "UNUSED_VARIABLE",
            "HIGH",
            15,
            "Unused variable",
            "Remove unused code",
            "CWE-9999",
            "Code Quality");

    final String code = "Runtime.getRuntime().exec(userInput);";

    final SecurityFinding enrichedHigh =
        scorer.enrichWithConfidenceScore(findingWithHighCvssCwe, code);
    final SecurityFinding enrichedLow =
        scorer.enrichWithConfidenceScore(findingWithLowCvssCwe, code);

    assertThat(enrichedHigh.confidence()).isGreaterThan(enrichedLow.confidence());
  }

  @Test
  @DisplayName("should_decrease_confidence_for_safe_patterns")
  void should_decrease_confidence_for_safe_patterns() {
    final SecurityFinding finding =
        new SecurityFinding(
            "SQL_INJECTION_JDBC",
            "HIGH",
            3,
            "Potential SQL injection",
            "Use PreparedStatement",
            "CWE-89",
            "A03:2021-Injection");

    final String safeCode =
        """
        String query = "SELECT * FROM users WHERE id = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, userId);
        """;

    final SecurityFinding enriched = scorer.enrichWithConfidenceScore(finding, safeCode);

    assertThat(enriched.confidence()).isNotNull();
    assertThat(enriched.confidence()).isLessThan(0.8);
  }

  @Test
  @DisplayName("should_cap_confidence_at_1_0")
  void should_cap_confidence_at_1_0() {
    final SecurityFinding finding =
        new SecurityFinding(
            "OS_COMMAND_INJECTION",
            "CRITICAL",
            10,
            "Command injection",
            "Sanitize input",
            "CWE-78",
            "A03:2021-Injection");

    final String code = "Runtime.getRuntime().exec(userInput);";

    final SecurityFinding enriched = scorer.enrichWithConfidenceScore(finding, code);

    assertThat(enriched.confidence()).isLessThanOrEqualTo(1.0);
  }

  @Test
  @DisplayName("should_floor_confidence_at_0_0")
  void should_floor_confidence_at_0_0() {
    final SecurityFinding finding =
        new SecurityFinding(
            "SQL_INJECTION_JDBC",
            "LOW",
            5,
            "Potential issue",
            "Review code",
            "CWE-9999",
            "Unknown");

    final String safeCode =
        """
        String query = "SELECT * FROM users WHERE id = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        """;

    final SecurityFinding enriched = scorer.enrichWithConfidenceScore(finding, safeCode);

    assertThat(enriched.confidence()).isGreaterThanOrEqualTo(0.0);
  }

  @Test
  @DisplayName("should_preserve_all_original_finding_fields")
  void should_preserve_all_original_finding_fields() {
    final SecurityFinding original =
        new SecurityFinding(
            "XSS_REQUEST_PARAMETER",
            "MEDIUM",
            20,
            "Cross-site scripting vulnerability",
            "Escape output",
            "CWE-79",
            "A03:2021-Injection");

    final String code = "response.getWriter().write(userInput);";

    final SecurityFinding enriched = scorer.enrichWithConfidenceScore(original, code);

    assertThat(enriched.type()).isEqualTo(original.type());
    assertThat(enriched.severity()).isEqualTo(original.severity());
    assertThat(enriched.line()).isEqualTo(original.line());
    assertThat(enriched.message()).isEqualTo(original.message());
    assertThat(enriched.recommendation()).isEqualTo(original.recommendation());
    assertThat(enriched.cweId()).isEqualTo(original.cweId());
    assertThat(enriched.owaspCategory()).isEqualTo(original.owaspCategory());
  }

  @Test
  @DisplayName("should_handle_null_code_gracefully")
  void should_handle_null_code_gracefully() {
    final SecurityFinding finding =
        new SecurityFinding(
            "SQL_INJECTION", "HIGH", 1, "SQL injection", "Use PreparedStatement", "CWE-89", "A03");

    final SecurityFinding enriched = scorer.enrichWithConfidenceScore(finding, null);

    assertThat(enriched.confidence()).isNotNull();
    assertThat(enriched.confidence()).isBetween(0.0, 1.0);
  }

  @Test
  @DisplayName("should_assign_different_scores_based_on_severity")
  void should_assign_different_scores_based_on_severity() {
    final SecurityFinding criticalFinding =
        new SecurityFinding(
            "HARDCODED_PASSWORD",
            "CRITICAL",
            5,
            "Hardcoded password",
            "Use environment variables",
            "CWE-798",
            "A02");

    final SecurityFinding mediumFinding =
        new SecurityFinding(
            "HARDCODED_PASSWORD",
            "MEDIUM",
            5,
            "Hardcoded password",
            "Use environment variables",
            "CWE-798",
            "A02");

    final String code = "String password = \"hardcoded123\";";

    final SecurityFinding enrichedCritical =
        scorer.enrichWithConfidenceScore(criticalFinding, code);
    final SecurityFinding enrichedMedium = scorer.enrichWithConfidenceScore(mediumFinding, code);

    assertThat(enrichedCritical.confidence()).isGreaterThan(enrichedMedium.confidence());
  }
}
