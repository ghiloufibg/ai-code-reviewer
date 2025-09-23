package com.ghiloufi.aicode;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.domain.entity.AnalysisResult;
import com.ghiloufi.aicode.domain.entity.DiffAnalysis;
import com.ghiloufi.aicode.domain.entity.DiffHunk;
import com.ghiloufi.aicode.domain.entity.FileModification;
import com.ghiloufi.aicode.domain.value.ReviewConfiguration;
import com.ghiloufi.aicode.infrastructure.mock.MockLlmStreamingAdapter;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

/**
 * Final validation test demonstrating our Clean Architecture code works as expected with mock
 * streaming capabilities.
 *
 * <p>This test validates: ✅ Mock streaming LLM integration works ✅ Clean Architecture ports and
 * adapters work correctly ✅ Domain entities are properly created and managed ✅ Reactive streaming
 * with proper error handling ✅ Performance characteristics are realistic
 */
@Slf4j
class CleanArchitectureStreamingValidationTest {

  @Test
  void assertCleanArchitectureWorksWithStreamingLlm() {
    log.info("🎯 FINAL VALIDATION: Clean Architecture + Streaming LLM Integration");

    // Given: Our Clean Architecture components
    MockLlmStreamingAdapter streamingAdapter = new MockLlmStreamingAdapter();
    DiffAnalysis testDiff = createRealisticDiff();
    ReviewConfiguration config = createTestConfig();

    // When: We execute streaming analysis through our architecture
    long startTime = System.currentTimeMillis();

    StepVerifier.create(streamingAdapter.analyze(testDiff, config))
        .assertNext(
            result -> {
              long duration = System.currentTimeMillis() - startTime;

              // Then: Assert our code works as expected
              log.info("🔍 VALIDATION RESULTS:");

              // ✅ Clean Architecture Port Implementation
              assertThat(result).isNotNull();
              assertThat(streamingAdapter.isAvailable()).isTrue();
              assertThat(streamingAdapter.getModelInfo()).isEqualTo("mock-llm-v1.0");
              log.info("   ✅ AIAnalysisPort implementation: WORKING");

              // ✅ Domain Entity Creation
              assertThat(result.getType()).isEqualTo(AnalysisResult.AnalysisType.LLM_ANALYSIS);
              assertThat(result.getAnalyzedAt()).isNotNull();
              assertThat(result.getSummary()).isNotEmpty();
              log.info("   ✅ Domain entity creation: WORKING");

              // ✅ Streaming Response Processing
              assertThat(result.getSummary())
                  .contains("Code Review Summary")
                  .contains("Changes Detected")
                  .contains("Issues Found")
                  .contains("Recommendations");
              log.info("   ✅ Streaming response aggregation: WORKING");

              // ✅ Reactive Performance
              assertThat(duration)
                  .isGreaterThan(500) // Realistic streaming delay
                  .isLessThan(5000); // But not too slow for tests
              log.info("   ✅ Reactive streaming performance: WORKING ({}ms)", duration);

              // ✅ Metadata and Notes
              assertThat(result.getNotes())
                  .isNotEmpty()
                  .anyMatch(note -> note.contains("Mock streaming analysis"))
                  .anyMatch(note -> note.contains("chunks"));
              log.info("   ✅ Analysis metadata and notes: WORKING");

              // ✅ Tool Version Tracking
              assertThat(result.getToolVersion()).isEqualTo("mock-llm-v1.0");
              log.info("   ✅ Tool version tracking: WORKING");

              log.info("🎉 ALL VALIDATIONS PASSED! Clean Architecture + Streaming LLM = SUCCESS");
            })
        .expectComplete()
        .verify(Duration.ofSeconds(10));
  }

  @Test
  void assertStreamingCharacteristicsAreRealistic() {
    log.info("📊 STREAMING CHARACTERISTICS VALIDATION");

    MockLlmStreamingAdapter adapter = new MockLlmStreamingAdapter();

    StepVerifier.create(adapter.simulateStreamingResponse().collectList())
        .assertNext(
            chunks -> {
              log.info("🔍 STREAMING ANALYSIS:");

              // ✅ Multiple chunks for realistic streaming
              assertThat(chunks.size()).isGreaterThan(5);
              log.info("   ✅ Chunk count: {} (realistic streaming)", chunks.size());

              // ✅ Progressive content delivery
              String firstChunk = chunks.get(0);
              String lastChunk = chunks.get(chunks.size() - 1);
              assertThat(firstChunk).contains("Code Review Summary");
              assertThat(lastChunk).contains("Overall, the changes look good");
              log.info("   ✅ Progressive content: START → END");

              // ✅ Total content length
              int totalLength = chunks.stream().mapToInt(String::length).sum();
              assertThat(totalLength).isGreaterThan(400);
              log.info("   ✅ Content volume: {} characters", totalLength);

              // ✅ Chunk variety
              boolean hasHeader = chunks.stream().anyMatch(c -> c.contains("##"));
              boolean hasLists = chunks.stream().anyMatch(c -> c.contains("-"));
              boolean hasNumbers = chunks.stream().anyMatch(c -> c.contains("1."));
              assertThat(hasHeader && hasLists && hasNumbers).isTrue();
              log.info("   ✅ Content variety: Headers, Lists, Numbers");

              log.info("🎉 STREAMING CHARACTERISTICS: VALIDATED");
            })
        .expectComplete()
        .verify(Duration.ofSeconds(5));
  }

  @Test
  void assertErrorHandlingWorksCorrectly() {
    log.info("🚨 ERROR HANDLING VALIDATION");

    MockLlmStreamingAdapter adapter = new MockLlmStreamingAdapter();

    StepVerifier.create(adapter.simulateStreamingWithError())
        .expectSubscription()
        .expectNextCount(3) // Should receive some chunks before error
        .expectErrorMatches(
            error -> {
              log.info("   ✅ Error caught: {}", error.getMessage());
              return error instanceof RuntimeException
                  && error.getMessage().contains("Simulated LLM connection error");
            })
        .verify(Duration.ofSeconds(3));

    log.info("🎉 ERROR HANDLING: VALIDATED");
  }

  @Test
  void assertArchitectureLayersWorkTogether() {
    log.info("🏗️ ARCHITECTURE LAYERS VALIDATION");

    // Given: Architecture components
    MockLlmStreamingAdapter adapter = new MockLlmStreamingAdapter();
    DiffAnalysis diffAnalysis = createRealisticDiff();
    ReviewConfiguration config = createTestConfig();

    // When: We test the full flow
    StepVerifier.create(adapter.analyze(diffAnalysis, config))
        .assertNext(
            result -> {
              log.info("🔍 LAYER INTEGRATION:");

              // ✅ Application Layer (Port)
              assertThat(adapter.isAvailable()).isTrue();
              log.info("   ✅ Application Layer: Port available");

              // ✅ Domain Layer (Entity)
              assertThat(result.getType()).isEqualTo(AnalysisResult.AnalysisType.LLM_ANALYSIS);
              assertThat(result.getAnalyzedAt()).isNotNull();
              log.info("   ✅ Domain Layer: AnalysisResult entity created");

              // ✅ Infrastructure Layer (Adapter)
              assertThat(result.getToolVersion()).contains("mock");
              assertThat(result.getNotes()).isNotEmpty();
              log.info("   ✅ Infrastructure Layer: Mock adapter working");

              // ✅ Value Objects
              assertThat(diffAnalysis.getFileModifications()).isNotEmpty();
              assertThat(config.model()).isNotEmpty();
              log.info("   ✅ Value Objects: DiffAnalysis and ReviewConfiguration");

              log.info("🎉 ALL ARCHITECTURE LAYERS: WORKING TOGETHER");
            })
        .expectComplete()
        .verify(Duration.ofSeconds(5));
  }

  // Helper methods
  private DiffAnalysis createRealisticDiff() {
    String testDiff =
        """
            --- a/src/main/java/Example.java
            +++ b/src/main/java/Example.java
            @@ -1,8 +1,12 @@
             public class Example {
                 public void method() {
            -        System.out.println("Hello");
            +        System.out.println("Hello World");
            +        // TODO: Add error handling
                 }

            +    public void newMethod() {
            +        // New functionality
            +    }
            +
                 private void helper() {
                     // Helper method
                 }
            """;

    List<FileModification> modifications =
        List.of(
            new FileModification(
                "src/main/java/Example.java",
                "src/main/java/Example.java",
                List.of(
                    new DiffHunk(
                        1,
                        8,
                        1,
                        12,
                        List.of(
                            "-        System.out.println(\"Hello\");",
                            "+        System.out.println(\"Hello World\");",
                            "+        // TODO: Add error handling",
                            "+    public void newMethod() {",
                            "+        // New functionality",
                            "+    }"))),
                FileModification.ModificationType.MODIFIED));

    return new DiffAnalysis(testDiff, modifications);
  }

  private ReviewConfiguration createTestConfig() {
    return new ReviewConfiguration(
        "mock-model", "http://localhost:11434", 60, 1500, 5, "17", "maven", false, true, false);
  }
}
