package com.ghiloufi.aicode.core.application.service;

import com.ghiloufi.aicode.core.domain.model.DiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.ReviewChunk;
import com.ghiloufi.aicode.core.domain.model.ReviewConfiguration;
import com.ghiloufi.aicode.core.domain.port.output.AIInteractionPort;
import com.ghiloufi.aicode.core.service.prompt.PromptBuilder;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class AIReviewStreamingService {

  private final AIInteractionPort aiPort;
  private final PromptBuilder promptBuilder;

  public Flux<ReviewChunk> reviewCodeStreaming(
      final DiffAnalysisBundle diff, final ReviewConfiguration config) {
    log.info("Starting AI code review for {} files", diff.getModifiedFileCount());

    final ReviewConfiguration configWithLlmMetadata =
        config.withLlmMetadata(aiPort.getProviderName(), aiPort.getModelName());

    return Mono.fromCallable(() -> promptBuilder.buildReviewPrompt(diff, configWithLlmMetadata))
        .doOnNext(prompt -> log.debug("Built review prompt: {} chars", prompt.length()))
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
