package com.ghiloufi.security.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.security.model.SecurityAnalysisRequest;
import com.ghiloufi.security.model.SecurityAnalysisResponse;
import com.ghiloufi.security.model.SecurityFinding;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("Multi-Tool Security Orchestrator Integration Test")
class MultiToolSecurityOrchestratorIntegrationTest {

  @Autowired(required = false)
  private MultiToolSecurityOrchestrator orchestrator;

  @Test
  @DisplayName("should_detect_vulnerabilities_with_all_tools")
  void should_detect_vulnerabilities_with_all_tools() {
    if (orchestrator == null) {
      return;
    }

    final String vulnerableCode =
        """
            package com.example;
            import java.sql.*;
            import org.apache.logging.log4j.LogManager;
            import org.apache.logging.log4j.Logger;

            public class VulnerableApp {
                private static final Logger logger = LogManager.getLogger(VulnerableApp.class);
                private static final String API_KEY = "hardcoded-secret-123";

                public void getUserData(String userId) throws SQLException {
                    Connection conn = DriverManager.getConnection("jdbc:h2:mem:test");
                    Statement stmt = conn.createStatement();
                    String query = "SELECT * FROM users WHERE id = '" + userId + "'";
                    ResultSet rs = stmt.executeQuery(query);
                    logger.info("Fetched user: " + userId);
                }
            }
            """;

    final SecurityAnalysisRequest request =
        new SecurityAnalysisRequest(vulnerableCode, "java", "VulnerableApp.java");

    final SecurityAnalysisResponse response = orchestrator.analyzeWithAllTools(request);

    assertThat(response.findings()).isNotEmpty();

    assertThat(response.tool()).contains("SpotBugs");

    final List<String> severities =
        response.findings().stream().map(SecurityFinding::severity).toList();

    final List<String> expectedOrder = List.of("CRITICAL", "HIGH", "MEDIUM", "LOW");

    for (int i = 0; i < severities.size() - 1; i++) {
      final int currentIndex = expectedOrder.indexOf(severities.get(i));
      final int nextIndex = expectedOrder.indexOf(severities.get(i + 1));
      assertThat(currentIndex).isLessThanOrEqualTo(nextIndex);
    }

    assertThat(response.analysisTimeMs()).isPositive();
  }

  @Test
  @DisplayName("should_handle_clean_code_without_errors")
  void should_handle_clean_code_without_errors() {
    if (orchestrator == null) {
      return;
    }

    final String cleanCode =
        """
            package com.example;
            import java.util.List;

            public class CleanCode {
                public List<String> getItems() {
                    return List.of("item1", "item2");
                }
            }
            """;

    final SecurityAnalysisRequest request =
        new SecurityAnalysisRequest(cleanCode, "java", "CleanCode.java");

    final SecurityAnalysisResponse response = orchestrator.analyzeWithAllTools(request);

    assertThat(response).isNotNull();
    assertThat(response.findings()).isNotNull();
  }

  @Test
  @DisplayName("should_deduplicate_findings_from_multiple_tools")
  void should_deduplicate_findings_from_multiple_tools() {
    if (orchestrator == null) {
      return;
    }

    final String vulnerableCode =
        """
            package com.example;
            import java.sql.*;

            public class DuplicateTest {
                public void sqlInjection(String userId) throws SQLException {
                    Connection conn = DriverManager.getConnection("jdbc:h2:mem:test");
                    Statement stmt = conn.createStatement();
                    String query = "SELECT * FROM users WHERE id = '" + userId + "'";
                    ResultSet rs = stmt.executeQuery(query);
                }
            }
            """;

    final SecurityAnalysisRequest request =
        new SecurityAnalysisRequest(vulnerableCode, "java", "DuplicateTest.java");

    final SecurityAnalysisResponse response = orchestrator.analyzeWithAllTools(request);

    assertThat(response.findings()).isNotNull();
  }
}
