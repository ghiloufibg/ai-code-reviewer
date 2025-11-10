package com.ghiloufi.aicode.core.infrastructure.adapter;

import com.ghiloufi.aicode.core.domain.port.output.AIInteractionPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Slf4j
public final class OpenAIAdapter implements AIInteractionPort {

  private final WebClient webClient;
  private final String model;

  public OpenAIAdapter(final String baseUrl, final String model, final String apiKey) {
    this.model = model;
    this.webClient =
        WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .defaultHeader("Content-Type", "application/json")
            .build();
    log.info("OpenAI adapter initialized: {}", model);
  }

  @Override
  public Flux<String> streamCompletion(final String prompt) {
    log.debug("Sending prompt: {} chars", prompt.length());

    final String requestBody =
        String.format(
            """
            {
              "model": "%s",
              "messages": [
                {
                  "role": "system",
                  "content": "You are a code reviewer. Analyze code and provide constructive feedback."
                },
                {
                  "role": "user",
                  "content": %s
                }
              ],
              "stream": true,
              "temperature": 0.1
            }
            """,
            model, escapeJson(prompt));

    return webClient
        .post()
        .uri("/chat/completions")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(requestBody)
        .accept(MediaType.TEXT_EVENT_STREAM)
        .retrieve()
        .bodyToFlux(String.class)
        .filter(line -> !line.trim().isEmpty() && !line.equals("data: [DONE]"))
        .map(
            line -> {
              if (line.startsWith("data: ")) {
                return line.substring(6);
              }
              return line;
            })
        .filter(json -> json.contains("\"content\""))
        .map(this::extractContent)
        .doOnError(error -> log.error("Streaming error: {}", error.getMessage()))
        .doOnComplete(() -> log.debug("Streaming completed"));
  }

  private String extractContent(final String json) {
    try {
      final int contentStart = json.indexOf("\"content\":\"") + 11;
      if (contentStart == 10) {
        return "";
      }
      int contentEnd = json.indexOf("\"", contentStart);
      while (contentEnd > 0 && json.charAt(contentEnd - 1) == '\\') {
        contentEnd = json.indexOf("\"", contentEnd + 1);
      }
      if (contentEnd == -1) {
        return "";
      }
      return json.substring(contentStart, contentEnd).replace("\\n", "\n").replace("\\\"", "\"");
    } catch (final Exception e) {
      log.debug("Content extraction failed");
      return "";
    }
  }

  private String escapeJson(final String text) {
    return "\""
        + text.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        + "\"";
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
