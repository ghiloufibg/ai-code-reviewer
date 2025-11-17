package com.ghiloufi.aicode.core.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.security.config.DangerousPatternsConfig;
import com.ghiloufi.aicode.core.security.config.SafePatternsConfig;
import com.ghiloufi.aicode.core.security.detector.SensitiveFileDetector;
import com.ghiloufi.aicode.core.security.model.SafePattern;
import com.ghiloufi.aicode.core.security.model.SecurityIssue;
import com.ghiloufi.aicode.core.security.model.Severity;
import com.ghiloufi.aicode.core.security.validator.PatternValidator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Pattern Validation Integration Tests")
final class PatternValidationIntegrationTest {

  private PatternValidator patternValidator;
  private SensitiveFileDetector fileDetector;
  private List<SafePattern> safePatterns;

  @BeforeEach
  final void setUp() {
    final DangerousPatternsConfig dangerousConfig = new DangerousPatternsConfig();
    final SafePatternsConfig safeConfig = new SafePatternsConfig();

    patternValidator = new PatternValidator(dangerousConfig.dangerousPatterns());
    fileDetector = new SensitiveFileDetector();
    safePatterns = safeConfig.safePatterns();
  }

  @Test
  @DisplayName("should_detect_multiple_vulnerability_patterns_in_vulnerable_code")
  final void should_detect_multiple_vulnerabilities() {
    final String vulnerableCode =
        """
        public class VulnerableService {
            private String dbPassword = "HardcodedPass123!";
            private String apiKey = "sk-1234567890abcdefghijklmnop";

            public void executeQuery(String userId) {
                String query = "SELECT * FROM users WHERE id = " + userId;
                statement.executeQuery(query);
            }

            public void deserialize(InputStream input) throws Exception {
                ObjectInputStream ois = new ObjectInputStream(input);
                Object obj = ois.readObject();
            }

            public void encrypt() {
                Cipher cipher = Cipher.getInstance("DES");
                MessageDigest md = MessageDigest.getInstance("MD5");
            }
        }
        """;

    final List<SecurityIssue> issues = patternValidator.validateCode(vulnerableCode);

    assertThat(issues).hasSizeGreaterThanOrEqualTo(4);

    assertThat(issues.stream().anyMatch(i -> i.category().equals("HARDCODED_CREDENTIALS")))
        .isTrue();
    assertThat(issues.stream().anyMatch(i -> i.category().equals("SQL_INJECTION"))).isTrue();
    assertThat(issues.stream().anyMatch(i -> i.category().equals("UNSAFE_DESERIALIZATION")))
        .isTrue();
    assertThat(issues.stream().anyMatch(i -> i.category().equals("WEAK_CRYPTO"))).isTrue();

    final long criticalCount =
        issues.stream().filter(i -> i.severity() == Severity.CRITICAL).count();
    assertThat(criticalCount).isGreaterThanOrEqualTo(2);
  }

  @Test
  @DisplayName("should_not_flag_code_using_safe_patterns")
  final void should_not_flag_safe_patterns() {
    final String safeCode =
        """
        public class SecureService {
            public void executeQuery(String userId) {
                String query = "SELECT * FROM users WHERE id = ?";
                PreparedStatement ps = connection.prepareStatement(query);
                ps.setString(1, userId);
                ResultSet rs = ps.executeQuery();
            }

            public void generateToken() {
                SecureRandom random = new SecureRandom();
                byte[] token = new byte[32];
                random.nextBytes(token);
            }

            public void encrypt() {
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                MessageDigest md = MessageDigest.getInstance("SHA-256");
            }

            public void sanitizeInput(String userInput) {
                String clean = validate(userInput);
                return clean;
            }
        }
        """;

    final List<SecurityIssue> issues = patternValidator.validateCode(safeCode);

    assertThat(issues).isEmpty();

    final boolean usesSafePatterns =
        safePatterns.stream().anyMatch(pattern -> pattern.matches(safeCode));
    assertThat(usesSafePatterns).isTrue();
  }

  @Test
  @DisplayName("should_combine_pattern_validation_with_file_sensitivity_detection")
  final void should_combine_pattern_and_file_detection() {
    final String configFileCode =
        """
        database.password=HardcodedPassword123
        api.key=sk-1234567890abcdefghijklmnop
        """;

    final String configFilePath = "application.properties";

    final List<SecurityIssue> issues = patternValidator.validateCode(configFileCode);
    final boolean isSensitiveFile = fileDetector.isSensitiveFile(configFilePath);
    final double requiredConfidence = fileDetector.getRequiredConfidenceThreshold(configFilePath);

    assertThat(issues).isNotEmpty();
    assertThat(isSensitiveFile).isTrue();
    assertThat(requiredConfidence).isEqualTo(0.95);
  }

  @Test
  @DisplayName("should_detect_xml_vulnerabilities")
  final void should_detect_xml_vulnerabilities() {
    final String xmlVulnerableCode =
        """
        public class XmlParser {
            public Document parse(InputStream xml) throws Exception {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                return db.parse(xml);
            }

            public void executeXPath(String userName) throws Exception {
                String xpathQuery = "/users/user[@name='" + userName + "']";
                XPath.compile(xpathQuery);
            }
        }
        """;

    final List<SecurityIssue> issues = patternValidator.validateCode(xmlVulnerableCode);

    assertThat(issues).hasSizeGreaterThanOrEqualTo(2);
    assertThat(issues.stream().anyMatch(i -> i.category().equals("XXE_VULNERABILITY"))).isTrue();
    assertThat(issues.stream().anyMatch(i -> i.category().equals("XPATH_INJECTION"))).isTrue();
  }

  @Test
  @DisplayName("should_provide_actionable_recommendations_for_all_issues")
  final void should_provide_actionable_recommendations() {
    final String vulnerableCode =
        """
        String password = "MyHardcodedPassword123";
        Cipher cipher = Cipher.getInstance("DES");
        Random random = new Random();
        """;

    final List<SecurityIssue> issues = patternValidator.validateCode(vulnerableCode);

    assertThat(issues).isNotEmpty();
    assertThat(issues.stream().allMatch(i -> i.recommendation() != null)).isTrue();
    assertThat(issues.stream().allMatch(i -> i.recommendation().length() > 20)).isTrue();
    assertThat(
            issues.stream()
                .anyMatch(
                    i ->
                        i.recommendation().contains("Use") || i.recommendation().contains("Avoid")))
        .isTrue();
  }

  @Test
  @DisplayName("should_detect_issues_with_line_numbers_for_better_reporting")
  final void should_detect_with_line_numbers() {
    final String multiLineCode =
        """
        public class Test {
            private String line2 = "safe";
            private String password = "HardcodedPass123!";
            private String line4 = "safe";
            public void query(String id) {
                executeQuery("SELECT * FROM users WHERE id = " + id);
            }
        }
        """;

    final List<SecurityIssue> issues = patternValidator.validateCodeWithLineNumbers(multiLineCode);

    assertThat(issues).hasSizeGreaterThanOrEqualTo(2);
    assertThat(issues.stream().anyMatch(i -> i.description().contains("line 3"))).isTrue();
    assertThat(issues.stream().anyMatch(i -> i.description().contains("line 6"))).isTrue();
  }

  @Test
  @DisplayName("should_calculate_risk_score_from_multiple_patterns")
  final void should_calculate_risk_score() {
    final String mixedVulnerabilitiesCode =
        """
        String apiKey = "sk-hardcoded123456789";
        executeQuery("SELECT * FROM users WHERE id = " + userId);
        Random random = new Random();
        """;

    final List<SecurityIssue> issues = patternValidator.validateCode(mixedVulnerabilitiesCode);

    final double totalRiskScore = issues.stream().mapToDouble(i -> i.severity().getWeight()).sum();

    assertThat(issues).hasSizeGreaterThanOrEqualTo(2);
    assertThat(totalRiskScore).isGreaterThan(10.0);
  }

  @Test
  @DisplayName("should_handle_edge_cases_gracefully")
  final void should_handle_edge_cases() {
    assertThat(patternValidator.validateCode(null)).isEmpty();
    assertThat(patternValidator.validateCode("")).isEmpty();
    assertThat(patternValidator.validateCode("   ")).isEmpty();

    final String commentOnlyCode =
        """
        // This is a comment
        /* Multi-line comment */
        """;
    final List<SecurityIssue> commentIssues = patternValidator.validateCode(commentOnlyCode);
    assertThat(commentIssues).isEmpty();
  }

  @Test
  @DisplayName("should_differentiate_severity_levels_correctly")
  final void should_differentiate_severity_levels() {
    final String codeWithVariousSeverities =
        """
        String password = "HardcodedPass123!";
        Random random = new Random();
        ObjectInputStream ois = new ObjectInputStream(input);
        """;

    final List<SecurityIssue> issues = patternValidator.validateCode(codeWithVariousSeverities);

    final long criticalCount =
        issues.stream().filter(i -> i.severity() == Severity.CRITICAL).count();
    final long highCount = issues.stream().filter(i -> i.severity() == Severity.HIGH).count();
    final long mediumCount = issues.stream().filter(i -> i.severity() == Severity.MEDIUM).count();

    assertThat(criticalCount).isGreaterThan(0);
    assertThat(highCount + mediumCount).isGreaterThan(0);
  }
}
