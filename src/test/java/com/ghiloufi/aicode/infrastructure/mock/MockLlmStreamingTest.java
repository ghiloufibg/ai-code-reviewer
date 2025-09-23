package com.ghiloufi.aicode.infrastructure.mock;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.domain.entity.AnalysisResult;
import com.ghiloufi.aicode.domain.entity.DiffAnalysis;
import com.ghiloufi.aicode.domain.entity.DiffHunk;
import com.ghiloufi.aicode.domain.entity.FileModification;
import com.ghiloufi.aicode.domain.value.ReviewConfiguration;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * Unit test for mock streaming LLM responses.
 *
 * <p>This test demonstrates how to test streaming behavior without running actual LLM
 * infrastructure, making it perfect for: - Unit tests - CI/CD pipelines with resource constraints -
 * Fast feedback during development
 *
 * <p>Run with: mvn test -Dtest=MockLlmStreamingTest
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("mock-test")
@Slf4j
class MockLlmStreamingTest {

  private MockLlmStreamingAdapter mockAdapter;
  private DiffAnalysis testDiffAnalysis;
  private ReviewConfiguration testConfig;

  @BeforeEach
  void setUp() {
    mockAdapter = new MockLlmStreamingAdapter();
    testDiffAnalysis = createTestDiffAnalysis();
    testConfig = createTestReviewConfiguration();
  }

  @Test
  void testMockStreamingResponse() {
    // When: We simulate streaming response
    Flux<String> streamingResponse = mockAdapter.simulateStreamingResponse();

    // Then: We receive predictable streaming chunks
    AtomicInteger chunkCount = new AtomicInteger(0);
    StringBuilder collectedResponse = new StringBuilder();

    StepVerifier.create(streamingResponse)
        .expectSubscription()
        .thenConsumeWhile(
            chunk -> {
              int count = chunkCount.incrementAndGet();
              collectedResponse.append(chunk);
              log.info(
                  "Received mock chunk #{}: {}",
                  count,
                  chunk.replace("\\n", " ").substring(0, Math.min(50, chunk.length())));

              // Verify expected chunks
              switch (count) {
                case 1 -> assertThat(chunk).contains("Code Review Summary");
                case 2 -> assertThat(chunk).contains("analyzed your code diff");
                case 3 -> assertThat(chunk).contains("Changes Detected");
              }
              return true;
            })
        .expectComplete()
        .verify(Duration.ofSeconds(5));

    // Verify streaming characteristics
    assertThat(chunkCount.get()).isEqualTo(9); // Expected number of chunks
    assertThat(collectedResponse.toString())
        .contains("Code Review Summary")
        .contains("Missing Error Handling")
        .contains("Recommendations");

    log.info(
        "Mock streaming test completed: {} chunks, {} total characters",
        chunkCount.get(),
        collectedResponse.length());
  }

  @Test
  void testFullAnalysisWithMockStreaming() {
    // When: We perform full analysis using mock streaming
    StepVerifier.create(mockAdapter.analyzeDiff(testDiffAnalysis, testConfig))
        .assertNext(
            analysisResult -> {
              // Then: We get a predictable AnalysisResult
              assertThat(analysisResult).isNotNull();
              assertThat(analysisResult.getType())
                  .isEqualTo(AnalysisResult.AnalysisType.LLM_ANALYSIS);
              assertThat(analysisResult.getSummary())
                  .contains("Code Review Summary")
                  .contains("Missing Error Handling");
              assertThat(analysisResult.getToolVersion()).isEqualTo("mock-llm-v1.0");
              assertThat(analysisResult.getNotes()).contains("Mock streaming analysis").hasSize(3);

              log.info(
                  "Mock analysis completed: {}", analysisResult.getSummary().substring(0, 100));
            })
        .expectComplete()
        .verify(Duration.ofSeconds(5));
  }

  @Test
  void testStreamingErrorHandling() {
    // When: We simulate streaming with error
    Flux<String> errorResponse = mockAdapter.simulateStreamingWithError();

    // Then: Error should be propagated correctly
    StepVerifier.create(errorResponse)
        .expectSubscription()
        .expectNextCount(3) // Should receive 3 chunks before error
        .expectErrorMatches(
            throwable ->
                throwable instanceof RuntimeException
                    && throwable.getMessage().contains("Simulated LLM connection error"))
        .verify(Duration.ofSeconds(2));
  }

  @Test
  void testStreamingTimeout() {
    // When: We simulate slow streaming that should timeout
    Flux<String> slowResponse =
        mockAdapter
            .simulateStreamingWithTimeout()
            .timeout(Duration.ofSeconds(1)); // Short timeout for test

    // Then: Should timeout as expected
    StepVerifier.create(slowResponse)
        .expectSubscription()
        .expectError()
        .verify(Duration.ofSeconds(2));
  }

  @Test
  void testStreamingPerformanceCharacteristics() {
    // When: We measure streaming performance
    long startTime = System.currentTimeMillis();

    StepVerifier.create(mockAdapter.simulateStreamingResponse())
        .expectSubscription()
        .expectNextCount(9) // Expect all 9 chunks
        .expectComplete()
        .verify(Duration.ofSeconds(5));

    long duration = System.currentTimeMillis() - startTime;

    // Then: Should complete within reasonable time
    assertThat(duration)
        .isGreaterThan(800) // Should take at least 800ms (9 chunks * 100ms delay)
        .isLessThan(2000); // But not too long for a unit test

    log.info("Mock streaming completed in {}ms", duration);
  }

  @Test
  void testStreamingChunkContent() {
    // When: We collect and analyze streaming chunks
    StepVerifier.create(mockAdapter.simulateStreamingResponse().collectList())
        .assertNext(
            chunks -> {
              // Then: Chunks should have expected content structure
              assertThat(chunks).hasSize(9);

              // Verify specific chunk contents
              assertThat(chunks.get(0)).startsWith("## Code Review Summary");
              assertThat(chunks.get(2)).contains("### Changes Detected:");
              assertThat(chunks.get(5)).contains("### Issues Found:");
              assertThat(chunks.get(7)).contains("### Recommendations:");
              assertThat(chunks.get(8)).contains("Overall, the changes look good");

              // Verify chunk progression makes sense
              String fullContent = String.join("", chunks);
              assertThat(fullContent)
                  .contains("Code Review Summary")
                  .contains("Changes Detected")
                  .contains("Issues Found")
                  .contains("Recommendations");
            })
        .expectComplete()
        .verify(Duration.ofSeconds(5));
  }

  private DiffAnalysis createTestDiffAnalysis() {
    String testDiff =
        """
            --- a/src/main/java/Example.java
            +++ b/src/main/java/Example.java
            @@ -1,5 +1,7 @@
             public class Example {
                 public void method() {
            -        System.out.println("Hello");
            +        System.out.println("Hello World");
            +        // TODO: Add error handling
                 }
            +    public void newMethod() {}
             }
            """;

    List<FileModification> fileModifications =
        List.of(
            new FileModification(
                "src/main/java/Example.java",
                "src/main/java/Example.java",
                List.of(
                    new DiffHunk(
                        1,
                        5,
                        1,
                        7,
                        List.of(
                            "-        System.out.println(\"Hello\");",
                            "+        System.out.println(\"Hello World\");",
                            "+        // TODO: Add error handling"))),
                FileModification.ModificationType.MODIFIED));

    return new DiffAnalysis(testDiff, fileModifications);
  }

  private ReviewConfiguration createTestReviewConfiguration() {
    return new ReviewConfiguration(
        "mock-model", null, 30, 1000, 3, "17", "maven", false, true, false);
  }
}
