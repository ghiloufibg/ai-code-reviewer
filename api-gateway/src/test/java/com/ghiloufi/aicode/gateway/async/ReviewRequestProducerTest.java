package com.ghiloufi.aicode.gateway.async;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import com.ghiloufi.aicode.core.domain.model.async.AsyncReviewRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.RecordId;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

final class ReviewRequestProducerTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  final void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  @Nested
  @DisplayName("send")
  final class SendTests {

    @Test
    @DisplayName("should_serialize_and_send_github_request_successfully")
    final void should_serialize_and_send_github_request_successfully() {
      final TestReviewRequestProducer producer =
          new TestReviewRequestProducer(true, null, objectMapper);
      final AsyncReviewRequest request =
          AsyncReviewRequest.create("req-123", SourceProvider.GITHUB, "owner/repo", 42);

      StepVerifier.create(producer.send(request))
          .assertNext(
              recordId -> {
                assertThat(recordId).isNotNull();
                assertThat(recordId.getValue()).isEqualTo("1234567890-0");
              })
          .verifyComplete();

      assertThat(producer.getCapturedRequest()).isNotNull();
      assertThat(producer.getCapturedRequest().requestId()).isEqualTo("req-123");
      assertThat(producer.getCapturedRequest().provider()).isEqualTo(SourceProvider.GITHUB);
      assertThat(producer.getCapturedRequest().repositoryId()).isEqualTo("owner/repo");
      assertThat(producer.getCapturedRequest().changeRequestId()).isEqualTo(42);
    }

    @Test
    @DisplayName("should_serialize_and_send_gitlab_request_successfully")
    final void should_serialize_and_send_gitlab_request_successfully() {
      final TestReviewRequestProducer producer =
          new TestReviewRequestProducer(true, null, objectMapper);
      final AsyncReviewRequest request =
          AsyncReviewRequest.create("req-456", SourceProvider.GITLAB, "group/project", 99);

      StepVerifier.create(producer.send(request))
          .assertNext(
              recordId -> {
                assertThat(recordId).isNotNull();
              })
          .verifyComplete();

      assertThat(producer.getCapturedRequest()).isNotNull();
      assertThat(producer.getCapturedRequest().provider()).isEqualTo(SourceProvider.GITLAB);
      assertThat(producer.getCapturedRequest().repositoryId()).isEqualTo("group/project");
    }

    @Test
    @DisplayName("should_propagate_error_when_redis_fails")
    final void should_propagate_error_when_redis_fails() {
      final TestReviewRequestProducer producer =
          new TestReviewRequestProducer(
              false, new RuntimeException("Redis unavailable"), objectMapper);
      final AsyncReviewRequest request =
          AsyncReviewRequest.create("req-error", SourceProvider.GITHUB, "test/repo", 1);

      StepVerifier.create(producer.send(request))
          .expectErrorMatches(
              error ->
                  error instanceof RuntimeException
                      && error.getMessage().equals("Redis unavailable"))
          .verify();
    }

    @Test
    @DisplayName("should_include_request_id_in_stream_record")
    final void should_include_request_id_in_stream_record() {
      final TestReviewRequestProducer producer =
          new TestReviewRequestProducer(true, null, objectMapper);
      final String expectedRequestId = "unique-request-id-789";
      final AsyncReviewRequest request =
          AsyncReviewRequest.create(expectedRequestId, SourceProvider.GITHUB, "owner/repo", 1);

      StepVerifier.create(producer.send(request)).expectNextCount(1).verifyComplete();

      assertThat(producer.getCapturedRequest().requestId()).isEqualTo(expectedRequestId);
    }

    @Test
    @DisplayName("should_handle_special_characters_in_repository_id")
    final void should_handle_special_characters_in_repository_id() {
      final TestReviewRequestProducer producer =
          new TestReviewRequestProducer(true, null, objectMapper);
      final AsyncReviewRequest request =
          AsyncReviewRequest.create(
              "req-special", SourceProvider.GITLAB, "org/sub-group/project-name", 5);

      StepVerifier.create(producer.send(request)).expectNextCount(1).verifyComplete();

      assertThat(producer.getCapturedRequest().repositoryId())
          .isEqualTo("org/sub-group/project-name");
    }
  }

  @Nested
  @DisplayName("serialization")
  final class SerializationTests {

    @Test
    @DisplayName("should_serialize_request_to_valid_json")
    final void should_serialize_request_to_valid_json() throws Exception {
      final AsyncReviewRequest request =
          AsyncReviewRequest.create("req-json", SourceProvider.GITHUB, "owner/repo", 100);

      final String json = objectMapper.writeValueAsString(request);

      assertThat(json).contains("\"requestId\":\"req-json\"");
      assertThat(json).contains("\"provider\":\"GITHUB\"");
      assertThat(json).contains("\"repositoryId\":\"owner/repo\"");
      assertThat(json).contains("\"changeRequestId\":100");
    }

    @Test
    @DisplayName("should_deserialize_request_from_json")
    final void should_deserialize_request_from_json() throws Exception {
      final String json =
          "{\"requestId\":\"req-deserialize\",\"provider\":\"GITLAB\",\"repositoryId\":\"group/project\",\"changeRequestId\":50}";

      final AsyncReviewRequest request = objectMapper.readValue(json, AsyncReviewRequest.class);

      assertThat(request.requestId()).isEqualTo("req-deserialize");
      assertThat(request.provider()).isEqualTo(SourceProvider.GITLAB);
      assertThat(request.repositoryId()).isEqualTo("group/project");
      assertThat(request.changeRequestId()).isEqualTo(50);
    }
  }

  private static final class TestReviewRequestProducer extends ReviewRequestProducer {
    private AsyncReviewRequest capturedRequest;
    private final boolean success;
    private final RuntimeException error;
    private final ObjectMapper mapper;

    TestReviewRequestProducer(
        final boolean success, final RuntimeException error, final ObjectMapper mapper) {
      super(null, mapper);
      this.success = success;
      this.error = error;
      this.mapper = mapper;
    }

    @Override
    public Mono<RecordId> send(final AsyncReviewRequest request) {
      this.capturedRequest = request;
      if (success) {
        return Mono.just(RecordId.of("1234567890-0"));
      }
      return Mono.error(error);
    }

    AsyncReviewRequest getCapturedRequest() {
      return capturedRequest;
    }
  }
}
