package com.ghiloufi.aicode.core.application.service;

import com.ghiloufi.aicode.core.application.service.audit.ContextAuditHolder;
import com.ghiloufi.aicode.core.domain.model.EnrichedDiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.ReviewChunk;
import com.ghiloufi.aicode.core.domain.model.ReviewConfiguration;
import com.ghiloufi.aicode.core.domain.port.output.AIInteractionPort;
import com.ghiloufi.aicode.core.service.prompt.PromptBuilder;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class AIReviewStreamingService {

  private final AIInteractionPort aiPort;
  private final PromptBuilder promptBuilder;
  private final ContextAuditHolder contextAuditHolder;

  @Autowired
  public AIReviewStreamingService(
      final AIInteractionPort aiPort,
      final PromptBuilder promptBuilder,
      @Autowired(required = false) final ContextAuditHolder contextAuditHolder) {
    this.aiPort = aiPort;
    this.promptBuilder = promptBuilder;
    this.contextAuditHolder = contextAuditHolder;
  }

  public AIReviewStreamingService(
      final AIInteractionPort aiPort, final PromptBuilder promptBuilder) {
    this(aiPort, promptBuilder, null);
  }

  public Flux<ReviewChunk> reviewCodeStreaming(
      final EnrichedDiffAnalysisBundle enrichedDiff, final ReviewConfiguration config) {
    log.info(
        "Starting AI code review for {} files with {} context matches",
        enrichedDiff.getModifiedFileCount(),
        enrichedDiff.getContextMatchCount());

    final ReviewConfiguration configWithLlmMetadata =
        config.withLlmMetadata(aiPort.getProviderName(), aiPort.getModelName());

    return Mono.fromCallable(
            () -> promptBuilder.buildReviewPrompt(enrichedDiff, configWithLlmMetadata))
        .doOnNext(
            prompt -> {
              log.debug(
                  "Built review prompt: {} chars (context: {})",
                  prompt.length(),
                  enrichedDiff.hasContext());
              if (enrichedDiff.hasContext() && contextAuditHolder != null) {
                contextAuditHolder.setPromptText(prompt);
              }
            })
        .flatMapMany(aiPort::streamCompletion)
        .map(content -> ReviewChunk.of(ReviewChunk.ChunkType.ANALYSIS, content))
        .doOnNext(chunk -> log.debug("Received review chunk: {} chars", chunk.content().length()))
        .doOnComplete(() -> log.info("AI code review completed successfully"))
        .doOnError(e -> log.error("AI code review failed", e))
        .timeout(Duration.ofSeconds(60));
  }

  public ReviewConfiguration getLlmMetadata() {
    return ReviewConfiguration.defaults()
        .withLlmMetadata(aiPort.getProviderName(), aiPort.getModelName());
  }
}
