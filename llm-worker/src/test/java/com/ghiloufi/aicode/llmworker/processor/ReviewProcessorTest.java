package com.ghiloufi.aicode.llmworker.processor;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.ReviewResult;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ReviewProcessor Tests (Legacy Mode)")
@SuppressWarnings("deprecation")
final class ReviewProcessorTest {

  @Nested
  @DisplayName("Domain Mapping")
  final class DomainMapping {

    @Test
    @DisplayName("should_map_schema_to_domain_with_issues")
    final void should_map_schema_to_domain_with_issues() {
      final AtomicReference<ReviewResult> capturedResult = new AtomicReference<>();
      final TestReviewService reviewService = new TestReviewService();
      final TestPublisher publisher = new TestPublisher(capturedResult);
      final TestProviderProperties properties = new TestProviderProperties("openai", "gpt-4o");

      reviewService.setResult(
          new ReviewResultSchema(
              "Code review summary",
              List.of(
                  new IssueSchema(
                      "src/Main.java",
                      10,
                      Severity.major,
                      "Null check",
                      "Add null check",
                      0.9,
                      "High confidence")),
              List.of()));

      final ReviewProcessor processor =
          new ReviewProcessor(reviewService, null, publisher, properties);

      processor.process("req-123", "{}", "Review this code");

      assertThat(capturedResult.get()).isNotNull();
      assertThat(capturedResult.get().getSummary()).isEqualTo("Code review summary");
      assertThat(capturedResult.get().getIssues()).hasSize(1);
      assertThat(capturedResult.get().getIssues().get(0).getFile()).isEqualTo("src/Main.java");
      assertThat(capturedResult.get().getIssues().get(0).getStartLine()).isEqualTo(10);
      assertThat(capturedResult.get().getIssues().get(0).getSeverity()).isEqualTo("major");
    }

    @Test
    @DisplayName("should_map_schema_to_domain_with_notes")
    final void should_map_schema_to_domain_with_notes() {
      final AtomicReference<ReviewResult> capturedResult = new AtomicReference<>();
      final TestReviewService reviewService = new TestReviewService();
      final TestPublisher publisher = new TestPublisher(capturedResult);
      final TestProviderProperties properties = new TestProviderProperties("openai", "gpt-4o");

      reviewService.setResult(
          new ReviewResultSchema(
              "Summary",
              List.of(),
              List.of(new NoteSchema("src/Utils.java", 25, "Consider using Optional"))));

      final ReviewProcessor processor =
          new ReviewProcessor(reviewService, null, publisher, properties);

      processor.process("req-456", "{}", "Review");

      assertThat(capturedResult.get()).isNotNull();
      assertThat(capturedResult.get().getNonBlockingNotes()).hasSize(1);
      assertThat(capturedResult.get().getNonBlockingNotes().get(0).getFile())
          .isEqualTo("src/Utils.java");
      assertThat(capturedResult.get().getNonBlockingNotes().get(0).getLine()).isEqualTo(25);
    }

    @Test
    @DisplayName("should_handle_null_issues_list")
    final void should_handle_null_issues_list() {
      final AtomicReference<ReviewResult> capturedResult = new AtomicReference<>();
      final TestReviewService reviewService = new TestReviewService();
      final TestPublisher publisher = new TestPublisher(capturedResult);
      final TestProviderProperties properties = new TestProviderProperties("openai", "gpt-4o");

      reviewService.setResult(new ReviewResultSchema("Clean code", null, null));

      final ReviewProcessor processor =
          new ReviewProcessor(reviewService, null, publisher, properties);

      processor.process("req-789", "{}", "Review");

      assertThat(capturedResult.get()).isNotNull();
      assertThat(capturedResult.get().getIssues()).isEmpty();
      assertThat(capturedResult.get().getNonBlockingNotes()).isEmpty();
    }

    @Test
    @DisplayName("should_map_all_severity_levels_correctly")
    final void should_map_all_severity_levels_correctly() {
      final AtomicReference<ReviewResult> capturedResult = new AtomicReference<>();
      final TestReviewService reviewService = new TestReviewService();
      final TestPublisher publisher = new TestPublisher(capturedResult);
      final TestProviderProperties properties = new TestProviderProperties("openai", "gpt-4o");

      reviewService.setResult(
          new ReviewResultSchema(
              "Multi-severity",
              List.of(
                  new IssueSchema("a.java", 1, Severity.critical, "Critical", "Fix", null, null),
                  new IssueSchema("b.java", 2, Severity.major, "Major", "Fix", null, null),
                  new IssueSchema("c.java", 3, Severity.minor, "Minor", "Fix", null, null),
                  new IssueSchema("d.java", 4, Severity.info, "Info", "Consider", null, null)),
              List.of()));

      final ReviewProcessor processor =
          new ReviewProcessor(reviewService, null, publisher, properties);

      processor.process("req-sev", "{}", "Review");

      assertThat(capturedResult.get().getIssues()).hasSize(4);
      assertThat(capturedResult.get().getIssues().get(0).getSeverity()).isEqualTo("critical");
      assertThat(capturedResult.get().getIssues().get(1).getSeverity()).isEqualTo("major");
      assertThat(capturedResult.get().getIssues().get(2).getSeverity()).isEqualTo("minor");
      assertThat(capturedResult.get().getIssues().get(3).getSeverity()).isEqualTo("info");
    }

    @Test
    @DisplayName("should_handle_null_severity_as_info")
    final void should_handle_null_severity_as_info() {
      final AtomicReference<ReviewResult> capturedResult = new AtomicReference<>();
      final TestReviewService reviewService = new TestReviewService();
      final TestPublisher publisher = new TestPublisher(capturedResult);
      final TestProviderProperties properties = new TestProviderProperties("openai", "gpt-4o");

      reviewService.setResult(
          new ReviewResultSchema(
              "Null severity",
              List.of(new IssueSchema("a.java", 1, null, "Issue", "Fix", null, null)),
              List.of()));

      final ReviewProcessor processor =
          new ReviewProcessor(reviewService, null, publisher, properties);

      processor.process("req-null-sev", "{}", "Review");

      assertThat(capturedResult.get().getIssues().get(0).getSeverity()).isEqualTo("info");
    }

    @Test
    @DisplayName("should_filter_issues_with_zero_start_line")
    final void should_filter_issues_with_zero_start_line() {
      final AtomicReference<ReviewResult> capturedResult = new AtomicReference<>();
      final TestReviewService reviewService = new TestReviewService();
      final TestPublisher publisher = new TestPublisher(capturedResult);
      final TestProviderProperties properties = new TestProviderProperties("openai", "gpt-4o");

      reviewService.setResult(
          new ReviewResultSchema(
              "Mixed issues",
              List.of(
                  new IssueSchema("valid.java", 10, Severity.major, "Valid", "Fix", 0.9, "High"),
                  new IssueSchema("invalid.java", 0, Severity.minor, "Invalid", "Skip", 0.5, "Low"),
                  new IssueSchema("another.java", 20, Severity.info, "Valid", "Fix", 0.8, "Good")),
              List.of()));

      final ReviewProcessor processor =
          new ReviewProcessor(reviewService, null, publisher, properties);

      processor.process("req-filter-issue", "{}", "Review");

      assertThat(capturedResult.get().getIssues()).hasSize(2);
      assertThat(capturedResult.get().getIssues().get(0).getFile()).isEqualTo("valid.java");
      assertThat(capturedResult.get().getIssues().get(1).getFile()).isEqualTo("another.java");
    }

    @Test
    @DisplayName("should_filter_issues_with_negative_start_line")
    final void should_filter_issues_with_negative_start_line() {
      final AtomicReference<ReviewResult> capturedResult = new AtomicReference<>();
      final TestReviewService reviewService = new TestReviewService();
      final TestPublisher publisher = new TestPublisher(capturedResult);
      final TestProviderProperties properties = new TestProviderProperties("openai", "gpt-4o");

      reviewService.setResult(
          new ReviewResultSchema(
              "Negative line",
              List.of(
                  new IssueSchema("valid.java", 5, Severity.major, "Valid", "Fix", null, null),
                  new IssueSchema("bad.java", -1, Severity.major, "Bad", "Skip", null, null)),
              List.of()));

      final ReviewProcessor processor =
          new ReviewProcessor(reviewService, null, publisher, properties);

      processor.process("req-filter-neg", "{}", "Review");

      assertThat(capturedResult.get().getIssues()).hasSize(1);
      assertThat(capturedResult.get().getIssues().get(0).getFile()).isEqualTo("valid.java");
    }

    @Test
    @DisplayName("should_filter_notes_with_zero_line")
    final void should_filter_notes_with_zero_line() {
      final AtomicReference<ReviewResult> capturedResult = new AtomicReference<>();
      final TestReviewService reviewService = new TestReviewService();
      final TestPublisher publisher = new TestPublisher(capturedResult);
      final TestProviderProperties properties = new TestProviderProperties("openai", "gpt-4o");

      reviewService.setResult(
          new ReviewResultSchema(
              "Mixed notes",
              List.of(),
              List.of(
                  new NoteSchema("valid.java", 15, "Valid note"),
                  new NoteSchema("invalid.java", 0, "Invalid note"),
                  new NoteSchema("another.java", 30, "Another valid"))));

      final ReviewProcessor processor =
          new ReviewProcessor(reviewService, null, publisher, properties);

      processor.process("req-filter-note", "{}", "Review");

      assertThat(capturedResult.get().getNonBlockingNotes()).hasSize(2);
      assertThat(capturedResult.get().getNonBlockingNotes().get(0).getFile())
          .isEqualTo("valid.java");
      assertThat(capturedResult.get().getNonBlockingNotes().get(1).getFile())
          .isEqualTo("another.java");
    }

    @Test
    @DisplayName("should_filter_notes_with_negative_line")
    final void should_filter_notes_with_negative_line() {
      final AtomicReference<ReviewResult> capturedResult = new AtomicReference<>();
      final TestReviewService reviewService = new TestReviewService();
      final TestPublisher publisher = new TestPublisher(capturedResult);
      final TestProviderProperties properties = new TestProviderProperties("openai", "gpt-4o");

      reviewService.setResult(
          new ReviewResultSchema(
              "Negative note line",
              List.of(),
              List.of(
                  new NoteSchema("valid.java", 10, "Valid"),
                  new NoteSchema("bad.java", -5, "Bad note"))));

      final ReviewProcessor processor =
          new ReviewProcessor(reviewService, null, publisher, properties);

      processor.process("req-filter-neg-note", "{}", "Review");

      assertThat(capturedResult.get().getNonBlockingNotes()).hasSize(1);
      assertThat(capturedResult.get().getNonBlockingNotes().get(0).getFile())
          .isEqualTo("valid.java");
    }
  }

  @Nested
  @DisplayName("Provider Configuration")
  final class ProviderConfiguration {

    @Test
    @DisplayName("should_use_openai_provider_model")
    final void should_use_openai_provider_model() {
      final AtomicReference<ReviewResult> capturedResult = new AtomicReference<>();
      final AtomicReference<String> capturedProvider = new AtomicReference<>();
      final AtomicReference<String> capturedModel = new AtomicReference<>();
      final TestReviewService reviewService = new TestReviewService();
      final TestPublisher publisher =
          new TestPublisher(capturedResult, capturedProvider, capturedModel);
      final TestProviderProperties properties = new TestProviderProperties("openai", "gpt-4o-mini");

      reviewService.setResult(new ReviewResultSchema("Summary", List.of(), List.of()));

      final ReviewProcessor processor =
          new ReviewProcessor(reviewService, null, publisher, properties);

      processor.process("req-provider", "{}", "Review");

      assertThat(capturedProvider.get()).isEqualTo("openai");
      assertThat(capturedModel.get()).isEqualTo("gpt-4o-mini");
    }

    @Test
    @DisplayName("should_use_anthropic_provider_model")
    final void should_use_anthropic_provider_model() {
      final AtomicReference<ReviewResult> capturedResult = new AtomicReference<>();
      final AtomicReference<String> capturedProvider = new AtomicReference<>();
      final AtomicReference<String> capturedModel = new AtomicReference<>();
      final TestReviewService reviewService = new TestReviewService();
      final TestPublisher publisher =
          new TestPublisher(capturedResult, capturedProvider, capturedModel);
      final TestProviderProperties properties =
          new TestProviderProperties("anthropic", "claude-sonnet");

      reviewService.setResult(new ReviewResultSchema("Summary", List.of(), List.of()));

      final ReviewProcessor processor =
          new ReviewProcessor(reviewService, null, publisher, properties);

      processor.process("req-anthropic", "{}", "Review");

      assertThat(capturedProvider.get()).isEqualTo("anthropic");
      assertThat(capturedModel.get()).isEqualTo("claude-sonnet");
    }

    @Test
    @DisplayName("should_use_gemini_provider_model")
    final void should_use_gemini_provider_model() {
      final AtomicReference<ReviewResult> capturedResult = new AtomicReference<>();
      final AtomicReference<String> capturedProvider = new AtomicReference<>();
      final AtomicReference<String> capturedModel = new AtomicReference<>();
      final TestReviewService reviewService = new TestReviewService();
      final TestPublisher publisher =
          new TestPublisher(capturedResult, capturedProvider, capturedModel);
      final TestProviderProperties properties =
          new TestProviderProperties("gemini", "gemini-1.5-pro");

      reviewService.setResult(new ReviewResultSchema("Summary", List.of(), List.of()));

      final ReviewProcessor processor =
          new ReviewProcessor(reviewService, null, publisher, properties);

      processor.process("req-gemini", "{}", "Review");

      assertThat(capturedProvider.get()).isEqualTo("gemini");
      assertThat(capturedModel.get()).isEqualTo("gemini-1.5-pro");
    }

    @Test
    @DisplayName("should_use_ollama_provider_model")
    final void should_use_ollama_provider_model() {
      final AtomicReference<ReviewResult> capturedResult = new AtomicReference<>();
      final AtomicReference<String> capturedProvider = new AtomicReference<>();
      final AtomicReference<String> capturedModel = new AtomicReference<>();
      final TestReviewService reviewService = new TestReviewService();
      final TestPublisher publisher =
          new TestPublisher(capturedResult, capturedProvider, capturedModel);
      final TestProviderProperties properties = new TestProviderProperties("ollama", "codellama");

      reviewService.setResult(new ReviewResultSchema("Summary", List.of(), List.of()));

      final ReviewProcessor processor =
          new ReviewProcessor(reviewService, null, publisher, properties);

      processor.process("req-ollama", "{}", "Review");

      assertThat(capturedProvider.get()).isEqualTo("ollama");
      assertThat(capturedModel.get()).isEqualTo("codellama");
    }

    @Test
    @DisplayName("should_return_unknown_for_unknown_provider")
    final void should_return_unknown_for_unknown_provider() {
      final AtomicReference<ReviewResult> capturedResult = new AtomicReference<>();
      final AtomicReference<String> capturedProvider = new AtomicReference<>();
      final AtomicReference<String> capturedModel = new AtomicReference<>();
      final TestReviewService reviewService = new TestReviewService();
      final TestPublisher publisher =
          new TestPublisher(capturedResult, capturedProvider, capturedModel);
      final TestProviderProperties properties =
          new TestProviderProperties("unknown-provider", "some-model");

      reviewService.setResult(new ReviewResultSchema("Summary", List.of(), List.of()));

      final ReviewProcessor processor =
          new ReviewProcessor(reviewService, null, publisher, properties);

      processor.process("req-unknown", "{}", "Review");

      assertThat(capturedProvider.get()).isEqualTo("unknown-provider");
      assertThat(capturedModel.get()).isEqualTo("unknown");
    }
  }

  @Nested
  @DisplayName("Error Handling")
  final class ErrorHandling {

    @Test
    @DisplayName("should_publish_error_when_review_fails")
    final void should_publish_error_when_review_fails() {
      final AtomicReference<String> capturedErrorRequestId = new AtomicReference<>();
      final AtomicReference<String> capturedErrorMessage = new AtomicReference<>();
      final TestReviewService reviewService = new TestReviewService();
      reviewService.setShouldThrow(true);
      final TestPublisher publisher =
          new TestPublisher(capturedErrorRequestId, capturedErrorMessage);
      final TestProviderProperties properties = new TestProviderProperties("openai", "gpt-4o");

      final ReviewProcessor processor =
          new ReviewProcessor(reviewService, null, publisher, properties);

      processor.process("req-error", "{}", "Review");

      assertThat(capturedErrorRequestId.get()).isEqualTo("req-error");
      assertThat(capturedErrorMessage.get()).isNotNull();
    }
  }

  private static final class TestReviewService implements ReviewService {
    private ReviewResultSchema result;
    private boolean shouldThrow;

    void setResult(final ReviewResultSchema result) {
      this.result = result;
    }

    void setShouldThrow(final boolean shouldThrow) {
      this.shouldThrow = shouldThrow;
    }

    @Override
    public ReviewResultSchema performReview(final String userPrompt) {
      if (shouldThrow) {
        throw new RuntimeException("LLM call failed");
      }
      return result;
    }

    @Override
    public ReviewResultSchema performReview(final String systemPrompt, final String userPrompt) {
      if (shouldThrow) {
        throw new RuntimeException("LLM call failed");
      }
      return result;
    }
  }

  private static final class TestPublisher extends ReviewResultPublisher {
    private final AtomicReference<ReviewResult> capturedResult;
    private final AtomicReference<String> capturedProvider;
    private final AtomicReference<String> capturedModel;
    private final AtomicReference<String> capturedErrorRequestId;
    private final AtomicReference<String> capturedErrorMessage;

    TestPublisher(final AtomicReference<ReviewResult> capturedResult) {
      this(capturedResult, new AtomicReference<>(), new AtomicReference<>());
    }

    TestPublisher(
        final AtomicReference<ReviewResult> capturedResult,
        final AtomicReference<String> capturedProvider,
        final AtomicReference<String> capturedModel) {
      super(null, null);
      this.capturedResult = capturedResult;
      this.capturedProvider = capturedProvider;
      this.capturedModel = capturedModel;
      this.capturedErrorRequestId = new AtomicReference<>();
      this.capturedErrorMessage = new AtomicReference<>();
    }

    TestPublisher(
        final AtomicReference<String> capturedErrorRequestId,
        final AtomicReference<String> capturedErrorMessage) {
      super(null, null);
      this.capturedResult = new AtomicReference<>();
      this.capturedProvider = new AtomicReference<>();
      this.capturedModel = new AtomicReference<>();
      this.capturedErrorRequestId = capturedErrorRequestId;
      this.capturedErrorMessage = capturedErrorMessage;
    }

    @Override
    public void publish(
        final String requestId,
        final String requestPayload,
        final ReviewResult result,
        final String llmProvider,
        final String llmModel,
        final long processingTimeMs) {
      capturedResult.set(result);
      capturedProvider.set(llmProvider);
      capturedModel.set(llmModel);
    }

    @Override
    public void publishError(final String requestId, final String errorMessage) {
      capturedErrorRequestId.set(requestId);
      capturedErrorMessage.set(errorMessage);
    }
  }

  private static final class TestProviderProperties extends ProviderProperties {
    private final String testProvider;
    private final String testModel;

    TestProviderProperties(final String provider, final String model) {
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
