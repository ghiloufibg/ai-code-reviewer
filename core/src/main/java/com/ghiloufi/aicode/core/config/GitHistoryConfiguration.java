package com.ghiloufi.aicode.core.config;

import com.ghiloufi.aicode.core.application.service.context.GitHistoryCoChangeAnalyzer;
import com.ghiloufi.aicode.core.application.service.context.HistoryBasedContextStrategy;
import com.ghiloufi.aicode.core.domain.port.output.ContextRetrievalStrategy;
import com.ghiloufi.aicode.core.domain.port.output.SCMPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
    prefix = "context-retrieval.git-history",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false)
public class GitHistoryConfiguration {

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
}
