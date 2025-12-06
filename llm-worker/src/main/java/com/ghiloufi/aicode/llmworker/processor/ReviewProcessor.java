package com.ghiloufi.aicode.llmworker.processor;

import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.llmworker.config.ProviderProperties;
import com.ghiloufi.aicode.llmworker.publisher.ReviewResultPublisher;
import com.ghiloufi.aicode.llmworker.schema.IssueSchema;
import com.ghiloufi.aicode.llmworker.schema.NoteSchema;
import com.ghiloufi.aicode.llmworker.schema.ReviewResultSchema;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewProcessor {

  private final StructuredReviewService reviewService;
  private final ReviewResultPublisher resultPublisher;
  private final ProviderProperties providerProperties;

  public void process(String requestId, String userPrompt) {
    log.info("Processing review request: {}", requestId);

    final long startTime = System.currentTimeMillis();

    try {
      final ReviewResultSchema schema = reviewService.performReview(userPrompt);

      final ReviewResult result = mapToDomain(schema);

      final long processingTime = System.currentTimeMillis() - startTime;

      resultPublisher.publish(requestId, result, getLlmProvider(), getLlmModel(), processingTime);

      log.info("Review completed: {} in {}ms", requestId, processingTime);

    } catch (Exception e) {
      log.error("Review failed: {}", requestId, e);
      resultPublisher.publishError(requestId, e.getMessage());
    }
  }

  private ReviewResult mapToDomain(ReviewResultSchema schema) {
    final List<ReviewResult.Issue> issues =
        schema.issues() != null ? schema.issues().stream().map(this::mapIssue).toList() : List.of();

    final List<ReviewResult.Note> notes =
        schema.nonBlockingNotes() != null
            ? schema.nonBlockingNotes().stream().map(this::mapNote).toList()
            : List.of();

    return ReviewResult.builder()
        .summary(schema.summary())
        .issues(issues)
        .nonBlockingNotes(notes)
        .llmProvider(getLlmProvider())
        .llmModel(getLlmModel())
        .build();
  }

  private ReviewResult.Issue mapIssue(IssueSchema issueSchema) {
    return ReviewResult.Issue.issueBuilder()
        .file(issueSchema.file())
        .startLine(issueSchema.startLine())
        .severity(issueSchema.severity() != null ? issueSchema.severity().name() : "info")
        .title(issueSchema.title())
        .suggestion(issueSchema.suggestion())
        .confidenceScore(issueSchema.confidenceScore())
        .confidenceExplanation(issueSchema.confidenceExplanation())
        .suggestedFix(issueSchema.suggestedFix())
        .build();
  }

  private ReviewResult.Note mapNote(NoteSchema noteSchema) {
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
