package com.ghiloufi.security.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghiloufi.security.model.DifferentialSecurityAnalysisRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Differential Security Analysis Integration Test")
class DifferentialSecurityAnalysisIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  @DisplayName("should_analyze_diff_through_controller")
  void should_analyze_diff_through_controller() throws Exception {
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

    final MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/security/analyze-diff")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.verdict").exists())
            .andExpect(jsonPath("$.newFindings").isArray())
            .andExpect(jsonPath("$.fixedFindings").isArray())
            .andExpect(jsonPath("$.existingFindings").isArray())
            .andExpect(jsonPath("$.tool").exists())
            .andExpect(jsonPath("$.toolVersion").exists())
            .andExpect(jsonPath("$.analysisTimeMs").exists())
            .andReturn();

    final String responseContent = result.getResponse().getContentAsString();
    assertThat(responseContent).contains("verdict");
    assertThat(responseContent).contains("newFindings");
    assertThat(responseContent).contains("fixedFindings");
    assertThat(responseContent).contains("existingFindings");
  }

  @Test
  @DisplayName("should_validate_required_fields")
  void should_validate_required_fields() throws Exception {
    final DifferentialSecurityAnalysisRequest invalidRequest =
        new DifferentialSecurityAnalysisRequest("", "", "", "");

    mockMvc
        .perform(
            post("/api/v1/security/analyze-diff")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("should_return_unsafe_verdict_when_vulnerability_introduced")
  void should_return_unsafe_verdict_when_vulnerability_introduced() throws Exception {
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

    mockMvc
        .perform(
            post("/api/v1/security/analyze-diff")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.verdict").value("UNSAFE"))
        .andExpect(jsonPath("$.newFindings").isNotEmpty());
  }

  @Test
  @DisplayName("should_return_improved_verdict_when_vulnerability_fixed")
  void should_return_improved_verdict_when_vulnerability_fixed() throws Exception {
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

    mockMvc
        .perform(
            post("/api/v1/security/analyze-diff")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.verdict").value("IMPROVED"))
        .andExpect(jsonPath("$.fixedFindings").isNotEmpty());
  }

  @Test
  @DisplayName("should_return_safe_verdict_for_clean_code")
  void should_return_safe_verdict_for_clean_code() throws Exception {
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

            public int subtract(int a, int b) {
                return a - b;
            }
        }
        """;

    final DifferentialSecurityAnalysisRequest request =
        new DifferentialSecurityAnalysisRequest(oldCode, newCode, "java", "Calculator.java");

    mockMvc
        .perform(
            post("/api/v1/security/analyze-diff")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.verdict").value("SAFE"))
        .andExpect(jsonPath("$.newFindings").isEmpty())
        .andExpect(jsonPath("$.fixedFindings").isEmpty());
  }
}
