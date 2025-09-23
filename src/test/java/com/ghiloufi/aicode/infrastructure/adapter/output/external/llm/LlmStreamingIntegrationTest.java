package com.ghiloufi.aicode.infrastructure.adapter.output.external.llm;

import com.ghiloufi.aicode.domain.entity.AnalysisResult;
import com.ghiloufi.aicode.domain.entity.DiffAnalysis;
import com.ghiloufi.aicode.domain.entity.FileModification;
import com.ghiloufi.aicode.domain.entity.DiffHunk;
import com.ghiloufi.aicode.domain.value.ReviewConfiguration;
import com.ghiloufi.aicode.infrastructure.config.LlmIntegrationTestConfig;
import com.ghiloufi.aicode.infrastructure.testcontainer.OllamaTestContainer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for LLM streaming responses using embedded Ollama.
 *
 * <p>This test demonstrates true LLM integration testing with:
 * - Real LLM inference via Ollama TestContainer
 * - Streaming response validation
 * - Timeout and error handling
 * - Performance characteristics
 *
 * <p>Run with: mvn test -Dtest=LlmStreamingIntegrationTest -Dllm.integration.test=true
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("integration-test")
@ContextConfiguration(classes = {OllamaTestContainer.class, LlmIntegrationTestConfig.class})
@Testcontainers
@EnabledIfSystemProperty(named = "llm.integration.test", matches = "true")
@Slf4j
class LlmStreamingIntegrationTest {

    @Autowired
    private OllamaLlmStreamingAdapter ollamaAdapter;

    @Test
    void testStreamingLlmAnalysis() {
        // Given: A code diff for analysis
        DiffAnalysis diffAnalysis = createTestDiffAnalysis();
        ReviewConfiguration config = createTestReviewConfiguration();

        // When: We perform streaming analysis
        Flux<String> streamingResponse = ollamaAdapter.streamAnalyzeDiff(diffAnalysis, config);

        // Then: We receive streaming chunks and can verify streaming behavior
        AtomicInteger chunkCount = new AtomicInteger(0);
        StringBuilder collectedResponse = new StringBuilder();

        StepVerifier.create(streamingResponse)
            .expectSubscription()
            .thenConsumeWhile(chunk -> {
                log.info("Received chunk #{}: {}", chunkCount.incrementAndGet(),
                    chunk.length() > 100 ? chunk.substring(0, 100) + "..." : chunk);
                collectedResponse.append(chunk);
                return true; // Continue consuming all chunks
            })
            .expectComplete()
            .verify(Duration.ofMinutes(2)); // Allow sufficient time for LLM response

        // Verify streaming characteristics
        assertThat(chunkCount.get()).isGreaterThan(1); // Should receive multiple chunks
        assertThat(collectedResponse.toString()).isNotEmpty();
        log.info("Streaming test completed: {} chunks, {} total characters",
                chunkCount.get(), collectedResponse.length());
    }

    @Test
    void testFullAnalysisWithStreaming() {
        // Given: A code diff for analysis
        DiffAnalysis diffAnalysis = createTestDiffAnalysis();
        ReviewConfiguration config = createTestReviewConfiguration();

        // When: We perform full analysis (which uses streaming internally)
        StepVerifier.create(ollamaAdapter.analyzeDiff(diffAnalysis, config))
            .assertNext(analysisResult -> {
                // Then: We get a complete AnalysisResult
                assertThat(analysisResult).isNotNull();
                assertThat(analysisResult.getType()).isEqualTo(AnalysisResult.AnalysisType.LLM_ANALYSIS);
                assertThat(analysisResult.getSummary()).isNotEmpty();
                assertThat(analysisResult.getToolVersion()).startsWith("ollama-");

                log.info("Analysis completed: {}", analysisResult.getSummary());
                log.info("Analysis notes: {}", analysisResult.getNotes());
            })
            .expectComplete()
            .verify(Duration.ofMinutes(2));
    }

    @Test
    void testStreamingTimeout() {
        // Given: A very large diff that might timeout
        DiffAnalysis largeDiffAnalysis = createLargeDiffAnalysis();
        ReviewConfiguration config = createTestReviewConfiguration();

        // When/Then: The streaming should handle timeout gracefully
        StepVerifier.create(
                ollamaAdapter.streamAnalyzeDiff(largeDiffAnalysis, config)
                    .timeout(Duration.ofSeconds(30)) // Shorter timeout for test
            )
            .expectSubscription()
            .expectNextCount(0) // Might not receive any chunks before timeout
            .expectError()
            .verify();
    }

    @Test
    void testStreamingErrorHandling() {
        // Given: Invalid configuration that should cause an error
        DiffAnalysis diffAnalysis = createTestDiffAnalysis();
        ReviewConfiguration invalidConfig = new ReviewConfiguration(
            "non-existent-model", // This should cause an error
            null, 30, 1000, 3, "17", "maven",
            false, true, false
        );

        // When/Then: The streaming should handle errors gracefully
        StepVerifier.create(ollamaAdapter.streamAnalyzeDiff(diffAnalysis, invalidConfig))
            .expectSubscription()
            .expectError()
            .verify(Duration.ofSeconds(10));
    }

    /**
     * Creates a realistic test diff for analysis.
     */
    private DiffAnalysis createTestDiffAnalysis() {
        String testDiff = """
            --- a/src/main/java/Example.java
            +++ b/src/main/java/Example.java
            @@ -1,10 +1,12 @@
             public class Example {
                 public void method() {
            -        System.out.println("Hello");
            +        System.out.println("Hello World");
            +        // TODO: Add error handling
                 }

            +    public void newMethod() {
            +        // New functionality
            +    }
             }
            """;

        List<FileModification> fileModifications = List.of(
            new FileModification(
                "src/main/java/Example.java",
                "src/main/java/Example.java",
                List.of(new DiffHunk(1, 10, 1, 12,
                    List.of("-        System.out.println(\"Hello\");",
                           "+        System.out.println(\"Hello World\");",
                           "+        // TODO: Add error handling"))),
                FileModification.ModificationType.MODIFIED
            )
        );

        return new DiffAnalysis(testDiff, fileModifications);
    }

    /**
     * Creates a large diff to test timeout behavior.
     */
    private DiffAnalysis createLargeDiffAnalysis() {
        StringBuilder largeDiff = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            largeDiff.append(String.format("""
                --- a/src/main/java/Class%d.java
                +++ b/src/main/java/Class%d.java
                @@ -1,50 +1,50 @@
                // Large class with many changes...
                """, i, i));
        }

        return new DiffAnalysis(largeDiff.toString(), List.of());
    }

    /**
     * Creates test review configuration.
     */
    private ReviewConfiguration createTestReviewConfiguration() {
        return new ReviewConfiguration(
            "codellama:7b-code", // Use the model we pulled in TestContainer
            "http://localhost:11434", // Will be overridden by test config
            60, 1500, 5, "17", "maven",
            false, true, false
        );
    }
}