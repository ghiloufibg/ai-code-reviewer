package com.ghiloufi.aicode.llmworker.processor;

import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.model.async.AsyncReviewRequest;
import com.ghiloufi.aicode.llmworker.config.ProviderProperties;
import com.ghiloufi.aicode.llmworker.publisher.ReviewResultPublisher;
import com.ghiloufi.aicode.llmworker.schema.IssueSchema;
import com.ghiloufi.aicode.llmworker.schema.NoteSchema;
import com.ghiloufi.aicode.llmworker.schema.ReviewResultSchema;
import com.ghiloufi.aicode.llmworker.service.AsyncReviewOrchestrator;
import com.ghiloufi.aicode.llmworker.service.AsyncReviewResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewProcessor {

  private final ReviewService reviewService;
  private final AsyncReviewOrchestrator asyncReviewOrchestrator;
  private final ReviewResultPublisher resultPublisher;
  private final ProviderProperties providerProperties;

  public void process(final String requestId, final AsyncReviewRequest request) {
    log.info(
        "Processing async review request: {} for {} PR #{}",
        requestId,
        request.provider(),
        request.changeRequestId());

    final long startTime = System.currentTimeMillis();

    try {
      final AsyncReviewResult asyncResult = asyncReviewOrchestrator.performAsyncReview(request);
      final ReviewResult result =
          mapToDomain(asyncResult.schema()).withFilesAnalyzed(asyncResult.filesAnalyzed());
      final long processingTime = System.currentTimeMillis() - startTime;

      resultPublisher.publish(
          requestId, request, result, getLlmProvider(), getLlmModel(), processingTime);

      log.info("Async review completed: {} in {}ms", requestId, processingTime);

    } catch (final Exception e) {
      log.error("Async review failed: {}", requestId, e);
      resultPublisher.publishError(requestId, e.getMessage());
    }
  }

  @Deprecated
  public void process(
      final String requestId, final String requestPayload, final String userPrompt) {
    log.info("Processing review request (legacy): {}", requestId);

    final long startTime = System.currentTimeMillis();

    try {
      final ReviewResultSchema schema = reviewService.performReview(userPrompt);

      final ReviewResult result = mapToDomain(schema);

      final long processingTime = System.currentTimeMillis() - startTime;

      resultPublisher.publish(
          requestId, requestPayload, result, getLlmProvider(), getLlmModel(), processingTime);

      log.info("Review completed: {} in {}ms", requestId, processingTime);

    } catch (final Exception e) {
      log.error("Review failed: {}", requestId, e);
      resultPublisher.publishError(requestId, e.getMessage());
    }
  }

  private ReviewResult mapToDomain(final ReviewResultSchema schema) {
    final List<ReviewResult.Issue> issues =
        schema.issues() != null
            ? schema.issues().stream().filter(this::isValidIssue).map(this::mapIssue).toList()
            : List.of();

    final List<ReviewResult.Note> notes =
        schema.nonBlockingNotes() != null
            ? schema.nonBlockingNotes().stream()
                .filter(this::isValidNote)
                .map(this::mapNote)
                .toList()
            : List.of();

    return ReviewResult.builder()
        .summary(schema.summary())
        .issues(issues)
        .nonBlockingNotes(notes)
        .llmProvider(getLlmProvider())
        .llmModel(getLlmModel())
        .build();
  }

  private boolean isValidIssue(final IssueSchema issueSchema) {
    if (issueSchema.startLine() <= 0) {
      log.warn(
          "LLM returned invalid startLine={} for issue in file '{}': {}. "
              + "Issue will be skipped. Consider improving prompt constraints.",
          issueSchema.startLine(),
          issueSchema.file(),
          issueSchema.title());
      return false;
    }
    return true;
  }

  private boolean isValidNote(final NoteSchema noteSchema) {
    if (noteSchema.line() <= 0) {
      log.warn(
          "LLM returned invalid line={} for note in file '{}': {}. "
              + "Note will be skipped. Consider improving prompt constraints.",
          noteSchema.line(),
          noteSchema.file(),
          noteSchema.note());
      return false;
    }
    return true;
  }

  private ReviewResult.Issue mapIssue(final IssueSchema issueSchema) {
    return ReviewResult.Issue.issueBuilder()
        .file(issueSchema.file())
        .startLine(issueSchema.startLine())
        .severity(issueSchema.severity() != null ? issueSchema.severity().name() : "info")
        .title(issueSchema.title())
        .suggestion(issueSchema.suggestion())
        .confidenceScore(issueSchema.confidenceScore())
        .confidenceExplanation(issueSchema.confidenceExplanation())
        .build();
  }

  private ReviewResult.Note mapNote(final NoteSchema noteSchema) {
    return ReviewResult.Note.noteBuilder()
        .file(noteSchema.file())
        .line(noteSchema.line())
        .note(noteSchema.note())
        .build();
  }

  private String getLlmProvider() {
    return providerProperties.getProvider();
  }

  private String getLlmModel() {
    return switch (providerProperties.getProvider()) {
      case "openai" -> providerProperties.getOpenai().getModel();
      case "anthropic" -> providerProperties.getAnthropic().getModel();
      case "gemini" -> providerProperties.getGemini().getModel();
      case "ollama" -> providerProperties.getOllama().getModel();
      default -> "unknown";
    };
  }
}
