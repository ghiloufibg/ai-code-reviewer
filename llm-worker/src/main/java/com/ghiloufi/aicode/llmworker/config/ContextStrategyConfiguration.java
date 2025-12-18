package com.ghiloufi.aicode.llmworker.config;

import com.ghiloufi.aicode.core.domain.port.output.ContextRetrievalStrategy;
import com.ghiloufi.aicode.core.domain.port.output.SCMPort;
import com.ghiloufi.aicode.llmworker.service.context.GitHistoryCoChangeAnalyzer;
import com.ghiloufi.aicode.llmworker.strategy.HistoryBasedContextStrategy;
import com.ghiloufi.aicode.llmworker.strategy.MetadataBasedContextStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ContextStrategyConfiguration {

  @Bean
  public GitHistoryCoChangeAnalyzer gitHistoryCoChangeAnalyzer(
      final SCMPort scmPort,
      @Value("${context-retrieval.git-history.time-window-days:90}") final int lookbackDays,
      @Value("${context-retrieval.git-history.analysis-depth:100}") final int maxResults) {
    return new GitHistoryCoChangeAnalyzer(scmPort, lookbackDays, maxResults);
  }

  @Bean
  public ContextRetrievalStrategy historyBasedContextStrategy(
      final GitHistoryCoChangeAnalyzer analyzer) {
    return new HistoryBasedContextStrategy(analyzer);
  }

  @Bean
  public ContextRetrievalStrategy metadataBasedContextStrategy(final SCMPort scmPort) {
    return new MetadataBasedContextStrategy(scmPort);
  }
}
