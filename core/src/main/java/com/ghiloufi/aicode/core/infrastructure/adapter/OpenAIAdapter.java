package com.ghiloufi.aicode.core.infrastructure.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghiloufi.aicode.core.domain.port.output.AIInteractionPort;
import com.ghiloufi.aicode.core.infrastructure.adapter.openai.OpenAIChatRequest;
import com.ghiloufi.aicode.core.infrastructure.adapter.openai.OpenAIStreamResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Slf4j
public final class OpenAIAdapter implements AIInteractionPort {

  private static final String SSE_DONE_MARKER = "[DONE]";

  private final WebClient webClient;
  private final ObjectMapper objectMapper;
  private final String model;

  public OpenAIAdapter(
      final String baseUrl,
      final String model,
      final String apiKey,
      final ObjectMapper objectMapper) {
    this.model = model;
    this.objectMapper = objectMapper;
    this.webClient =
        WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .defaultHeader("Content-Type", "application/json")
            .build();
    log.info("OpenAI adapter initialized: {}", model);
  }

  @Override
  public Flux<String> streamCompletion(final String systemPrompt, final String userPrompt) {
    log.debug(
        "Sending prompt: system={} chars, user={} chars",
        systemPrompt.length(),
        userPrompt.length());

    final OpenAIChatRequest request =
        OpenAIChatRequest.forCodeReview(model, systemPrompt, userPrompt);

    return webClient
        .post()
        .uri("/chat/completions")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(serializeRequest(request))
        .accept(MediaType.TEXT_EVENT_STREAM)
        .retrieve()
        .bodyToFlux(String.class)
        .filter(this::isValidSseData)
        .map(this::parseStreamResponse)
        .filter(OpenAIStreamResponse::hasContent)
        .map(response -> response.extractFirstContent().orElse(""))
        .doOnError(error -> log.error("Streaming error: {}", error.getMessage()))
        .doOnComplete(() -> log.debug("Streaming completed"));
  }

  private String serializeRequest(final OpenAIChatRequest request) {
    try {
      return objectMapper.writeValueAsString(request);
    } catch (final JsonProcessingException e) {
      log.error("Failed to serialize OpenAI request: {}", e.getMessage());
      throw new IllegalStateException("Failed to serialize request", e);
    }
  }

  private boolean isValidSseData(final String data) {
    final String trimmed = data.trim();
    return !trimmed.isEmpty() && !trimmed.equals(SSE_DONE_MARKER);
  }

  private OpenAIStreamResponse parseStreamResponse(final String data) {
    final String json = data.trim();
    try {
      return objectMapper.readValue(json, OpenAIStreamResponse.class);
    } catch (final JsonProcessingException e) {
      log.debug("Failed to parse stream response: {}", e.getMessage());
      return new OpenAIStreamResponse(null, null, null);
    }
  }

  @Override
  public String getProviderName() {
    return "openai";
  }

  @Override
  public String getModelName() {
    return model;
  }
}
