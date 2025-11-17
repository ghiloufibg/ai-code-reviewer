package com.ghiloufi.aicode.core.security.detector;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SensitiveFileDetector Tests")
final class SensitiveFileDetectorTest {

  private SensitiveFileDetector detector;

  @BeforeEach
  final void setUp() {
    detector = new SensitiveFileDetector();
  }

  @Test
  @DisplayName("should_detect_config_files_as_sensitive")
  final void should_detect_config_files() {
    assertThat(detector.isSensitiveFile("application.properties")).isTrue();
    assertThat(detector.isSensitiveFile("application.yml")).isTrue();
    assertThat(detector.isSensitiveFile("application-prod.properties")).isTrue();
    assertThat(detector.isSensitiveFile("/config/database.yml")).isTrue();
  }

  @Test
  @DisplayName("should_detect_credential_files_as_sensitive")
  final void should_detect_credential_files() {
    assertThat(detector.isSensitiveFile(".env")).isTrue();
    assertThat(detector.isSensitiveFile(".env.production")).isTrue();
    assertThat(detector.isSensitiveFile("/app/credentials")).isTrue();
    assertThat(detector.isSensitiveFile("/config/secrets")).isTrue();
  }

  @Test
  @DisplayName("should_detect_key_files_as_sensitive")
  final void should_detect_key_files() {
    assertThat(detector.isSensitiveFile("server.key")).isTrue();
    assertThat(detector.isSensitiveFile("server.pem")).isTrue();
    assertThat(detector.isSensitiveFile("ca.crt")).isTrue();
    assertThat(detector.isSensitiveFile("keystore.jks")).isTrue();
    assertThat(detector.isSensitiveFile("truststore.p12")).isTrue();
  }

  @Test
  @DisplayName("should_detect_ssh_keys_as_sensitive")
  final void should_detect_ssh_keys() {
    assertThat(detector.isSensitiveFile("id_rsa")).isTrue();
    assertThat(detector.isSensitiveFile("id_dsa")).isTrue();
    assertThat(detector.isSensitiveFile("id_ecdsa")).isTrue();
    assertThat(detector.isSensitiveFile("id_ed25519")).isTrue();
    assertThat(detector.isSensitiveFile("/.ssh/id_rsa")).isTrue();
  }

  @Test
  @DisplayName("should_detect_sensitive_directories")
  final void should_detect_sensitive_directories() {
    assertThat(detector.isSensitiveFile("/config/app.properties")).isTrue();
    assertThat(detector.isSensitiveFile("/security/auth.yml")).isTrue();
    assertThat(detector.isSensitiveFile("/auth/oauth.config")).isTrue();
    assertThat(detector.isSensitiveFile("/credentials/api.key")).isTrue();
    assertThat(detector.isSensitiveFile("/secrets/db.password")).isTrue();
  }

  @Test
  @DisplayName("should_not_detect_regular_source_files_as_sensitive")
  final void should_not_detect_regular_files() {
    assertThat(detector.isSensitiveFile("User.java")).isFalse();
    assertThat(detector.isSensitiveFile("UserService.java")).isFalse();
    assertThat(detector.isSensitiveFile("README.md")).isFalse();
    assertThat(detector.isSensitiveFile("pom.xml")).isFalse();
    assertThat(detector.isSensitiveFile("/src/main/java/Main.java")).isFalse();
  }

  @Test
  @DisplayName("should_return_higher_confidence_threshold_for_sensitive_files")
  final void should_return_higher_threshold_for_sensitive() {
    assertThat(detector.getRequiredConfidenceThreshold("application.properties")).isEqualTo(0.95);
    assertThat(detector.getRequiredConfidenceThreshold(".env")).isEqualTo(0.95);
    assertThat(detector.getRequiredConfidenceThreshold("server.key")).isEqualTo(0.95);
  }

  @Test
  @DisplayName("should_return_standard_confidence_threshold_for_regular_files")
  final void should_return_standard_threshold_for_regular() {
    assertThat(detector.getRequiredConfidenceThreshold("User.java")).isEqualTo(0.9);
    assertThat(detector.getRequiredConfidenceThreshold("README.md")).isEqualTo(0.9);
  }

  @Test
  @DisplayName("should_provide_sensitivity_reason_for_sensitive_files")
  final void should_provide_sensitivity_reason() {
    assertThat(detector.getSensitivityReason("application.properties")).isNotNull();
    assertThat(detector.getSensitivityReason("server.key")).contains("extension");
    assertThat(detector.getSensitivityReason("/config/app.txt")).contains("directory");
  }

  @Test
  @DisplayName("should_return_null_reason_for_non_sensitive_files")
  final void should_return_null_reason_for_non_sensitive() {
    assertThat(detector.getSensitivityReason("User.java")).isNull();
    assertThat(detector.getSensitivityReason("README.md")).isNull();
  }

  @Test
  @DisplayName("should_handle_null_filepath_gracefully")
  final void should_handle_null_filepath() {
    assertThat(detector.isSensitiveFile(null)).isFalse();
    assertThat(detector.getSensitivityReason(null)).isNull();
  }

  @Test
  @DisplayName("should_handle_empty_filepath_gracefully")
  final void should_handle_empty_filepath() {
    assertThat(detector.isSensitiveFile("")).isFalse();
    assertThat(detector.isSensitiveFile("   ")).isFalse();
  }

  @Test
  @DisplayName("should_be_case_insensitive")
  final void should_be_case_insensitive() {
    assertThat(detector.isSensitiveFile("APPLICATION.PROPERTIES")).isTrue();
    assertThat(detector.isSensitiveFile("Server.KEY")).isTrue();
    assertThat(detector.isSensitiveFile("/CONFIG/app.yml")).isTrue();
  }

  @Test
  @DisplayName("should_handle_windows_and_unix_paths")
  final void should_handle_different_path_separators() {
    assertThat(detector.isSensitiveFile("C:\\config\\app.properties")).isTrue();
    assertThat(detector.isSensitiveFile("/etc/config/app.properties")).isTrue();
    assertThat(detector.isSensitiveFile("\\secrets\\api.key")).isTrue();
  }
}
