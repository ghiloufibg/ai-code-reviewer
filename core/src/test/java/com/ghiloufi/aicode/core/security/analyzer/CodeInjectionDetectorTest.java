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

@DisplayName("CodeInjectionDetector Tests")
final class CodeInjectionDetectorTest {

  private CodeInjectionDetector detector;
  private JavaParser parser;

  @BeforeEach
  final void setUp() {
    detector = new CodeInjectionDetector();
    parser = new JavaParser();
  }

  @Test
  @DisplayName("should_detect_scriptengine_eval_with_user_input_as_critical")
  final void should_detect_scriptengine_eval_with_user_input() {
    final String code =
        """
        import javax.script.ScriptEngine;
        public class Test {
            public void evaluate(ScriptEngine engine) throws Exception {
                engine.eval(request.getParameter("code"));
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(code);

    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.CRITICAL);
    assertThat(issues.get(0).category()).isEqualTo("CODE_INJECTION");
    assertThat(issues.get(0).description()).contains("eval()", "dynamic input");
  }

  @Test
  @DisplayName("should_detect_scriptengine_eval_with_concatenation_as_critical")
  final void should_detect_scriptengine_eval_with_concatenation() {
    final String code =
        """
        import javax.script.ScriptEngine;
        public class Test {
            public void evaluate(ScriptEngine engine, String userCode) throws Exception {
                engine.eval("var x = " + userCode);
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(code);

    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.CRITICAL);
    assertThat(issues.get(0).category()).isEqualTo("CODE_INJECTION");
  }

  @Test
  @DisplayName("should_detect_scriptengine_eval_without_dynamic_input_as_high")
  final void should_detect_scriptengine_eval_without_dynamic_input() {
    final String code =
        """
        import javax.script.ScriptEngine;
        public class Test {
            public void evaluate(ScriptEngine engine) throws Exception {
                engine.eval("var x = 42");
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(code);

    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.HIGH);
    assertThat(issues.get(0).category()).isEqualTo("DYNAMIC_CODE_EXECUTION");
  }

  @Test
  @DisplayName("should_detect_scriptengine_compile_with_dynamic_input_as_critical")
  final void should_detect_scriptengine_compile_with_dynamic_input() {
    final String code =
        """
        import javax.script.ScriptEngine;
        public class Test {
            public void compile(ScriptEngine engine) throws Exception {
                engine.compile(System.getProperty("script"));
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(code);

    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.CRITICAL);
    assertThat(issues.get(0).category()).isEqualTo("CODE_INJECTION");
  }

  @Test
  @DisplayName("should_detect_scriptenginemanager_creation_as_high")
  final void should_detect_scriptenginemanager_creation() {
    final String code =
        """
        import javax.script.ScriptEngineManager;
        public class Test {
            public void createEngine() {
                new ScriptEngineManager().getEngineByName("JavaScript");
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(code);

    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.HIGH);
    assertThat(issues.get(0).category()).isEqualTo("DYNAMIC_CODE_EXECUTION");
    assertThat(issues.get(0).description()).contains("ScriptEngineManager");
  }

  @Test
  @DisplayName("should_detect_toolprovider_compiler_usage_as_high")
  final void should_detect_toolprovider_compiler_usage() {
    final String code =
        """
        import javax.tools.ToolProvider;
        public class Test {
            public void compileCode() {
                ToolProvider.getSystemJavaCompiler();
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(code);

    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.HIGH);
    assertThat(issues.get(0).category()).isEqualTo("DYNAMIC_CODE_EXECUTION");
  }

  @Test
  @DisplayName("should_not_detect_safe_code")
  final void should_not_detect_safe_code() {
    final String code =
        """
        public class Test {
            public void safeMethod() {
                String code = "some code";
                System.out.println(code);
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
        import javax.script.ScriptEngine;
        import javax.script.ScriptEngineManager;
        public class Test {
            public void method1(ScriptEngine engine) throws Exception {
                engine.eval(request.getParameter("code"));
            }
            public void method2() {
                new ScriptEngineManager();
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(code);

    assertThat(issues).hasSize(2);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.CRITICAL);
    assertThat(issues.get(1).severity()).isEqualTo(Severity.HIGH);
  }

  private List<SecurityIssue> analyzeCode(final String code) {
    final CompilationUnit cu = parser.parse(code).getResult().orElseThrow();
    final List<SecurityIssue> issues = new ArrayList<>();
    detector.visit(cu, issues);
    return issues;
  }
}
