package com.ghiloufi.aicode.infrastructure.mock;

import com.ghiloufi.aicode.application.port.output.LlmAnalysisPort;
import com.ghiloufi.aicode.application.port.output.AIAnalysisPort;
import com.ghiloufi.aicode.domain.entity.AnalysisResult;
import com.ghiloufi.aicode.domain.entity.DiffAnalysis;
import com.ghiloufi.aicode.domain.value.ReviewConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * Mock LLM adapter that simulates streaming responses for testing.
 *
 * <p>This adapter provides predictable streaming behavior for unit tests
 * and CI/CD environments where running actual LLM containers isn't feasible.
 *
 * <p>Use this in environments where resources are limited or when you need
 * deterministic test behavior.
 */
@TestComponent
@Profile("mock-test")
@RequiredArgsConstructor
@Slf4j
public class MockLlmStreamingAdapter implements LlmAnalysisPort, AIAnalysisPort {

    private static final List<String> SIMULATED_STREAMING_CHUNKS = List.of(
        "## Code Review Summary\\n\\n",
        "I've analyzed your code diff and found the following:\\n\\n",
        "### Changes Detected:\\n",
        "- Modified output message from 'Hello' to 'Hello World'\\n",
        "- Added TODO comment for error handling\\n",
        "- Added new method 'newMethod()'\\n\\n",
        "### Issues Found:\\n",
        "1. **Missing Error Handling**: The TODO comment indicates missing error handling\\n",
        "2. **Method Documentation**: New method lacks JavaDoc documentation\\n\\n",
        "### Recommendations:\\n",
        "- Implement the error handling mentioned in the TODO\\n",
        "- Add proper documentation for new methods\\n",
        "- Consider adding unit tests for new functionality\\n\\n",
        "Overall, the changes look good but need some improvements."
    );

    @Override
    public Mono<AnalysisResult> analyzeDiff(DiffAnalysis diffAnalysis, ReviewConfiguration config) {
        return simulateStreamingResponse()
            .collectList()
            .map(chunks -> createMockAnalysisResult(chunks, diffAnalysis));
    }

    @Override
    public Mono<AnalysisResult> analyze(DiffAnalysis diffAnalysis, ReviewConfiguration configuration) {
        return analyzeDiff(diffAnalysis, configuration);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getModelInfo() {
        return "mock-llm-v1.0";
    }

    /**
     * Simulates streaming LLM response with realistic timing and chunking.
     */
    public Flux<String> simulateStreamingResponse() {
        return Flux.fromIterable(SIMULATED_STREAMING_CHUNKS)
            .delayElements(Duration.ofMillis(100)) // Simulate network latency
            .doOnNext(chunk -> log.debug("Streaming mock chunk: {}", chunk.replace("\\n", " ")))
            .doOnComplete(() -> log.info("Mock streaming analysis completed"));
    }

    /**
     * Creates a realistic AnalysisResult from mock streaming chunks.
     */
    private AnalysisResult createMockAnalysisResult(List<String> chunks, DiffAnalysis diffAnalysis) {
        String fullResponse = String.join("", chunks);
        log.info("Created mock analysis result from {} chunks", chunks.size());

        // Simulate realistic analysis result with issues
        return new AnalysisResult(
            AnalysisResult.AnalysisType.LLM_ANALYSIS,
            fullResponse,
            List.of(
                // Would create CodeIssue objects here in real implementation
            ),
            List.of(
                "Mock streaming analysis",
                "Processed " + chunks.size() + " chunks",
                "Analyzed " + diffAnalysis.getFileModifications().size() + " files"
            ),
            "mock-llm-v1.0"
        );
    }

    /**
     * Simulates streaming with error injection for error handling tests.
     */
    public Flux<String> simulateStreamingWithError() {
        return Flux.fromIterable(SIMULATED_STREAMING_CHUNKS.subList(0, 3))
            .delayElements(Duration.ofMillis(50))
            .concatWith(Flux.error(new RuntimeException("Simulated LLM connection error")));
    }

    /**
     * Simulates streaming with timeout for timeout handling tests.
     */
    public Flux<String> simulateStreamingWithTimeout() {
        return Flux.fromIterable(SIMULATED_STREAMING_CHUNKS)
            .delayElements(Duration.ofSeconds(10)) // Very slow to trigger timeout
            .doOnNext(chunk -> log.debug("Slow mock chunk: {}", chunk));
    }
}