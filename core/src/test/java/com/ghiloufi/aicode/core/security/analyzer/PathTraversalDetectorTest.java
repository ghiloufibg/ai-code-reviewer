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

@DisplayName("PathTraversalDetector Tests")
final class PathTraversalDetectorTest {

  private PathTraversalDetector detector;
  private JavaParser parser;

  @BeforeEach
  final void setUp() {
    detector = new PathTraversalDetector();
    parser = new JavaParser();
  }

  @Test
  @DisplayName("should_detect_file_creation_with_string_concatenation_as_high")
  final void should_detect_file_creation_with_concatenation() {
    final String code =
        """
        import java.io.File;
        public class Test {
            public void createFile(String filename) {
                new File("/data/" + filename);
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(code);

    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.HIGH);
    assertThat(issues.get(0).category()).isEqualTo("PATH_TRAVERSAL");
    assertThat(issues.get(0).description()).contains("string concatenation");
  }

  @Test
  @DisplayName("should_detect_file_creation_with_user_input_as_medium")
  final void should_detect_file_creation_with_user_input() {
    final String code =
        """
        import java.io.File;
        public class Test {
            public void createFile() {
                new File(request.getParameter("path"));
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(code);

    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.MEDIUM);
    assertThat(issues.get(0).category()).isEqualTo("PATH_TRAVERSAL_RISK");
    assertThat(issues.get(0).description()).contains("user input");
  }

  @Test
  @DisplayName("should_detect_path_get_with_concatenation_as_high")
  final void should_detect_path_get_with_concatenation() {
    final String code =
        """
        import java.nio.file.Paths;
        public class Test {
            public void getPath(String subdir) {
                Paths.get("/base/" + subdir);
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(code);

    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.HIGH);
    assertThat(issues.get(0).category()).isEqualTo("PATH_TRAVERSAL");
  }

  @Test
  @DisplayName("should_detect_path_resolve_with_concatenation_as_high")
  final void should_detect_path_resolve_with_concatenation() {
    final String code =
        """
        import java.nio.file.Path;
        public class Test {
            public void resolvePath(Path base, String subpath) {
                base.resolve("data/" + subpath);
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(code);

    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.HIGH);
    assertThat(issues.get(0).category()).isEqualTo("PATH_TRAVERSAL");
    assertThat(issues.get(0).description()).contains("resolve");
  }

  @Test
  @DisplayName("should_detect_fileinputstream_with_concatenation_as_high")
  final void should_detect_fileinputstream_with_concatenation() {
    final String code =
        """
        import java.io.FileInputStream;
        public class Test {
            public void readFile(String filename) throws Exception {
                new FileInputStream("/data/" + filename);
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(code);

    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.HIGH);
    assertThat(issues.get(0).category()).isEqualTo("PATH_TRAVERSAL");
  }

  @Test
  @DisplayName("should_detect_fileoutputstream_with_user_input_as_medium")
  final void should_detect_fileoutputstream_with_user_input() {
    final String code =
        """
        import java.io.FileOutputStream;
        public class Test {
            public void writeFile() throws Exception {
                new FileOutputStream(request.getParameter("outputFile"));
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(code);

    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.MEDIUM);
    assertThat(issues.get(0).category()).isEqualTo("PATH_TRAVERSAL_RISK");
  }

  @Test
  @DisplayName("should_detect_file_separator_usage_as_high")
  final void should_detect_file_separator_usage() {
    final String code =
        """
        import java.io.File;
        public class Test {
            public void createFile(String filename) {
                Paths.get("/data" + File.separator + filename);
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(code);

    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.HIGH);
    assertThat(issues.get(0).category()).isEqualTo("PATH_TRAVERSAL");
  }

  @Test
  @DisplayName("should_not_detect_safe_file_operations")
  final void should_not_detect_safe_file_operations() {
    final String code =
        """
        import java.io.File;
        public class Test {
            public void createFile() {
                new File("/data/safe.txt");
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
        import java.io.File;
        import java.io.FileInputStream;
        public class Test {
            public void method1(String name) {
                new File("/data/" + name);
            }
            public void method2() throws Exception {
                new FileInputStream(request.getParameter("file"));
            }
        }
        """;

    final List<SecurityIssue> issues = analyzeCode(code);

    assertThat(issues).hasSize(2);
    assertThat(issues.get(0).severity()).isEqualTo(Severity.HIGH);
    assertThat(issues.get(1).severity()).isEqualTo(Severity.MEDIUM);
  }

  private List<SecurityIssue> analyzeCode(final String code) {
    final CompilationUnit cu = parser.parse(code).getResult().orElseThrow();
    final List<SecurityIssue> issues = new ArrayList<>();
    detector.visit(cu, issues);
    return issues;
  }
}
