package com.ghiloufi.aicode.service.analysis;

import com.ghiloufi.aicode.api.model.*;
import com.ghiloufi.aicode.service.analysis.model.AnalysisEntity;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Generates realistic dummy data for analysis results. Creates credible code issues, suggestions,
 * and metrics for demonstration.
 */
@Component
@Slf4j
public class DummyDataGenerator {

  private static final List<String> COMMON_FILES =
      Arrays.asList(
          "src/main/java/UserService.java",
          "src/main/java/OrderController.java",
          "src/main/java/PaymentProcessor.java",
          "src/main/java/SecurityUtils.java",
          "src/main/java/DatabaseConnection.java",
          "src/main/java/EmailService.java",
          "src/test/java/UserServiceTest.java",
          "src/main/resources/application.properties");

  private static final List<IssueTemplate> ISSUE_TEMPLATES =
      Arrays.asList(
          // Security Issues
          new IssueTemplate(
              CodeIssue.SeverityEnum.CRITICAL,
              CodeIssue.TypeEnum.VULNERABILITY,
              "SQL Injection vulnerability detected",
              "Direct string concatenation in SQL query without parameterization",
              "Use PreparedStatement with parameterized queries",
              "CRITICAL_SQL_INJECTION"),
          new IssueTemplate(
              CodeIssue.SeverityEnum.HIGH,
              CodeIssue.TypeEnum.VULNERABILITY,
              "Potential XSS vulnerability",
              "User input is not properly sanitized before output",
              "Sanitize user input using appropriate encoding",
              "XSS_VULNERABILITY"),
          new IssueTemplate(
              CodeIssue.SeverityEnum.MEDIUM,
              CodeIssue.TypeEnum.VULNERABILITY,
              "Weak cryptographic algorithm",
              "MD5 hash algorithm is cryptographically broken",
              "Use SHA-256 or stronger hashing algorithms",
              "WEAK_CRYPTO"),

          // Performance Issues
          new IssueTemplate(
              CodeIssue.SeverityEnum.HIGH,
              CodeIssue.TypeEnum.PERFORMANCE,
              "N+1 query problem detected",
              "Multiple database queries in loop causing performance degradation",
              "Use batch queries or eager loading to reduce database calls",
              "N_PLUS_ONE_QUERY"),
          new IssueTemplate(
              CodeIssue.SeverityEnum.MEDIUM,
              CodeIssue.TypeEnum.PERFORMANCE,
              "Inefficient string concatenation",
              "String concatenation in loop creates multiple temporary objects",
              "Use StringBuilder for efficient string concatenation",
              "INEFFICIENT_STRING_CONCAT"),

          // Code Quality Issues
          new IssueTemplate(
              CodeIssue.SeverityEnum.MEDIUM,
              CodeIssue.TypeEnum.CODE_SMELL,
              "Method too long",
              "Method exceeds recommended length of 50 lines",
              "Extract method to improve readability and maintainability",
              "METHOD_TOO_LONG"),
          new IssueTemplate(
              CodeIssue.SeverityEnum.LOW,
              CodeIssue.TypeEnum.STYLE,
              "Unused import statement",
              "Import statement is declared but never used",
              "Remove unused import to clean up code",
              "UNUSED_IMPORT"),
          new IssueTemplate(
              CodeIssue.SeverityEnum.MEDIUM,
              CodeIssue.TypeEnum.BUG,
              "Null pointer dereference risk",
              "Variable may be null when dereferenced",
              "Add null check before dereferencing",
              "NULL_POINTER_RISK"),
          new IssueTemplate(
              CodeIssue.SeverityEnum.LOW,
              CodeIssue.TypeEnum.MAINTAINABILITY,
              "Magic number usage",
              "Hardcoded numeric literal should be replaced with named constant",
              "Define a named constant for better maintainability",
              "MAGIC_NUMBER"));

  private static final List<SuggestionTemplate> SUGGESTION_TEMPLATES =
      Arrays.asList(
          new SuggestionTemplate(
              "Extract common validation logic",
              "Multiple methods contain similar validation patterns",
              "Create a ValidationUtils class to centralize validation logic",
              CodeSuggestion.CategoryEnum.REFACTORING,
              CodeSuggestion.ImpactEnum.MEDIUM),
          new SuggestionTemplate(
              "Implement caching for expensive operations",
              "Database queries are repeated without caching",
              "Add Redis or in-memory caching to improve performance",
              CodeSuggestion.CategoryEnum.OPTIMIZATION,
              CodeSuggestion.ImpactEnum.HIGH),
          new SuggestionTemplate(
              "Add logging for better observability",
              "Critical operations lack proper logging",
              "Add structured logging with correlation IDs",
              CodeSuggestion.CategoryEnum.BEST_PRACTICE,
              CodeSuggestion.ImpactEnum.LOW),
          new SuggestionTemplate(
              "Implement proper exception handling",
              "Generic exception catching reduces error visibility",
              "Catch specific exceptions and handle appropriately",
              CodeSuggestion.CategoryEnum.BUG_FIX,
              CodeSuggestion.ImpactEnum.MEDIUM));

  /** Generate complete analysis results with realistic dummy data */
  public AnalysisResults generateAnalysisResults(String analysisId, AnalysisEntity entity) {
    log.debug("Generating dummy analysis results for: {}", analysisId);

    // Generate issues and suggestions
    List<CodeIssue> issues = generateCodeIssues(entity);
    List<CodeSuggestion> suggestions = generateCodeSuggestions(entity);

    // Create analysis summary
    AnalysisSummary summary = createAnalysisSummary(issues, entity);

    // Create metrics
    AnalysisMetrics metrics = createAnalysisMetrics(entity);

    // Create file results
    List<FileAnalysisResult> fileResults = generateFileResults(entity, issues);

    return new AnalysisResults()
        .id(analysisId)
        .status(AnalysisResults.StatusEnum.COMPLETED)
        .summary(summary)
        .issues(issues)
        .suggestions(suggestions)
        .metrics(metrics)
        .fileResults(fileResults);
  }

  /** Generate realistic code issues */
  private List<CodeIssue> generateCodeIssues(AnalysisEntity entity) {
    List<CodeIssue> issues = new ArrayList<>();
    Random random = ThreadLocalRandom.current();

    // Generate 15-25 issues per analysis
    int issueCount = 15 + random.nextInt(11);

    for (int i = 0; i < issueCount; i++) {
      IssueTemplate template = ISSUE_TEMPLATES.get(random.nextInt(ISSUE_TEMPLATES.size()));
      String fileName =
          entity.getUploadedFiles().isEmpty()
              ? getRandomFileName()
              : entity.getUploadedFiles().get(random.nextInt(entity.getUploadedFiles().size()));

      CodeIssue issue =
          new CodeIssue()
              .id("issue-" + UUID.randomUUID().toString().substring(0, 8))
              .type(template.type)
              .severity(template.severity)
              .title(template.title)
              .description(template.description)
              .file(fileName)
              .line(random.nextInt(200) + 1)
              .column(random.nextInt(80) + 1)
              .suggestion(template.suggestion)
              .rule(template.rule)
              .tool(random.nextBoolean() ? "AI Analysis" : "Static Analysis")
              .effort(getEffortEstimate(template.severity));

      // Add code snippet for some issues
      if (random.nextBoolean()) {
        issue.code(generateCodeSnippet(template));
      }

      issues.add(issue);
    }

    return issues;
  }

  /** Generate code improvement suggestions */
  private List<CodeSuggestion> generateCodeSuggestions(AnalysisEntity entity) {
    List<CodeSuggestion> suggestions = new ArrayList<>();
    Random random = ThreadLocalRandom.current();

    // Generate 3-8 suggestions
    int suggestionCount = 3 + random.nextInt(6);

    for (int i = 0; i < suggestionCount; i++) {
      SuggestionTemplate template =
          SUGGESTION_TEMPLATES.get(random.nextInt(SUGGESTION_TEMPLATES.size()));
      String fileName =
          entity.getUploadedFiles().isEmpty()
              ? getRandomFileName()
              : entity.getUploadedFiles().get(random.nextInt(entity.getUploadedFiles().size()));

      CodeSuggestion suggestion =
          new CodeSuggestion()
              .id("suggestion-" + UUID.randomUUID().toString().substring(0, 8))
              .title(template.title)
              .description(template.description)
              .file(fileName)
              .startLine(random.nextInt(150) + 1)
              .endLine(random.nextInt(150) + 1)
              .originalCode(generateOriginalCode())
              .suggestedCode(generateSuggestedCode())
              .reasoning(template.reasoning)
              .impact(template.impact)
              .category(template.category);

      suggestions.add(suggestion);
    }

    return suggestions;
  }

  /** Create analysis summary with statistics */
  private AnalysisSummary createAnalysisSummary(List<CodeIssue> issues, AnalysisEntity entity) {
    Map<CodeIssue.SeverityEnum, Long> severityCounts = new HashMap<>();
    for (CodeIssue issue : issues) {
      severityCounts.merge(issue.getSeverity(), 1L, Long::sum);
    }

    int overallScore = calculateOverallScore(severityCounts);

    return new AnalysisSummary()
        .overallScore(overallScore)
        .totalIssues(issues.size())
        .criticalIssues(severityCounts.getOrDefault(CodeIssue.SeverityEnum.CRITICAL, 0L).intValue())
        .majorIssues(severityCounts.getOrDefault(CodeIssue.SeverityEnum.HIGH, 0L).intValue())
        .minorIssues(
            severityCounts.getOrDefault(CodeIssue.SeverityEnum.MEDIUM, 0L).intValue()
                + severityCounts.getOrDefault(CodeIssue.SeverityEnum.LOW, 0L).intValue())
        .filesAnalyzed(entity.getFilesCount())
        .linesAnalyzed(entity.getFilesCount() * 150) // Estimate 150 lines per file
        .analysisTime("2.5 minutes");
  }

  /** Create analysis metrics */
  private AnalysisMetrics createAnalysisMetrics(AnalysisEntity entity) {
    return new AnalysisMetrics()
        .totalFiles(entity.getFilesCount())
        .totalLines(entity.getFilesCount() * 150)
        .analysisTime(150) // 2.5 minutes in seconds
        .modelTokensUsed(ThreadLocalRandom.current().nextInt(5000, 15000))
        .staticAnalysisTime(45)
        .aiAnalysisTime(105);
  }

  /** Generate file-specific analysis results */
  private List<FileAnalysisResult> generateFileResults(
      AnalysisEntity entity, List<CodeIssue> allIssues) {
    List<FileAnalysisResult> fileResults = new ArrayList<>();

    for (String fileName : entity.getUploadedFiles()) {
      // Filter issues for this file
      List<CodeIssue> fileIssues =
          allIssues.stream().filter(issue -> fileName.equals(issue.getFile())).toList();

      FileAnalysisResult result =
          new FileAnalysisResult()
              .path(fileName)
              .status(FileAnalysisResult.StatusEnum.ANALYZED)
              .language(detectLanguage(fileName))
              .lines(ThreadLocalRandom.current().nextInt(50, 300))
              .issues(fileIssues);

      fileResults.add(result);
    }

    return fileResults;
  }

  // Helper methods
  private String getRandomFileName() {
    return COMMON_FILES.get(ThreadLocalRandom.current().nextInt(COMMON_FILES.size()));
  }

  private String getEffortEstimate(CodeIssue.SeverityEnum severity) {
    return switch (severity) {
      case CRITICAL -> "High";
      case HIGH -> "Medium";
      case MEDIUM -> "Low";
      case LOW, INFO -> "Trivial";
    };
  }

  private String generateCodeSnippet(IssueTemplate template) {
    return switch (template.rule) {
      case "CRITICAL_SQL_INJECTION" -> "String sql = \"SELECT * FROM users WHERE id = \" + userId;";
      case "INEFFICIENT_STRING_CONCAT" ->
          "String result = \"\";\nfor (String item : items) {\n    result += item;\n}";
      case "NULL_POINTER_RISK" -> "user.getName().toUpperCase();";
      default -> "// Code snippet for " + template.title;
    };
  }

  private String generateOriginalCode() {
    return "if (user != null && user.isActive()) {\n    processUser(user);\n}";
  }

  private String generateSuggestedCode() {
    return "Optional.ofNullable(user)\n    .filter(User::isActive)\n    .ifPresent(this::processUser);";
  }

  private int calculateOverallScore(Map<CodeIssue.SeverityEnum, Long> severityCounts) {
    int score = 100;
    score -= severityCounts.getOrDefault(CodeIssue.SeverityEnum.CRITICAL, 0L) * 20;
    score -= severityCounts.getOrDefault(CodeIssue.SeverityEnum.HIGH, 0L) * 10;
    score -= severityCounts.getOrDefault(CodeIssue.SeverityEnum.MEDIUM, 0L) * 5;
    score -= severityCounts.getOrDefault(CodeIssue.SeverityEnum.LOW, 0L) * 2;
    return Math.max(score, 0);
  }

  private String detectLanguage(String fileName) {
    if (fileName.endsWith(".java")) return "Java";
    if (fileName.endsWith(".js") || fileName.endsWith(".ts")) return "JavaScript/TypeScript";
    if (fileName.endsWith(".py")) return "Python";
    if (fileName.endsWith(".properties")) return "Properties";
    return "Unknown";
  }

  // Template classes for consistent data generation
  private record IssueTemplate(
      CodeIssue.SeverityEnum severity,
      CodeIssue.TypeEnum type,
      String title,
      String description,
      String suggestion,
      String rule) {}

  private record SuggestionTemplate(
      String title,
      String description,
      String reasoning,
      CodeSuggestion.CategoryEnum category,
      CodeSuggestion.ImpactEnum impact) {}
}
