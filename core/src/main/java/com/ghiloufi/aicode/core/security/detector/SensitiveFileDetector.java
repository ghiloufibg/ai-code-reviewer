package com.ghiloufi.aicode.core.security.detector;

import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public final class SensitiveFileDetector {

  private static final Set<String> SENSITIVE_EXTENSIONS =
      Set.of(
          ".config",
          ".properties",
          ".yml",
          ".yaml",
          ".env",
          ".key",
          ".pem",
          ".crt",
          ".p12",
          ".jks",
          ".keystore",
          ".truststore",
          ".pfx");

  private static final Set<String> SENSITIVE_FILENAMES =
      Set.of(
          "application.properties",
          "application.yml",
          "application.yaml",
          "application-prod.properties",
          "application-prod.yml",
          "application-production.properties",
          "application-production.yml",
          "credentials",
          "secrets",
          "password",
          "secret.txt",
          ".env",
          ".env.production",
          "id_rsa",
          "id_dsa",
          "id_ecdsa",
          "id_ed25519");

  private static final List<String> SENSITIVE_PATH_PATTERNS =
      List.of("/config/", "/security/", "/auth/", "/credentials/", "/secrets/", "/.ssh/", "/keys/");

  public boolean isSensitiveFile(final String filePath) {
    if (filePath == null || filePath.isBlank()) {
      return false;
    }

    final String lowerPath = filePath.toLowerCase();
    final String fileName = extractFileName(filePath);

    if (SENSITIVE_EXTENSIONS.stream().anyMatch(lowerPath::endsWith)) {
      log.debug("File detected as sensitive due to extension: {}", filePath);
      return true;
    }

    if (SENSITIVE_FILENAMES.contains(fileName.toLowerCase())) {
      log.debug("File detected as sensitive due to filename: {}", filePath);
      return true;
    }

    if (SENSITIVE_PATH_PATTERNS.stream().anyMatch(lowerPath::contains)) {
      log.debug("File detected as sensitive due to path pattern: {}", filePath);
      return true;
    }

    return false;
  }

  public double getRequiredConfidenceThreshold(final String filePath) {
    return isSensitiveFile(filePath) ? 0.95 : 0.9;
  }

  public String getSensitivityReason(final String filePath) {
    if (!isSensitiveFile(filePath)) {
      return null;
    }

    final String lowerPath = filePath.toLowerCase();
    final String fileName = extractFileName(filePath);

    if (SENSITIVE_EXTENSIONS.stream().anyMatch(lowerPath::endsWith)) {
      return "File has sensitive extension (config, credentials, keys)";
    }

    if (SENSITIVE_FILENAMES.contains(fileName.toLowerCase())) {
      return "File is a known sensitive configuration file";
    }

    if (SENSITIVE_PATH_PATTERNS.stream().anyMatch(lowerPath::contains)) {
      return "File is in a sensitive directory (config, security, auth, credentials)";
    }

    return "File matches sensitive file patterns";
  }

  private String extractFileName(final String filePath) {
    final int lastSlash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
    return lastSlash >= 0 ? filePath.substring(lastSlash + 1) : filePath;
  }
}
