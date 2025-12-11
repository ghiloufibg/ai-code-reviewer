package com.ghiloufi.aicode.core.infrastructure.resilience;

import java.time.Duration;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@Getter
@ConfigurationProperties(prefix = "timeouts")
public final class TimeoutProperties {

  private final Duration llmRequest;
  private final Duration scmRequest;
  private final Duration healthCheck;
  private final Duration databaseQuery;
  private final Duration contextRetrieval;

  public TimeoutProperties(
      @DefaultValue("120s") final Duration llmRequest,
      @DefaultValue("30s") final Duration scmRequest,
      @DefaultValue("5s") final Duration healthCheck,
      @DefaultValue("10s") final Duration databaseQuery,
      @DefaultValue("5s") final Duration contextRetrieval) {
    this.llmRequest = llmRequest;
    this.scmRequest = scmRequest;
    this.healthCheck = healthCheck;
    this.databaseQuery = databaseQuery;
    this.contextRetrieval = contextRetrieval;
  }
}
