package com.ghiloufi.aicode.core.service.accumulator;

import com.ghiloufi.aicode.core.domain.model.ReviewChunk;
import com.ghiloufi.aicode.core.domain.model.ReviewConfiguration;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.service.ReviewSummaryFormatter;
import com.ghiloufi.aicode.core.service.filter.ConfidenceFilter;
import com.ghiloufi.aicode.core.service.prompt.JsonReviewResultParser;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ReviewChunkAccumulator {

  private final ReviewSummaryFormatter summaryFormatter;
  private final JsonReviewResultParser jsonParser;
  private final ConfidenceFilter confidenceFilter;

  public ReviewChunkAccumulator(
      final ReviewSummaryFormatter summaryFormatter,
      final JsonReviewResultParser jsonParser,
      final ConfidenceFilter confidenceFilter) {
    this.summaryFormatter = summaryFormatter;
    this.jsonParser = jsonParser;
    this.confidenceFilter = confidenceFilter;
  }

  public ReviewResult accumulateChunks(final List<ReviewChunk> chunks) {
    return accumulateChunks(chunks, ReviewConfiguration.defaults());
  }

  public ReviewResult accumulateChunks(
      final List<ReviewChunk> chunks, final ReviewConfiguration config) {
    final List<ReviewChunk> validChunks =
        Optional.ofNullable(chunks)
            .orElseThrow(() -> new IllegalArgumentException("Chunks list cannot be null"));

    log.debug("Accumulating {} review chunks into ReviewResult", validChunks.size());

    final String accumulatedContent = concatenateChunkContents(validChunks);

    if (!looksLikeJson(accumulatedContent)) {
      throw new IllegalArgumentException(
          "Expected JSON response from LLM, but received non-JSON content. "
              + "Ensure the LLM is configured to return structured JSON format.");
    }

    log.info("Detected JSON response, parsing as ReviewResult");
    final ReviewResult parsedResult =
        jsonParser.parse(accumulatedContent).withRawLlmResponse(accumulatedContent);
    log.info(
        "Successfully parsed JSON ReviewResult: {} issues, {} notes",
        parsedResult.getIssues().size(),
        parsedResult.getNonBlockingNotes().size());

    final ReviewResult filteredResult =
        confidenceFilter.filterByConfidence(parsedResult, config.minimumConfidenceThreshold());

    logAccumulatedReviewSummary(filteredResult);

    return filteredResult;
  }

  private String concatenateChunkContents(final List<ReviewChunk> chunks) {
    final StringBuilder builder = new StringBuilder();
    for (final ReviewChunk chunk : chunks) {
      builder.append(chunk.content());
    }
    return builder.toString();
  }

  private boolean looksLikeJson(final String content) {
    final String trimmed = content.trim();

    if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
      return true;
    }

    if (trimmed.startsWith("```json")) {
      return trimmed.contains("{") && trimmed.contains("}");
    }

    if (trimmed.startsWith("```")) {
      return trimmed.contains("{") && trimmed.contains("}");
    }

    if (trimmed.startsWith("[")) {
      return false;
    }

    final int openBraceIndex = trimmed.indexOf('{');
    final int closeBraceIndex = trimmed.lastIndexOf('}');

    return openBraceIndex >= 0 && closeBraceIndex > openBraceIndex;
  }

  private void logAccumulatedReviewSummary(final ReviewResult result) {
    log.info(summaryFormatter.formatSummary(result));
  }
}
