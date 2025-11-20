package com.ghiloufi.aicode.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.CommitResult;
import com.ghiloufi.aicode.core.domain.model.DiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.GitDiffDocument;
import com.ghiloufi.aicode.core.domain.model.GitLabRepositoryId;
import com.ghiloufi.aicode.core.domain.model.MergeRequestId;
import com.ghiloufi.aicode.core.domain.model.MergeRequestSummary;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.model.RepositoryInfo;
import com.ghiloufi.aicode.core.domain.model.ReviewChunk;
import com.ghiloufi.aicode.core.domain.model.ReviewConfiguration;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.model.ReviewState;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import com.ghiloufi.aicode.core.domain.port.output.SCMPort;
import com.ghiloufi.aicode.core.infrastructure.factory.SCMProviderFactory;
import com.ghiloufi.aicode.core.infrastructure.persistence.PostgresReviewRepository;
import com.ghiloufi.aicode.core.service.accumulator.ReviewChunkAccumulator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

final class ReviewManagementServiceTest {

  private ReviewManagementService reviewManagementService;
  private TestSCMPort testSCMPort;
  private SCMProviderFactory scmProviderFactory;
  private TestAIReviewStreamingService testAIReviewStreamingService;
  private TestReviewChunkAccumulator testReviewChunkAccumulator;
  private TestPostgresReviewRepository testReviewRepository;

  @BeforeEach
  final void setUp() {
    testSCMPort = new TestSCMPort();
    scmProviderFactory = new TestSCMProviderFactory(testSCMPort);
    testAIReviewStreamingService = new TestAIReviewStreamingService();
    testReviewChunkAccumulator = new TestReviewChunkAccumulator();
    testReviewRepository = new TestPostgresReviewRepository();
    reviewManagementService =
        new ReviewManagementService(
            testAIReviewStreamingService,
            scmProviderFactory,
            testReviewChunkAccumulator,
            testReviewRepository);
  }

  @Test
  @DisplayName("should_return_open_merge_requests_when_repository_is_valid")
  final void should_return_open_merge_requests_when_repository_is_valid() {
    final RepositoryIdentifier repository = new GitLabRepositoryId("test-project");

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
                "Fix: Database connection issue",
                "Resolves connection timeout",
                "opened",
                "jane_smith",
                "fix/db-connection",
                "main",
                Instant.parse("2025-01-03T08:00:00Z"),
                Instant.parse("2025-01-03T09:00:00Z"),
                "https://gitlab.com/test/project/-/merge_requests/2"));

    testSCMPort.setMergeRequests(expectedMergeRequests);

    final Flux<MergeRequestSummary> result =
        reviewManagementService.getOpenChangeRequests(repository);

    StepVerifier.create(result)
        .assertNext(
            mr -> {
              assertThat(mr.iid()).isEqualTo(1);
              assertThat(mr.title()).isEqualTo("Feature: Add authentication");
              assertThat(mr.author()).isEqualTo("john_doe");
              assertThat(mr.state()).isEqualTo("opened");
            })
        .assertNext(
            mr -> {
              assertThat(mr.iid()).isEqualTo(2);
              assertThat(mr.title()).isEqualTo("Fix: Database connection issue");
              assertThat(mr.author()).isEqualTo("jane_smith");
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("should_return_empty_flux_when_no_open_merge_requests_exist")
  final void should_return_empty_flux_when_no_open_merge_requests_exist() {
    final RepositoryIdentifier repository = new GitLabRepositoryId("empty-project");

    testSCMPort.setMergeRequests(List.of());

    final Flux<MergeRequestSummary> result =
        reviewManagementService.getOpenChangeRequests(repository);

    StepVerifier.create(result).verifyComplete();
  }

  @Test
  @DisplayName("should_propagate_error_when_scm_port_fails")
  final void should_propagate_error_when_scm_port_fails() {
    final RepositoryIdentifier repository = new GitLabRepositoryId("failing-project");

    testSCMPort.setShouldFail(true);

    final Flux<MergeRequestSummary> result =
        reviewManagementService.getOpenChangeRequests(repository);

    StepVerifier.create(result)
        .expectErrorMatches(
            error ->
                error instanceof RuntimeException
                    && error.getMessage().equals("SCM operation failed"))
        .verify();
  }

  @Test
  @DisplayName("should_return_all_repositories_when_provider_is_valid")
  final void should_return_all_repositories_when_provider_is_valid() {
    final SourceProvider provider = SourceProvider.GITLAB;

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

    testSCMPort.setRepositories(expectedRepositories);

    final Flux<RepositoryInfo> result = reviewManagementService.getAllRepositories(provider);

    StepVerifier.create(result)
        .assertNext(
            repo -> {
              assertThat(repo.id()).isEqualTo(1L);
              assertThat(repo.name()).isEqualTo("project1");
              assertThat(repo.fullName()).isEqualTo("group/project1");
              assertThat(repo.isPrivate()).isFalse();
            })
        .assertNext(
            repo -> {
              assertThat(repo.id()).isEqualTo(2L);
              assertThat(repo.name()).isEqualTo("project2");
              assertThat(repo.fullName()).isEqualTo("group/project2");
              assertThat(repo.isPrivate()).isTrue();
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("should_return_empty_flux_when_no_repositories_exist")
  final void should_return_empty_flux_when_no_repositories_exist() {
    final SourceProvider provider = SourceProvider.GITLAB;

    testSCMPort.setRepositories(List.of());

    final Flux<RepositoryInfo> result = reviewManagementService.getAllRepositories(provider);

    StepVerifier.create(result).verifyComplete();
  }

  @Test
  @DisplayName("should_propagate_error_when_repository_fetch_fails")
  final void should_propagate_error_when_repository_fetch_fails() {
    final SourceProvider provider = SourceProvider.GITLAB;

    testSCMPort.setShouldFailRepositories(true);

    final Flux<RepositoryInfo> result = reviewManagementService.getAllRepositories(provider);

    StepVerifier.create(result)
        .expectErrorMatches(
            error ->
                error instanceof RuntimeException
                    && error.getMessage().equals("Repository fetch failed"))
        .verify();
  }

  @Test
  @DisplayName("should_publish_review_successfully_when_valid_data_provided")
  final void should_publish_review_successfully_when_valid_data_provided() {
    final RepositoryIdentifier repository = new GitLabRepositoryId("test-project");
    final MergeRequestId changeRequest = new MergeRequestId(1);

    final ReviewResult reviewResult = new ReviewResult();
    reviewResult.summary = "Test review summary";
    final ReviewResult.Issue issue = new ReviewResult.Issue();
    issue.file = "Test.java";
    issue.start_line = 10;
    issue.severity = "MEDIUM";
    issue.title = "Test issue";
    issue.suggestion = "Test suggestion";
    reviewResult.issues.add(issue);

    testSCMPort.setShouldFailPublish(false);

    final reactor.core.publisher.Mono<Void> result =
        reviewManagementService.publishReview(repository, changeRequest, reviewResult);

    StepVerifier.create(result).verifyComplete();
  }

  @Test
  @DisplayName("should_propagate_error_when_publish_review_fails")
  final void should_propagate_error_when_publish_review_fails() {
    final RepositoryIdentifier repository = new GitLabRepositoryId("failing-project");
    final MergeRequestId changeRequest = new MergeRequestId(1);

    final ReviewResult reviewResult = new ReviewResult();
    reviewResult.summary = "Test review";

    testSCMPort.setShouldFailPublish(true);

    final reactor.core.publisher.Mono<Void> result =
        reviewManagementService.publishReview(repository, changeRequest, reviewResult);

    StepVerifier.create(result)
        .expectErrorMatches(
            error ->
                error instanceof RuntimeException
                    && error.getMessage().equals("Publish review failed"))
        .verify();
  }

  @Test
  @DisplayName("should_handle_empty_review_result_when_publishing")
  final void should_handle_empty_review_result_when_publishing() {
    final RepositoryIdentifier repository = new GitLabRepositoryId("test-project");
    final MergeRequestId changeRequest = new MergeRequestId(1);

    final ReviewResult emptyReviewResult = new ReviewResult();
    emptyReviewResult.summary = "No issues found";

    testSCMPort.setShouldFailPublish(false);

    final reactor.core.publisher.Mono<Void> result =
        reviewManagementService.publishReview(repository, changeRequest, emptyReviewResult);

    StepVerifier.create(result).verifyComplete();
  }

  @Test
  @DisplayName("should_stream_chunks_and_publish_review_when_successful")
  final void should_stream_chunks_and_publish_review_when_successful() {
    final RepositoryIdentifier repository = new GitLabRepositoryId("test-project");
    final MergeRequestId changeRequest = new MergeRequestId(1);

    final List<ReviewChunk> chunks =
        List.of(
            new ReviewChunk(ReviewChunk.ChunkType.SECURITY, "Security issue in file.java:10", null),
            new ReviewChunk(
                ReviewChunk.ChunkType.PERFORMANCE, "Performance issue in file.java:20", null),
            new ReviewChunk(ReviewChunk.ChunkType.SUGGESTION, "Code improvement suggestion", null));

    testAIReviewStreamingService.setChunks(chunks);
    testSCMPort.setDiffAnalysisBundle(createTestDiffAnalysisBundle());
    testSCMPort.setShouldFailPublish(false);

    final Flux<ReviewChunk> result =
        reviewManagementService.streamAndPublishReview(repository, changeRequest);

    StepVerifier.create(result)
        .assertNext(chunk -> assertThat(chunk.type()).isEqualTo(ReviewChunk.ChunkType.SECURITY))
        .assertNext(chunk -> assertThat(chunk.type()).isEqualTo(ReviewChunk.ChunkType.PERFORMANCE))
        .assertNext(chunk -> assertThat(chunk.type()).isEqualTo(ReviewChunk.ChunkType.SUGGESTION))
        .verifyComplete();

    assertThat(testSCMPort.isPublishReviewCalled()).isTrue();
    assertThat(testReviewChunkAccumulator.isAccumulateChunksCalled()).isTrue();
  }

  @Test
  @DisplayName("should_stream_chunks_but_not_publish_when_chunks_are_empty")
  final void should_stream_chunks_but_not_publish_when_chunks_are_empty() {
    final RepositoryIdentifier repository = new GitLabRepositoryId("test-project");
    final MergeRequestId changeRequest = new MergeRequestId(1);

    testAIReviewStreamingService.setChunks(List.of());
    testSCMPort.setDiffAnalysisBundle(createTestDiffAnalysisBundle());

    final Flux<ReviewChunk> result =
        reviewManagementService.streamAndPublishReview(repository, changeRequest);

    StepVerifier.create(result).verifyComplete();

    assertThat(testSCMPort.isPublishReviewCalled()).isFalse();
  }

  @Test
  @DisplayName("should_stream_chunks_and_handle_publish_error_gracefully")
  final void should_stream_chunks_and_handle_publish_error_gracefully() {
    final RepositoryIdentifier repository = new GitLabRepositoryId("test-project");
    final MergeRequestId changeRequest = new MergeRequestId(1);

    final List<ReviewChunk> chunks =
        List.of(
            new ReviewChunk(ReviewChunk.ChunkType.SECURITY, "Security issue", null),
            new ReviewChunk(ReviewChunk.ChunkType.SUGGESTION, "Improvement suggestion", null));

    testAIReviewStreamingService.setChunks(chunks);
    testSCMPort.setDiffAnalysisBundle(createTestDiffAnalysisBundle());
    testSCMPort.setShouldFailPublish(true);

    final Flux<ReviewChunk> result =
        reviewManagementService.streamAndPublishReview(repository, changeRequest);

    StepVerifier.create(result)
        .assertNext(chunk -> assertThat(chunk.type()).isEqualTo(ReviewChunk.ChunkType.SECURITY))
        .assertNext(chunk -> assertThat(chunk.type()).isEqualTo(ReviewChunk.ChunkType.SUGGESTION))
        .verifyComplete();
  }

  @Test
  @DisplayName("should_propagate_error_when_streaming_fails")
  final void should_propagate_error_when_streaming_fails() {
    final RepositoryIdentifier repository = new GitLabRepositoryId("test-project");
    final MergeRequestId changeRequest = new MergeRequestId(1);

    testAIReviewStreamingService.setShouldFail(true);
    testSCMPort.setDiffAnalysisBundle(createTestDiffAnalysisBundle());

    final Flux<ReviewChunk> result =
        reviewManagementService.streamAndPublishReview(repository, changeRequest);

    StepVerifier.create(result)
        .expectErrorMatches(
            error ->
                error instanceof RuntimeException && error.getMessage().equals("Streaming failed"))
        .verify();
  }

  private DiffAnalysisBundle createTestDiffAnalysisBundle() {
    final GitDiffDocument gitDiffDocument = new GitDiffDocument(List.of());
    final String rawDiff = "--- a/file.java\n+++ b/file.java\n@@ -1,1 +1,1 @@\n-old\n+new";
    final RepositoryIdentifier repo =
        RepositoryIdentifier.create(SourceProvider.GITLAB, "test/repo");
    return new DiffAnalysisBundle(repo, gitDiffDocument, rawDiff);
  }

  private static final class TestSCMPort implements SCMPort {
    private List<MergeRequestSummary> mergeRequests = List.of();
    private List<RepositoryInfo> repositories = List.of();
    private boolean shouldFail = false;
    private boolean shouldFailRepositories = false;
    private boolean shouldFailPublish = false;
    private DiffAnalysisBundle diffAnalysisBundle;
    private final AtomicBoolean publishReviewCalled = new AtomicBoolean(false);

    final void setMergeRequests(final List<MergeRequestSummary> mergeRequests) {
      this.mergeRequests = mergeRequests;
    }

    final void setRepositories(final List<RepositoryInfo> repositories) {
      this.repositories = repositories;
    }

    final void setShouldFail(final boolean shouldFail) {
      this.shouldFail = shouldFail;
    }

    final void setShouldFailRepositories(final boolean shouldFailRepositories) {
      this.shouldFailRepositories = shouldFailRepositories;
    }

    final void setShouldFailPublish(final boolean shouldFailPublish) {
      this.shouldFailPublish = shouldFailPublish;
    }

    final void setDiffAnalysisBundle(final DiffAnalysisBundle diffAnalysisBundle) {
      this.diffAnalysisBundle = diffAnalysisBundle;
    }

    final boolean isPublishReviewCalled() {
      return publishReviewCalled.get();
    }

    @Override
    public reactor.core.publisher.Mono<com.ghiloufi.aicode.core.domain.model.DiffAnalysisBundle>
        getDiff(
            final com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier repo,
            final com.ghiloufi.aicode.core.domain.model.ChangeRequestIdentifier changeRequest) {
      if (diffAnalysisBundle != null) {
        return Mono.just(diffAnalysisBundle);
      }
      return reactor.core.publisher.Mono.empty();
    }

    @Override
    public reactor.core.publisher.Mono<Void> publishReview(
        final com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier repo,
        final com.ghiloufi.aicode.core.domain.model.ChangeRequestIdentifier changeRequest,
        final com.ghiloufi.aicode.core.domain.model.ReviewResult reviewResult) {
      publishReviewCalled.set(true);
      if (shouldFailPublish) {
        return reactor.core.publisher.Mono.error(new RuntimeException("Publish review failed"));
      }
      return reactor.core.publisher.Mono.empty();
    }

    @Override
    public reactor.core.publisher.Mono<Boolean> isChangeRequestOpen(
        final com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier repo,
        final com.ghiloufi.aicode.core.domain.model.ChangeRequestIdentifier changeRequest) {
      return reactor.core.publisher.Mono.just(true);
    }

    @Override
    public reactor.core.publisher.Mono<com.ghiloufi.aicode.core.domain.model.RepositoryInfo>
        getRepository(final com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier repo) {
      return reactor.core.publisher.Mono.empty();
    }

    @Override
    public Flux<MergeRequestSummary> getOpenChangeRequests(
        final com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier repo) {
      if (shouldFail) {
        return Flux.error(new RuntimeException("SCM operation failed"));
      }
      return Flux.fromIterable(mergeRequests);
    }

    @Override
    public Flux<RepositoryInfo> getAllRepositories() {
      if (shouldFailRepositories) {
        return Flux.error(new RuntimeException("Repository fetch failed"));
      }
      return Flux.fromIterable(repositories);
    }

    @Override
    public SourceProvider getProviderType() {
      return SourceProvider.GITLAB;
    }

    @Override
    public Mono<CommitResult> applyFix(
        final RepositoryIdentifier repo,
        final String branchName,
        final String filePath,
        final String fixDiff,
        final String commitMessage) {
      return Mono.empty();
    }

    @Override
    public Mono<Boolean> hasWriteAccess(final RepositoryIdentifier repo) {
      return Mono.just(true);
    }

    @Override
    public Mono<java.util.List<String>> listRepositoryFiles() {
      return Mono.just(java.util.List.of());
    }

    @Override
    public reactor.core.publisher.Flux<com.ghiloufi.aicode.core.domain.model.CommitInfo>
        getCommitsFor(
            final RepositoryIdentifier repo,
            final String filePath,
            final java.time.LocalDate since,
            final int maxResults) {
      return reactor.core.publisher.Flux.empty();
    }

    @Override
    public reactor.core.publisher.Flux<com.ghiloufi.aicode.core.domain.model.CommitInfo>
        getCommitsSince(
            final RepositoryIdentifier repo,
            final java.time.LocalDate since,
            final int maxResults) {
      return reactor.core.publisher.Flux.empty();
    }
  }

  private static final class TestSCMProviderFactory extends SCMProviderFactory {

    private final SCMPort scmPort;

    TestSCMProviderFactory(final SCMPort scmPort) {
      super(java.util.List.of(scmPort));
      this.scmPort = scmPort;
    }

    @Override
    public SCMPort getProvider(final SourceProvider provider) {
      return scmPort;
    }
  }

  private static final class TestAIReviewStreamingService extends AIReviewStreamingService {

    private List<ReviewChunk> chunks = List.of();
    private boolean shouldFail = false;

    TestAIReviewStreamingService() {
      super(null, null);
    }

    final void setChunks(final List<ReviewChunk> chunks) {
      this.chunks = chunks;
    }

    final void setShouldFail(final boolean shouldFail) {
      this.shouldFail = shouldFail;
    }

    @Override
    public Flux<ReviewChunk> reviewCodeStreaming(
        final DiffAnalysisBundle diff, final ReviewConfiguration config) {
      if (shouldFail) {
        return Flux.error(new RuntimeException("Streaming failed"));
      }
      return Flux.fromIterable(chunks);
    }

    @Override
    public ReviewConfiguration getLlmMetadata() {
      return ReviewConfiguration.defaults().withLlmMetadata("test-provider", "test-model");
    }
  }

  private static final class TestReviewChunkAccumulator extends ReviewChunkAccumulator {

    private final AtomicBoolean accumulateChunksCalled = new AtomicBoolean(false);

    TestReviewChunkAccumulator() {
      super(null, null, null);
    }

    final boolean isAccumulateChunksCalled() {
      return accumulateChunksCalled.get();
    }

    @Override
    public ReviewResult accumulateChunks(final List<ReviewChunk> chunks) {
      return accumulateChunks(
          chunks, com.ghiloufi.aicode.core.domain.model.ReviewConfiguration.defaults());
    }

    @Override
    public ReviewResult accumulateChunks(
        final List<ReviewChunk> chunks,
        final com.ghiloufi.aicode.core.domain.model.ReviewConfiguration config) {
      accumulateChunksCalled.set(true);

      final ReviewResult result = new ReviewResult();
      result.summary = "Test review summary";

      final List<ReviewResult.Issue> issues = new ArrayList<>();
      final List<ReviewResult.Note> notes = new ArrayList<>();

      for (final ReviewChunk chunk : chunks) {
        switch (chunk.type()) {
          case SECURITY, PERFORMANCE -> {
            final ReviewResult.Issue issue = new ReviewResult.Issue();
            issue.severity = chunk.type().name();
            issue.title = "Test issue";
            issue.file = "test.java";
            issue.start_line = 10;
            issue.suggestion = chunk.content();
            issues.add(issue);
          }
          case SUGGESTION -> {
            final ReviewResult.Note note = new ReviewResult.Note();
            note.file = "test.java";
            note.line = 20;
            note.note = chunk.content();
            notes.add(note);
          }
          default -> {}
        }
      }

      result.issues.addAll(issues);
      result.non_blocking_notes.addAll(notes);

      return result;
    }
  }

  private static final class TestPostgresReviewRepository extends PostgresReviewRepository {
    public TestPostgresReviewRepository() {
      super(null, null);
    }

    @Override
    public Mono<Void> save(final String reviewId, final ReviewResult result) {
      return Mono.empty();
    }

    @Override
    public Mono<Optional<ReviewResult>> findById(final String reviewId) {
      return Mono.just(Optional.empty());
    }

    @Override
    public Mono<Void> updateState(final String reviewId, final ReviewState newState) {
      return Mono.empty();
    }

    @Override
    public Mono<Void> updateResultAndState(
        final String reviewId, final ReviewResult result, final ReviewState newState) {
      return Mono.empty();
    }

    @Override
    public Mono<Optional<ReviewState.StateTransition>> getState(final String reviewId) {
      return Mono.just(Optional.empty());
    }
  }
}
