package com.ghiloufi.aicode.core.infrastructure.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghiloufi.aicode.core.config.TicketAnalysisPromptProperties;
import com.ghiloufi.aicode.core.domain.model.StructuredTicketAnalysis;
import com.ghiloufi.aicode.core.domain.model.TicketBusinessContext;
import com.ghiloufi.aicode.core.domain.port.output.TicketAnalysisPort;
import com.ghiloufi.aicode.core.infrastructure.adapter.openai.OpenAIChatRequest;
import com.ghiloufi.aicode.core.infrastructure.adapter.openai.OpenAIStreamResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
public final class OpenAITicketAnalysisAdapter implements TicketAnalysisPort {

  private static final String SSE_DATA_PREFIX = "data: ";
  private static final String SSE_DONE_MARKER = "[DONE]";

  private final WebClient webClient;
  private final ObjectMapper objectMapper;
  private final String model;
  private final TicketAnalysisPromptProperties promptProperties;

  public OpenAITicketAnalysisAdapter(
      final String baseUrl,
      final String model,
      final String apiKey,
      final ObjectMapper objectMapper,
      final TicketAnalysisPromptProperties promptProperties) {
    this.model = model;
    this.objectMapper = objectMapper;
    this.promptProperties = promptProperties;
    this.webClient =
        WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .defaultHeader("Content-Type", "application/json")
            .build();
    log.info("OpenAI ticket analysis adapter initialized: {}", model);
  }

  @Override
  public Mono<StructuredTicketAnalysis> analyzeTicket(final TicketBusinessContext rawContext) {
    if (rawContext.isEmpty() || !rawContext.hasDescription()) {
      log.debug("Skipping ticket analysis: empty or no description");
      return Mono.just(createEmptyAnalysis(rawContext));
    }

    log.debug("Analyzing ticket: {}", rawContext.ticketId());

    final String ticketContent = formatTicketForAnalysis(rawContext);
    final OpenAIChatRequest request =
        OpenAIChatRequest.forTicketAnalysis(model, promptProperties.getSystem(), ticketContent);

    return webClient
        .post()
        .uri("/chat/completions")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(serializeRequest(request))
        .accept(MediaType.TEXT_EVENT_STREAM)
        .retrieve()
        .bodyToFlux(String.class)
        .filter(this::isValidSseLine)
        .map(this::extractJsonFromSseLine)
        .map(this::parseStreamResponse)
        .filter(OpenAIStreamResponse::hasContent)
        .map(response -> response.extractFirstContent().orElse(""))
        .reduce(String::concat)
        .flatMap(json -> parseAnalysisResponse(json, rawContext))
        .doOnSuccess(result -> log.debug("Ticket analysis completed: {}", rawContext.ticketId()))
        .doOnError(error -> log.error("Ticket analysis failed: {}", error.getMessage()))
        .onErrorReturn(createEmptyAnalysis(rawContext));
  }

  private String formatTicketForAnalysis(final TicketBusinessContext context) {
    return """
        TICKET ID: %s
        TITLE: %s

        DESCRIPTION:
        %s
        """
        .formatted(context.ticketId(), context.title(), context.fullDescription());
  }

  private Mono<StructuredTicketAnalysis> parseAnalysisResponse(
      final String json, final TicketBusinessContext rawContext) {
    try {
      final TicketAnalysisResponse response =
          objectMapper.readValue(json, TicketAnalysisResponse.class);
      return Mono.just(
          StructuredTicketAnalysis.fromRawContext(
              rawContext,
              response.objective(),
              response.acceptanceCriteria(),
              response.businessRules(),
              response.technicalNotes()));
    } catch (final JsonProcessingException e) {
      log.warn("Failed to parse ticket analysis response: {}", e.getMessage());
      return Mono.just(createEmptyAnalysis(rawContext));
    }
  }

  private StructuredTicketAnalysis createEmptyAnalysis(final TicketBusinessContext rawContext) {
    return StructuredTicketAnalysis.fromRawContext(rawContext, "", List.of(), List.of(), List.of());
  }

  private String serializeRequest(final OpenAIChatRequest request) {
    try {
      return objectMapper.writeValueAsString(request);
    } catch (final JsonProcessingException e) {
      log.error("Failed to serialize OpenAI request: {}", e.getMessage());
      throw new IllegalStateException("Failed to serialize request", e);
    }
  }

  private boolean isValidSseLine(final String line) {
    final String trimmed = line.trim();
    return !trimmed.isEmpty() && !trimmed.equals(SSE_DATA_PREFIX + SSE_DONE_MARKER);
  }

  private String extractJsonFromSseLine(final String line) {
    if (line.startsWith(SSE_DATA_PREFIX)) {
      return line.substring(SSE_DATA_PREFIX.length());
    }
    return line;
  }

  private OpenAIStreamResponse parseStreamResponse(final String json) {
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

  private record TicketAnalysisResponse(
      String objective,
      List<String> acceptanceCriteria,
      List<String> businessRules,
      List<String> technicalNotes) {}
}
