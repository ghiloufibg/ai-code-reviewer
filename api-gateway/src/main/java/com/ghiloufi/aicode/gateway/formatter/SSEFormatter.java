package com.ghiloufi.aicode.gateway.formatter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghiloufi.aicode.core.domain.model.ReviewChunk;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public final class SSEFormatter {

  private final ObjectMapper objectMapper;

  public String formatReviewChunk(final ReviewChunk chunk) {
    try {
      final Map<String, Object> chunkData =
          Map.of(
              "type", chunk.type().name(),
              "content", chunk.content(),
              "metadata", Optional.ofNullable(chunk.metadata()).orElse(""),
              "timestamp", System.currentTimeMillis());

      final String json = objectMapper.writeValueAsString(chunkData);
      return "data: " + json + "\n\n";
    } catch (final JsonProcessingException e) {
      log.warn("Failed to format review chunk as SSE: {}", e.getMessage());
      return "data: {\"error\":\"Failed to format chunk\"}\n\n";
    }
  }

  public String formatDone() {
    try {
      final Map<String, Object> doneData =
          Map.of(
              "type", "DONE",
              "message", "Analysis stream completed",
              "timestamp", System.currentTimeMillis());

      final String json = objectMapper.writeValueAsString(doneData);
      return "data: " + json + "\n\n";
    } catch (final JsonProcessingException e) {
      log.warn("Failed to format SSE done message: {}", e.getMessage());
      return "data: [DONE]\n\n";
    }
  }

  public String formatPublished() {
    try {
      final Map<String, Object> publishedData =
          Map.of(
              "type", "PUBLISHED",
              "message", "Review published to repository",
              "timestamp", System.currentTimeMillis());

      final String json = objectMapper.writeValueAsString(publishedData);
      return "data: " + json + "\n\n";
    } catch (final JsonProcessingException e) {
      log.warn("Failed to format SSE published message: {}", e.getMessage());
      return "data: [PUBLISHED]\n\n";
    }
  }

  public String formatError(final Throwable error) {
    try {
      final Map<String, Object> errorData =
          Map.of(
              "type",
              "ERROR",
              "error",
              Optional.ofNullable(error.getMessage()).orElse("Unknown error"),
              "timestamp",
              System.currentTimeMillis());

      final String json = objectMapper.writeValueAsString(errorData);
      return "data: " + json + "\n\n";
    } catch (final JsonProcessingException e) {
      log.error("Failed to format SSE error message: {}", e.getMessage());
      return "data: {\"error\":\"Internal error\"}\n\n";
    }
  }
}
