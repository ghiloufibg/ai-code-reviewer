package com.ghiloufi.aicode.core.application.service.context;

import com.ghiloufi.aicode.core.domain.model.CoChangeMetrics;
import com.ghiloufi.aicode.core.domain.model.CommitInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CoChangeFrequencyCalculator {

  public Map<String, Integer> calculateFrequency(
      final String targetFile, final List<CommitInfo> commits) {
    final Map<String, Integer> frequency = new HashMap<>();

    for (final CommitInfo commit : commits) {
      if (commit.touchedFile(targetFile)) {
        for (final String changedFile : commit.changedFiles()) {
          if (!changedFile.equals(targetFile)) {
            frequency.merge(changedFile, 1, Integer::sum);
          }
        }
      }
    }

    return frequency;
  }

  public List<CoChangeMetrics> normalizeFrequency(final Map<String, Integer> frequency) {
    if (frequency.isEmpty()) {
      return List.of();
    }

    final int maxFrequency = frequency.values().stream().max(Integer::compareTo).orElse(1);

    return frequency.entrySet().stream()
        .map(
            entry -> {
              final double normalized = (double) entry.getValue() / maxFrequency;
              return new CoChangeMetrics(entry.getKey(), entry.getValue(), normalized);
            })
        .toList();
  }

  public List<CoChangeMetrics> calculateAndNormalize(
      final String targetFile, final List<CommitInfo> commits) {
    final Map<String, Integer> frequency = calculateFrequency(targetFile, commits);
    return normalizeFrequency(frequency);
  }
}
