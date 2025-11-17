package com.ghiloufi.security.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.security.model.SecurityFinding;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Dependency Check Adapter Test")
class DependencyCheckAdapterTest {

  private DependencyCheckAdapter dependencyCheckAdapter;

  @BeforeEach
  void setUp() {
    dependencyCheckAdapter = new DependencyCheckAdapter();
  }

  @Test
  @DisplayName("should_return_tool_name")
  void should_return_tool_name() {
    final String toolName = dependencyCheckAdapter.getToolName();

    assertThat(toolName).isEqualTo("Dependency Security Scanner");
  }

  @Test
  @DisplayName("should_return_tool_version")
  void should_return_tool_version() {
    final String version = dependencyCheckAdapter.getToolVersion();

    assertThat(version).isEqualTo("1.0.0");
  }

  @Test
  @DisplayName("should_detect_log4shell_vulnerability")
  void should_detect_log4shell_vulnerability() {
    final String codeWithVulnerableDependency =
        """
                package com.example;
                import org.apache.logging.log4j.LogManager;
                import org.apache.logging.log4j.Logger;

                public class Application {
                    private static final Logger logger = LogManager.getLogger(Application.class);

                    public void logUserInput(String input) {
                        logger.info("User input: {}", input);
                    }
                }
                """;

    final List<SecurityFinding> findings =
        dependencyCheckAdapter.analyze(codeWithVulnerableDependency, "Application.java");

    assertThat(findings)
        .anySatisfy(
            finding -> {
              assertThat(finding.type()).contains("CVE-2021-44228");
              assertThat(finding.severity()).isEqualTo("CRITICAL");
            });
  }

  @Test
  @DisplayName("should_detect_spring4shell_vulnerability")
  void should_detect_spring4shell_vulnerability() {
    final String codeWithSpring =
        """
                package com.example;
                import org.springframework.core.SpringVersion;

                public class SpringApp {
                    public String getVersion() {
                        return SpringVersion.getVersion();
                    }
                }
                """;

    final List<SecurityFinding> findings =
        dependencyCheckAdapter.analyze(codeWithSpring, "SpringApp.java");

    assertThat(findings)
        .anySatisfy(
            finding -> {
              assertThat(finding.type()).contains("CVE-2022-22965");
              assertThat(finding.severity()).isEqualTo("HIGH");
            });
  }

  @Test
  @DisplayName("should_return_empty_list_when_no_vulnerable_dependencies")
  void should_return_empty_list_when_no_vulnerable_dependencies() {
    final String safeCode =
        """
                package com.example;
                import java.util.ArrayList;
                import java.util.List;

                public class SafeCode {
                    public List<String> getItems() {
                        return new ArrayList<>();
                    }
                }
                """;

    final List<SecurityFinding> findings =
        dependencyCheckAdapter.analyze(safeCode, "SafeCode.java");

    assertThat(findings).isEmpty();
  }
}
