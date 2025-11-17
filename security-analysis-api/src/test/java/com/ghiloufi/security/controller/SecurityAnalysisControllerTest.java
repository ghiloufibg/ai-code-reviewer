package com.ghiloufi.security.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghiloufi.security.model.SecurityAnalysisRequest;
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
class SecurityAnalysisControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  @DisplayName("should_analyze_code_and_return_findings_for_sql_injection")
  void should_analyze_code_and_return_findings_for_sql_injection() throws Exception {
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

    final SecurityAnalysisRequest request =
        new SecurityAnalysisRequest(vulnerableCode, "java", "VulnerableDAO.java");

    final MvcResult result =
        mockMvc
            .perform(
                post("/api/security/analyze")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.tool").value(containsString("SpotBugs")))
            .andExpect(jsonPath("$.toolVersion").isNotEmpty())
            .andExpect(jsonPath("$.findings").isArray())
            .andExpect(jsonPath("$.findings").isNotEmpty())
            .andExpect(jsonPath("$.analysisTimeMs").isNumber())
            .andReturn();

    final String responseBody = result.getResponse().getContentAsString();
    assertThat(responseBody).contains("SQL");
  }

  @Test
  @DisplayName("should_analyze_code_and_return_findings_for_weak_cryptography")
  void should_analyze_code_and_return_findings_for_weak_cryptography() throws Exception {
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

    final SecurityAnalysisRequest request =
        new SecurityAnalysisRequest(vulnerableCode, "java", "WeakCrypto.java");

    mockMvc
        .perform(
            post("/api/security/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.tool").value(containsString("SpotBugs")))
        .andExpect(jsonPath("$.findings").isArray());
  }

  @Test
  @DisplayName("should_return_empty_findings_for_secure_code")
  void should_return_empty_findings_for_secure_code() throws Exception {
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

    final SecurityAnalysisRequest request =
        new SecurityAnalysisRequest(secureCode, "java", "SecureClass.java");

    mockMvc
        .perform(
            post("/api/security/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.tool").value(containsString("SpotBugs")))
        .andExpect(jsonPath("$.findings").isArray())
        .andExpect(jsonPath("$.findings").isEmpty());
  }

  @Test
  @DisplayName("should_return_400_when_request_has_blank_code")
  void should_return_400_when_request_has_blank_code() throws Exception {
    final SecurityAnalysisRequest request = new SecurityAnalysisRequest("", "java", "Test.java");

    mockMvc
        .perform(
            post("/api/security/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("should_return_400_when_request_has_blank_language")
  void should_return_400_when_request_has_blank_language() throws Exception {
    final SecurityAnalysisRequest request =
        new SecurityAnalysisRequest("public class Test {}", "", "Test.java");

    mockMvc
        .perform(
            post("/api/security/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("should_return_400_when_request_has_blank_filename")
  void should_return_400_when_request_has_blank_filename() throws Exception {
    final SecurityAnalysisRequest request =
        new SecurityAnalysisRequest("public class Test {}", "java", "");

    mockMvc
        .perform(
            post("/api/security/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("should_return_422_when_language_is_not_supported")
  void should_return_422_when_language_is_not_supported() throws Exception {
    final SecurityAnalysisRequest request =
        new SecurityAnalysisRequest("def hello():\n    print('hello')", "python", "test.py");

    mockMvc
        .perform(
            post("/api/security/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.error").value("Unsupported language"))
        .andExpect(jsonPath("$.message").value("Only Java is supported in version 0.0.2"));
  }

  @Test
  @DisplayName("should_handle_compilation_errors_gracefully")
  void should_handle_compilation_errors_gracefully() throws Exception {
    final String invalidCode =
        """
        package com.example;
        public class Invalid {
            this is not valid java code
        }
        """;

    final SecurityAnalysisRequest request =
        new SecurityAnalysisRequest(invalidCode, "java", "Invalid.java");

    mockMvc
        .perform(
            post("/api/security/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.findings").isEmpty());
  }

  @Test
  @DisplayName("should_return_400_for_malformed_json")
  void should_return_400_for_malformed_json() throws Exception {
    final String malformedJson = "{\"code\": \"test\", \"language\": ";

    mockMvc
        .perform(
            post("/api/security/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("should_return_415_for_unsupported_content_type")
  void should_return_415_for_unsupported_content_type() throws Exception {
    final SecurityAnalysisRequest request =
        new SecurityAnalysisRequest("public class Test {}", "java", "Test.java");

    mockMvc
        .perform(
            post("/api/security/analyze")
                .contentType(MediaType.TEXT_PLAIN)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnsupportedMediaType());
  }

  @Test
  @DisplayName("should_return_400_when_all_fields_are_blank")
  void should_return_400_when_all_fields_are_blank() throws Exception {
    final SecurityAnalysisRequest request = new SecurityAnalysisRequest("", "", "");

    mockMvc
        .perform(
            post("/api/security/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }
}
