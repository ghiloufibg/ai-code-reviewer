package com.ghiloufi.aicode.infrastructure.adapter.output.external.sast;

import com.ghiloufi.aicode.application.port.output.StaticAnalysisPort;
import com.ghiloufi.aicode.domain.entity.AnalysisResult;
import com.ghiloufi.aicode.domain.entity.DiffAnalysis;
import com.ghiloufi.aicode.domain.value.CodeIssue;
import com.ghiloufi.aicode.service.analysis.StaticAnalysisRunner;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * Infrastructure adapter for static analysis operations.
 *
 * <p>Wraps the existing StaticAnalysisRunner to provide analysis results in the domain format.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StaticAnalysisAdapter implements StaticAnalysisPort {

  private final StaticAnalysisRunner staticAnalysisRunner;

  @Override
  public Flux<AnalysisResult> analyze(DiffAnalysis diffAnalysis) {
    log.info("Running static analysis on {} files", diffAnalysis.getFileModifications().size());

    return staticAnalysisRunner
        .runAndCollect()
        .map(this::convertToAnalysisResults)
        .flatMapMany(Flux::fromIterable)
        .subscribeOn(Schedulers.boundedElastic());
  }

  @Override
  public boolean isAvailable() {
    return staticAnalysisRunner.isTargetDirectoryAccessible();
  }

  @Override
  public Flux<String> getAvailableTools() {
    return Flux.fromIterable(staticAnalysisRunner.getAvailableAnalysisFiles().entrySet())
        .filter(Map.Entry::getValue)
        .map(Map.Entry::getKey);
  }

  /** Converts static analysis results to domain analysis results. */
  private List<AnalysisResult> convertToAnalysisResults(Map<String, Object> results) {
    List<AnalysisResult> analysisResults = new ArrayList<>();

    for (Map.Entry<String, Object> entry : results.entrySet()) {
      String tool = entry.getKey();
      Object result = entry.getValue();

      if (result instanceof String content && !content.isEmpty()) {
        AnalysisResult.AnalysisType type = mapToolToAnalysisType(tool);
        List<CodeIssue> issues = parseToolOutput(tool, content);
        String summary = generateSummary(tool, issues);

        AnalysisResult analysisResult =
            new AnalysisResult(type, summary, issues, List.of(), getToolVersion(tool));
        analysisResults.add(analysisResult);
        log.debug("Converted {} results: {} issues", tool, issues.size());
      }
    }

    return analysisResults;
  }

  /** Maps tool name to analysis type. */
  private AnalysisResult.AnalysisType mapToolToAnalysisType(String tool) {
    return switch (tool.toLowerCase()) {
      case "checkstyle" -> AnalysisResult.AnalysisType.STATIC_ANALYSIS_CHECKSTYLE;
      case "pmd" -> AnalysisResult.AnalysisType.STATIC_ANALYSIS_PMD;
      case "spotbugs" -> AnalysisResult.AnalysisType.STATIC_ANALYSIS_SPOTBUGS;
      case "semgrep" -> AnalysisResult.AnalysisType.STATIC_ANALYSIS_SEMGREP;
      default -> AnalysisResult.AnalysisType.STATIC_ANALYSIS_CHECKSTYLE;
    };
  }

  /**
   * Parses tool output to extract issues. This is a simplified implementation - in production you'd
   * have sophisticated parsers for each tool's output format.
   */
  private List<CodeIssue> parseToolOutput(String tool, String content) {
    List<CodeIssue> issues = new ArrayList<>();

    // Simplified parsing - count non-empty lines as issues
    String[] lines = content.split("\n");
    for (int i = 0; i < lines.length && i < 10; i++) { // Limit for demo
      String line = lines[i].trim();
      if (!line.isEmpty() && !line.startsWith("<?xml") && !line.startsWith("<")) {
        issues.add(
            CodeIssue.warning(
                "unknown.java",
                i + 1,
                tool + " issue",
                line.length() > 100 ? line.substring(0, 100) + "..." : line));
      }
    }

    return issues;
  }

  /** Generates summary for tool results. */
  private String generateSummary(String tool, List<CodeIssue> issues) {
    return String.format("%s analysis completed. Found %d issue(s).", tool, issues.size());
  }

  /** Gets tool version - simplified implementation. */
  private String getToolVersion(String tool) {
    return tool + "-1.0";
  }
}
