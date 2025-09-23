package com.ghiloufi.aicode.infrastructure.config;

import com.ghiloufi.aicode.infrastructure.adapter.output.external.llm.OllamaLlmStreamingAdapter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * Test configuration for LLM integration testing.
 *
 * <p>Configures WebClient and LLM adapters for integration tests
 * with proper timeouts and streaming support.
 */
@TestConfiguration
@Profile("integration-test")
public class LlmIntegrationTestConfig {

    @Bean
    public WebClient llmWebClient() {
        return WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
            .build();
    }

    @Bean
    public OllamaLlmStreamingAdapter ollamaLlmStreamingAdapter(
            WebClient llmWebClient,
            String ollamaBaseUrl,
            String ollamaModelName) {
        return new OllamaLlmStreamingAdapter(llmWebClient, ollamaBaseUrl, ollamaModelName);
    }
}