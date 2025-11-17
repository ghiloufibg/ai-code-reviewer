package com.ghiloufi.aicode.core.security.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.security.analyzer.CodeInjectionDetector;
import com.ghiloufi.aicode.core.security.analyzer.CommandInjectionDetector;
import com.ghiloufi.aicode.core.security.analyzer.PathTraversalDetector;
import com.ghiloufi.aicode.core.security.analyzer.ReflectionAbuseDetector;
import com.ghiloufi.aicode.core.security.config.DangerousPatternsConfig;
import com.ghiloufi.aicode.core.security.detector.SensitiveFileDetector;
import com.ghiloufi.aicode.core.security.mapper.SecurityResultMapper;
import com.ghiloufi.aicode.core.security.validator.PatternValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SecurityAnalysisService Tests")
final class SecurityAnalysisServiceTest {

  private SecurityAnalysisService service;

  @BeforeEach
  final void setUp() {
    final DangerousPatternsConfig dangerousConfig = new DangerousPatternsConfig();
    final CommandInjectionDetector commandInjectionDetector = new CommandInjectionDetector();
    final ReflectionAbuseDetector reflectionAbuseDetector = new ReflectionAbuseDetector();
    final CodeInjectionDetector codeInjectionDetector = new CodeInjectionDetector();
    final PathTraversalDetector pathTraversalDetector = new PathTraversalDetector();
    final PatternValidator patternValidator =
        new PatternValidator(dangerousConfig.dangerousPatterns());
    final SensitiveFileDetector fileDetector = new SensitiveFileDetector();
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
  @DisplayName("should_return_empty_result_for_null_code")
  final void should_return_empty_result_for_null_code() {
    final ReviewResult result = service.analyzeCode(null, "Test.java");

    assertThat(result).isNotNull();
    assertThat(result.summary).isEqualTo("Security Analysis");
    assertThat(result.issues).isEmpty();
  }

  @Test
  @DisplayName("should_return_empty_result_for_blank_code")
  final void should_return_empty_result_for_blank_code() {
    final ReviewResult result = service.analyzeCode("   ", "Test.java");

    assertThat(result).isNotNull();
    assertThat(result.summary).isEqualTo("Security Analysis");
    assertThat(result.issues).isEmpty();
  }

  @Test
  @DisplayName("should_detect_command_injection_vulnerabilities")
  final void should_detect_command_injection() {
    final String vulnerableCode =
        """
        public class Test {
            public void execute(String input) {
                Runtime.getRuntime().exec("cmd " + input);
            }
        }
        """;

    final ReviewResult result = service.analyzeCode(vulnerableCode, "Test.java");

    assertThat(result.issues).hasSizeGreaterThanOrEqualTo(1);
    assertThat(result.issues.stream().anyMatch(i -> i.title.contains("COMMAND_INJECTION")))
        .isTrue();
  }

  @Test
  @DisplayName("should_detect_sql_injection_patterns")
  final void should_detect_sql_injection() {
    final String vulnerableCode =
        """
        String query = "SELECT * FROM users WHERE id = " + userId;
        statement.executeQuery(query);
        """;

    final ReviewResult result = service.analyzeCode(vulnerableCode, "Database.java");

    assertThat(result.issues).hasSizeGreaterThanOrEqualTo(1);
    assertThat(result.issues.stream().anyMatch(i -> i.title.contains("SQL_INJECTION"))).isTrue();
  }

  @Test
  @DisplayName("should_detect_hardcoded_credentials")
  final void should_detect_hardcoded_credentials() {
    final String vulnerableCode =
        """
        String password = "HardcodedPassword123";
        String apiKey = "sk-1234567890abcdefghijklmnop";
        """;

    final ReviewResult result = service.analyzeCode(vulnerableCode, "Config.java");

    assertThat(result.issues).hasSizeGreaterThanOrEqualTo(1);
    assertThat(result.issues.stream().anyMatch(i -> i.title.contains("HARDCODED_CREDENTIALS")))
        .isTrue();
  }

  @Test
  @DisplayName("should_detect_reflection_abuse")
  final void should_detect_reflection_abuse() {
    final String vulnerableCode =
        """
        public class Test {
            public void load(String className) throws Exception {
                Class<?> clazz = Class.forName(className);
                Object instance = clazz.getDeclaredConstructor().newInstance();
            }
        }
        """;

    final ReviewResult result = service.analyzeCode(vulnerableCode, "Test.java");

    assertThat(result.issues).hasSizeGreaterThanOrEqualTo(1);
    assertThat(
            result.issues.stream()
                .anyMatch(
                    i ->
                        i.title.contains("REFLECTION_ABUSE")
                            || i.title.contains("REFLECTION_USAGE")))
        .isTrue();
  }

  @Test
  @DisplayName("should_detect_path_traversal_vulnerabilities")
  final void should_detect_path_traversal() {
    final String vulnerableCode =
        """
        public class Test {
            public void readFile(String userPath) throws Exception {
                Path path = Path.of("/base/" + userPath);
                Files.readAllBytes(path);
            }
        }
        """;

    final ReviewResult result = service.analyzeCode(vulnerableCode, "FileHandler.java");

    assertThat(result.issues).hasSizeGreaterThanOrEqualTo(1);
    assertThat(result.issues.stream().anyMatch(i -> i.title.contains("PATH_TRAVERSAL"))).isTrue();
  }

  @Test
  @DisplayName("should_detect_weak_cryptography")
  final void should_detect_weak_crypto() {
    final String vulnerableCode =
        """
        Cipher cipher = Cipher.getInstance("DES");
        MessageDigest md = MessageDigest.getInstance("MD5");
        """;

    final ReviewResult result = service.analyzeCode(vulnerableCode, "Crypto.java");

    assertThat(result.issues).hasSizeGreaterThanOrEqualTo(1);
    assertThat(result.issues.stream().anyMatch(i -> i.title.contains("WEAK_CRYPTO"))).isTrue();
  }

  @Test
  @DisplayName("should_not_detect_issues_in_safe_code")
  final void should_not_detect_issues_in_safe_code() {
    final String safeCode =
        """
        public class Test {
            public void executeQuery(String userId) {
                String query = "SELECT * FROM users WHERE id = ?";
                PreparedStatement ps = connection.prepareStatement(query);
                ps.setString(1, userId);
                ResultSet rs = ps.executeQuery();
            }
        }
        """;

    final ReviewResult result = service.analyzeCode(safeCode, "Test.java");

    assertThat(result.issues).isEmpty();
  }

  @Test
  @DisplayName("should_map_line_numbers_correctly_with_start_line")
  final void should_map_line_numbers_with_start_line() {
    final String vulnerableCode =
        """
        public class Test {
            public void execute(String input) {
                Runtime.getRuntime().exec("cmd " + input);
            }
        }
        """;

    final ReviewResult result = service.analyzeCodeWithLineMapping(vulnerableCode, "Test.java", 10);

    assertThat(result.issues).hasSizeGreaterThanOrEqualTo(1);
    final ReviewResult.Issue issue = result.issues.getFirst();
    assertThat(issue.start_line).isGreaterThanOrEqualTo(10);
  }

  @Test
  @DisplayName("should_combine_ast_and_pattern_analysis")
  final void should_combine_ast_and_pattern_analysis() {
    final String vulnerableCode =
        """
        public class Test {
            private String password = "HardcodedPass123";

            public void execute(String input) {
                Runtime.getRuntime().exec("cmd " + input);
            }
        }
        """;

    final ReviewResult result = service.analyzeCode(vulnerableCode, "Test.java");

    assertThat(result.issues).hasSizeGreaterThanOrEqualTo(2);
    assertThat(result.issues.stream().anyMatch(i -> i.title.contains("COMMAND_INJECTION")))
        .isTrue();
    assertThat(result.issues.stream().anyMatch(i -> i.title.contains("HARDCODED_CREDENTIALS")))
        .isTrue();
  }

  @Test
  @DisplayName("should_set_correct_severity_levels")
  final void should_set_correct_severity_levels() {
    final String vulnerableCode =
        """
        public class Test {
            private String password = "HardcodedPass123";

            public void execute(String input) {
                Runtime.getRuntime().exec("cmd " + input);
                Random random = new Random();
            }
        }
        """;

    final ReviewResult result = service.analyzeCode(vulnerableCode, "Test.java");

    final long criticalCount =
        result.issues.stream().filter(i -> "critical".equals(i.severity)).count();
    final long mediumCount =
        result.issues.stream().filter(i -> "medium".equals(i.severity)).count();

    assertThat(criticalCount).isGreaterThan(0);
    assertThat(mediumCount).isGreaterThan(0);
  }

  @Test
  @DisplayName("should_include_confidence_scores_for_all_issues")
  final void should_include_confidence_scores() {
    final String vulnerableCode =
        """
        String query = "SELECT * FROM users WHERE id = " + userId;
        statement.executeQuery(query);
        """;

    final ReviewResult result = service.analyzeCode(vulnerableCode, "Database.java");

    assertThat(result.issues).isNotEmpty();
    assertThat(result.issues.stream().allMatch(i -> i.confidenceScore != null)).isTrue();
    assertThat(result.issues.stream().allMatch(i -> i.confidenceScore >= 0.5)).isTrue();
  }

  @Test
  @DisplayName("should_include_suggested_fixes_for_issues")
  final void should_include_suggested_fixes() {
    final String vulnerableCode =
        """
        String query = "SELECT * FROM users WHERE id = " + userId;
        statement.executeQuery(query);
        """;

    final ReviewResult result = service.analyzeCode(vulnerableCode, "Database.java");

    assertThat(result.issues).isNotEmpty();
    assertThat(result.issues.stream().allMatch(i -> i.suggestedFix != null)).isTrue();
    assertThat(result.issues.stream().allMatch(i -> !i.suggestedFix.isBlank())).isTrue();
  }

  @Test
  @DisplayName("should_set_file_path_for_all_issues")
  final void should_set_file_path() {
    final String vulnerableCode =
        """
        public class Test {
            public void execute(String input) {
                Runtime.getRuntime().exec("cmd " + input);
            }
        }
        """;

    final ReviewResult result = service.analyzeCode(vulnerableCode, "MyFile.java");

    assertThat(result.issues).isNotEmpty();
    assertThat(result.issues.stream().allMatch(i -> "MyFile.java".equals(i.file))).isTrue();
  }
}
