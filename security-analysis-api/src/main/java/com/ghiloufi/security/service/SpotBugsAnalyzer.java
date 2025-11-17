package com.ghiloufi.security.service;

import com.ghiloufi.security.mapper.SpotBugsSeverityMapper;
import com.ghiloufi.security.model.SecurityAnalysisResponse;
import com.ghiloufi.security.model.SecurityFinding;
import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.config.UserPreferences;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.tools.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SpotBugsAnalyzer {

  private static final Logger logger = LoggerFactory.getLogger(SpotBugsAnalyzer.class);

  private static final String SPOTBUGS_VERSION = "4.8.3";

  public SecurityAnalysisResponse analyze(
      final String code, final String language, final String filename) {
    final long startTime = System.currentTimeMillis();

    if (!"java".equalsIgnoreCase(language)) {
      logger.warn("Unsupported language: {}, only Java is supported", language);
      return new SecurityAnalysisResponse(List.of(), "SpotBugs", SPOTBUGS_VERSION, 0L);
    }

    try {
      final Path tempDir = Files.createTempDirectory("spotbugs-analysis");
      final Path sourceFile = tempDir.resolve(filename);
      Files.writeString(sourceFile, code);

      final Path classFile = compileJavaCode(sourceFile, tempDir);

      if (classFile == null) {
        logger.error("Compilation failed for {}", filename);
        return new SecurityAnalysisResponse(
            List.of(), "SpotBugs", SPOTBUGS_VERSION, System.currentTimeMillis() - startTime);
      }

      final List<SecurityFinding> findings = runSpotBugsAnalysis(classFile, tempDir);

      cleanupTempDirectory(tempDir);

      return new SecurityAnalysisResponse(
          findings, "SpotBugs", SPOTBUGS_VERSION, System.currentTimeMillis() - startTime);

    } catch (final Exception e) {
      logger.error("Error during security analysis", e);
      return new SecurityAnalysisResponse(
          List.of(), "SpotBugs", SPOTBUGS_VERSION, System.currentTimeMillis() - startTime);
    }
  }

  private Path compileJavaCode(final Path sourceFile, final Path outputDir) {
    try {
      final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
      if (compiler == null) {
        logger.error("Java compiler not available. Ensure JDK (not JRE) is being used.");
        return null;
      }

      final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
      final StandardJavaFileManager fileManager =
          compiler.getStandardFileManager(diagnostics, null, null);

      final Iterable<? extends JavaFileObject> compilationUnits =
          fileManager.getJavaFileObjects(sourceFile);

      final List<String> options =
          Arrays.asList(
              "-d", outputDir.toString(), "-classpath", System.getProperty("java.class.path"));

      final JavaCompiler.CompilationTask task =
          compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits);

      final boolean success = task.call();

      if (!success) {
        for (final Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
          logger.debug(
              "Compilation error: {} at line {}",
              diagnostic.getMessage(Locale.ENGLISH),
              diagnostic.getLineNumber());
        }
        return null;
      }

      fileManager.close();

      final String className = sourceFile.getFileName().toString().replace(".java", ".class");
      return outputDir.resolve(className);

    } catch (final Exception e) {
      logger.error("Error during Java compilation", e);
      return null;
    }
  }

  private List<SecurityFinding> runSpotBugsAnalysis(final Path classFile, final Path tempDir) {
    try {
      final Path classDir = classFile.getParent();

      final Project project = new Project();
      project.addFile(classDir.toAbsolutePath().toString());
      project.addAuxClasspathEntry(System.getProperty("java.class.path"));

      final BugCollectionBugReporter bugReporter = new BugCollectionBugReporter(project);
      bugReporter.setPriorityThreshold(Priorities.LOW_PRIORITY);

      final FindBugs2 findBugs = new FindBugs2();
      findBugs.setProject(project);
      findBugs.setBugReporter(bugReporter);
      findBugs.setRankThreshold(15);

      final DetectorFactoryCollection detectorFactoryCollection =
          DetectorFactoryCollection.instance();
      findBugs.setDetectorFactoryCollection(detectorFactoryCollection);

      final UserPreferences userPreferences = UserPreferences.createDefaultUserPreferences();
      userPreferences.setEffort(UserPreferences.EFFORT_MIN);
      findBugs.setUserPreferences(userPreferences);

      findBugs.execute();

      return transformFindings(bugReporter.getBugCollection());

    } catch (final Exception e) {
      logger.error("Error running SpotBugs analysis", e);
      return List.of();
    }
  }

  private List<SecurityFinding> transformFindings(final BugCollection bugCollection) {
    final List<SecurityFinding> findings = new ArrayList<>();

    for (final BugInstance bug : bugCollection.getCollection()) {
      final String type = bug.getType();
      final String severity = SpotBugsSeverityMapper.mapPriority(bug.getPriority());
      final int line = bug.getPrimarySourceLineAnnotation().getStartLine();
      final String message = bug.getMessageWithoutPrefix();
      final String recommendation = getRecommendation(type);
      final String cweId = extractCweId(bug);
      final String owaspCategory = mapToOwaspCategory(type);

      findings.add(
          new SecurityFinding(type, severity, line, message, recommendation, cweId, owaspCategory));
    }

    return findings;
  }

  private String getRecommendation(final String bugType) {
    return switch (bugType) {
      case "SQL_INJECTION_JDBC", "SQL_INJECTION_JPA", "SQL_INJECTION_HIBERNATE" ->
          "Use parameterized queries (PreparedStatement) instead of string concatenation";
      case "WEAK_CIPHER", "DES_USAGE", "TDES_USAGE" ->
          "Use strong encryption algorithms like AES-256-GCM";
      case "HARD_CODE_PASSWORD", "HARD_CODE_KEY" ->
          "Store credentials in secure configuration or key management system";
      case "PATH_TRAVERSAL_IN", "PATH_TRAVERSAL_OUT" ->
          "Validate and sanitize file paths, use whitelisting";
      case "XSS_REQUEST_PARAMETER_TO_SEND_ERROR", "XSS_REQUEST_PARAMETER_TO_SERVLET_WRITER" ->
          "Sanitize user input and encode output properly";
      case "COMMAND_INJECTION" -> "Avoid executing system commands with user input";
      default -> "Review and remediate security issue according to OWASP guidelines";
    };
  }

  private String extractCweId(final BugInstance bug) {
    final BugPattern pattern = bug.getBugPattern();
    if (pattern == null) {
      return null;
    }

    final String detailHtml = pattern.getDetailHTML();
    if (detailHtml != null && detailHtml.contains("CWE-")) {
      final int startIndex = detailHtml.indexOf("CWE-");
      final int endIndex = detailHtml.indexOf("</", startIndex);
      if (endIndex > startIndex) {
        return detailHtml.substring(startIndex, endIndex).trim();
      }
    }

    return mapToCweId(bug.getType());
  }

  private String mapToCweId(final String bugType) {
    return switch (bugType) {
      case "SQL_INJECTION_JDBC", "SQL_INJECTION_JPA", "SQL_INJECTION_HIBERNATE" -> "CWE-89";
      case "WEAK_CIPHER", "DES_USAGE", "TDES_USAGE" -> "CWE-327";
      case "HARD_CODE_PASSWORD", "HARD_CODE_KEY" -> "CWE-798";
      case "PATH_TRAVERSAL_IN", "PATH_TRAVERSAL_OUT" -> "CWE-22";
      case "XSS_REQUEST_PARAMETER_TO_SEND_ERROR", "XSS_REQUEST_PARAMETER_TO_SERVLET_WRITER" ->
          "CWE-79";
      case "COMMAND_INJECTION" -> "CWE-78";
      default -> null;
    };
  }

  private String mapToOwaspCategory(final String bugType) {
    return switch (bugType) {
      case "SQL_INJECTION_JDBC", "SQL_INJECTION_JPA", "SQL_INJECTION_HIBERNATE" ->
          "A03:2021-Injection";
      case "WEAK_CIPHER", "DES_USAGE", "TDES_USAGE", "HARD_CODE_PASSWORD", "HARD_CODE_KEY" ->
          "A02:2021-Cryptographic Failures";
      case "PATH_TRAVERSAL_IN", "PATH_TRAVERSAL_OUT" -> "A01:2021-Broken Access Control";
      case "XSS_REQUEST_PARAMETER_TO_SEND_ERROR", "XSS_REQUEST_PARAMETER_TO_SERVLET_WRITER" ->
          "A03:2021-Injection";
      case "COMMAND_INJECTION" -> "A03:2021-Injection";
      default -> "A00:2021-Unknown";
    };
  }

  private void cleanupTempDirectory(final Path tempDir) {
    try {
      Files.walk(tempDir)
          .sorted((a, b) -> -a.compareTo(b))
          .forEach(
              path -> {
                try {
                  Files.deleteIfExists(path);
                } catch (final IOException e) {
                  logger.debug("Failed to delete temporary file: {}", path, e);
                }
              });
    } catch (final IOException e) {
      logger.warn("Failed to cleanup temporary directory: {}", tempDir, e);
    }
  }
}
