package com.ghiloufi.aicode.agentworker.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Getter
@Validated
@ConfigurationProperties(prefix = "agent")
public final class AgentWorkerProperties {

  private final ConsumerProperties consumer;
  private final CloneProperties clone;
  private final DockerProperties docker;
  private final AggregationProperties aggregation;
  private final DecisionProperties decision;

  public AgentWorkerProperties(
      @DefaultValue ConsumerProperties consumer,
      @DefaultValue CloneProperties clone,
      @DefaultValue DockerProperties docker,
      @DefaultValue AggregationProperties aggregation,
      @DefaultValue DecisionProperties decision) {
    this.consumer = consumer;
    this.clone = clone;
    this.docker = docker;
    this.aggregation = aggregation;
    this.decision = decision;
  }

  @Getter
  public static final class ConsumerProperties {

    @NotBlank private final String streamKey;
    @NotBlank private final String consumerGroup;
    @NotBlank private final String consumerId;

    @Min(1)
    private final int batchSize;

    private final Duration pollTimeout;

    public ConsumerProperties(
        @DefaultValue("review:agent-requests") String streamKey,
        @DefaultValue("agent-workers") String consumerGroup,
        @DefaultValue("agent-worker-1") String consumerId,
        @DefaultValue("1") int batchSize,
        @DefaultValue("5s") Duration pollTimeout) {
      this.streamKey = streamKey;
      this.consumerGroup = consumerGroup;
      this.consumerId = consumerId;
      this.batchSize = batchSize;
      this.pollTimeout = pollTimeout;
    }
  }

  @Getter
  public static final class CloneProperties {

    @Min(1)
    private final int depth;

    private final Duration timeout;
    @NotBlank private final String authMethod;

    public CloneProperties(
        @DefaultValue("1") int depth,
        @DefaultValue("120s") Duration timeout,
        @DefaultValue("token") String authMethod) {
      this.depth = depth;
      this.timeout = timeout;
      this.authMethod = authMethod;
    }
  }

  @Getter
  public static final class DockerProperties {

    @NotBlank private final String host;
    @NotBlank private final String analysisImage;
    private final ResourceLimitsProperties resourceLimits;
    private final Duration timeout;
    private final boolean autoPull;

    public DockerProperties(
        @DefaultValue("unix:///var/run/docker.sock") String host,
        @DefaultValue("ai-code-reviewer-analysis:latest") String analysisImage,
        @DefaultValue ResourceLimitsProperties resourceLimits,
        @DefaultValue("600s") Duration timeout,
        @DefaultValue("true") boolean autoPull) {
      this.host = host;
      this.analysisImage = analysisImage;
      this.resourceLimits = resourceLimits;
      this.timeout = timeout;
      this.autoPull = autoPull;
    }

    @Getter
    public static final class ResourceLimitsProperties {

      private final long memoryBytes;
      private final long nanoCpus;

      public ResourceLimitsProperties(
          @DefaultValue("2147483648") long memoryBytes, @DefaultValue("2000000000") long nanoCpus) {
        this.memoryBytes = memoryBytes;
        this.nanoCpus = nanoCpus;
      }
    }
  }

  @Getter
  public static final class AggregationProperties {

    private final DeduplicationProperties deduplication;
    private final FilteringProperties filtering;

    public AggregationProperties(
        @DefaultValue DeduplicationProperties deduplication,
        @DefaultValue FilteringProperties filtering) {
      this.deduplication = deduplication;
      this.filtering = filtering;
    }

    @Getter
    public static final class DeduplicationProperties {

      private final boolean enabled;
      private final double similarityThreshold;

      public DeduplicationProperties(
          @DefaultValue("true") boolean enabled, @DefaultValue("0.85") double similarityThreshold) {
        this.enabled = enabled;
        this.similarityThreshold = similarityThreshold;
      }
    }

    @Getter
    public static final class FilteringProperties {

      private final double minConfidence;
      private final int maxIssuesPerFile;

      public FilteringProperties(
          @DefaultValue("0.7") double minConfidence, @DefaultValue("10") int maxIssuesPerFile) {
        this.minConfidence = minConfidence;
        this.maxIssuesPerFile = maxIssuesPerFile;
      }
    }
  }

  @Getter
  public static final class DecisionProperties {

    @NotBlank private final String llmProvider;
    @NotBlank private final String llmModel;

    @Min(1)
    private final int maxReasoningIterations;

    public DecisionProperties(
        @DefaultValue("openai") String llmProvider,
        @DefaultValue("gpt-4o") String llmModel,
        @DefaultValue("3") int maxReasoningIterations) {
      this.llmProvider = llmProvider;
      this.llmModel = llmModel;
      this.maxReasoningIterations = maxReasoningIterations;
    }
  }
}
