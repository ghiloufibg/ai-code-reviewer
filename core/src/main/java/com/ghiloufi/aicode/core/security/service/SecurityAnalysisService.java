package com.ghiloufi.aicode.core.security.service;

import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.security.analyzer.CodeInjectionDetector;
import com.ghiloufi.aicode.core.security.analyzer.CommandInjectionDetector;
import com.ghiloufi.aicode.core.security.analyzer.PathTraversalDetector;
import com.ghiloufi.aicode.core.security.analyzer.ReflectionAbuseDetector;
import com.ghiloufi.aicode.core.security.mapper.SecurityResultMapper;
import com.ghiloufi.aicode.core.security.model.SecurityIssue;
import com.ghiloufi.aicode.core.security.validator.PatternValidator;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public final class SecurityAnalysisService {

  private final CommandInjectionDetector commandInjectionDetector;
  private final ReflectionAbuseDetector reflectionAbuseDetector;
  private final CodeInjectionDetector codeInjectionDetector;
  private final PathTraversalDetector pathTraversalDetector;
  private final PatternValidator patternValidator;
  private final SecurityResultMapper resultMapper;
  private final JavaParser javaParser;

  public SecurityAnalysisService(
      final CommandInjectionDetector commandInjectionDetector,
      final ReflectionAbuseDetector reflectionAbuseDetector,
      final CodeInjectionDetector codeInjectionDetector,
      final PathTraversalDetector pathTraversalDetector,
      final PatternValidator patternValidator,
      final SecurityResultMapper resultMapper) {
    this.commandInjectionDetector = commandInjectionDetector;
    this.reflectionAbuseDetector = reflectionAbuseDetector;
    this.codeInjectionDetector = codeInjectionDetector;
    this.pathTraversalDetector = pathTraversalDetector;
    this.patternValidator = patternValidator;
    this.resultMapper = resultMapper;

    final ParserConfiguration config = new ParserConfiguration();
    config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
    this.javaParser = new JavaParser(config);

    log.info("SecurityAnalysisService initialized with AST and pattern analyzers");
  }

  public ReviewResult analyzeCode(final String code, final String filePath) {
    if (code == null || code.isBlank()) {
      log.debug("Empty code provided for analysis, returning empty result");
      return createEmptyResult();
    }

    final ReviewResult result = new ReviewResult();
    result.summary = "Security Analysis";
    result.issues = new ArrayList<>();

    final List<SecurityIssue> astIssues = runAstAnalysis(code);
    final List<SecurityIssue> patternIssues = patternValidator.validateCodeWithLineNumbers(code);

    for (final SecurityIssue issue : astIssues) {
      final ReviewResult.Issue reviewIssue = resultMapper.toReviewIssue(issue, filePath);
      result.issues.add(reviewIssue);
    }

    for (final SecurityIssue issue : patternIssues) {
      final ReviewResult.Issue reviewIssue = resultMapper.toReviewIssue(issue, filePath);
      result.issues.add(reviewIssue);
    }

    log.info(
        "Security analysis completed for file: {} - Found {} issues ({} AST, {} pattern)",
        filePath,
        result.issues.size(),
        astIssues.size(),
        patternIssues.size());
    return result;
  }

  public ReviewResult analyzeCodeWithLineMapping(
      final String code, final String filePath, final int startLine) {
    if (code == null || code.isBlank()) {
      log.debug("Empty code provided for analysis, returning empty result");
      return createEmptyResult();
    }

    final ReviewResult result = new ReviewResult();
    result.summary = "Security Analysis";
    result.issues = new ArrayList<>();

    final List<SecurityIssue> astIssues = runAstAnalysis(code);
    final List<SecurityIssue> patternIssues = patternValidator.validateCodeWithLineNumbers(code);

    for (final SecurityIssue issue : astIssues) {
      final int lineNum = extractLineNumber(issue);
      final int adjustedLine = lineNum > 0 ? lineNum + startLine - 1 : startLine;
      final ReviewResult.Issue reviewIssue =
          resultMapper.toReviewIssue(issue, filePath, adjustedLine);
      result.issues.add(reviewIssue);
    }

    for (final SecurityIssue issue : patternIssues) {
      final int lineNum = extractLineNumber(issue);
      final int adjustedLine = lineNum > 0 ? lineNum + startLine - 1 : startLine;
      final ReviewResult.Issue reviewIssue =
          resultMapper.toReviewIssue(issue, filePath, adjustedLine);
      result.issues.add(reviewIssue);
    }

    log.info(
        "Security analysis completed for file: {} (lines {}-{}) - Found {} issues",
        filePath,
        startLine,
        startLine + code.lines().count(),
        result.issues.size());
    return result;
  }

  private List<SecurityIssue> runAstAnalysis(final String code) {
    final List<SecurityIssue> allIssues = new ArrayList<>();

    try {
      final var parseResult = javaParser.parse(code);

      if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
        final CompilationUnit cu = parseResult.getResult().get();

        cu.accept(commandInjectionDetector, allIssues);
        cu.accept(reflectionAbuseDetector, allIssues);
        cu.accept(codeInjectionDetector, allIssues);
        cu.accept(pathTraversalDetector, allIssues);
      } else {
        log.debug("Failed to parse code for AST analysis: {}", parseResult.getProblems());
      }
    } catch (final Exception e) {
      log.warn("AST analysis failed: {}", e.getMessage(), e);
    }

    return allIssues;
  }

  private int extractLineNumber(final SecurityIssue issue) {
    if (issue.description() == null) {
      log.debug("Issue description is null, returning 0");
      return 0;
    }

    final String linePattern = "line ";
    final int lineIndex = issue.description().indexOf(linePattern);
    if (lineIndex == -1) {
      log.debug("Pattern 'line ' not found in description: {}", issue.description());
      return 0;
    }

    try {
      final int startIndex = lineIndex + linePattern.length();
      int endIndex = issue.description().indexOf(' ', startIndex);
      if (endIndex == -1) {
        endIndex = issue.description().indexOf('.', startIndex);
      }
      if (endIndex == -1) {
        endIndex = issue.description().indexOf(')', startIndex);
      }
      if (endIndex == -1) {
        endIndex = issue.description().length();
      }

      if (endIndex > startIndex) {
        final String lineNumStr = issue.description().substring(startIndex, endIndex);
        final int lineNum = Integer.parseInt(lineNumStr);
        log.debug("Extracted line number {} from description: {}", lineNum, issue.description());
        return lineNum;
      }
    } catch (final NumberFormatException e) {
      log.debug("Could not extract line number from description: {}", issue.description(), e);
    }

    log.debug(
        "Failed to extract line number, returning 0 for description: {}", issue.description());
    return 0;
  }

  private ReviewResult createEmptyResult() {
    final ReviewResult result = new ReviewResult();
    result.summary = "Security Analysis";
    result.issues = new ArrayList<>();
    return result;
  }
}
