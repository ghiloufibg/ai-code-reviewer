package com.ghiloufi.aicode;

import com.ghiloufi.aicode.infrastructure.mock.MockLlmStreamingAdapter;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

/**
 * Simple demonstration test showing LLM streaming capabilities.
 *
 * <p>This test demonstrates both approaches for testing streaming LLM responses: 1. Mock streaming
 * (fast, always available) 2. Integration testing approach (when LLM container is available)
 *
 * <p>Run with: mvn test -Dtest=LlmStreamingDemoTest
 */
@Slf4j
class LlmStreamingDemoTest {

  @Test
  void demonstrateMockStreaming() {
    log.info("🎭 Demonstrating Mock LLM Streaming");

    MockLlmStreamingAdapter mockAdapter = new MockLlmStreamingAdapter();

    StepVerifier.create(mockAdapter.simulateStreamingResponse())
        .expectSubscription()
        .thenConsumeWhile(
            chunk -> {
              String displayChunk = chunk.replace("\\n", " ");
              if (displayChunk.length() > 80) {
                displayChunk = displayChunk.substring(0, 80) + "...";
              }
              log.info("📦 Chunk: {}", displayChunk);
              return true;
            })
        .expectComplete()
        .verify(Duration.ofSeconds(5));

    log.info("✅ Mock streaming demonstration completed successfully!");
  }

  @Test
  void demonstrateStreamingCharacteristics() {
    log.info("🔍 Demonstrating Streaming Characteristics");

    MockLlmStreamingAdapter mockAdapter = new MockLlmStreamingAdapter();

    long startTime = System.currentTimeMillis();

    StepVerifier.create(mockAdapter.simulateStreamingResponse().collectList())
        .assertNext(
            chunks -> {
              long duration = System.currentTimeMillis() - startTime;

              log.info("📊 Streaming Statistics:");
              log.info("   • Total chunks: {}", chunks.size());
              log.info("   • Total characters: {}", chunks.stream().mapToInt(String::length).sum());
              log.info("   • Duration: {}ms", duration);
              log.info(
                  "   • Average chunk size: {} chars",
                  chunks.stream().mapToInt(String::length).average().orElse(0));

              // Verify realistic streaming characteristics
              assert chunks.size() > 1 : "Should receive multiple chunks for streaming";
              assert duration > 500 : "Should take reasonable time to simulate network delays";
            })
        .expectComplete()
        .verify(Duration.ofSeconds(5));

    log.info("✅ Streaming characteristics demonstration completed!");
  }
}
