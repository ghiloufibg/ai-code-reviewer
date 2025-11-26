package com.ghiloufi.aicode.core.service.filter;

import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public final class ConfidenceFilter {

  public ReviewResult filterByConfidence(final ReviewResult result, final double minimumThreshold) {
    if (result == null) {
      throw new IllegalArgumentException("ReviewResult cannot be null");
    }

    if (minimumThreshold < 0.0 || minimumThreshold > 1.0) {
      log.warn("Invalid minimum confidence threshold {}, using default 0.5", minimumThreshold);
      return filterByConfidence(result, 0.5);
    }

    if (result.getIssues() == null || result.getIssues().isEmpty()) {
      log.debug("No issues to filter");
      return result;
    }

    final int originalIssueCount = result.getIssues().size();
    final List<ReviewResult.Issue> filteredIssues =
        result.getIssues().stream()
            .filter(
                issue -> {
                  final double confidence =
                      issue.getConfidenceScore() != null ? issue.getConfidenceScore() : 0.5;
                  final boolean meetsThreshold = confidence >= minimumThreshold;

                  if (!meetsThreshold) {
                    log.info(
                        "Filtered out issue '{}' (confidence: {}, threshold: {})",
                        issue.getTitle(),
                        confidence,
                        minimumThreshold);
                  }

                  return meetsThreshold;
                })
            .collect(Collectors.toList());

    final int filteredCount = originalIssueCount - filteredIssues.size();
    if (filteredCount > 0) {
      log.info(
          "Confidence filtering: {} issues removed (threshold: {}), {} issues retained",
          filteredCount,
          minimumThreshold,
          filteredIssues.size());
    } else {
      log.debug("All {} issues meet confidence threshold {}", originalIssueCount, minimumThreshold);
    }

    return result.withIssues(filteredIssues);
  }
}
