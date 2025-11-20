package com.ghiloufi.aicode.core.application.service.context;

import com.ghiloufi.aicode.core.domain.model.CoChangeAnalysisResult;
import com.ghiloufi.aicode.core.domain.model.CoChangeMetrics;
import com.ghiloufi.aicode.core.domain.model.CommitInfo;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.port.output.SCMPort;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import reactor.core.publisher.Mono;

public final class GitHistoryCoChangeAnalyzer {

  private final SCMPort scmPort;
  private final int lookbackDays;
  private final int maxResults;
  private final CoChangeFrequencyCalculator calculator;

  public GitHistoryCoChangeAnalyzer(
      final SCMPort scmPort, final int lookbackDays, final int maxResults) {
    this.scmPort = Objects.requireNonNull(scmPort, "SCMPort cannot be null");
    this.lookbackDays = lookbackDays;
    this.maxResults = maxResults;
    this.calculator = new CoChangeFrequencyCalculator();
  }

  public Mono<CoChangeAnalysisResult> analyzeCoChanges(
      final RepositoryIdentifier repo, final String targetFile) {

    final LocalDate since = LocalDate.now().minusDays(lookbackDays);

    return scmPort
        .getCommitsFor(repo, targetFile, since, maxResults)
        .collectList()
        .map(commits -> buildAnalysisResult(targetFile, commits));
  }

  private CoChangeAnalysisResult buildAnalysisResult(
      final String targetFile, final List<CommitInfo> commits) {

    final Map<String, Integer> frequency = calculator.calculateFrequency(targetFile, commits);
    final List<CoChangeMetrics> metrics = calculator.normalizeFrequency(frequency);

    final int maxFrequency = frequency.values().stream().max(Integer::compareTo).orElse(0);

    return new CoChangeAnalysisResult(targetFile, metrics, frequency, maxFrequency);
  }
}
