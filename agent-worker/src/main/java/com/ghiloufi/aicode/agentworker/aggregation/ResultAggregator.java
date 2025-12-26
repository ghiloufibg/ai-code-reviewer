package com.ghiloufi.aicode.agentworker.aggregation;

import com.ghiloufi.aicode.agentworker.analysis.TestExecutionResult;
import com.ghiloufi.aicode.agentworker.config.AgentWorkerProperties;
import com.ghiloufi.aicode.agentworker.mapper.TestResultMapper;
import com.ghiloufi.aicode.core.domain.model.AggregatedFindings;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.model.TestResults;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResultAggregator {

  private static final String SOURCE_AI = "ai";
  private static final String SOURCE_TESTS = "tests";

  private final TestResultMapper testResultMapper;
  private final AgentWorkerProperties properties;

  public AggregatedFindings aggregate(ReviewResult aiReviewResult, TestExecutionResult testResult) {

    final List<ReviewResult.Issue> allIssues = new ArrayList<>();
    final List<ReviewResult.Note> allNotes = new ArrayList<>();
    final Map<String, Integer> findingCounts = new HashMap<>();

    if (aiReviewResult != null) {
      final var aiIssues = filterByConfidence(aiReviewResult.getIssues());
      allIssues.addAll(aiIssues);
      allNotes.addAll(aiReviewResult.getNonBlockingNotes());
      findingCounts.put(SOURCE_AI, aiIssues.size());
      log.debug("Added {} AI issues (after confidence filtering)", aiIssues.size());
    }

    if (testResult != null && testResult.executed() && !testResult.success()) {
      final var testIssues = testResultMapper.mapFailedTests(testResult.testResults());
      allIssues.addAll(testIssues);
      findingCounts.put(SOURCE_TESTS, testIssues.size());
      log.debug("Added {} test failure issues", testIssues.size());
    } else {
      findingCounts.put(SOURCE_TESTS, 0);
    }

    final var deduplicatedIssues = deduplicateIssues(allIssues);
    final var limitedIssues = limitIssuesPerFile(deduplicatedIssues);

    final var summary = buildSummary(aiReviewResult, testResult, limitedIssues.size());
    final var overallConfidence = calculateOverallConfidence(limitedIssues);

    final var severityCounts = countBySeverity(limitedIssues);
    final int totalBefore = allIssues.size();
    final int totalAfterDedup = deduplicatedIssues.size();
    final int totalFiltered = totalAfterDedup - limitedIssues.size();

    log.info(
        "Aggregated {} issues ({} AI, {} tests), {} notes, confidence: {}",
        limitedIssues.size(),
        findingCounts.getOrDefault(SOURCE_AI, 0),
        findingCounts.getOrDefault(SOURCE_TESTS, 0),
        allNotes.size(),
        String.format("%.2f", overallConfidence));

    return AggregatedFindings.builder()
        .issues(limitedIssues)
        .notes(allNotes)
        .summary(summary)
        .findingCountsBySource(findingCounts)
        .findingCountsBySeverity(severityCounts)
        .overallConfidence(overallConfidence)
        .totalFindingsBeforeDedup(totalBefore)
        .totalFindingsAfterDedup(totalAfterDedup)
        .totalFindingsFiltered(totalFiltered)
        .build();
  }

  public AggregatedFindings aggregate(
      final ReviewResult aiReviewResult, final TestResults cicdTestResults) {

    final List<ReviewResult.Issue> allIssues = new ArrayList<>();
    final List<ReviewResult.Note> allNotes = new ArrayList<>();
    final Map<String, Integer> findingCounts = new HashMap<>();

    if (aiReviewResult != null) {
      final var aiIssues = filterByConfidence(aiReviewResult.getIssues());
      allIssues.addAll(aiIssues);
      allNotes.addAll(aiReviewResult.getNonBlockingNotes());
      findingCounts.put(SOURCE_AI, aiIssues.size());
      log.debug("Added {} AI issues (after confidence filtering)", aiIssues.size());
    }

    if (cicdTestResults != null && cicdTestResults.hasFailures()) {
      final var testIssues = testResultMapper.mapTestFailures(cicdTestResults);
      allIssues.addAll(testIssues);
      findingCounts.put(SOURCE_TESTS, testIssues.size());
      log.debug("Added {} CI/CD test failure issues", testIssues.size());
    } else {
      findingCounts.put(SOURCE_TESTS, 0);
    }

    final var deduplicatedIssues = deduplicateIssues(allIssues);
    final var limitedIssues = limitIssuesPerFile(deduplicatedIssues);

    final var summary = buildSummaryFromCicd(aiReviewResult, cicdTestResults, limitedIssues.size());
    final var overallConfidence = calculateOverallConfidence(limitedIssues);

    final var severityCounts = countBySeverity(limitedIssues);
    final int totalBefore = allIssues.size();
    final int totalAfterDedup = deduplicatedIssues.size();
    final int totalFiltered = totalAfterDedup - limitedIssues.size();

    log.info(
        "Aggregated {} issues ({} AI, {} CI/CD tests), {} notes, confidence: {}",
        limitedIssues.size(),
        findingCounts.getOrDefault(SOURCE_AI, 0),
        findingCounts.getOrDefault(SOURCE_TESTS, 0),
        allNotes.size(),
        String.format("%.2f", overallConfidence));

    return AggregatedFindings.builder()
        .issues(limitedIssues)
        .notes(allNotes)
        .summary(summary)
        .findingCountsBySource(findingCounts)
        .findingCountsBySeverity(severityCounts)
        .overallConfidence(overallConfidence)
        .totalFindingsBeforeDedup(totalBefore)
        .totalFindingsAfterDedup(totalAfterDedup)
        .totalFindingsFiltered(totalFiltered)
        .build();
  }

  private Map<String, Integer> countBySeverity(List<ReviewResult.Issue> issues) {
    final Map<String, Integer> counts = new HashMap<>();
    for (final var issue : issues) {
      final var severity =
          issue.getSeverity() != null ? issue.getSeverity().toLowerCase() : "unknown";
      counts.merge(severity, 1, Integer::sum);
    }
    return counts;
  }

  private List<ReviewResult.Issue> filterByConfidence(List<ReviewResult.Issue> issues) {
    if (issues == null) {
      return List.of();
    }

    final var minConfidence = properties.getAggregation().getFiltering().getMinConfidence();

    return issues.stream()
        .filter(
            issue ->
                issue.getConfidenceScore() == null || issue.getConfidenceScore() >= minConfidence)
        .toList();
  }

  private List<ReviewResult.Issue> deduplicateIssues(List<ReviewResult.Issue> issues) {
    if (!properties.getAggregation().getDeduplication().isEnabled()) {
      return issues;
    }

    final Set<String> seen = new HashSet<>();
    final List<ReviewResult.Issue> deduplicated = new ArrayList<>();

    for (final var issue : issues) {
      final var key = buildDeduplicationKey(issue);
      if (!seen.contains(key)) {
        seen.add(key);
        deduplicated.add(issue);
      } else {
        log.trace(
            "Deduplicated issue: {} at {}:{}",
            issue.getTitle(),
            issue.getFile(),
            issue.getStartLine());
      }
    }

    if (deduplicated.size() < issues.size()) {
      log.debug(
          "Deduplicated {} issues (removed {} duplicates)",
          deduplicated.size(),
          issues.size() - deduplicated.size());
    }

    return deduplicated;
  }

  private String buildDeduplicationKey(ReviewResult.Issue issue) {
    return String.format(
        "%s:%d:%s",
        issue.getFile(), issue.getStartLine(), normalizeForDeduplication(issue.getTitle()));
  }

  private String normalizeForDeduplication(String text) {
    if (text == null) {
      return "";
    }
    return text.toLowerCase().replaceAll("[^a-z0-9]", "").trim();
  }

  private List<ReviewResult.Issue> limitIssuesPerFile(List<ReviewResult.Issue> issues) {
    final var maxPerFile = properties.getAggregation().getFiltering().getMaxIssuesPerFile();
    final Map<String, Integer> fileIssueCounts = new HashMap<>();
    final List<ReviewResult.Issue> limited = new ArrayList<>();

    for (final var issue : issues) {
      final var file = issue.getFile();
      final var count = fileIssueCounts.getOrDefault(file, 0);

      if (count < maxPerFile) {
        limited.add(issue);
        fileIssueCounts.put(file, count + 1);
      } else {
        log.trace("Skipped issue for file {} (limit {} reached)", file, maxPerFile);
      }
    }

    if (limited.size() < issues.size()) {
      log.debug(
          "Limited issues per file: {} → {} (max {} per file)",
          issues.size(),
          limited.size(),
          maxPerFile);
    }

    return limited;
  }

  private String buildSummary(
      ReviewResult aiReviewResult, TestExecutionResult testResult, int totalIssues) {

    final var sb = new StringBuilder();

    if (aiReviewResult != null && aiReviewResult.getSummary() != null) {
      sb.append(aiReviewResult.getSummary());
    }

    if (testResult != null && testResult.executed()) {
      if (!sb.isEmpty()) {
        sb.append("\n\n");
      }
      sb.append("**Test Execution:** ");
      if (testResult.success()) {
        sb.append("✅ All ").append(testResult.totalTests()).append(" tests passed");
      } else {
        sb.append("❌ ")
            .append(testResult.failedTests())
            .append(" of ")
            .append(testResult.totalTests())
            .append(" tests failed");
      }
    }

    if (sb.isEmpty()) {
      sb.append("Analysis complete. Found ").append(totalIssues).append(" issues.");
    }

    return sb.toString();
  }

  private String buildSummaryFromCicd(
      ReviewResult aiReviewResult, TestResults testResults, int totalIssues) {

    final var sb = new StringBuilder();

    if (aiReviewResult != null && aiReviewResult.getSummary() != null) {
      sb.append(aiReviewResult.getSummary());
    }

    if (testResults != null && testResults.wasExecuted()) {
      if (!sb.isEmpty()) {
        sb.append("\n\n");
      }
      sb.append("**CI/CD Test Results:** ");
      if (testResults.passed()) {
        sb.append("✅ ").append(testResults.formatSummary());
      } else {
        sb.append("❌ ").append(testResults.formatSummary());
      }
    }

    if (sb.isEmpty()) {
      sb.append("Analysis complete. Found ").append(totalIssues).append(" issues.");
    }

    return sb.toString();
  }

  private double calculateOverallConfidence(List<ReviewResult.Issue> issues) {
    if (issues.isEmpty()) {
      return 1.0;
    }

    return issues.stream()
        .filter(i -> i.getConfidenceScore() != null)
        .mapToDouble(ReviewResult.Issue::getConfidenceScore)
        .average()
        .orElse(0.7);
  }
}
