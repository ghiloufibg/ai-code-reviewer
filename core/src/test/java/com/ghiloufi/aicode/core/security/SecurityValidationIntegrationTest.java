package com.ghiloufi.aicode.core.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.security.analyzer.CodeInjectionDetector;
import com.ghiloufi.aicode.core.security.analyzer.CommandInjectionDetector;
import com.ghiloufi.aicode.core.security.analyzer.PathTraversalDetector;
import com.ghiloufi.aicode.core.security.analyzer.ReflectionAbuseDetector;
import com.ghiloufi.aicode.core.security.model.SecurityIssue;
import com.ghiloufi.aicode.core.security.model.Severity;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Security Validation Integration Tests")
final class SecurityValidationIntegrationTest {

  private List<com.ghiloufi.aicode.core.security.analyzer.BaseSecurityVisitor> detectors;
  private JavaParser parser;

  @BeforeEach
  final void setUp() {
    detectors =
        List.of(
            new CommandInjectionDetector(),
            new ReflectionAbuseDetector(),
            new CodeInjectionDetector(),
            new PathTraversalDetector());
    parser = new JavaParser();
  }

  @Test
  @DisplayName("should_detect_multiple_vulnerability_types_in_vulnerable_code")
  final void should_detect_multiple_vulnerabilities() {
    final String vulnerableCode =
        """
        import java.io.File;
        import javax.script.ScriptEngine;
        import javax.servlet.http.HttpServletRequest;
        public class VulnerableService {
            public void processUserRequest(HttpServletRequest request, String filename, ScriptEngine engine) throws Exception {
                Runtime.getRuntime().exec("curl " + request.getParameter("url"));

                Class.forName(request.getParameter("class")).newInstance();

                engine.eval(request.getParameter("script"));

                new File("/data/" + filename);
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(vulnerableCode);

    assertThat(issues).hasSizeGreaterThanOrEqualTo(4);

    assertThat(issues.stream().anyMatch(i -> i.category().contains("COMMAND"))).isTrue();
    assertThat(issues.stream().anyMatch(i -> i.category().contains("REFLECTION"))).isTrue();
    assertThat(issues.stream().anyMatch(i -> i.category().contains("CODE"))).isTrue();
    assertThat(issues.stream().anyMatch(i -> i.category().contains("PATH"))).isTrue();

    final long criticalCount =
        issues.stream().filter(i -> i.severity() == Severity.CRITICAL).count();
    assertThat(criticalCount).isGreaterThanOrEqualTo(3);
  }

  @Test
  @DisplayName("should_not_detect_issues_in_secure_code")
  final void should_not_detect_issues_in_secure_code() {
    final String secureCode =
        """
        import java.nio.file.Path;
        import java.nio.file.Paths;
        public class SecureService {
            private static final String BASE_DIR = "/data";

            public void processRequest(String input) {
                final String sanitized = sanitizeInput(input);
                final Path safePath = Paths.get(BASE_DIR).resolve(sanitized).normalize();

                if (!safePath.startsWith(BASE_DIR)) {
                    throw new SecurityException("Path traversal attempt detected");
                }

                System.out.println("Processing: " + safePath);
            }

            private String sanitizeInput(String input) {
                return input.replaceAll("[^a-zA-Z0-9_.-]", "");
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(secureCode);

    assertThat(issues).isEmpty();
  }

  @Test
  @DisplayName("should_detect_command_injection_in_ai_generated_code_sample")
  final void should_detect_command_injection_in_ai_code() {
    final String aiGeneratedCode =
        """
        public class GitOperations {
            public String executeGitCommand(String repo, String branch) throws Exception {
                Process process = Runtime.getRuntime().exec("git clone " + repo + " -b " + branch);
                return readProcessOutput(process);
            }

            private String readProcessOutput(Process process) {
                return "output";
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(aiGeneratedCode);

    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.CRITICAL);
    assertThat(issues.get(0).category()).isEqualTo("COMMAND_INJECTION");
    assertThat(issues.get(0).description()).contains("Runtime.exec()");
  }

  @Test
  @DisplayName("should_detect_reflection_abuse_in_plugin_loader")
  final void should_detect_reflection_abuse_in_plugin_loader() {
    final String pluginLoaderCode =
        """
        import java.util.Map;
        public class PluginLoader {
            public Object loadPlugin(Map<String, String> config) throws Exception {
                return Class.forName(config.get("pluginClass")).getDeclaredConstructor().newInstance();
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(pluginLoaderCode);

    assertThat(issues).hasSizeGreaterThanOrEqualTo(1);
    assertThat(issues.stream().anyMatch(i -> i.category().contains("REFLECTION"))).isTrue();
  }

  @Test
  @DisplayName("should_detect_path_traversal_in_file_upload_handler")
  final void should_detect_path_traversal_in_file_upload() {
    final String fileUploadCode =
        """
        import java.io.File;
        import java.io.FileOutputStream;
        public class FileUploadHandler {
            public void saveUploadedFile(String filename, byte[] content) throws Exception {
                String uploadDir = "/uploads/";
                File file = new File(uploadDir + filename);

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(content);
                }
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(fileUploadCode);

    assertThat(issues).hasSizeGreaterThanOrEqualTo(1);
    assertThat(issues.stream().anyMatch(i -> i.category().contains("PATH_TRAVERSAL"))).isTrue();
  }

  @Test
  @DisplayName("should_calculate_correct_risk_scores_for_mixed_severity")
  final void should_calculate_risk_scores() {
    final String mixedSeverityCode =
        """
        import java.io.File;
        import javax.script.ScriptEngine;
        public class MixedSeverity {
            public void critical(String input, ScriptEngine engine) throws Exception {
                engine.eval(input);
            }

            public void high() throws Exception {
                Runtime.getRuntime().exec("ls");
            }

            public void medium(String filename) {
                new File(filename);
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(mixedSeverityCode);

    final double riskScore =
        issues.stream().mapToDouble(issue -> issue.severity().getWeight()).sum();

    assertThat(issues).hasSizeGreaterThanOrEqualTo(2);
    assertThat(riskScore).isGreaterThan(10.0);
  }

  @Test
  @DisplayName("should_provide_detailed_descriptions_for_all_issues")
  final void should_provide_detailed_descriptions() {
    final String code =
        """
        public class Test {
            public void test(String input) throws Exception {
                Runtime.getRuntime().exec("cmd " + input);
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(code);

    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).description()).isNotBlank();
    assertThat(issues.get(0).description()).contains("line");
    assertThat(issues.get(0).description().length()).isGreaterThan(50);
  }

  private List<SecurityIssue> analyzeCode(final String code) {
    final CompilationUnit cu = parser.parse(code).getResult().orElseThrow();
    final List<SecurityIssue> allIssues = new ArrayList<>();

    for (final com.ghiloufi.aicode.core.security.analyzer.BaseSecurityVisitor detector :
        detectors) {
      detector.visit(cu, allIssues);
    }

    return allIssues;
  }
}
