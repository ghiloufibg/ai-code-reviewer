package com.ghiloufi.security.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.security.model.SecurityFinding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Remediation Generator Test")
class RemediationGeneratorTest {

  private RemediationGenerator generator;

  @BeforeEach
  void setUp() {
    generator = new RemediationGenerator();
  }

  @Test
  @DisplayName("should_generate_sql_injection_remediation_with_prepared_statement")
  void should_generate_sql_injection_remediation_with_prepared_statement() {
    final SecurityFinding finding =
        new SecurityFinding(
            "SQL_INJECTION_JDBC",
            "HIGH",
            3,
            "SQL injection vulnerability",
            "Use PreparedStatement",
            "CWE-89",
            "A03:2021-Injection");

    final String vulnerableCode =
        """
        public void getUser(String userId) {
            String query = "SELECT * FROM users WHERE id = '" + userId + "'";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
        }
        """;

    final String remediation = generator.generateRemediation(finding, vulnerableCode);

    assertThat(remediation).contains("PreparedStatement");
    assertThat(remediation).contains("?");
    assertThat(remediation).contains("setString");
    assertThat(remediation).contains("OWASP");
    assertThat(remediation).contains("CWE-89");
  }

  @Test
  @DisplayName("should_generate_xss_remediation_with_escaping")
  void should_generate_xss_remediation_with_escaping() {
    final SecurityFinding finding =
        new SecurityFinding(
            "XSS_REQUEST_PARAMETER",
            "MEDIUM",
            5,
            "Cross-site scripting vulnerability",
            "Escape output",
            "CWE-79",
            "A03:2021-Injection");

    final String vulnerableCode =
        """
        public void displayMessage(HttpServletRequest request, HttpServletResponse response) {
            String message = request.getParameter("msg");
            response.getWriter().write(message);
        }
        """;

    final String remediation = generator.generateRemediation(finding, vulnerableCode);

    assertThat(remediation).containsAnyOf("StringEscapeUtils", "ESAPI", "th:text");
    assertThat(remediation).contains("OWASP");
    assertThat(remediation).contains("CWE-79");
  }

  @Test
  @DisplayName("should_generate_hardcoded_secret_remediation_with_env_variables")
  void should_generate_hardcoded_secret_remediation_with_env_variables() {
    final SecurityFinding finding =
        new SecurityFinding(
            "HARDCODED_PASSWORD",
            "CRITICAL",
            3,
            "Hardcoded password detected",
            "Use environment variables",
            "CWE-798",
            "A02:2021-Cryptographic Failures");

    final String vulnerableCode =
        """
        public class DatabaseConfig {
            private static final String PASSWORD = "hardcoded_password_123";
            private static final String API_KEY = "sk_live_abcdef123456";
        }
        """;

    final String remediation = generator.generateRemediation(finding, vulnerableCode);

    assertThat(remediation).containsAnyOf("System.getenv", "application.properties", "@Value");
    assertThat(remediation).contains("environment");
    assertThat(remediation).contains("OWASP");
    assertThat(remediation).contains("CWE-798");
  }

  @Test
  @DisplayName("should_generate_weak_cipher_remediation_with_aes_gcm")
  void should_generate_weak_cipher_remediation_with_aes_gcm() {
    final SecurityFinding finding =
        new SecurityFinding(
            "WEAK_CIPHER_DES",
            "HIGH",
            4,
            "Weak cipher DES detected",
            "Use AES-256",
            "CWE-327",
            "A02:2021-Cryptographic Failures");

    final String vulnerableCode =
        """
        public byte[] encrypt(String data) throws Exception {
            KeyGenerator keyGen = KeyGenerator.getInstance("DES");
            SecretKey key = keyGen.generateKey();
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(data.getBytes());
        }
        """;

    final String remediation = generator.generateRemediation(finding, vulnerableCode);

    assertThat(remediation).contains("AES");
    assertThat(remediation).contains("GCM");
    assertThat(remediation).contains("SecureRandom");
    assertThat(remediation).contains("OWASP");
    assertThat(remediation).contains("CWE-327");
  }

  @Test
  @DisplayName("should_generate_default_remediation_for_unknown_vulnerability_type")
  void should_generate_default_remediation_for_unknown_vulnerability_type() {
    final SecurityFinding finding =
        new SecurityFinding(
            "UNKNOWN_VULNERABILITY_TYPE",
            "MEDIUM",
            10,
            "Unknown vulnerability",
            "Review code",
            "CWE-9999",
            "A01:2021-Broken Access Control");

    final String code = "public void someMethod() {}";

    final String remediation = generator.generateRemediation(finding, code);

    assertThat(remediation).contains("OWASP");
    assertThat(remediation).contains("A01:2021-Broken Access Control");
  }

  @Test
  @DisplayName("should_handle_null_code_gracefully")
  void should_handle_null_code_gracefully() {
    final SecurityFinding finding =
        new SecurityFinding(
            "SQL_INJECTION", "HIGH", 1, "SQL injection", "Use PreparedStatement", "CWE-89", "A03");

    final String remediation = generator.generateRemediation(finding, null);

    assertThat(remediation).isNotNull();
    assertThat(remediation).isNotEmpty();
  }

  @Test
  @DisplayName("should_include_code_examples_in_remediation")
  void should_include_code_examples_in_remediation() {
    final SecurityFinding finding =
        new SecurityFinding(
            "SQL_INJECTION_JDBC",
            "HIGH",
            5,
            "SQL injection",
            "Use PreparedStatement",
            "CWE-89",
            "A03");

    final String code =
        """
        String query = "SELECT * FROM users WHERE id = '" + userId + "'";
        """;

    final String remediation = generator.generateRemediation(finding, code);

    assertThat(remediation).contains("```java");
    assertThat(remediation).contains("```");
  }

  @Test
  @DisplayName("should_include_references_to_owasp_and_cwe")
  void should_include_references_to_owasp_and_cwe() {
    final SecurityFinding finding =
        new SecurityFinding(
            "XSS_REQUEST_PARAMETER",
            "MEDIUM",
            3,
            "XSS vulnerability",
            "Escape output",
            "CWE-79",
            "A03");

    final String code = "response.getWriter().write(userInput);";

    final String remediation = generator.generateRemediation(finding, code);

    assertThat(remediation).contains("OWASP");
    assertThat(remediation).contains("CWE");
    assertThat(remediation).containsAnyOf("https://", "http://");
  }

  @Test
  @DisplayName("should_handle_multiple_vulnerability_types_in_same_code")
  void should_handle_multiple_vulnerability_types_in_same_code() {
    final String mixedVulnerableCode =
        """
        public void process(String userId, String message) {
            String query = "SELECT * FROM users WHERE id = '" + userId + "'";
            response.getWriter().write(message);
        }
        """;

    final SecurityFinding sqlInjection =
        new SecurityFinding("SQL_INJECTION", "HIGH", 2, "SQL injection", "Fix it", "CWE-89", "A03");

    final SecurityFinding xss =
        new SecurityFinding("XSS_REQUEST", "MEDIUM", 3, "XSS", "Fix it", "CWE-79", "A03");

    final String sqlRemediation = generator.generateRemediation(sqlInjection, mixedVulnerableCode);
    final String xssRemediation = generator.generateRemediation(xss, mixedVulnerableCode);

    assertThat(sqlRemediation).contains("PreparedStatement");
    assertThat(xssRemediation).containsAnyOf("escape", "sanitize", "ESAPI");
  }
}
