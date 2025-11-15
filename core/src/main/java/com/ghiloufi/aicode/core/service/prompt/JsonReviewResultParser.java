package com.ghiloufi.aicode.core.service.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.exception.JsonValidationException;
import com.ghiloufi.aicode.core.service.validation.ReviewResultValidator;
import com.ghiloufi.aicode.core.service.validation.ValidationResult;
import com.google.json.JsonSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public final class JsonReviewResultParser {

  private final ReviewResultValidator validator;
  private final ObjectMapper objectMapper;

  public ReviewResult parse(final String jsonResponse) {
    if (jsonResponse == null) {
      throw new IllegalArgumentException("JSON response cannot be null");
    }

    if (jsonResponse.isBlank()) {
      throw new IllegalArgumentException("JSON response cannot be empty");
    }

    final String cleanedJson = cleanJsonResponse(jsonResponse);
    validateJsonStructure(cleanedJson);

    log.debug("Validating JSON response against schema");
    final ValidationResult validationResult = validator.validate(cleanedJson);

    if (!validationResult.isValid()) {
      final String errors = String.join(", ", validationResult.errors());
      log.error("JSON validation failed: {}", errors);
      log.debug("Invalid JSON content: {}", cleanedJson);
      throw new JsonValidationException("JSON validation failed: " + errors);
    }

    log.debug("JSON validation successful, parsing into ReviewResult");

    try {
      final ReviewResult result = objectMapper.readValue(cleanedJson, ReviewResult.class);
      normalizeConfidenceScores(result);
      log.info(
          "Successfully parsed ReviewResult: {} issues, {} notes",
          result.issues.size(),
          result.non_blocking_notes.size());
      return result;
    } catch (final Exception e) {
      log.error("Failed to parse JSON into ReviewResult", e);
      throw new JsonValidationException(
          "Failed to parse JSON into ReviewResult: " + e.getMessage(), e);
    }
  }

  private void normalizeConfidenceScores(final ReviewResult result) {
    if (result.issues == null || result.issues.isEmpty()) {
      return;
    }

    for (final ReviewResult.Issue issue : result.issues) {
      if (issue.confidenceScore == null) {
        log.warn(
            "Issue '{}' at {}:{} has null confidence score, defaulting to 0.5",
            issue.title,
            issue.file,
            issue.start_line);
        issue.confidenceScore = 0.5;
      }

      if (issue.confidenceScore < 0.0) {
        log.warn(
            "Issue '{}' has invalid confidence score {}, clamping to 0.0",
            issue.title,
            issue.confidenceScore);
        issue.confidenceScore = 0.0;
      } else if (issue.confidenceScore > 1.0) {
        log.warn(
            "Issue '{}' has invalid confidence score {}, clamping to 1.0",
            issue.title,
            issue.confidenceScore);
        issue.confidenceScore = 1.0;
      }

      if (issue.confidenceExplanation == null || issue.confidenceExplanation.isBlank()) {
        log.debug("Issue '{}' has missing confidence explanation, using default", issue.title);
        issue.confidenceExplanation = "No explanation provided";
      }
    }
  }

  private void validateJsonStructure(final String json) {
    if (!json.contains("{") || !json.contains("}")) {
      throw new JsonValidationException("Invalid JSON structure: missing opening or closing brace");
    }

    if (!json.trim().startsWith("{")) {
      log.warn("JSON does not start with opening brace after cleaning");
    }

    if (!json.trim().endsWith("}")) {
      log.warn("JSON does not end with closing brace after cleaning");
    }
  }

  private String cleanJsonResponse(final String jsonResponse) {
    String cleaned = jsonResponse.trim();

    cleaned = stripMarkdownCodeBlocks(cleaned);
    cleaned = extractJsonObject(cleaned);
    cleaned = JsonSanitizer.sanitize(cleaned);
    cleaned = removeSchemaProperty(cleaned);

    log.debug(
        "Cleaned JSON response: {} -> {} characters", jsonResponse.length(), cleaned.length());

    return cleaned;
  }

  private String stripMarkdownCodeBlocks(final String content) {
    String result = content;

    if (result.startsWith("```json")) {
      result = result.substring("```json".length());
    } else if (result.startsWith("```")) {
      result = result.substring("```".length());
    }

    if (result.endsWith("```")) {
      result = result.substring(0, result.length() - "```".length());
    }

    return result.trim();
  }

  private String extractJsonObject(final String content) {
    final String trimmed = content.trim();

    if (trimmed.startsWith("{")) {
      return trimmed;
    }

    final int jsonStart = trimmed.indexOf('{');
    if (jsonStart < 0) {
      return trimmed;
    }

    final int jsonEnd = trimmed.lastIndexOf('}');
    if (jsonEnd < 0 || jsonEnd < jsonStart) {
      return trimmed.substring(jsonStart);
    }

    log.debug("Extracted JSON object from position {} to {}", jsonStart, jsonEnd);
    return trimmed.substring(jsonStart, jsonEnd + 1);
  }

  private String removeSchemaProperty(final String json) {
    try {
      final var jsonNode = objectMapper.readTree(json);
      if (jsonNode.has("$schema")) {
        final var objectNode = (ObjectNode) jsonNode;
        objectNode.remove("$schema");
        final String result = objectMapper.writeValueAsString(objectNode);
        log.debug("Removed $schema property from JSON response");
        return result;
      }
      return json;
    } catch (final Exception e) {
      log.warn("Failed to remove $schema property, returning original JSON", e);
      return json;
    }
  }
}
