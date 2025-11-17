package com.ghiloufi.aicode.core.security.analyzer;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.security.model.SecurityIssue;
import com.ghiloufi.aicode.core.security.model.Severity;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CommandInjectionDetector Tests")
final class CommandInjectionDetectorTest {

  private CommandInjectionDetector detector;
  private JavaParser parser;

  @BeforeEach
  final void setUp() {
    detector = new CommandInjectionDetector();
    parser = new JavaParser();
  }

  @Test
  @DisplayName("should_detect_runtime_exec_with_string_concatenation_as_critical")
  final void should_detect_runtime_exec_with_concatenation() {
    final String code =
        """
        public class Test {
            public void execute(String userInput) {
                Runtime.getRuntime().exec("cmd /c " + userInput);
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(code);

    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.CRITICAL);
    assertThat(issues.get(0).category()).isEqualTo("COMMAND_INJECTION");
    assertThat(issues.get(0).description()).contains("Runtime.exec()", "string concatenation");
  }

  @Test
  @DisplayName("should_detect_runtime_exec_without_concatenation_as_high")
  final void should_detect_runtime_exec_without_concatenation() {
    final String code =
        """
        public class Test {
            public void execute() {
                Runtime.getRuntime().exec("ls -la");
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(code);

    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.HIGH);
    assertThat(issues.get(0).category()).isEqualTo("COMMAND_EXECUTION");
    assertThat(issues.get(0).description()).contains("Runtime.exec()");
  }

  @Test
  @DisplayName("should_detect_processbuilder_with_string_concatenation_as_critical")
  final void should_detect_processbuilder_with_concatenation() {
    final String code =
        """
        public class Test {
            public void execute(String userInput) {
                new ProcessBuilder("bash", "-c", "echo " + userInput).start();
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(code);

    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.CRITICAL);
    assertThat(issues.get(0).category()).isEqualTo("COMMAND_INJECTION");
    assertThat(issues.get(0).description()).contains("ProcessBuilder", "string concatenation");
  }

  @Test
  @DisplayName("should_detect_processbuilder_without_concatenation_as_medium")
  final void should_detect_processbuilder_without_concatenation() {
    final String code =
        """
        public class Test {
            public void execute() {
                new ProcessBuilder("ls", "-la").start();
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(code);

    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.MEDIUM);
    assertThat(issues.get(0).category()).isEqualTo("COMMAND_EXECUTION");
    assertThat(issues.get(0).description()).contains("ProcessBuilder");
  }

  @Test
  @DisplayName("should_detect_runtime_exec_with_string_format_as_critical")
  final void should_detect_runtime_exec_with_string_format() {
    final String code =
        """
        public class Test {
            public void execute(String userInput) {
                Runtime.getRuntime().exec(String.format("cmd /c %s", userInput));
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(code);

    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.CRITICAL);
    assertThat(issues.get(0).category()).isEqualTo("COMMAND_INJECTION");
  }

  @Test
  @DisplayName("should_not_detect_safe_code")
  final void should_not_detect_safe_code() {
    final String code =
        """
        public class Test {
            public void execute() {
                System.out.println("Safe code");
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(code);

    assertThat(issues).isEmpty();
  }

  @Test
  @DisplayName("should_detect_multiple_violations_in_same_file")
  final void should_detect_multiple_violations() {
    final String code =
        """
        public class Test {
            public void method1(String input) {
                Runtime.getRuntime().exec("cmd " + input);
            }
            public void method2() {
                new ProcessBuilder("bash", "-c", "ls").start();
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(code);

    assertThat(issues).hasSize(2);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.CRITICAL);
    assertThat(issues.get(1).severity()).isEqualTo(Severity.MEDIUM);
  }

  private List<SecurityIssue> analyzeCode(final String code) {
    final CompilationUnit cu = parser.parse(code).getResult().orElseThrow();
    final List<SecurityIssue> issues = new ArrayList<>();
    detector.visit(cu, issues);
    return issues;
  }
}
