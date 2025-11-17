package com.ghiloufi.security.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.security.model.SecurityFinding;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Context-Aware Security Analyzer Test")
class ContextAwareSecurityAnalyzerTest {

  private ContextAwareSecurityAnalyzer analyzer;

  @BeforeEach
  void setUp() {
    analyzer = new ContextAwareSecurityAnalyzer();
  }

  @Test
  @DisplayName("should_not_flag_prepared_statement_as_sql_injection")
  void should_not_flag_prepared_statement_as_sql_injection() {
    final String safeCode =
        """
        public void getUser(String userId) throws SQLException {
            String query = "SELECT * FROM users WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
        }
        """;

    final SecurityFinding sqlInjectionFinding =
        new SecurityFinding(
            "SQL_INJECTION_JDBC",
            "HIGH",
            3,
            "Potential SQL injection",
            "Use PreparedStatement",
            "CWE-89",
            "A03:2021-Injection");

    final List<SecurityFinding> findings = List.of(sqlInjectionFinding);
    final List<SecurityFinding> filtered = analyzer.filterFalsePositives(findings, safeCode);

    assertThat(filtered).isEmpty();
  }

  @Test
  @DisplayName("should_flag_actual_sql_injection_with_string_concatenation")
  void should_flag_actual_sql_injection_with_string_concatenation() {
    final String vulnerableCode =
        """
        public void getUser(String userId) throws SQLException {
            Statement stmt = conn.createStatement();
            String query = "SELECT * FROM users WHERE id = '" + userId + "'";
            ResultSet rs = stmt.executeQuery(query);
        }
        """;

    final SecurityFinding sqlInjectionFinding =
        new SecurityFinding(
            "SQL_INJECTION_JDBC",
            "HIGH",
            3,
            "SQL injection via string concatenation",
            "Use PreparedStatement",
            "CWE-89",
            "A03:2021-Injection");

    final List<SecurityFinding> findings = List.of(sqlInjectionFinding);
    final List<SecurityFinding> filtered = analyzer.filterFalsePositives(findings, vulnerableCode);

    assertThat(filtered).hasSize(1);
    assertThat(filtered.get(0).type()).contains("SQL_INJECTION");
  }

  @Test
  @DisplayName("should_not_flag_framework_protected_xss")
  void should_not_flag_framework_protected_xss() {
    final String safeThymeleafCode =
        """
        <!DOCTYPE html>
        <html xmlns:th="http://www.thymeleaf.org">
        <body>
            <p th:text="${userInput}">Default text</p>
        </body>
        </html>
        """;

    final SecurityFinding xssFinding =
        new SecurityFinding(
            "XSS_REQUEST_PARAMETER_TO_JSP_WRITER",
            "MEDIUM",
            4,
            "Potential XSS vulnerability",
            "Escape output",
            "CWE-79",
            "A03:2021-Injection");

    final List<SecurityFinding> findings = List.of(xssFinding);
    final List<SecurityFinding> filtered =
        analyzer.filterFalsePositives(findings, safeThymeleafCode);

    assertThat(filtered).isEmpty();
  }

  @Test
  @DisplayName("should_not_flag_hardcoded_secrets_in_test_code")
  void should_not_flag_hardcoded_secrets_in_test_code() {
    final String testCode =
        """
        @Test
        public void testAuthentication() {
            String testApiKey = "test-api-key-12345";
            AuthService service = new AuthService(testApiKey);
            assertThat(service.isValid()).isTrue();
        }
        """;

    final SecurityFinding hardcodedSecretFinding =
        new SecurityFinding(
            "HARDCODED_CREDENTIAL",
            "HIGH",
            3,
            "Hardcoded API key detected",
            "Use environment variables",
            "CWE-798",
            "A02:2021-Cryptographic Failures");

    final List<SecurityFinding> findings = List.of(hardcodedSecretFinding);
    final List<SecurityFinding> filtered = analyzer.filterFalsePositives(findings, testCode);

    assertThat(filtered).isEmpty();
  }

  @Test
  @DisplayName("should_flag_hardcoded_secrets_in_production_code")
  void should_flag_hardcoded_secrets_in_production_code() {
    final String productionCode =
        """
        @Service
        public class PaymentService {
            private static final String API_KEY = "sk_live_1234567890abcdef";

            public void processPayment(Payment payment) {
                // Use API_KEY for payment processing
            }
        }
        """;

    final SecurityFinding hardcodedSecretFinding =
        new SecurityFinding(
            "HARDCODED_CREDENTIAL",
            "CRITICAL",
            3,
            "Hardcoded production API key",
            "Use environment variables",
            "CWE-798",
            "A02:2021-Cryptographic Failures");

    final List<SecurityFinding> findings = List.of(hardcodedSecretFinding);
    final List<SecurityFinding> filtered = analyzer.filterFalsePositives(findings, productionCode);

    assertThat(filtered).hasSize(1);
    assertThat(filtered.get(0).type()).contains("HARDCODED");
  }

  @Test
  @DisplayName("should_preserve_non_false_positive_findings")
  void should_preserve_non_false_positive_findings() {
    final String codeWithMultipleIssues =
        """
        public class SecurityIssues {
            public void weakCrypto() throws Exception {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] hash = md.digest("password".getBytes());
            }
        }
        """;

    final SecurityFinding weakCryptoFinding =
        new SecurityFinding(
            "WEAK_MESSAGE_DIGEST_MD5",
            "MEDIUM",
            3,
            "Weak cryptographic hash MD5",
            "Use SHA-256 or stronger",
            "CWE-328",
            "A02:2021-Cryptographic Failures");

    final List<SecurityFinding> findings = List.of(weakCryptoFinding);
    final List<SecurityFinding> filtered =
        analyzer.filterFalsePositives(findings, codeWithMultipleIssues);

    assertThat(filtered).hasSize(1);
    assertThat(filtered.get(0).type()).contains("WEAK_MESSAGE_DIGEST");
  }

  @Test
  @DisplayName("should_handle_empty_findings_list")
  void should_handle_empty_findings_list() {
    final String code = "public class Empty {}";
    final List<SecurityFinding> findings = List.of();
    final List<SecurityFinding> filtered = analyzer.filterFalsePositives(findings, code);

    assertThat(filtered).isEmpty();
  }

  @Test
  @DisplayName("should_handle_null_code_gracefully")
  void should_handle_null_code_gracefully() {
    final SecurityFinding finding =
        new SecurityFinding(
            "SQL_INJECTION", "HIGH", 1, "Test", "Fix it", "CWE-89", "A03:2021-Injection");

    final List<SecurityFinding> findings = List.of(finding);
    final List<SecurityFinding> filtered = analyzer.filterFalsePositives(findings, null);

    assertThat(filtered).hasSize(1);
  }
}
