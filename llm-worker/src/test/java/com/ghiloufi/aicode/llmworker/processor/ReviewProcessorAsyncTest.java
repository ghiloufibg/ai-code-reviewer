package com.ghiloufi.aicode.llmworker.processor;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import com.ghiloufi.aicode.core.domain.model.async.AsyncReviewRequest;
import com.ghiloufi.aicode.llmworker.config.ProviderProperties;
import com.ghiloufi.aicode.llmworker.config.ProviderProperties.AnthropicProperties;
import com.ghiloufi.aicode.llmworker.config.ProviderProperties.GeminiProperties;
import com.ghiloufi.aicode.llmworker.config.ProviderProperties.OllamaProperties;
import com.ghiloufi.aicode.llmworker.config.ProviderProperties.OpenAiProperties;
import com.ghiloufi.aicode.llmworker.publisher.ReviewResultPublisher;
import com.ghiloufi.aicode.llmworker.schema.IssueSchema;
import com.ghiloufi.aicode.llmworker.schema.NoteSchema;
import com.ghiloufi.aicode.llmworker.schema.ReviewResultSchema;
import com.ghiloufi.aicode.llmworker.schema.Severity;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ReviewProcessor Async Tests")
final class ReviewProcessorAsyncTest {

  @Nested
  @DisplayName("Process Async Review Request")
  final class ProcessAsyncReviewRequest {

    @Test
    @DisplayName("should_process_async_request_successfully")
    void should_process_async_request_successfully() {
      final AtomicReference<ReviewResult> capturedResult = new AtomicReference<>();
      final AtomicReference<AsyncReviewRequest> capturedRequest = new AtomicReference<>();

      final ReviewResultSchema schema =
          new ReviewResultSchema(
              "Async review summary",
              List.of(
                  new IssueSchema(
                      "Service.java", 15, Severity.major, "Bug", "Fix it", 0.9, "High")),
              List.of());

      final TestReviewService reviewService = new TestReviewService(schema);
      final TestAsyncPublisher publisher = new TestAsyncPublisher(capturedResult, capturedRequest);
      final TestProviderProperties properties = new TestProviderProperties("openai", "gpt-4o");

      final ReviewProcessor processor =
          new ReviewProcessor(reviewService, null, publisher, properties);

      final AsyncReviewRequest request =
          new AsyncReviewRequest(
              "async-123", SourceProvider.GITHUB, "owner/repo", 42, Instant.now());

      processor.process("async-123", "{}", "Review this code");

      assertThat(capturedResult.get()).isNotNull();
      assertThat(capturedResult.get().getSummary()).isEqualTo("Async review summary");
    }

    @Test
    @DisplayName("should_map_issues_from_result")
    void should_map_issues_from_result() {
      final AtomicReference<ReviewResult> capturedResult = new AtomicReference<>();

      final ReviewResultSchema schema =
          new ReviewResultSchema(
              "Security issues found",
              List.of(
                  new IssueSchema(
                      "Auth.java",
                      10,
                      Severity.critical,
                      "SQL Injection",
                      "Use prepared",
                      0.95,
                      "Very high"),
                  new IssueSchema(
                      "User.java", 25, Severity.major, "XSS", "Escape output", 0.85, "High")),
              List.of());

      final TestReviewService reviewService = new TestReviewService(schema);
      final TestAsyncPublisher publisher =
          new TestAsyncPublisher(capturedResult, new AtomicReference<>());
      final TestProviderProperties properties = new TestProviderProperties("anthropic", "claude-3");

      final ReviewProcessor processor =
          new ReviewProcessor(reviewService, null, publisher, properties);

      processor.process("async-sec", "{}", "Check for SQL injection");

      assertThat(capturedResult.get().getIssues()).hasSize(2);
      assertThat(capturedResult.get().getIssues().get(0).getSeverity()).isEqualTo("critical");
      assertThat(capturedResult.get().getIssues().get(1).getSeverity()).isEqualTo("major");
    }

    @Test
    @DisplayName("should_map_notes_from_result")
    void should_map_notes_from_result() {
      final AtomicReference<ReviewResult> capturedResult = new AtomicReference<>();

      final ReviewResultSchema schema =
          new ReviewResultSchema(
              "Good code with suggestions",
              List.of(),
              List.of(
                  new NoteSchema("Utils.java", 50, "Consider using Stream API"),
                  new NoteSchema("Config.java", 12, "Add validation")));

      final TestReviewService reviewService = new TestReviewService(schema);
      final TestAsyncPublisher publisher =
          new TestAsyncPublisher(capturedResult, new AtomicReference<>());
      final TestProviderProperties properties = new TestProviderProperties("gemini", "gemini-pro");

      final ReviewProcessor processor =
          new ReviewProcessor(reviewService, null, publisher, properties);

      processor.process("async-notes", "{}", "Review all");

      assertThat(capturedResult.get().getNonBlockingNotes()).hasSize(2);
      assertThat(capturedResult.get().getNonBlockingNotes().get(0).getLine()).isEqualTo(50);
    }

    @Test
    @DisplayName("should_filter_invalid_issues_with_zero_line")
    void should_filter_invalid_issues_with_zero_line() {
      final AtomicReference<ReviewResult> capturedResult = new AtomicReference<>();

      final ReviewResultSchema schema =
          new ReviewResultSchema(
              "Mixed validity",
              List.of(
                  new IssueSchema("Valid.java", 10, Severity.minor, "Valid", "OK", 0.8, "Good"),
                  new IssueSchema("Invalid.java", 0, Severity.major, "Invalid", "Skip", 0.5, "Low"),
                  new IssueSchema(
                      "Another.java", -1, Severity.info, "Negative", "Skip", 0.3, "Low")),
              List.of());

      final TestReviewService reviewService = new TestReviewService(schema);
      final TestAsyncPublisher publisher =
          new TestAsyncPublisher(capturedResult, new AtomicReference<>());
      final TestProviderProperties properties = new TestProviderProperties("openai", "gpt-4o");

      final ReviewProcessor processor =
          new ReviewProcessor(reviewService, null, publisher, properties);

      processor.process("async-filter", "{}", "Review");

      assertThat(capturedResult.get().getIssues()).hasSize(1);
      assertThat(capturedResult.get().getIssues().get(0).getFile()).isEqualTo("Valid.java");
    }

    @Test
    @DisplayName("should_filter_invalid_notes_with_zero_line")
    void should_filter_invalid_notes_with_zero_line() {
      final AtomicReference<ReviewResult> capturedResult = new AtomicReference<>();

      final ReviewResultSchema schema =
          new ReviewResultSchema(
              "Notes with invalid lines",
              List.of(),
              List.of(
                  new NoteSchema("Valid.java", 15, "Valid note"),
                  new NoteSchema("Invalid.java", 0, "Invalid note"),
                  new NoteSchema("Negative.java", -5, "Negative line")));

      final TestReviewService reviewService = new TestReviewService(schema);
      final TestAsyncPublisher publisher =
          new TestAsyncPublisher(capturedResult, new AtomicReference<>());
      final TestProviderProperties properties = new TestProviderProperties("ollama", "llama3");

      final ReviewProcessor processor =
          new ReviewProcessor(reviewService, null, publisher, properties);

      processor.process("async-notes-filter", "{}", "Review");

      assertThat(capturedResult.get().getNonBlockingNotes()).hasSize(1);
      assertThat(capturedResult.get().getNonBlockingNotes().get(0).getFile())
          .isEqualTo("Valid.java");
    }
  }

  @Nested
  @DisplayName("Error Handling")
  final class ErrorHandling {

    @Test
    @DisplayName("should_publish_error_when_review_fails")
    void should_publish_error_when_review_fails() {
      final AtomicReference<String> capturedErrorRequestId = new AtomicReference<>();
      final AtomicReference<String> capturedErrorMessage = new AtomicReference<>();

      final TestReviewService reviewService = new TestReviewService(null);
      reviewService.setShouldThrow(true);
      final TestErrorPublisher publisher =
          new TestErrorPublisher(capturedErrorRequestId, capturedErrorMessage);
      final TestProviderProperties properties = new TestProviderProperties("openai", "gpt-4o");

      final ReviewProcessor processor =
          new ReviewProcessor(reviewService, null, publisher, properties);

      processor.process("async-error", "{}", "Review");

      assertThat(capturedErrorRequestId.get()).isEqualTo("async-error");
      assertThat(capturedErrorMessage.get()).contains("Review failed");
    }
  }

  @Nested
  @DisplayName("Provider Model Selection")
  final class ProviderModelSelection {

    @Test
    @DisplayName("should_use_gemini_model")
    void should_use_gemini_model() {
      final AtomicReference<String> capturedProvider = new AtomicReference<>();
      final AtomicReference<String> capturedModel = new AtomicReference<>();

      final ReviewResultSchema schema = new ReviewResultSchema("Test", List.of(), List.of());
      final TestReviewService reviewService = new TestReviewService(schema);
      final TestProviderModelPublisher publisher =
          new TestProviderModelPublisher(capturedProvider, capturedModel);
      final TestProviderProperties properties =
          new TestProviderProperties("gemini", "gemini-1.5-pro");

      final ReviewProcessor processor =
          new ReviewProcessor(reviewService, null, publisher, properties);

      processor.process("gemini-req", "{}", "Review");

      assertThat(capturedProvider.get()).isEqualTo("gemini");
      assertThat(capturedModel.get()).isEqualTo("gemini-1.5-pro");
    }

    @Test
    @DisplayName("should_use_ollama_model")
    void should_use_ollama_model() {
      final AtomicReference<String> capturedProvider = new AtomicReference<>();
      final AtomicReference<String> capturedModel = new AtomicReference<>();

      final ReviewResultSchema schema = new ReviewResultSchema("Test", List.of(), List.of());
      final TestReviewService reviewService = new TestReviewService(schema);
      final TestProviderModelPublisher publisher =
          new TestProviderModelPublisher(capturedProvider, capturedModel);
      final TestProviderProperties properties = new TestProviderProperties("ollama", "codellama");

      final ReviewProcessor processor =
          new ReviewProcessor(reviewService, null, publisher, properties);

      processor.process("ollama-req", "{}", "Review");

      assertThat(capturedProvider.get()).isEqualTo("ollama");
      assertThat(capturedModel.get()).isEqualTo("codellama");
    }

    @Test
    @DisplayName("should_return_unknown_for_unknown_provider")
    void should_return_unknown_for_unknown_provider() {
      final AtomicReference<String> capturedModel = new AtomicReference<>();

      final ReviewResultSchema schema = new ReviewResultSchema("Test", List.of(), List.of());
      final TestReviewService reviewService = new TestReviewService(schema);
      final TestProviderModelPublisher publisher =
          new TestProviderModelPublisher(new AtomicReference<>(), capturedModel);
      final TestProviderProperties properties = new TestProviderProperties("unknown", "model");

      final ReviewProcessor processor =
          new ReviewProcessor(reviewService, null, publisher, properties);

      processor.process("unknown-req", "{}", "Review");

      assertThat(capturedModel.get()).isEqualTo("unknown");
    }
  }

  private static final class TestReviewService implements ReviewService {
    private final ReviewResultSchema result;
    private boolean shouldThrow;

    TestReviewService(ReviewResultSchema result) {
      this.result = result;
    }

    void setShouldThrow(boolean shouldThrow) {
      this.shouldThrow = shouldThrow;
    }

    @Override
    public ReviewResultSchema performReview(String userPrompt) {
      if (shouldThrow) {
        throw new RuntimeException("Review failed");
      }
      return result;
    }

    @Override
    public ReviewResultSchema performReview(String systemPrompt, String userPrompt) {
      if (shouldThrow) {
        throw new RuntimeException("Review failed");
      }
      return result;
    }
  }

  private static final class TestAsyncPublisher extends ReviewResultPublisher {
    private final AtomicReference<ReviewResult> capturedResult;
    private final AtomicReference<AsyncReviewRequest> capturedRequest;

    TestAsyncPublisher(
        AtomicReference<ReviewResult> capturedResult,
        AtomicReference<AsyncReviewRequest> capturedRequest) {
      super(null, null);
      this.capturedResult = capturedResult;
      this.capturedRequest = capturedRequest;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void publish(
        String requestId,
        String requestPayload,
        ReviewResult result,
        String llmProvider,
        String llmModel,
        long processingTimeMs) {
      capturedResult.set(result);
    }

    @Override
    public void publish(
        String requestId,
        AsyncReviewRequest request,
        ReviewResult result,
        String llmProvider,
        String llmModel,
        long processingTimeMs) {
      capturedResult.set(result);
      capturedRequest.set(request);
    }
  }

  private static final class TestErrorPublisher extends ReviewResultPublisher {
    private final AtomicReference<String> capturedErrorRequestId;
    private final AtomicReference<String> capturedErrorMessage;

    TestErrorPublisher(
        AtomicReference<String> capturedErrorRequestId,
        AtomicReference<String> capturedErrorMessage) {
      super(null, null);
      this.capturedErrorRequestId = capturedErrorRequestId;
      this.capturedErrorMessage = capturedErrorMessage;
    }

    @Override
    public void publishError(String requestId, String errorMessage) {
      capturedErrorRequestId.set(requestId);
      capturedErrorMessage.set(errorMessage);
    }
  }

  private static final class TestProviderModelPublisher extends ReviewResultPublisher {
    private final AtomicReference<String> capturedProvider;
    private final AtomicReference<String> capturedModel;

    TestProviderModelPublisher(
        AtomicReference<String> capturedProvider, AtomicReference<String> capturedModel) {
      super(null, null);
      this.capturedProvider = capturedProvider;
      this.capturedModel = capturedModel;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void publish(
        String requestId,
        String requestPayload,
        ReviewResult result,
        String llmProvider,
        String llmModel,
        long processingTimeMs) {
      capturedProvider.set(llmProvider);
      capturedModel.set(llmModel);
    }
  }

  private static final class TestProviderProperties extends ProviderProperties {
    private final String testProvider;
    private final String testModel;

    TestProviderProperties(String provider, String model) {
      super(
          provider,
          new OpenAiProperties(null, model, null),
          new AnthropicProperties(null, model),
          new GeminiProperties(null, model),
          new OllamaProperties(null, model),
          Duration.ofSeconds(120));
      this.testProvider = provider;
      this.testModel = model;
    }

    @Override
    public String getProvider() {
      return testProvider;
    }

    @Override
    public OpenAiProperties getOpenai() {
      return new OpenAiProperties(null, testModel, null);
    }

    @Override
    public AnthropicProperties getAnthropic() {
      return new AnthropicProperties(null, testModel);
    }

    @Override
    public GeminiProperties getGemini() {
      return new GeminiProperties(null, testModel);
    }

    @Override
    public OllamaProperties getOllama() {
      return new OllamaProperties(null, testModel);
    }
  }
}
