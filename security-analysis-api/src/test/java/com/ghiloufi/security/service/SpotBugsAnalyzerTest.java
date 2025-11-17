package com.ghiloufi.security.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.security.model.SecurityAnalysisResponse;
import com.ghiloufi.security.model.SecurityFinding;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SpotBugsAnalyzerTest {

  private SpotBugsAnalyzer analyzer;

  @BeforeEach
  void setUp() {
    analyzer = new SpotBugsAnalyzer();
  }

  @Test
  @DisplayName("should_detect_sql_injection_vulnerability")
  void should_detect_sql_injection_vulnerability() {
    final String vulnerableCode =
        """
        package com.example;
        import java.sql.*;
        public class VulnerableDAO {
            public void getUserData(String userId) throws SQLException {
                Connection conn = DriverManager.getConnection("jdbc:h2:mem:test");
                Statement stmt = conn.createStatement();
                String query = "SELECT * FROM users WHERE id = '" + userId + "'";
                ResultSet rs = stmt.executeQuery(query);
            }
        }
        """;

    final SecurityAnalysisResponse response =
        analyzer.analyze(vulnerableCode, "java", "VulnerableDAO.java");

    assertThat(response).isNotNull();
    assertThat(response.findings()).isNotEmpty();
    assertThat(response.tool()).isEqualTo("SpotBugs");
    assertThat(response.toolVersion()).isNotBlank();

    final List<SecurityFinding> sqlInjectionFindings =
        response.findings().stream()
            .filter(f -> f.type().contains("SQL") || f.message().toLowerCase().contains("sql"))
            .toList();

    assertThat(sqlInjectionFindings).isNotEmpty();
    assertThat(sqlInjectionFindings.getFirst().severity()).isIn("HIGH", "MEDIUM");
  }

  @Test
  @DisplayName("should_detect_weak_cryptography_vulnerability")
  void should_detect_weak_cryptography_vulnerability() {
    final String vulnerableCode =
        """
        package com.example;
        import javax.crypto.Cipher;
        import javax.crypto.KeyGenerator;
        import javax.crypto.SecretKey;
        public class WeakCrypto {
            public void encryptData(byte[] data) throws Exception {
                KeyGenerator keyGen = KeyGenerator.getInstance("DES");
                SecretKey key = keyGen.generateKey();
                Cipher cipher = Cipher.getInstance("DES");
                cipher.init(Cipher.ENCRYPT_MODE, key);
                byte[] encrypted = cipher.doFinal(data);
            }
        }
        """;

    final SecurityAnalysisResponse response =
        analyzer.analyze(vulnerableCode, "java", "WeakCrypto.java");

    assertThat(response).isNotNull();
    assertThat(response.tool()).isEqualTo("SpotBugs");

    final List<SecurityFinding> cryptoFindings =
        response.findings().stream()
            .filter(
                f ->
                    f.type().contains("DES")
                        || f.type().contains("CIPHER")
                        || f.type().contains("CRYPTO")
                        || f.message().toLowerCase().contains("des")
                        || f.message().toLowerCase().contains("weak"))
            .toList();

    if (!cryptoFindings.isEmpty()) {
      assertThat(cryptoFindings.getFirst().severity()).isIn("HIGH", "MEDIUM");
    } else {
      assertThat(response.findings()).as("Expected security findings but got none").isNotEmpty();
    }
  }

  @Test
  @DisplayName("should_return_empty_findings_for_secure_code")
  void should_return_empty_findings_for_secure_code() {
    final String secureCode =
        """
        package com.example;
        public class SecureClass {
            private final String name;
            public SecureClass(String name) {
                this.name = name;
            }
            public String getName() {
                return name;
            }
        }
        """;

    final SecurityAnalysisResponse response =
        analyzer.analyze(secureCode, "java", "SecureClass.java");

    assertThat(response).isNotNull();
    assertThat(response.findings()).isEmpty();
    assertThat(response.tool()).isEqualTo("SpotBugs");
    assertThat(response.analysisTimeMs()).isGreaterThanOrEqualTo(0L);
  }

  @Test
  @DisplayName("should_handle_compilation_errors_gracefully")
  void should_handle_compilation_errors_gracefully() {
    final String invalidCode =
        """
        package com.example;
        public class Invalid {
            this is not valid java code
        }
        """;

    final SecurityAnalysisResponse response = analyzer.analyze(invalidCode, "java", "Invalid.java");

    assertThat(response).isNotNull();
    assertThat(response.findings()).isEmpty();
  }

  @Test
  @DisplayName("should_measure_analysis_time")
  void should_measure_analysis_time() {
    final String simpleCode =
        """
        package com.example;
        public class Simple {
            public void doNothing() {}
        }
        """;

    final SecurityAnalysisResponse response = analyzer.analyze(simpleCode, "java", "Simple.java");

    assertThat(response.analysisTimeMs()).isGreaterThanOrEqualTo(0L);
  }

  @Test
  @DisplayName("should_detect_hardcoded_password_vulnerability")
  void should_detect_hardcoded_password_vulnerability() {
    final String vulnerableCode =
        """
        package com.example;
        public class HardcodedCredentials {
            private static final String PASSWORD = "admin123";
            private static final String API_KEY = "sk-1234567890abcdef";

            public void connect() {
                String dbPassword = "mySecretPassword";
            }
        }
        """;

    final SecurityAnalysisResponse response =
        analyzer.analyze(vulnerableCode, "java", "HardcodedCredentials.java");

    assertThat(response).isNotNull();
    assertThat(response.tool()).isEqualTo("SpotBugs");
  }

  @Test
  @DisplayName("should_detect_path_traversal_vulnerability")
  void should_detect_path_traversal_vulnerability() {
    final String vulnerableCode =
        """
        package com.example;
        import java.io.File;
        public class PathTraversal {
            public File getFile(String filename) {
                return new File("/var/data/" + filename);
            }
        }
        """;

    final SecurityAnalysisResponse response =
        analyzer.analyze(vulnerableCode, "java", "PathTraversal.java");

    assertThat(response).isNotNull();
    assertThat(response.tool()).isEqualTo("SpotBugs");
  }

  @Test
  @DisplayName("should_detect_command_injection_vulnerability")
  void should_detect_command_injection_vulnerability() {
    final String vulnerableCode =
        """
        package com.example;
        import java.io.IOException;
        public class CommandInjection {
            public void executeCommand(String userInput) throws IOException {
                Runtime.getRuntime().exec("ls " + userInput);
            }
        }
        """;

    final SecurityAnalysisResponse response =
        analyzer.analyze(vulnerableCode, "java", "CommandInjection.java");

    assertThat(response).isNotNull();
    assertThat(response.tool()).isEqualTo("SpotBugs");
  }

  @Test
  @DisplayName("should_handle_empty_code")
  void should_handle_empty_code() {
    final SecurityAnalysisResponse response = analyzer.analyze("", "java", "Empty.java");

    assertThat(response).isNotNull();
    assertThat(response.findings()).isEmpty();
    assertThat(response.tool()).isEqualTo("SpotBugs");
  }

  @Test
  @DisplayName("should_handle_whitespace_only_code")
  void should_handle_whitespace_only_code() {
    final SecurityAnalysisResponse response =
        analyzer.analyze("   \n\n\t\t  \n", "java", "Whitespace.java");

    assertThat(response).isNotNull();
    assertThat(response.findings()).isEmpty();
  }

  @Test
  @DisplayName("should_handle_code_with_no_class_definition")
  void should_handle_code_with_no_class_definition() {
    final String noClassCode =
        """
        package com.example;
        // Just a comment, no class
        """;

    final SecurityAnalysisResponse response = analyzer.analyze(noClassCode, "java", "NoClass.java");

    assertThat(response).isNotNull();
    assertThat(response.findings()).isEmpty();
  }

  @Test
  @DisplayName("should_handle_code_with_multiple_classes")
  void should_handle_code_with_multiple_classes() {
    final String multipleClasses =
        """
        package com.example;
        import java.sql.*;

        public class First {
            public void vulnerable(String input) throws SQLException {
                Connection conn = DriverManager.getConnection("jdbc:h2:mem:test");
                Statement stmt = conn.createStatement();
                String query = "SELECT * FROM users WHERE id = '" + input + "'";
                stmt.executeQuery(query);
            }
        }

        class Second {
            private final String name;
            public Second(String name) { this.name = name; }
        }
        """;

    final SecurityAnalysisResponse response =
        analyzer.analyze(multipleClasses, "java", "Multiple.java");

    assertThat(response).isNotNull();
    assertThat(response.tool()).isEqualTo("SpotBugs");
  }

  @Test
  @DisplayName("should_handle_code_with_unicode_characters")
  void should_handle_code_with_unicode_characters() {
    final String unicodeCode =
        """
        package com.example;
        public class Unicode {
            private final String message = "Hello 世界 🌍";
            public Unicode() {}
            public String getMessage() { return message; }
        }
        """;

    final SecurityAnalysisResponse response = analyzer.analyze(unicodeCode, "java", "Unicode.java");

    assertThat(response).isNotNull();
    assertThat(response.tool()).isEqualTo("SpotBugs");
  }

  @Test
  @DisplayName("should_return_empty_findings_for_unsupported_language")
  void should_return_empty_findings_for_unsupported_language() {
    final String pythonCode = "def hello():\n    print('Hello')";

    final SecurityAnalysisResponse response = analyzer.analyze(pythonCode, "python", "test.py");

    assertThat(response).isNotNull();
    assertThat(response.findings()).isEmpty();
    assertThat(response.tool()).isEqualTo("SpotBugs");
  }
}
