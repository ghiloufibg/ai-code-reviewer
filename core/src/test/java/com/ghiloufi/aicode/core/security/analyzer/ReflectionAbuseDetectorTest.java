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

@DisplayName("ReflectionAbuseDetector Tests")
final class ReflectionAbuseDetectorTest {

  private ReflectionAbuseDetector detector;
  private JavaParser parser;

  @BeforeEach
  final void setUp() {
    detector = new ReflectionAbuseDetector();
    parser = new JavaParser();
  }

  @Test
  @DisplayName("should_detect_class_forname_with_user_input_as_critical")
  final void should_detect_class_forname_with_user_input() {
    final String code =
        """
        public class Test {
            public void loadClass(String className) throws Exception {
                Class.forName(request.getParameter("class"));
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(code);

    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.CRITICAL);
    assertThat(issues.get(0).category()).isEqualTo("REFLECTION_ABUSE");
    assertThat(issues.get(0).description()).contains("forName()", "dynamic input");
  }

  @Test
  @DisplayName("should_detect_class_forname_with_concatenation_as_critical")
  final void should_detect_class_forname_with_concatenation() {
    final String code =
        """
        public class Test {
            public void loadClass(String name) throws Exception {
                Class.forName("com.example." + name);
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(code);

    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.CRITICAL);
    assertThat(issues.get(0).category()).isEqualTo("REFLECTION_ABUSE");
  }

  @Test
  @DisplayName("should_detect_class_forname_without_dynamic_input_as_high")
  final void should_detect_class_forname_without_dynamic_input() {
    final String code =
        """
        public class Test {
            public void loadClass() throws Exception {
                Class.forName("com.example.MyClass");
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(code);

    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.HIGH);
    assertThat(issues.get(0).category()).isEqualTo("REFLECTION_USAGE");
    assertThat(issues.get(0).description()).contains("forName()");
  }

  @Test
  @DisplayName("should_detect_method_invoke_with_dynamic_input_as_critical")
  final void should_detect_method_invoke_with_dynamic_input() {
    final String code =
        """
        public class Test {
            public void invokeMethod(Method method) throws Exception {
                method.invoke(null, request.getParameter("arg"));
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(code);

    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.CRITICAL);
    assertThat(issues.get(0).category()).isEqualTo("REFLECTION_ABUSE");
    assertThat(issues.get(0).description()).contains("invoke()", "dynamic input");
  }

  @Test
  @DisplayName("should_detect_getdeclaredmethod_with_concatenation_as_critical")
  final void should_detect_getdeclaredmethod_with_concatenation() {
    final String code =
        """
        public class Test {
            public void getMethod(String methodName) throws Exception {
                Class.forName("Test").getDeclaredMethod("get" + methodName);
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(code);

    assertThat(issues).hasSize(2);
    assertThat(issues.stream().anyMatch(i -> i.severity() == Severity.CRITICAL)).isTrue();
    assertThat(issues.stream().anyMatch(i -> i.category().equals("REFLECTION_ABUSE"))).isTrue();
  }

  @Test
  @DisplayName("should_detect_newinstance_without_dynamic_input_as_high")
  final void should_detect_newinstance_without_dynamic_input() {
    final String code =
        """
        public class Test {
            public void createInstance() throws Exception {
                Class.forName("Test").newInstance();
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(code);

    assertThat(issues).hasSize(2);
    assertThat(issues.stream().allMatch(i -> i.severity() == Severity.HIGH)).isTrue();
  }

  @Test
  @DisplayName("should_not_detect_safe_code")
  final void should_not_detect_safe_code() {
    final String code =
        """
        public class Test {
            public void safeMethod() {
                String className = "Test";
                System.out.println(className);
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(code);

    assertThat(issues).isEmpty();
  }

  @Test
  @DisplayName("should_detect_system_getproperty_in_reflection_as_critical")
  final void should_detect_system_getproperty_in_reflection() {
    final String code =
        """
        public class Test {
            public void loadClass() throws Exception {
                Class.forName(System.getProperty("class.name"));
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(code);

    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.CRITICAL);
    assertThat(issues.get(0).category()).isEqualTo("REFLECTION_ABUSE");
  }

  private List<SecurityIssue> analyzeCode(final String code) {
    final CompilationUnit cu = parser.parse(code).getResult().orElseThrow();
    final List<SecurityIssue> issues = new ArrayList<>();
    detector.visit(cu, issues);
    return issues;
  }
}
