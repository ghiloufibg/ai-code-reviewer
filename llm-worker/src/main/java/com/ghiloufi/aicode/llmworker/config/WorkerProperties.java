package com.ghiloufi.aicode.llmworker.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@Getter
@ConfigurationProperties(prefix = "worker")
public class WorkerProperties {

  private final String consumerGroup;
  private final String consumerId;
  private final String streamKey;
  private final int batchSize;
  private final int timeoutSeconds;

  public WorkerProperties(
      @DefaultValue("llm-workers") String consumerGroup,
      String consumerId,
      @DefaultValue("review:requests") String streamKey,
      @DefaultValue("10") int batchSize,
      @DefaultValue("120") int timeoutSeconds) {
    this.consumerGroup = consumerGroup;
    this.consumerId = consumerId != null ? consumerId : "worker-" + ProcessHandle.current().pid();
    this.streamKey = streamKey;
    this.batchSize = batchSize;
    this.timeoutSeconds = timeoutSeconds;
  }
}
