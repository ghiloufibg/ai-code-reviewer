package com.ghiloufi.security.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghiloufi.security.adapter.semgrep.SemgrepExtra;
import com.ghiloufi.security.adapter.semgrep.SemgrepResult;
import com.ghiloufi.security.adapter.semgrep.SemgrepResults;
import com.ghiloufi.security.exception.SecurityAnalysisException;
import com.ghiloufi.security.model.SecurityFinding;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "security.analysis.tools.semgrep.enabled", havingValue = "true")
public class SemgrepAdapter implements SecurityToolAdapter {

  private static final Logger logger = LoggerFactory.getLogger(SemgrepAdapter.class);

  private static final String SEMGREP_COMMAND = "semgrep";
  private static final String SEMGREP_CONFIG = "p/owasp-top-ten";
  private static final int TIMEOUT_SECONDS = 30;

  private final ObjectMapper objectMapper;

  public SemgrepAdapter(final ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public String getToolName() {
    return "Semgrep";
  }

  @Override
  public String getToolVersion() {
    try {
      final ProcessBuilder pb = new ProcessBuilder(SEMGREP_COMMAND, "--version");
      final Process process = pb.start();

      final String output = readOutput(process);

      if (process.waitFor(5, TimeUnit.SECONDS)) {
        return output.trim().lines().findFirst().orElse("unknown");
      }

      return "unknown";

    } catch (final Exception e) {
      logger.debug("Failed to get Semgrep version", e);
      return "unknown";
    }
  }

  @Override
  public List<SecurityFinding> analyze(final String code, final String filename) {
    if (!isAvailable()) {
      logger.warn("Semgrep is not available. Install it with: python3 -m pip install semgrep");
      return List.of();
    }

    try {
      final Path tempFile = createTempJavaFile(code, filename);

      final ProcessBuilder pb =
          new ProcessBuilder(
              SEMGREP_COMMAND,
              "--config",
              SEMGREP_CONFIG,
              "--json",
              "--quiet",
              "--no-git-ignore",
              tempFile.toString());

      final Process process = pb.start();
      final String output = readOutput(process);

      if (process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        final List<SecurityFinding> findings = parseSemgrepJson(output, filename);
        cleanupTempFile(tempFile);
        return findings;
      } else {
        process.destroyForcibly();
        cleanupTempFile(tempFile);
        throw new SecurityAnalysisException(
            "Semgrep timeout after " + TIMEOUT_SECONDS + " seconds");
      }

    } catch (final SecurityAnalysisException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Semgrep analysis failed", e);
      return List.of();
    }
  }

  private Path createTempJavaFile(final String code, final String filename) throws IOException {
    final Path tempDir = Files.createTempDirectory("semgrep-analysis");
    final Path tempFile = tempDir.resolve(filename);
    Files.writeString(tempFile, code);
    return tempFile;
  }

  List<SecurityFinding> parseSemgrepJson(final String json, final String filename) {
    try {
      final SemgrepResults results = objectMapper.readValue(json, SemgrepResults.class);

      return results.results().stream().map(this::toSecurityFinding).toList();

    } catch (final Exception e) {
      logger.error("Failed to parse Semgrep JSON output", e);
      return List.of();
    }
  }

  private SecurityFinding toSecurityFinding(final SemgrepResult result) {
    final SemgrepExtra extra = result.extra();

    return new SecurityFinding(
        result.checkId(),
        mapSeverity(extra.severity()),
        result.start().line(),
        extra.message(),
        getRecommendation(result.checkId()),
        extractCweId(extra.metadata()),
        extractOwaspCategory(extra.metadata()));
  }

  private String mapSeverity(final String semgrepSeverity) {
    return switch (semgrepSeverity.toUpperCase()) {
      case "ERROR" -> "HIGH";
      case "WARNING" -> "MEDIUM";
      case "INFO" -> "LOW";
      default -> "MEDIUM";
    };
  }

  private String getRecommendation(final String checkId) {
    if (checkId.contains("sql-injection")) {
      return "Use parameterized queries (PreparedStatement) to prevent SQL injection";
    }
    if (checkId.contains("hardcoded") || checkId.contains("secret")) {
      return "Store secrets in environment variables or secure key management system";
    }
    if (checkId.contains("xss") || checkId.contains("cross-site-scripting")) {
      return "Sanitize user input and use proper output encoding";
    }
    if (checkId.contains("crypto") || checkId.contains("cipher")) {
      return "Use strong modern cryptographic algorithms like AES-256-GCM";
    }
    if (checkId.contains("path-traversal")) {
      return "Validate and sanitize file paths using whitelist approach";
    }
    if (checkId.contains("xxe") || checkId.contains("xml-external-entity")) {
      return "Disable XML external entity processing in XML parsers";
    }
    return "Review security vulnerability and apply appropriate remediation";
  }

  private String extractCweId(final Map<String, Object> metadata) {
    if (metadata == null) {
      return null;
    }

    final Object cweObj = metadata.get("cwe");
    if (cweObj instanceof String cweString) {
      return cweString;
    }
    if (cweObj instanceof List<?> cweList && !cweList.isEmpty()) {
      return cweList.get(0).toString();
    }

    return null;
  }

  private String extractOwaspCategory(final Map<String, Object> metadata) {
    if (metadata == null) {
      return "A00:2021-Unknown";
    }

    final Object owaspObj = metadata.get("owasp");
    if (owaspObj instanceof String owaspString) {
      return owaspString;
    }
    if (owaspObj instanceof List<?> owaspList && !owaspList.isEmpty()) {
      return owaspList.get(0).toString();
    }

    return "A00:2021-Unknown";
  }

  private String readOutput(final Process process) throws IOException {
    final StringBuilder output = new StringBuilder();

    try (final BufferedReader reader =
        new BufferedReader(new InputStreamReader(process.getInputStream()))) {

      String line;
      while ((line = reader.readLine()) != null) {
        output.append(line).append("\n");
      }
    }

    return output.toString();
  }

  private void cleanupTempFile(final Path tempFile) {
    try {
      final Path tempDir = tempFile.getParent();
      Files.deleteIfExists(tempFile);
      Files.deleteIfExists(tempDir);
    } catch (final IOException e) {
      logger.debug("Failed to cleanup temporary file: {}", tempFile, e);
    }
  }
}
