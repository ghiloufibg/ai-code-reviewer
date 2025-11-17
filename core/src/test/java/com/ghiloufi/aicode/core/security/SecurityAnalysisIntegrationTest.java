package com.ghiloufi.aicode.core.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.security.analyzer.CodeInjectionDetector;
import com.ghiloufi.aicode.core.security.analyzer.CommandInjectionDetector;
import com.ghiloufi.aicode.core.security.analyzer.PathTraversalDetector;
import com.ghiloufi.aicode.core.security.analyzer.ReflectionAbuseDetector;
import com.ghiloufi.aicode.core.security.config.DangerousPatternsConfig;
import com.ghiloufi.aicode.core.security.detector.SensitiveFileDetector;
import com.ghiloufi.aicode.core.security.mapper.SecurityResultMapper;
import com.ghiloufi.aicode.core.security.service.SecurityAnalysisService;
import com.ghiloufi.aicode.core.security.validator.PatternValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Security Analysis Integration Tests")
final class SecurityAnalysisIntegrationTest {

  private SecurityAnalysisService service;
  private SensitiveFileDetector fileDetector;

  @BeforeEach
  final void setUp() {
    final DangerousPatternsConfig dangerousConfig = new DangerousPatternsConfig();
    final CommandInjectionDetector commandInjectionDetector = new CommandInjectionDetector();
    final ReflectionAbuseDetector reflectionAbuseDetector = new ReflectionAbuseDetector();
    final CodeInjectionDetector codeInjectionDetector = new CodeInjectionDetector();
    final PathTraversalDetector pathTraversalDetector = new PathTraversalDetector();
    final PatternValidator patternValidator =
        new PatternValidator(dangerousConfig.dangerousPatterns());
    fileDetector = new SensitiveFileDetector();
    final SecurityResultMapper resultMapper = new SecurityResultMapper(fileDetector);

    service =
        new SecurityAnalysisService(
            commandInjectionDetector,
            reflectionAbuseDetector,
            codeInjectionDetector,
            pathTraversalDetector,
            patternValidator,
            resultMapper);
  }

  @Test
  @DisplayName("should_perform_complete_security_analysis_on_vulnerable_code")
  final void should_perform_complete_analysis() {
    final String vulnerableCode =
        """
        public class VulnerableService {
            private String dbPassword = "HardcodedPass123!";
            private String apiKey = "sk-1234567890abcdefghijklmnop";

            public void executeQuery(String userId) {
                String query = "SELECT * FROM users WHERE id = " + userId;
                statement.executeQuery(query);
            }

            public void executeCommand(String input) {
                Runtime.getRuntime().exec("cmd " + input);
            }

            public void loadClass(String className) throws Exception {
                Class<?> clazz = Class.forName(className);
                Object instance = clazz.getDeclaredConstructor().newInstance();
            }

            public void encrypt() {
                Cipher cipher = Cipher.getInstance("DES");
                MessageDigest md = MessageDigest.getInstance("MD5");
            }

            public void generateToken() {
                Random random = new Random();
                byte[] token = new byte[32];
                random.nextBytes(token);
            }
        }
        """;

    final ReviewResult result = service.analyzeCode(vulnerableCode, "VulnerableService.java");

    assertThat(result.summary).isEqualTo("Security Analysis");
    assertThat(result.issues).hasSizeGreaterThanOrEqualTo(5);

    assertThat(result.issues.stream().anyMatch(i -> i.title.contains("HARDCODED_CREDENTIALS")))
        .isTrue();
    assertThat(result.issues.stream().anyMatch(i -> i.title.contains("SQL_INJECTION"))).isTrue();
    assertThat(result.issues.stream().anyMatch(i -> i.title.contains("COMMAND_INJECTION")))
        .isTrue();
    assertThat(
            result.issues.stream()
                .anyMatch(
                    i ->
                        i.title.contains("REFLECTION_ABUSE")
                            || i.title.contains("REFLECTION_USAGE")))
        .isTrue();
    assertThat(result.issues.stream().anyMatch(i -> i.title.contains("WEAK_CRYPTO"))).isTrue();

    assertThat(result.issues.stream().allMatch(i -> i.file.equals("VulnerableService.java")))
        .isTrue();
    assertThat(result.issues.stream().allMatch(i -> i.severity != null)).isTrue();
    assertThat(result.issues.stream().allMatch(i -> i.confidenceScore != null)).isTrue();
    assertThat(result.issues.stream().allMatch(i -> i.suggestedFix != null)).isTrue();
  }

  @Test
  @DisplayName("should_apply_higher_confidence_to_sensitive_files")
  final void should_apply_higher_confidence_to_sensitive_files() {
    final String configCode =
        """
        database.password=HardcodedPassword123
        api.key=sk-1234567890abcdefghijklmnop
        """;

    final ReviewResult result = service.analyzeCode(configCode, "application.properties");

    assertThat(result.issues).isNotEmpty();
    assertThat(result.issues.stream().allMatch(i -> i.confidenceScore >= 0.95)).isTrue();
    assertThat(
            result.issues.stream()
                .allMatch(
                    i ->
                        i.confidenceExplanation.contains("sensitive")
                            || i.confidenceExplanation.contains("configuration")))
        .isTrue();
  }

  @Test
  @DisplayName("should_handle_mixed_safe_and_unsafe_code")
  final void should_handle_mixed_code() {
    final String mixedCode =
        """
        public class MixedService {
            public void safeQuery(String userId) {
                String query = "SELECT * FROM users WHERE id = ?";
                PreparedStatement ps = connection.prepareStatement(query);
                ps.setString(1, userId);
                ResultSet rs = ps.executeQuery();
            }

            public void unsafeQuery(String userId) {
                String query = "SELECT * FROM users WHERE id = " + userId;
                statement.executeQuery(query);
            }

            public void safeRandom() {
                SecureRandom random = new SecureRandom();
                byte[] token = new byte[32];
                random.nextBytes(token);
            }

            public void unsafeRandom() {
                Random random = new Random();
                byte[] token = new byte[32];
                random.nextBytes(token);
            }
        }
        """;

    final ReviewResult result = service.analyzeCode(mixedCode, "MixedService.java");

    assertThat(result.issues).hasSizeGreaterThanOrEqualTo(2);
    assertThat(result.issues.stream().anyMatch(i -> i.title.contains("SQL_INJECTION"))).isTrue();
    assertThat(result.issues.stream().anyMatch(i -> i.title.contains("INSECURE_RANDOM"))).isTrue();
  }

  @Test
  @DisplayName("should_correctly_categorize_severity_levels")
  final void should_categorize_severity_levels() {
    final String vulnerableCode =
        """
        public class Test {
            private String password = "HardcodedPass123";
            private String apiKey = "sk-abcdefghijklmnop";

            public void executeCommand(String input) {
                Runtime.getRuntime().exec("cmd " + input);
            }

            public void query(String userId) {
                String query = "SELECT * FROM users WHERE id = " + userId;
                statement.executeQuery(query);
            }

            public void loadClass(String className) throws Exception {
                Class<?> clazz = Class.forName(className);
            }

            public void generateToken() {
                Random random = new Random();
            }
        }
        """;

    final ReviewResult result = service.analyzeCode(vulnerableCode, "Test.java");

    final long criticalCount =
        result.issues.stream().filter(i -> "critical".equals(i.severity)).count();
    final long highCount = result.issues.stream().filter(i -> "high".equals(i.severity)).count();
    final long mediumCount =
        result.issues.stream().filter(i -> "medium".equals(i.severity)).count();

    assertThat(criticalCount).isGreaterThan(0);
    assertThat(highCount + mediumCount).isGreaterThan(0);
  }

  @Test
  @DisplayName("should_include_comprehensive_information_in_review_issues")
  final void should_include_comprehensive_information() {
    final String vulnerableCode =
        """
        String query = "SELECT * FROM users WHERE id = " + userId;
        statement.executeQuery(query);
        """;

    final ReviewResult result = service.analyzeCode(vulnerableCode, "Database.java");

    assertThat(result.issues).isNotEmpty();

    for (final ReviewResult.Issue issue : result.issues) {
      assertThat(issue.file).isNotNull().isNotBlank();
      assertThat(issue.severity).isNotNull().isNotBlank();
      assertThat(issue.title).isNotNull().contains("[Security]");
      assertThat(issue.suggestion).isNotNull().contains("**Security Issue Detected**");
      assertThat(issue.confidenceScore).isNotNull().isGreaterThanOrEqualTo(0.5);
      assertThat(issue.confidenceExplanation).isNotNull().isNotBlank();
      assertThat(issue.suggestedFix).isNotNull().isNotBlank();
    }
  }

  @Test
  @DisplayName("should_map_line_numbers_in_analysis_with_line_mapping")
  final void should_map_line_numbers_correctly() {
    final String vulnerableCode =
        """
        public class Test {
            public void executeCommand(String input) {
                Runtime.getRuntime().exec("cmd " + input);
            }
        }
        """;

    final ReviewResult result =
        service.analyzeCodeWithLineMapping(vulnerableCode, "Test.java", 100);

    assertThat(result.issues).hasSizeGreaterThanOrEqualTo(1);

    for (final ReviewResult.Issue issue : result.issues) {
      assertThat(issue.start_line).isGreaterThanOrEqualTo(100);
    }
  }

  @Test
  @DisplayName("should_handle_empty_code_gracefully")
  final void should_handle_empty_code() {
    final ReviewResult result = service.analyzeCode("", "Empty.java");

    assertThat(result).isNotNull();
    assertThat(result.summary).isEqualTo("Security Analysis");
    assertThat(result.issues).isEmpty();
  }

  @Test
  @DisplayName("should_handle_safe_code_without_false_positives")
  final void should_handle_safe_code_without_false_positives() {
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
        }
        """;

    final ReviewResult result = service.analyzeCode(safeCode, "SecureService.java");

    assertThat(result.issues).isEmpty();
  }

  @Test
  @DisplayName("should_detect_all_critical_vulnerabilities_in_complex_code")
  final void should_detect_all_critical_vulnerabilities() {
    final String complexVulnerableCode =
        """
        public class ComplexVulnerable {
            private String password = "MyHardcodedPassword123";

            public void processRequest(HttpServletRequest request) throws Exception {
                String className = request.getParameter("class");
                Class<?> clazz = Class.forName(className);

                String command = request.getParameter("cmd");
                Runtime.getRuntime().exec(command);

                String userId = request.getParameter("user");
                String query = "SELECT * FROM users WHERE id = " + userId;
                statement.executeQuery(query);

                String xpathQuery = "/users/user[@name='" + request.getParameter("name") + "']";
                XPath.compile(xpathQuery);

                String script = request.getParameter("script");
                ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
                engine.eval(script);
            }
        }
        """;

    final ReviewResult result =
        service.analyzeCode(complexVulnerableCode, "ComplexVulnerable.java");

    assertThat(result.issues).hasSizeGreaterThanOrEqualTo(5);

    assertThat(result.issues.stream().anyMatch(i -> i.title.contains("HARDCODED_CREDENTIALS")))
        .isTrue();
    assertThat(
            result.issues.stream()
                .anyMatch(
                    i ->
                        i.title.contains("REFLECTION_ABUSE")
                            || i.title.contains("REFLECTION_USAGE")))
        .isTrue();
    assertThat(result.issues.stream().anyMatch(i -> i.title.contains("COMMAND_INJECTION")))
        .isTrue();
    assertThat(result.issues.stream().anyMatch(i -> i.title.contains("SQL_INJECTION"))).isTrue();
    assertThat(result.issues.stream().anyMatch(i -> i.title.contains("XPATH_INJECTION"))).isTrue();

    final long criticalIssues =
        result.issues.stream().filter(i -> "critical".equals(i.severity)).count();
    assertThat(criticalIssues).isGreaterThanOrEqualTo(4);
  }
}
