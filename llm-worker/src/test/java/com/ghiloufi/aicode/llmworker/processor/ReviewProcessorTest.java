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

@DisplayName("ReviewProcessor Tests")
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
                      "High confidence",
                      null)),
              List.of()));

      final ReviewProcessor processor = new ReviewProcessor(reviewService, publisher, properties);

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

      final ReviewProcessor processor = new ReviewProcessor(reviewService, publisher, properties);

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

      final ReviewProcessor processor = new ReviewProcessor(reviewService, publisher, properties);

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
                  new IssueSchema(
                      "a.java", 1, Severity.critical, "Critical", "Fix", null, null, null),
                  new IssueSchema("b.java", 2, Severity.major, "Major", "Fix", null, null, null),
                  new IssueSchema("c.java", 3, Severity.minor, "Minor", "Fix", null, null, null),
                  new IssueSchema(
                      "d.java", 4, Severity.info, "Info", "Consider", null, null, null)),
              List.of()));

      final ReviewProcessor processor = new ReviewProcessor(reviewService, publisher, properties);

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
              List.of(new IssueSchema("a.java", 1, null, "Issue", "Fix", null, null, null)),
              List.of()));

      final ReviewProcessor processor = new ReviewProcessor(reviewService, publisher, properties);

      processor.process("req-null-sev", "{}", "Review");

      assertThat(capturedResult.get().getIssues().get(0).getSeverity()).isEqualTo("info");
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

      final ReviewProcessor processor = new ReviewProcessor(reviewService, publisher, properties);

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

      final ReviewProcessor processor = new ReviewProcessor(reviewService, publisher, properties);

      processor.process("req-anthropic", "{}", "Review");

      assertThat(capturedProvider.get()).isEqualTo("anthropic");
      assertThat(capturedModel.get()).isEqualTo("claude-sonnet");
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

      final ReviewProcessor processor = new ReviewProcessor(reviewService, publisher, properties);

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
