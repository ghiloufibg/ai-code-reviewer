package com.ghiloufi.aicode.core.security.validator;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.security.config.DangerousPatternsConfig;
import com.ghiloufi.aicode.core.security.model.SecurityIssue;
import com.ghiloufi.aicode.core.security.model.Severity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PatternValidator Tests")
final class PatternValidatorTest {

  private PatternValidator validator;

  @BeforeEach
  final void setUp() {
    final DangerousPatternsConfig config = new DangerousPatternsConfig();
    validator = new PatternValidator(config.dangerousPatterns());
  }

  @Test
  @DisplayName("should_detect_sql_injection_vulnerability")
  final void should_detect_sql_injection() {
    final String vulnerableCode =
        """
        String query = "SELECT * FROM users WHERE id = " + userId;
        statement.executeQuery(query);
        """;

    final List<SecurityIssue> issues = validator.validateCode(vulnerableCode);

    assertThat(issues).hasSizeGreaterThanOrEqualTo(1);
    assertThat(issues.stream().anyMatch(i -> i.category().equals("SQL_INJECTION"))).isTrue();
    assertThat(
            issues.stream()
                .filter(i -> i.category().equals("SQL_INJECTION"))
                .findFirst()
                .get()
                .severity())
        .isEqualTo(Severity.CRITICAL);
  }

  @Test
  @DisplayName("should_detect_hardcoded_credentials")
  final void should_detect_hardcoded_credentials() {
    final String vulnerableCode =
        """
        String password = "MySecretPassword123!";
        String apiKey = "sk-1234567890abcdefghijklmnop";
        """;

    final List<SecurityIssue> issues = validator.validateCode(vulnerableCode);

    assertThat(issues).hasSizeGreaterThanOrEqualTo(1);
    assertThat(issues.stream().anyMatch(i -> i.category().equals("HARDCODED_CREDENTIALS")))
        .isTrue();
    assertThat(issues.stream().allMatch(i -> i.severity() == Severity.CRITICAL)).isTrue();
  }

  @Test
  @DisplayName("should_detect_eval_usage")
  final void should_detect_eval_usage() {
    final String vulnerableCode =
        """
        String userCode = request.getParameter("code");
        eval(userCode);
        """;

    final List<SecurityIssue> issues = validator.validateCode(vulnerableCode);

    assertThat(issues).hasSizeGreaterThanOrEqualTo(1);
    assertThat(issues.stream().anyMatch(i -> i.category().equals("EVAL_USAGE"))).isTrue();
    assertThat(
            issues.stream()
                .filter(i -> i.category().equals("EVAL_USAGE"))
                .findFirst()
                .get()
                .severity())
        .isEqualTo(Severity.CRITICAL);
  }

  @Test
  @DisplayName("should_detect_unsafe_deserialization")
  final void should_detect_unsafe_deserialization() {
    final String vulnerableCode =
        """
        ObjectInputStream ois = new ObjectInputStream(inputStream);
        Object obj = ois.readObject();
        """;

    final List<SecurityIssue> issues = validator.validateCode(vulnerableCode);

    assertThat(issues).hasSizeGreaterThanOrEqualTo(1);
    assertThat(issues.stream().anyMatch(i -> i.category().equals("UNSAFE_DESERIALIZATION")))
        .isTrue();
    assertThat(
            issues.stream()
                .filter(i -> i.category().equals("UNSAFE_DESERIALIZATION"))
                .findFirst()
                .get()
                .severity())
        .isEqualTo(Severity.HIGH);
  }

  @Test
  @DisplayName("should_detect_weak_cryptography")
  final void should_detect_weak_crypto() {
    final String vulnerableCode =
        """
        Cipher cipher = Cipher.getInstance("DES");
        MessageDigest md = MessageDigest.getInstance("MD5");
        """;

    final List<SecurityIssue> issues = validator.validateCode(vulnerableCode);

    assertThat(issues).hasSizeGreaterThanOrEqualTo(1);
    assertThat(issues.stream().anyMatch(i -> i.category().equals("WEAK_CRYPTO"))).isTrue();
  }

  @Test
  @DisplayName("should_detect_insecure_random")
  final void should_detect_insecure_random() {
    final String vulnerableCode =
        """
        Random random = new Random();
        int sessionId = random.nextInt();
        """;

    final List<SecurityIssue> issues = validator.validateCode(vulnerableCode);

    assertThat(issues).hasSizeGreaterThanOrEqualTo(1);
    assertThat(issues.stream().anyMatch(i -> i.category().equals("INSECURE_RANDOM"))).isTrue();
    assertThat(
            issues.stream()
                .filter(i -> i.category().equals("INSECURE_RANDOM"))
                .findFirst()
                .get()
                .severity())
        .isEqualTo(Severity.MEDIUM);
  }

  @Test
  @DisplayName("should_detect_xxe_vulnerability")
  final void should_detect_xxe_vulnerability() {
    final String vulnerableCode =
        """
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        """;

    final List<SecurityIssue> issues = validator.validateCode(vulnerableCode);

    assertThat(issues).hasSizeGreaterThanOrEqualTo(1);
    assertThat(issues.stream().anyMatch(i -> i.category().equals("XXE_VULNERABILITY"))).isTrue();
  }

  @Test
  @DisplayName("should_detect_xpath_injection")
  final void should_detect_xpath_injection() {
    final String vulnerableCode =
        """
        String xpathQuery = "/users/user[@name='" + userName + "']";
        XPath.compile(xpathQuery);
        """;

    final List<SecurityIssue> issues = validator.validateCode(vulnerableCode);

    assertThat(issues).hasSizeGreaterThanOrEqualTo(1);
    assertThat(issues.stream().anyMatch(i -> i.category().equals("XPATH_INJECTION"))).isTrue();
    assertThat(
            issues.stream()
                .filter(i -> i.category().equals("XPATH_INJECTION"))
                .findFirst()
                .get()
                .severity())
        .isEqualTo(Severity.CRITICAL);
  }

  @Test
  @DisplayName("should_detect_null_cipher")
  final void should_detect_null_cipher() {
    final String vulnerableCode =
        """
        Cipher cipher = Cipher.getInstance("NULL");
        """;

    final List<SecurityIssue> issues = validator.validateCode(vulnerableCode);

    assertThat(issues).hasSizeGreaterThanOrEqualTo(1);
    assertThat(issues.stream().anyMatch(i -> i.category().equals("NULL_CIPHER"))).isTrue();
    assertThat(
            issues.stream()
                .filter(i -> i.category().equals("NULL_CIPHER"))
                .findFirst()
                .get()
                .severity())
        .isEqualTo(Severity.CRITICAL);
  }

  @Test
  @DisplayName("should_not_detect_issues_in_safe_code")
  final void should_not_detect_issues_in_safe_code() {
    final String safeCode =
        """
        PreparedStatement ps = connection.prepareStatement("SELECT * FROM users WHERE id = ?");
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        """;

    final List<SecurityIssue> issues = validator.validateCode(safeCode);

    assertThat(issues).isEmpty();
  }

  @Test
  @DisplayName("should_provide_recommendations_for_all_issues")
  final void should_provide_recommendations() {
    final String vulnerableCode =
        """
        String password = "HardcodedPass123";
        """;

    final List<SecurityIssue> issues = validator.validateCode(vulnerableCode);

    assertThat(issues).isNotEmpty();
    assertThat(issues.stream().allMatch(i -> i.recommendation() != null)).isTrue();
    assertThat(issues.stream().allMatch(i -> !i.recommendation().isBlank())).isTrue();
  }

  @Test
  @DisplayName("should_include_line_numbers_when_requested")
  final void should_include_line_numbers() {
    final String vulnerableCode =
        """
        String line1 = "safe";
        String password = "HardcodedPass123";
        String line3 = "safe";
        """;

    final List<SecurityIssue> issues = validator.validateCodeWithLineNumbers(vulnerableCode);

    assertThat(issues).hasSizeGreaterThanOrEqualTo(1);
    assertThat(
            issues.stream()
                .filter(i -> i.category().equals("HARDCODED_CREDENTIALS"))
                .findFirst()
                .get()
                .description())
        .contains("line 2");
  }

  @Test
  @DisplayName("should_handle_empty_code_gracefully")
  final void should_handle_empty_code() {
    final List<SecurityIssue> issues = validator.validateCode("");

    assertThat(issues).isEmpty();
  }

  @Test
  @DisplayName("should_handle_null_code_gracefully")
  final void should_handle_null_code() {
    final List<SecurityIssue> issues = validator.validateCode(null);

    assertThat(issues).isEmpty();
  }
}
