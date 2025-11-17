package com.ghiloufi.security.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghiloufi.security.model.SecurityFinding;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Semgrep Adapter Integration Test")
class SemgrepAdapterIntegrationTest {

  private SemgrepAdapter semgrepAdapter;

  @BeforeEach
  void setUp() {
    semgrepAdapter = new SemgrepAdapter(new ObjectMapper());
  }

  @Test
  @DisplayName("should_return_tool_name")
  void should_return_tool_name() {
    final String toolName = semgrepAdapter.getToolName();
    assertThat(toolName).isEqualTo("Semgrep");
  }

  @Test
  @DisplayName("should_detect_tool_availability")
  void should_detect_tool_availability() {
    final boolean available = semgrepAdapter.isAvailable();

    if (available) {
      final String version = semgrepAdapter.getToolVersion();
      assertThat(version).isNotNull();
      assertThat(version).isNotEqualTo("unknown");
      assertThat(version).matches("\\d+\\.\\d+\\.\\d+");
    } else {
      final String version = semgrepAdapter.getToolVersion();
      assertThat(version).isEqualTo("unknown");
    }
  }

  @Test
  @DisplayName("should_execute_semgrep_and_return_valid_response_format")
  void should_execute_semgrep_and_return_valid_response_format() {
    assumeTrue(semgrepAdapter.isAvailable(), "Semgrep is not installed");

    final String vulnerableCode =
        """
        package com.example;
        import java.sql.*;

        public class UserDAO {
            public void getUser(String userId) throws SQLException {
                Connection conn = DriverManager.getConnection("jdbc:h2:mem:test");
                Statement stmt = conn.createStatement();
                String query = "SELECT * FROM users WHERE id = '" + userId + "'";
                ResultSet rs = stmt.executeQuery(query);
            }
        }
        """;

    final List<SecurityFinding> findings = semgrepAdapter.analyze(vulnerableCode, "UserDAO.java");

    assertThat(findings).isNotNull();
    findings.forEach(
        finding -> {
          assertThat(finding.type()).isNotNull();
          assertThat(finding.severity()).isIn("LOW", "MEDIUM", "HIGH", "CRITICAL");
          assertThat(finding.line()).isGreaterThan(0);
          assertThat(finding.message()).isNotNull();
        });
  }

  @Test
  @DisplayName("should_return_empty_when_semgrep_not_available")
  void should_return_empty_when_semgrep_not_available() {
    if (!semgrepAdapter.isAvailable()) {
      final String code = "public class Test {}";
      final List<SecurityFinding> findings = semgrepAdapter.analyze(code, "Test.java");
      assertThat(findings).isEmpty();
    }
  }

  @Test
  @DisplayName("should_return_empty_for_secure_code")
  void should_return_empty_for_secure_code() {
    assumeTrue(semgrepAdapter.isAvailable(), "Semgrep is not installed");

    final String secureCode =
        """
        package com.example;

        public class Calculator {
            public int add(int a, int b) {
                return a + b;
            }

            public int multiply(int a, int b) {
                return a * b;
            }
        }
        """;

    final List<SecurityFinding> findings = semgrepAdapter.analyze(secureCode, "Calculator.java");

    assertThat(findings).isEmpty();
  }
}
