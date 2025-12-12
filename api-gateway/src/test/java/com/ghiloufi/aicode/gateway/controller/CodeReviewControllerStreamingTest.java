package com.ghiloufi.aicode.gateway.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghiloufi.aicode.core.domain.model.ChangeRequestIdentifier;
import com.ghiloufi.aicode.core.domain.model.MergeRequestSummary;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.model.RepositoryInfo;
import com.ghiloufi.aicode.core.domain.model.ReviewChunk;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import com.ghiloufi.aicode.core.domain.port.input.ReviewManagementUseCase;
import com.ghiloufi.aicode.core.infrastructure.persistence.entity.ReviewIssueEntity;
import com.ghiloufi.aicode.core.infrastructure.persistence.repository.ReviewIssueRepository;
import com.ghiloufi.aicode.gateway.formatter.SSEFormatter;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

final class CodeReviewControllerStreamingTest {

  private WebTestClient webTestClient;
  private TestReviewManagementUseCase reviewManagementUseCase;
  private TestReviewIssueRepository reviewIssueRepository;
  private ObjectMapper objectMapper;

  @BeforeEach
  final void setUp() {
    reviewManagementUseCase = new TestReviewManagementUseCase();
    reviewIssueRepository = new TestReviewIssueRepository();
    objectMapper = new ObjectMapper();
    final SSEFormatter sseFormatter = new SSEFormatter(objectMapper);
    final CodeReviewController controller =
        new CodeReviewController(reviewManagementUseCase, sseFormatter, reviewIssueRepository);

    webTestClient =
        WebTestClient.bindToController(controller)
            .configureClient()
            .responseTimeout(Duration.ofSeconds(10))
            .build();
  }

  @Nested
  @DisplayName("streamReviewAnalysis")
  final class StreamReviewAnalysisTests {

    @Test
    @DisplayName("should_stream_review_chunks_for_gitlab_merge_request")
    final void should_stream_review_chunks_for_gitlab_merge_request() {
      final List<ReviewChunk> chunks =
          List.of(
              ReviewChunk.of(ReviewChunk.ChunkType.ANALYSIS, "Starting analysis..."),
              ReviewChunk.of(ReviewChunk.ChunkType.SECURITY, "Security check passed"),
              ReviewChunk.of(ReviewChunk.ChunkType.SUGGESTION, "Consider refactoring method"));
      reviewManagementUseCase.setReviewChunks(chunks);

      webTestClient
          .get()
          .uri("/api/v1/reviews/gitlab/group%2Fproject/change-requests/42/stream")
          .accept(MediaType.TEXT_EVENT_STREAM)
          .exchange()
          .expectStatus()
          .isOk()
          .expectHeader()
          .contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
          .returnResult(String.class)
          .getResponseBody()
          .take(4)
          .collectList()
          .block();

      assertThat(reviewManagementUseCase.wasStreamReviewCalled()).isTrue();
    }

    @Test
    @DisplayName("should_stream_review_chunks_for_github_pull_request")
    final void should_stream_review_chunks_for_github_pull_request() {
      final List<ReviewChunk> chunks =
          List.of(ReviewChunk.of(ReviewChunk.ChunkType.ANALYSIS, "Analyzing PR..."));
      reviewManagementUseCase.setReviewChunks(chunks);

      webTestClient
          .get()
          .uri("/api/v1/reviews/github/owner%2Frepo/change-requests/123/stream")
          .accept(MediaType.TEXT_EVENT_STREAM)
          .exchange()
          .expectStatus()
          .isOk();

      assertThat(reviewManagementUseCase.wasStreamReviewCalled()).isTrue();
    }

    @Test
    @DisplayName("should_handle_error_during_streaming")
    final void should_handle_error_during_streaming() {
      reviewManagementUseCase.setShouldFailStream(true);

      webTestClient
          .get()
          .uri("/api/v1/reviews/gitlab/test%2Fproject/change-requests/1/stream")
          .accept(MediaType.TEXT_EVENT_STREAM)
          .exchange()
          .expectStatus()
          .isOk();
    }

    @Test
    @DisplayName("should_decode_url_encoded_repository_id")
    final void should_decode_url_encoded_repository_id() {
      reviewManagementUseCase.setReviewChunks(List.of());

      webTestClient
          .get()
          .uri("/api/v1/reviews/gitlab/org%2Fsub-group%2Fproject/change-requests/99/stream")
          .accept(MediaType.TEXT_EVENT_STREAM)
          .exchange()
          .expectStatus()
          .isOk();

      assertThat(reviewManagementUseCase.wasStreamReviewCalled()).isTrue();
    }
  }

  @Nested
  @DisplayName("streamReviewAndPublish")
  final class StreamReviewAndPublishTests {

    @Test
    @DisplayName("should_stream_and_publish_review_for_gitlab")
    final void should_stream_and_publish_review_for_gitlab() {
      final List<ReviewChunk> chunks =
          List.of(
              ReviewChunk.of(ReviewChunk.ChunkType.ANALYSIS, "Analyzing..."),
              ReviewChunk.of(ReviewChunk.ChunkType.COMMENTARY, "Review complete"));
      reviewManagementUseCase.setReviewChunks(chunks);

      webTestClient
          .get()
          .uri("/api/v1/reviews/gitlab/group%2Fproject/change-requests/10/stream-and-publish")
          .accept(MediaType.TEXT_EVENT_STREAM)
          .exchange()
          .expectStatus()
          .isOk()
          .expectHeader()
          .contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM);

      assertThat(reviewManagementUseCase.wasStreamAndPublishCalled()).isTrue();
    }

    @Test
    @DisplayName("should_stream_and_publish_for_github")
    final void should_stream_and_publish_for_github() {
      reviewManagementUseCase.setReviewChunks(
          List.of(ReviewChunk.of(ReviewChunk.ChunkType.ANALYSIS, "GitHub PR analysis")));

      webTestClient
          .get()
          .uri("/api/v1/reviews/github/owner%2Frepo/change-requests/50/stream-and-publish")
          .accept(MediaType.TEXT_EVENT_STREAM)
          .exchange()
          .expectStatus()
          .isOk();

      assertThat(reviewManagementUseCase.wasStreamAndPublishCalled()).isTrue();
    }

    @Test
    @DisplayName("should_handle_error_during_stream_and_publish")
    final void should_handle_error_during_stream_and_publish() {
      reviewManagementUseCase.setShouldFailStreamAndPublish(true);

      webTestClient
          .get()
          .uri("/api/v1/reviews/gitlab/test%2Fproject/change-requests/1/stream-and-publish")
          .accept(MediaType.TEXT_EVENT_STREAM)
          .exchange()
          .expectStatus()
          .isOk();
    }
  }

  @Nested
  @DisplayName("getIssue")
  final class GetIssueTests {

    @Test
    @DisplayName("should_return_issue_when_found")
    final void should_return_issue_when_found() {
      final UUID issueId = UUID.randomUUID();
      final ReviewIssueEntity issue =
          ReviewIssueEntity.builder()
              .id(issueId)
              .filePath("src/main/java/Test.java")
              .startLine(42)
              .severity("HIGH")
              .title("Security vulnerability detected")
              .suggestion("Use parameterized queries")
              .confidenceScore(new BigDecimal("0.85"))
              .confidenceExplanation("High confidence based on pattern matching")
              .build();
      reviewIssueRepository.setIssue(issue);

      webTestClient
          .get()
          .uri("/api/v1/reviews/issues/" + issueId)
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody()
          .jsonPath("$.id")
          .isEqualTo(issueId.toString())
          .jsonPath("$.filePath")
          .isEqualTo("src/main/java/Test.java")
          .jsonPath("$.startLine")
          .isEqualTo(42)
          .jsonPath("$.severity")
          .isEqualTo("HIGH")
          .jsonPath("$.title")
          .isEqualTo("Security vulnerability detected")
          .jsonPath("$.suggestion")
          .isEqualTo("Use parameterized queries")
          .jsonPath("$.confidenceScore")
          .isEqualTo(0.85)
          .jsonPath("$.confidenceExplanation")
          .isEqualTo("High confidence based on pattern matching");
    }

    @Test
    @DisplayName("should_return_error_when_issue_not_found")
    final void should_return_error_when_issue_not_found() {
      final UUID nonExistentId = UUID.randomUUID();
      reviewIssueRepository.setIssue(null);

      webTestClient
          .get()
          .uri("/api/v1/reviews/issues/" + nonExistentId)
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody()
          .jsonPath("$.status")
          .isEqualTo("error")
          .jsonPath("$.message")
          .value(msg -> assertThat((String) msg).contains("Issue not found"));
    }

    @Test
    @DisplayName("should_handle_null_optional_fields")
    final void should_handle_null_optional_fields() {
      final UUID issueId = UUID.randomUUID();
      final ReviewIssueEntity issue =
          ReviewIssueEntity.builder()
              .id(issueId)
              .filePath("Test.java")
              .startLine(1)
              .severity("LOW")
              .title("Minor issue")
              .suggestion(null)
              .confidenceScore(null)
              .confidenceExplanation(null)
              .build();
      reviewIssueRepository.setIssue(issue);

      webTestClient
          .get()
          .uri("/api/v1/reviews/issues/" + issueId)
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody()
          .jsonPath("$.suggestion")
          .isEqualTo("")
          .jsonPath("$.confidenceScore")
          .isEqualTo(0.0)
          .jsonPath("$.confidenceExplanation")
          .isEqualTo("");
    }
  }

  private static final class TestReviewManagementUseCase implements ReviewManagementUseCase {
    private List<ReviewChunk> reviewChunks = List.of();
    private boolean streamReviewCalled = false;
    private boolean streamAndPublishCalled = false;
    private boolean shouldFailStream = false;
    private boolean shouldFailStreamAndPublish = false;

    void setReviewChunks(final List<ReviewChunk> chunks) {
      this.reviewChunks = chunks;
    }

    void setShouldFailStream(final boolean fail) {
      this.shouldFailStream = fail;
    }

    void setShouldFailStreamAndPublish(final boolean fail) {
      this.shouldFailStreamAndPublish = fail;
    }

    boolean wasStreamReviewCalled() {
      return streamReviewCalled;
    }

    boolean wasStreamAndPublishCalled() {
      return streamAndPublishCalled;
    }

    @Override
    public Flux<ReviewChunk> streamReview(
        final RepositoryIdentifier repository, final ChangeRequestIdentifier changeRequest) {
      this.streamReviewCalled = true;
      if (shouldFailStream) {
        return Flux.error(new RuntimeException("Stream failed"));
      }
      return Flux.fromIterable(reviewChunks);
    }

    @Override
    public Flux<ReviewChunk> streamAndPublishReview(
        final RepositoryIdentifier repository, final ChangeRequestIdentifier changeRequest) {
      this.streamAndPublishCalled = true;
      if (shouldFailStreamAndPublish) {
        return Flux.error(new RuntimeException("Stream and publish failed"));
      }
      return Flux.fromIterable(reviewChunks);
    }

    @Override
    public Mono<Void> publishReview(
        final RepositoryIdentifier repository,
        final ChangeRequestIdentifier changeRequest,
        final ReviewResult reviewResult) {
      return Mono.empty();
    }

    @Override
    public Flux<MergeRequestSummary> getOpenChangeRequests(final RepositoryIdentifier repository) {
      return Flux.empty();
    }

    @Override
    public Flux<RepositoryInfo> getAllRepositories(final SourceProvider provider) {
      return Flux.empty();
    }

    @Override
    public Mono<Void> publishReviewFromAsync(
        final SourceProvider provider,
        final String repositoryId,
        final int changeRequestId,
        final ReviewResult reviewResult) {
      return Mono.empty();
    }
  }

  private static final class TestReviewIssueRepository implements ReviewIssueRepository {
    private ReviewIssueEntity issue;

    void setIssue(final ReviewIssueEntity issue) {
      this.issue = issue;
    }

    @Override
    public Optional<ReviewIssueEntity> findByIdWithReview(final UUID issueId) {
      return Optional.ofNullable(issue);
    }

    @Override
    public void flush() {}

    @Override
    public <S extends ReviewIssueEntity> S saveAndFlush(final S entity) {
      return entity;
    }

    @Override
    public <S extends ReviewIssueEntity> List<S> saveAllAndFlush(final Iterable<S> entities) {
      return List.of();
    }

    @Override
    public void deleteAllInBatch(final Iterable<ReviewIssueEntity> entities) {}

    @Override
    public void deleteAllByIdInBatch(final Iterable<UUID> ids) {}

    @Override
    public void deleteAllInBatch() {}

    @Override
    public ReviewIssueEntity getOne(final UUID id) {
      return null;
    }

    @Override
    public ReviewIssueEntity getById(final UUID id) {
      return null;
    }

    @Override
    public ReviewIssueEntity getReferenceById(final UUID id) {
      return null;
    }

    @Override
    public <S extends ReviewIssueEntity> List<S> findAll(
        final org.springframework.data.domain.Example<S> example) {
      return List.of();
    }

    @Override
    public <S extends ReviewIssueEntity> List<S> findAll(
        final org.springframework.data.domain.Example<S> example,
        final org.springframework.data.domain.Sort sort) {
      return List.of();
    }

    @Override
    public <S extends ReviewIssueEntity> List<S> saveAll(final Iterable<S> entities) {
      return List.of();
    }

    @Override
    public List<ReviewIssueEntity> findAll() {
      return List.of();
    }

    @Override
    public List<ReviewIssueEntity> findAllById(final Iterable<UUID> ids) {
      return List.of();
    }

    @Override
    public <S extends ReviewIssueEntity> S save(final S entity) {
      return entity;
    }

    @Override
    public Optional<ReviewIssueEntity> findById(final UUID id) {
      return Optional.ofNullable(issue);
    }

    @Override
    public boolean existsById(final UUID id) {
      return issue != null;
    }

    @Override
    public long count() {
      return 0;
    }

    @Override
    public void deleteById(final UUID id) {}

    @Override
    public void delete(final ReviewIssueEntity entity) {}

    @Override
    public void deleteAllById(final Iterable<? extends UUID> ids) {}

    @Override
    public void deleteAll(final Iterable<? extends ReviewIssueEntity> entities) {}

    @Override
    public void deleteAll() {}

    @Override
    public List<ReviewIssueEntity> findAll(final org.springframework.data.domain.Sort sort) {
      return List.of();
    }

    @Override
    public org.springframework.data.domain.Page<ReviewIssueEntity> findAll(
        final org.springframework.data.domain.Pageable pageable) {
      return org.springframework.data.domain.Page.empty();
    }

    @Override
    public <S extends ReviewIssueEntity> Optional<S> findOne(
        final org.springframework.data.domain.Example<S> example) {
      return Optional.empty();
    }

    @Override
    public <S extends ReviewIssueEntity> org.springframework.data.domain.Page<S> findAll(
        final org.springframework.data.domain.Example<S> example,
        final org.springframework.data.domain.Pageable pageable) {
      return org.springframework.data.domain.Page.empty();
    }

    @Override
    public <S extends ReviewIssueEntity> long count(
        final org.springframework.data.domain.Example<S> example) {
      return 0;
    }

    @Override
    public <S extends ReviewIssueEntity> boolean exists(
        final org.springframework.data.domain.Example<S> example) {
      return false;
    }

    @Override
    public <S extends ReviewIssueEntity, R> R findBy(
        final org.springframework.data.domain.Example<S> example,
        final java.util.function.Function<
                org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R>
            queryFunction) {
      return null;
    }
  }
}
