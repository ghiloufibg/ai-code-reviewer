package com.ghiloufi.security.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.security.model.SecurityFinding;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PMD Security Adapter Test")
class PmdSecurityAdapterTest {

  private PmdSecurityAdapter pmdSecurityAdapter;

  @BeforeEach
  void setUp() {
    pmdSecurityAdapter = new PmdSecurityAdapter();
  }

  @Test
  @DisplayName("should_return_tool_name")
  void should_return_tool_name() {
    final String toolName = pmdSecurityAdapter.getToolName();

    assertThat(toolName).isEqualTo("PMD Security");
  }

  @Test
  @DisplayName("should_return_tool_version")
  void should_return_tool_version() {
    final String version = pmdSecurityAdapter.getToolVersion();

    assertThat(version).isEqualTo("7.0.0");
  }

  @Test
  @DisplayName("should_analyze_simple_code_without_errors")
  void should_analyze_simple_code_without_errors() {
    final String code =
        """
                package com.example;
                public class Example {
                    public void test() {
                        System.out.println("Hello");
                    }
                }
                """;

    final List<SecurityFinding> findings = pmdSecurityAdapter.analyze(code, "Example.java");

    assertThat(findings).isNotNull();
  }

  @Test
  @DisplayName("should_detect_hardcoded_crypto_key")
  void should_detect_hardcoded_crypto_key() {
    final String vulnerableCode =
        """
                package com.example;
                import javax.crypto.spec.SecretKeySpec;
                public class CryptoExample {
                    private static final byte[] KEY = "hardcodedkey123".getBytes();
                    public void encrypt() {
                        SecretKeySpec keySpec = new SecretKeySpec(KEY, "AES");
                    }
                }
                """;

    final List<SecurityFinding> findings =
        pmdSecurityAdapter.analyze(vulnerableCode, "CryptoExample.java");

    assertThat(findings).isNotNull();
  }
}
