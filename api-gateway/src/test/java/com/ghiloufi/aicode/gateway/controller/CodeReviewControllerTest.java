package com.ghiloufi.aicode.gateway.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghiloufi.aicode.core.domain.model.ChangeRequestIdentifier;
import com.ghiloufi.aicode.core.domain.model.CommitInfo;
import com.ghiloufi.aicode.core.domain.model.DiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.GitLabRepositoryId;
import com.ghiloufi.aicode.core.domain.model.MergeRequestId;
import com.ghiloufi.aicode.core.domain.model.MergeRequestSummary;
import com.ghiloufi.aicode.core.domain.model.PrMetadata;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.model.RepositoryInfo;
import com.ghiloufi.aicode.core.domain.model.ReviewChunk;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import com.ghiloufi.aicode.core.domain.port.input.ReviewManagementUseCase;
import com.ghiloufi.aicode.core.domain.port.output.SCMPort;
import com.ghiloufi.aicode.gateway.formatter.SSEFormatter;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

final class CodeReviewControllerTest {

  private WebTestClient webTestClient;
  private TestReviewManagementUseCase reviewManagementUseCase;

  @BeforeEach
  final void setUp() {
    reviewManagementUseCase = new TestReviewManagementUseCase();
    final ObjectMapper objectMapper = new ObjectMapper();
    final SSEFormatter sseFormatter = new SSEFormatter(objectMapper);
    final com.ghiloufi.aicode.core.infrastructure.persistence.repository.ReviewIssueRepository
        mockRepository =
            org.mockito.Mockito.mock(
                com.ghiloufi.aicode.core.infrastructure.persistence.repository.ReviewIssueRepository
                    .class);
    final CodeReviewController controller =
        new CodeReviewController(reviewManagementUseCase, sseFormatter, mockRepository);

    webTestClient =
        WebTestClient.bindToController(controller)
            .configureClient()
            .responseTimeout(Duration.ofSeconds(5))
            .build();
  }

  @Test
  @DisplayName("should_return_open_merge_requests_for_gitlab_repository")
  final void should_return_open_merge_requests_for_gitlab_repository() {
    final List<MergeRequestSummary> expectedMergeRequests =
        List.of(
            new MergeRequestSummary(
                1,
                "Feature: Add authentication",
                "Implementation of JWT auth",
                "opened",
                "john_doe",
                "feature/auth",
                "main",
                Instant.parse("2025-01-01T10:00:00Z"),
                Instant.parse("2025-01-02T15:30:00Z"),
                "https://gitlab.com/test/project/-/merge_requests/1"),
            new MergeRequestSummary(
                2,
                "Fix: Database connection",
                "Resolves timeout",
                "opened",
                "jane_smith",
                "fix/db",
                "main",
                Instant.parse("2025-01-03T08:00:00Z"),
                Instant.parse("2025-01-03T09:00:00Z"),
                "https://gitlab.com/test/project/-/merge_requests/2"));

    reviewManagementUseCase.setMergeRequests(expectedMergeRequests);

    webTestClient
        .get()
        .uri("/api/v1/reviews/gitlab/test-project/change-requests")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .contentType(MediaType.APPLICATION_JSON)
        .expectBodyList(MergeRequestSummary.class)
        .value(
            mergeRequests -> {
              assertThat(mergeRequests).hasSize(2);
              assertThat(mergeRequests.get(0).iid()).isEqualTo(1);
              assertThat(mergeRequests.get(0).title()).isEqualTo("Feature: Add authentication");
              assertThat(mergeRequests.get(1).iid()).isEqualTo(2);
              assertThat(mergeRequests.get(1).title()).isEqualTo("Fix: Database connection");
            });
  }

  @Test
  @DisplayName("should_return_empty_list_when_no_open_merge_requests")
  final void should_return_empty_list_when_no_open_merge_requests() {
    reviewManagementUseCase.setMergeRequests(List.of());

    webTestClient
        .get()
        .uri("/api/v1/reviews/gitlab/empty-project/change-requests")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBodyList(MergeRequestSummary.class)
        .value(mergeRequests -> assertThat(mergeRequests).isEmpty());
  }

  @Test
  @DisplayName("should_decode_url_encoded_repository_id")
  final void should_decode_url_encoded_repository_id() {
    final List<MergeRequestSummary> expectedMergeRequests =
        List.of(
            new MergeRequestSummary(
                1,
                "Test MR",
                "Description",
                "opened",
                "user",
                "branch",
                "main",
                Instant.now(),
                Instant.now(),
                "https://gitlab.com/org/repo/-/merge_requests/1"));

    reviewManagementUseCase.setMergeRequests(expectedMergeRequests);

    webTestClient
        .get()
        .uri("/api/v1/reviews/gitlab/org%2Fproject/change-requests")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBodyList(MergeRequestSummary.class)
        .value(mergeRequests -> assertThat(mergeRequests).hasSize(1));

    final RepositoryIdentifier capturedRepo = reviewManagementUseCase.getCapturedRepository();
    assertThat(capturedRepo).isInstanceOf(GitLabRepositoryId.class);
    assertThat(((GitLabRepositoryId) capturedRepo).projectId()).isEqualTo("org/project");
  }

  @Test
  @DisplayName("should_handle_invalid_provider")
  final void should_handle_invalid_provider() {
    webTestClient
        .get()
        .uri("/api/v1/reviews/invalid-provider/test-project/change-requests")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .is5xxServerError();
  }

  @Test
  @DisplayName("should_return_all_repositories_for_gitlab_provider")
  final void should_return_all_repositories_for_gitlab_provider() {
    final List<RepositoryInfo> expectedRepositories =
        List.of(
            new RepositoryInfo(
                "group/project1",
                "project1",
                "group",
                "First project",
                "Java",
                true,
                true,
                "main",
                1L,
                false,
                "https://gitlab.com/group/project1"),
            new RepositoryInfo(
                "group/project2",
                "project2",
                "group",
                "Second project",
                "Java",
                false,
                true,
                "develop",
                2L,
                true,
                "https://gitlab.com/group/project2"));

    reviewManagementUseCase.setRepositories(expectedRepositories);

    webTestClient
        .get()
        .uri("/api/v1/reviews/gitlab/repositories")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .contentType(MediaType.APPLICATION_JSON)
        .expectBodyList(RepositoryInfo.class)
        .value(
            repositories -> {
              assertThat(repositories).hasSize(2);
              assertThat(repositories.get(0).id()).isEqualTo(1L);
              assertThat(repositories.get(0).name()).isEqualTo("project1");
              assertThat(repositories.get(0).fullName()).isEqualTo("group/project1");
              assertThat(repositories.get(1).id()).isEqualTo(2L);
              assertThat(repositories.get(1).name()).isEqualTo("project2");
              assertThat(repositories.get(1).fullName()).isEqualTo("group/project2");
            });
  }

  @Test
  @DisplayName("should_return_empty_list_when_no_repositories")
  final void should_return_empty_list_when_no_repositories() {
    reviewManagementUseCase.setRepositories(List.of());

    webTestClient
        .get()
        .uri("/api/v1/reviews/gitlab/repositories")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBodyList(RepositoryInfo.class)
        .hasSize(0);
  }

  @Test
  @DisplayName("should_publish_review_successfully_for_gitlab_merge_request")
  final void should_publish_review_successfully_for_gitlab_merge_request() {
    final ReviewResult.Issue issue =
        ReviewResult.Issue.issueBuilder()
            .file("Test.java")
            .startLine(10)
            .severity("HIGH")
            .title("Security vulnerability")
            .suggestion("Fix the vulnerability")
            .build();
    final ReviewResult reviewResult =
        ReviewResult.builder().summary("Test review summary").issues(List.of(issue)).build();

    reviewManagementUseCase.setShouldFailPublish(false);

    webTestClient
        .post()
        .uri("/api/v1/reviews/gitlab/test-project/change-requests/1/review")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(reviewResult)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo("success")
        .jsonPath("$.message")
        .isEqualTo("Review published successfully")
        .jsonPath("$.provider")
        .isEqualTo("GITLAB")
        .jsonPath("$.repository")
        .isEqualTo("test-project")
        .jsonPath("$.changeRequestId")
        .isEqualTo(1);

    assertThat(reviewManagementUseCase.getCapturedRepository())
        .isInstanceOf(GitLabRepositoryId.class);
    assertThat(reviewManagementUseCase.getCapturedChangeRequest())
        .isInstanceOf(MergeRequestId.class);
    assertThat(((MergeRequestId) reviewManagementUseCase.getCapturedChangeRequest()).iid())
        .isEqualTo(1);
  }

  @Test
  @DisplayName("should_return_error_response_when_publish_fails")
  final void should_return_error_response_when_publish_fails() {
    final ReviewResult reviewResult = ReviewResult.builder().summary("Test review").build();

    reviewManagementUseCase.setShouldFailPublish(true);

    webTestClient
        .post()
        .uri("/api/v1/reviews/gitlab/failing-project/change-requests/1/review")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(reviewResult)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo("error")
        .jsonPath("$.message")
        .isEqualTo("Publish failed")
        .jsonPath("$.provider")
        .isEqualTo("GITLAB")
        .jsonPath("$.repository")
        .isEqualTo("failing-project");
  }

  @Test
  @DisplayName("should_handle_empty_review_result")
  final void should_handle_empty_review_result() {
    final ReviewResult emptyReviewResult =
        ReviewResult.builder().summary("No issues found").build();

    reviewManagementUseCase.setShouldFailPublish(false);

    webTestClient
        .post()
        .uri("/api/v1/reviews/gitlab/clean-project/change-requests/1/review")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(emptyReviewResult)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo("success")
        .jsonPath("$.message")
        .isEqualTo("Review published successfully");
  }

  private static final class TestReviewManagementUseCase implements ReviewManagementUseCase {
    private List<MergeRequestSummary> mergeRequests = List.of();
    private List<RepositoryInfo> repositories = List.of();
    private RepositoryIdentifier capturedRepository;
    private ChangeRequestIdentifier capturedChangeRequest;
    private boolean shouldFailPublish = false;

    final void setMergeRequests(final List<MergeRequestSummary> mergeRequests) {
      this.mergeRequests = mergeRequests;
    }

    final void setRepositories(final List<RepositoryInfo> repositories) {
      this.repositories = repositories;
    }

    final void setShouldFailPublish(final boolean shouldFailPublish) {
      this.shouldFailPublish = shouldFailPublish;
    }

    final RepositoryIdentifier getCapturedRepository() {
      return capturedRepository;
    }

    final ChangeRequestIdentifier getCapturedChangeRequest() {
      return capturedChangeRequest;
    }

    @Override
    public Flux<ReviewChunk> streamReview(
        final RepositoryIdentifier repository, final ChangeRequestIdentifier changeRequest) {
      return Flux.empty();
    }

    @Override
    public Flux<ReviewChunk> streamAndPublishReview(
        final RepositoryIdentifier repository, final ChangeRequestIdentifier changeRequest) {
      return Flux.empty();
    }

    @Override
    public Mono<Void> publishReview(
        final RepositoryIdentifier repository,
        final ChangeRequestIdentifier changeRequest,
        final ReviewResult reviewResult) {
      this.capturedRepository = repository;
      this.capturedChangeRequest = changeRequest;
      if (shouldFailPublish) {
        return Mono.error(new RuntimeException("Publish failed"));
      }
      return Mono.empty();
    }

    @Override
    public Flux<MergeRequestSummary> getOpenChangeRequests(final RepositoryIdentifier repository) {
      this.capturedRepository = repository;
      return Flux.fromIterable(mergeRequests);
    }

    @Override
    public Flux<RepositoryInfo> getAllRepositories(final SourceProvider provider) {
      return Flux.fromIterable(repositories);
    }
  }

  private static final class TestSCMPort implements SCMPort {

    @Override
    public Mono<DiffAnalysisBundle> getDiff(
        final RepositoryIdentifier repo, final ChangeRequestIdentifier changeRequest) {
      return Mono.empty();
    }

    @Override
    public Mono<Void> publishReview(
        final RepositoryIdentifier repo,
        final ChangeRequestIdentifier changeRequest,
        final ReviewResult reviewResult) {
      return Mono.empty();
    }

    @Override
    public Mono<Void> publishSummaryComment(
        final RepositoryIdentifier repo,
        final ChangeRequestIdentifier changeRequest,
        final String summaryComment) {
      return Mono.empty();
    }

    @Override
    public Mono<Boolean> isChangeRequestOpen(
        final RepositoryIdentifier repo, final ChangeRequestIdentifier changeRequest) {
      return Mono.just(true);
    }

    @Override
    public Mono<RepositoryInfo> getRepository(final RepositoryIdentifier repo) {
      return Mono.empty();
    }

    @Override
    public Flux<MergeRequestSummary> getOpenChangeRequests(final RepositoryIdentifier repo) {
      return Flux.empty();
    }

    @Override
    public Flux<RepositoryInfo> getAllRepositories() {
      return Flux.empty();
    }

    @Override
    public SourceProvider getProviderType() {
      return SourceProvider.GITLAB;
    }

    @Override
    public Mono<List<String>> listRepositoryFiles() {
      return Mono.just(List.of());
    }

    @Override
    public Flux<CommitInfo> getCommitsFor(
        final RepositoryIdentifier repo,
        final String filePath,
        final LocalDate since,
        final int maxResults) {
      return Flux.empty();
    }

    @Override
    public Flux<CommitInfo> getCommitsSince(
        final RepositoryIdentifier repo, final LocalDate since, final int maxResults) {
      return Flux.empty();
    }

    @Override
    public Mono<PrMetadata> getPullRequestMetadata(
        final RepositoryIdentifier repo, final ChangeRequestIdentifier changeRequest) {
      return Mono.empty();
    }

    @Override
    public Mono<String> getFileContent(final RepositoryIdentifier repo, final String filePath) {
      return Mono.empty();
    }
  }
}
