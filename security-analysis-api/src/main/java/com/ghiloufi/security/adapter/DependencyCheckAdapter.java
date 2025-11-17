package com.ghiloufi.security.adapter;

import com.ghiloufi.security.model.SecurityFinding;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
    name = "security.analysis.tools.dependency-check.enabled",
    havingValue = "true")
public class DependencyCheckAdapter implements SecurityToolAdapter {

  private static final Logger logger = LoggerFactory.getLogger(DependencyCheckAdapter.class);

  private static final String TOOL_VERSION = "1.0.0";

  private static final Pattern IMPORT_PATTERN =
      Pattern.compile("^import\\s+([\\w.]+);", Pattern.MULTILINE);

  private static final Map<String, VulnerableLibrary> KNOWN_VULNERABILITIES =
      Map.ofEntries(
          Map.entry(
              "org.apache.logging.log4j",
              new VulnerableLibrary(
                  "log4j-core",
                  "CVE-2021-44228",
                  "CRITICAL",
                  "Log4Shell - Remote Code Execution vulnerability",
                  "Upgrade to log4j 2.17.1 or later",
                  "CWE-502",
                  "A06:2021-Vulnerable and Outdated Components")),
          Map.entry(
              "org.springframework.core",
              new VulnerableLibrary(
                  "spring-core",
                  "CVE-2022-22965",
                  "HIGH",
                  "Spring4Shell - Remote Code Execution in Spring Framework",
                  "Upgrade to Spring Framework 5.3.18+ or 5.2.20+",
                  "CWE-94",
                  "A06:2021-Vulnerable and Outdated Components")),
          Map.entry(
              "com.fasterxml.jackson",
              new VulnerableLibrary(
                  "jackson-databind",
                  "CVE-2020-36518",
                  "HIGH",
                  "Jackson Databind deserialization vulnerability",
                  "Upgrade to jackson-databind 2.12.6.1 or later",
                  "CWE-502",
                  "A08:2021-Software and Data Integrity Failures")),
          Map.entry(
              "org.apache.commons.collections",
              new VulnerableLibrary(
                  "commons-collections",
                  "CVE-2015-6420",
                  "CRITICAL",
                  "Apache Commons Collections deserialization vulnerability",
                  "Upgrade to commons-collections 3.2.2 or commons-collections4 4.1+",
                  "CWE-502",
                  "A08:2021-Software and Data Integrity Failures")),
          Map.entry(
              "org.yaml.snakeyaml",
              new VulnerableLibrary(
                  "snakeyaml",
                  "CVE-2022-1471",
                  "HIGH",
                  "SnakeYAML deserialization vulnerability",
                  "Upgrade to snakeyaml 2.0 or later",
                  "CWE-502",
                  "A08:2021-Software and Data Integrity Failures")),
          Map.entry(
              "org.postgresql",
              new VulnerableLibrary(
                  "postgresql",
                  "CVE-2022-31197",
                  "HIGH",
                  "PostgreSQL JDBC Driver SQL Injection vulnerability",
                  "Upgrade to postgresql 42.4.1 or later",
                  "CWE-89",
                  "A03:2021-Injection")));

  @Override
  public String getToolName() {
    return "Dependency Security Scanner";
  }

  @Override
  public String getToolVersion() {
    return TOOL_VERSION;
  }

  @Override
  public List<SecurityFinding> analyze(final String code, final String filename) {
    final List<SecurityFinding> findings = new ArrayList<>();

    final List<String> imports = extractImports(code);

    for (final String importStatement : imports) {
      final String packageName = extractPackageName(importStatement);

      for (final Map.Entry<String, VulnerableLibrary> entry : KNOWN_VULNERABILITIES.entrySet()) {
        if (packageName.startsWith(entry.getKey())) {
          final VulnerableLibrary vuln = entry.getValue();
          final int lineNumber = findImportLineNumber(code, importStatement);

          findings.add(
              new SecurityFinding(
                  vuln.cveId(),
                  vuln.severity(),
                  lineNumber,
                  String.format(
                      "Vulnerable dependency detected: %s - %s",
                      vuln.library(), vuln.description()),
                  vuln.remediation(),
                  vuln.cweId(),
                  vuln.owaspCategory()));
        }
      }
    }

    if (!findings.isEmpty()) {
      logger.info("Found {} vulnerable dependencies in {}", findings.size(), filename);
    }

    return findings;
  }

  private List<String> extractImports(final String code) {
    final List<String> imports = new ArrayList<>();
    final Matcher matcher = IMPORT_PATTERN.matcher(code);

    while (matcher.find()) {
      imports.add(matcher.group(1));
    }

    return imports;
  }

  private String extractPackageName(final String importStatement) {
    final int lastDotIndex = importStatement.lastIndexOf('.');
    if (lastDotIndex > 0) {
      return importStatement.substring(0, lastDotIndex);
    }
    return importStatement;
  }

  private int findImportLineNumber(final String code, final String importStatement) {
    final String[] lines = code.split("\n");
    for (int i = 0; i < lines.length; i++) {
      if (lines[i].contains(importStatement)) {
        return i + 1;
      }
    }
    return 0;
  }

  private record VulnerableLibrary(
      String library,
      String cveId,
      String severity,
      String description,
      String remediation,
      String cweId,
      String owaspCategory) {}
}
