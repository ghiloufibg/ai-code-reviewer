package com.ghiloufi.aicode.infrastructure.adapter.output.external.llm;

import com.ghiloufi.aicode.application.port.output.LlmAnalysisPort;
import com.ghiloufi.aicode.domain.entity.AnalysisResult;
import com.ghiloufi.aicode.domain.entity.DiffAnalysis;
import com.ghiloufi.aicode.domain.value.ReviewConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Test adapter for Ollama LLM integration testing with streaming support.
 *
 * <p>This adapter provides real LLM integration for testing streaming responses
 * and actual AI analysis behavior in integration tests.
 */
@TestComponent
@Profile("integration-test")
@RequiredArgsConstructor
@Slf4j
public class OllamaLlmStreamingAdapter implements LlmAnalysisPort {

    private final WebClient webClient;
    private final String ollamaBaseUrl;
    private final String ollamaModelName;

    @Override
    public Mono<AnalysisResult> analyzeDiff(DiffAnalysis diffAnalysis, ReviewConfiguration config) {
        return streamAnalyzeDiff(diffAnalysis, config)
            .collectList()
            .map(chunks -> combineStreamingChunks(chunks, diffAnalysis));
    }

    /**
     * Tests streaming LLM responses - the key capability we need to verify.
     */
    public Flux<String> streamAnalyzeDiff(DiffAnalysis diffAnalysis, ReviewConfiguration config) {
        String prompt = buildPrompt(diffAnalysis);

        Map<String, Object> requestBody = Map.of(
            "model", ollamaModelName,
            "prompt", prompt,
            "stream", true,
            "options", Map.of(
                "temperature", 0.1,
                "top_p", 0.9,
                "max_tokens", 1000
            )
        );

        return webClient
            .post()
            .uri(ollamaBaseUrl + "/api/generate")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToFlux(String.class)
            .filter(chunk -> !chunk.trim().isEmpty())
            .map(this::extractContentFromChunk)
            .filter(content -> !content.isEmpty())
            .timeout(Duration.ofSeconds(60))
            .doOnNext(chunk -> log.debug("Received streaming chunk: {}", chunk))
            .doOnComplete(() -> log.info("Streaming analysis completed"))
            .doOnError(error -> log.error("Streaming analysis failed", error));
    }

    /**
     * Combines streaming chunks into final AnalysisResult.
     */
    private AnalysisResult combineStreamingChunks(List<String> chunks, DiffAnalysis diffAnalysis) {
        String fullResponse = String.join("", chunks);
        log.info("Combined {} streaming chunks into response of {} characters",
                chunks.size(), fullResponse.length());

        // Parse the LLM response (simplified for testing)
        return new AnalysisResult(
            AnalysisResult.AnalysisType.LLM_ANALYSIS,
            "Streaming LLM Analysis: " + fullResponse,
            List.of(), // Would parse issues from response in production
            List.of("Streamed in " + chunks.size() + " chunks"),
            "ollama-" + ollamaModelName
        );
    }

    /**
     * Builds the prompt for code analysis.
     */
    private String buildPrompt(DiffAnalysis diffAnalysis) {
        return String.format(
            "Review this code diff and provide feedback:\\n\\n" +
            "```diff\\n%s\\n```\\n\\n" +
            "Please provide:\\n" +
            "1. Summary of changes\\n" +
            "2. Potential issues\\n" +
            "3. Suggestions for improvement\\n",
            diffAnalysis.getRawDiff().length() > 2000 ?
                diffAnalysis.getRawDiff().substring(0, 2000) + "..." :
                diffAnalysis.getRawDiff()
        );
    }

    /**
     * Extracts content from Ollama streaming response chunk.
     */
    private String extractContentFromChunk(String chunk) {
        try {
            // Ollama returns NDJSON format: {"model":"...","response":"chunk content","done":false}
            if (chunk.contains("\"response\":")) {
                int start = chunk.indexOf("\"response\":\"") + 12;
                int end = chunk.indexOf("\"", start);
                if (end > start) {
                    return chunk.substring(start, end)
                        .replace("\\\\n", "\\n")
                        .replace("\\\\\"", "\"");
                }
            }
            return "";
        } catch (Exception e) {
            log.warn("Failed to extract content from chunk: {}", chunk, e);
            return "";
        }
    }
}