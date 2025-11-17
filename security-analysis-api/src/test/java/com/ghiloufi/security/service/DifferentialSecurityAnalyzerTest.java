package com.ghiloufi.security.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.security.model.DifferentialSecurityAnalysisRequest;
import com.ghiloufi.security.model.DifferentialSecurityAnalysisResponse;
import com.ghiloufi.security.model.SecurityVerdict;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("Differential Security Analyzer Test")
class DifferentialSecurityAnalyzerTest {

  @Autowired private DifferentialSecurityAnalyzer analyzer;

  @Test
  @DisplayName("should_classify_new_finding_when_vulnerability_introduced")
  void should_classify_new_finding_when_vulnerability_introduced() {
    final String oldCode =
        """
        package com.example;
        import java.sql.*;

        public class UserDAO {
            public void getUser(String userId) throws SQLException {
                Connection conn = DriverManager.getConnection("jdbc:h2:mem:test");
                PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE id = ?");
                stmt.setString(1, userId);
                ResultSet rs = stmt.executeQuery();
            }
        }
        """;

    final String newCode =
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

    final DifferentialSecurityAnalysisRequest request =
        new DifferentialSecurityAnalysisRequest(oldCode, newCode, "java", "UserDAO.java");

    final DifferentialSecurityAnalysisResponse response = analyzer.analyzeDiff(request);

    assertThat(response.verdict()).isEqualTo(SecurityVerdict.UNSAFE);
    assertThat(response.newFindings()).isNotEmpty();
    assertThat(response.fixedFindings()).isEmpty();
  }

  @Test
  @DisplayName("should_classify_fixed_finding_when_vulnerability_resolved")
  void should_classify_fixed_finding_when_vulnerability_resolved() {
    final String oldCode =
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

    final String newCode =
        """
        package com.example;
        import java.sql.*;

        public class UserDAO {
            public void getUser(String userId) throws SQLException {
                Connection conn = DriverManager.getConnection("jdbc:h2:mem:test");
                PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE id = ?");
                stmt.setString(1, userId);
                ResultSet rs = stmt.executeQuery();
            }
        }
        """;

    final DifferentialSecurityAnalysisRequest request =
        new DifferentialSecurityAnalysisRequest(oldCode, newCode, "java", "UserDAO.java");

    final DifferentialSecurityAnalysisResponse response = analyzer.analyzeDiff(request);

    assertThat(response.verdict()).isEqualTo(SecurityVerdict.IMPROVED);
    assertThat(response.newFindings()).isEmpty();
    assertThat(response.fixedFindings()).isNotEmpty();
  }

  @Test
  @DisplayName("should_return_safe_verdict_when_no_new_vulnerabilities")
  void should_return_safe_verdict_when_no_new_vulnerabilities() {
    final String oldCode =
        """
        package com.example;

        public class Calculator {
            public int add(int a, int b) {
                return a + b;
            }
        }
        """;

    final String newCode =
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

    final DifferentialSecurityAnalysisRequest request =
        new DifferentialSecurityAnalysisRequest(oldCode, newCode, "java", "Calculator.java");

    final DifferentialSecurityAnalysisResponse response = analyzer.analyzeDiff(request);

    assertThat(response.verdict()).isEqualTo(SecurityVerdict.SAFE);
    assertThat(response.newFindings()).isEmpty();
    assertThat(response.fixedFindings()).isEmpty();
    assertThat(response.existingFindings()).isEmpty();
  }

  @Test
  @DisplayName("should_classify_existing_finding_when_unchanged")
  void should_classify_existing_finding_when_unchanged() {
    final String oldCode =
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

            public void helper() {
                System.out.println("Helper method");
            }
        }
        """;

    final String newCode =
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

            public void helper() {
                System.out.println("Updated helper method");
            }
        }
        """;

    final DifferentialSecurityAnalysisRequest request =
        new DifferentialSecurityAnalysisRequest(oldCode, newCode, "java", "UserDAO.java");

    final DifferentialSecurityAnalysisResponse response = analyzer.analyzeDiff(request);

    assertThat(response.verdict()).isEqualTo(SecurityVerdict.SAFE);
    assertThat(response.newFindings()).isEmpty();
    assertThat(response.existingFindings()).isNotEmpty();
  }

  @Test
  @DisplayName("should_handle_line_number_shifts_correctly")
  void should_handle_line_number_shifts_correctly() {
    final String oldCode =
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

    final String newCode =
        """
        package com.example;
        import java.sql.*;
        import java.util.logging.Logger;

        public class UserDAO {
            private static final Logger logger = Logger.getLogger(UserDAO.class.getName());

            public void getUser(String userId) throws SQLException {
                logger.info("Fetching user: " + userId);
                Connection conn = DriverManager.getConnection("jdbc:h2:mem:test");
                Statement stmt = conn.createStatement();
                String query = "SELECT * FROM users WHERE id = '" + userId + "'";
                ResultSet rs = stmt.executeQuery(query);
            }
        }
        """;

    final DifferentialSecurityAnalysisRequest request =
        new DifferentialSecurityAnalysisRequest(oldCode, newCode, "java", "UserDAO.java");

    final DifferentialSecurityAnalysisResponse response = analyzer.analyzeDiff(request);

    assertThat(response.verdict()).isEqualTo(SecurityVerdict.SAFE);
    assertThat(response.newFindings()).isEmpty();
    assertThat(response.existingFindings()).isNotEmpty();
  }

  @Test
  @DisplayName("should_handle_empty_code_gracefully")
  void should_handle_empty_code_gracefully() {
    final String oldCode =
        """
        package com.example;

        public class SimpleExample {}
        """;

    final String newCode =
        """
        package com.example;

        public class SimpleExample {}
        """;

    final DifferentialSecurityAnalysisRequest request =
        new DifferentialSecurityAnalysisRequest(oldCode, newCode, "java", "SimpleExample.java");

    final DifferentialSecurityAnalysisResponse response = analyzer.analyzeDiff(request);

    assertThat(response.verdict()).isEqualTo(SecurityVerdict.SAFE);
    assertThat(response.newFindings()).isEmpty();
    assertThat(response.fixedFindings()).isEmpty();
    assertThat(response.existingFindings()).isEmpty();
  }

  @Test
  @DisplayName("should_return_unsafe_when_both_new_and_existing_vulnerabilities")
  void should_return_unsafe_when_both_new_and_existing_vulnerabilities() {
    final String oldCode =
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

    final String newCode =
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

            public void deleteUser(String userId) throws SQLException {
                Connection conn = DriverManager.getConnection("jdbc:h2:mem:test");
                Statement stmt = conn.createStatement();
                String query = "DELETE FROM users WHERE id = '" + userId + "'";
                stmt.executeUpdate(query);
            }
        }
        """;

    final DifferentialSecurityAnalysisRequest request =
        new DifferentialSecurityAnalysisRequest(oldCode, newCode, "java", "UserDAO.java");

    final DifferentialSecurityAnalysisResponse response = analyzer.analyzeDiff(request);

    assertThat(response.verdict()).isEqualTo(SecurityVerdict.UNSAFE);
    assertThat(response.newFindings()).isNotEmpty();
    assertThat(response.existingFindings()).isNotEmpty();
  }
}
