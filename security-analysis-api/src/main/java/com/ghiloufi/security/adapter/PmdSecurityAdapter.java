package com.ghiloufi.security.adapter;

import com.ghiloufi.security.model.SecurityFinding;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.rule.RulePriority;
import net.sourceforge.pmd.reporting.FileAnalysisListener;
import net.sourceforge.pmd.reporting.GlobalAnalysisListener;
import net.sourceforge.pmd.reporting.RuleViolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "security.analysis.tools.pmd.enabled", havingValue = "true")
public class PmdSecurityAdapter implements SecurityToolAdapter {

  private static final Logger logger = LoggerFactory.getLogger(PmdSecurityAdapter.class);

  private static final String PMD_VERSION = "7.0.0";
  private static final String SECURITY_RULESET = "category/java/security.xml";

  @Override
  public String getToolName() {
    return "PMD Security";
  }

  @Override
  public String getToolVersion() {
    return PMD_VERSION;
  }

  @Override
  public List<SecurityFinding> analyze(final String code, final String filename) {
    try {
      final Path tempFile = createTempJavaFile(code, filename);

      final PMDConfiguration config = new PMDConfiguration();
      config.setInputPathList(List.of(tempFile));
      config.addRuleSet(SECURITY_RULESET);
      config.setThreads(1);
      config.setIgnoreIncrementalAnalysis(true);

      final List<SecurityFinding> findings = new ArrayList<>();

      final GlobalAnalysisListener listener =
          new GlobalAnalysisListener() {
            @Override
            public FileAnalysisListener startFileAnalysis(
                final net.sourceforge.pmd.lang.document.TextFile file) {
              return new FileAnalysisListener() {
                @Override
                public void onRuleViolation(final RuleViolation violation) {
                  findings.add(toSecurityFinding(violation));
                }
              };
            }

            @Override
            public void close() throws Exception {}
          };

      try (final PmdAnalysis analysis = PmdAnalysis.create(config)) {
        analysis.addListener(listener);
        analysis.performAnalysis();
      }

      cleanupTempFile(tempFile);

      return findings;

    } catch (final Exception e) {
      logger.error("PMD Security analysis failed", e);
      return List.of();
    }
  }

  private Path createTempJavaFile(final String code, final String filename) throws IOException {
    final Path tempDir = Files.createTempDirectory("pmd-analysis");
    final Path tempFile = tempDir.resolve(filename);
    Files.writeString(tempFile, code);
    return tempFile;
  }

  private SecurityFinding toSecurityFinding(final RuleViolation violation) {
    final String ruleName = violation.getRule().getName();
    final String severity = mapPmdPriority(violation.getRule().getPriority());
    final int line = violation.getBeginLine();
    final String message = violation.getDescription();
    final String recommendation = getRecommendation(ruleName);
    final String cweId = extractCweFromRule(ruleName);
    final String owaspCategory = extractOwaspFromRule(ruleName);

    return new SecurityFinding(
        ruleName, severity, line, message, recommendation, cweId, owaspCategory);
  }

  private String mapPmdPriority(final RulePriority priority) {
    return switch (priority) {
      case HIGH -> "HIGH";
      case MEDIUM_HIGH, MEDIUM -> "MEDIUM";
      case MEDIUM_LOW, LOW -> "LOW";
    };
  }

  private String getRecommendation(final String ruleName) {
    return switch (ruleName) {
      case "HardCodedCryptoKey" ->
          "Store cryptographic keys in secure key management system (e.g., AWS KMS, HashiCorp Vault)";
      case "InsecureCryptoIv" ->
          "Generate cryptographically random IV using SecureRandom for each encryption operation";
      case "WeakCryptography" ->
          "Use strong modern cryptographic algorithms like AES-256-GCM or ChaCha20-Poly1305";
      case "AvoidMessageDigestField" ->
          "Create new MessageDigest instance for each use to ensure thread safety";
      case "InsecureRandomNumberGenerator" ->
          "Use java.security.SecureRandom instead of java.util.Random for security-sensitive operations";
      case "AvoidPrintStackTrace" ->
          "Use proper logging framework and avoid exposing stack traces to users";
      default -> "Review PMD security violation and apply appropriate security remediation";
    };
  }

  private String extractCweFromRule(final String ruleName) {
    return switch (ruleName) {
      case "HardCodedCryptoKey" -> "CWE-798";
      case "InsecureCryptoIv", "WeakCryptography" -> "CWE-327";
      case "InsecureRandomNumberGenerator" -> "CWE-338";
      case "AvoidPrintStackTrace" -> "CWE-209";
      default -> null;
    };
  }

  private String extractOwaspFromRule(final String ruleName) {
    return switch (ruleName) {
      case "HardCodedCryptoKey" -> "A02:2021-Cryptographic Failures";
      case "InsecureCryptoIv", "WeakCryptography" -> "A02:2021-Cryptographic Failures";
      case "InsecureRandomNumberGenerator" -> "A02:2021-Cryptographic Failures";
      case "AvoidPrintStackTrace" -> "A09:2021-Security Logging and Monitoring Failures";
      default -> "A00:2021-Unknown";
    };
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
