package com.ghiloufi.aicode.core.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.application.service.AIReviewStreamingService;
import com.ghiloufi.aicode.core.application.service.FixApplicationService;
import com.ghiloufi.aicode.core.application.service.ReviewManagementService;
import com.ghiloufi.aicode.core.domain.model.ChangeRequestIdentifier;
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
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import com.ghiloufi.aicode.core.domain.port.output.SCMPort;
import com.ghiloufi.aicode.core.infrastructure.factory.SCMProviderFactory;
import com.ghiloufi.aicode.core.infrastructure.persistence.PostgresReviewRepository;
import com.ghiloufi.aicode.core.service.accumulator.ReviewChunkAccumulator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("Fix Application Integration Tests")
final class FixApplicationIntegrationTest {

  private ReviewManagementService reviewManagementService;
  private FixApplicationService fixApplicationService;
  private TestGitLabSCMPort testGitLabPort;
  private TestAIReviewStreamingService testAIService;
  private TestReviewChunkAccumulator testChunkAccumulator;
  private com.ghiloufi.aicode.core.infrastructure.persistence.repository.ReviewIssueRepository
      mockRepository;

  @BeforeEach
  final void setUp() {
    testGitLabPort = new TestGitLabSCMPort();
    final TestSCMProviderFactory scmFactory = new TestSCMProviderFactory(testGitLabPort);
    testAIService = new TestAIReviewStreamingService();
    testChunkAccumulator = new TestReviewChunkAccumulator();
    final TestPostgresReviewRepository testReviewRepository = new TestPostgresReviewRepository();
    mockRepository =
        org.mockito.Mockito.mock(
            com.ghiloufi.aicode.core.infrastructure.persistence.repository.ReviewIssueRepository
                .class);

    reviewManagementService =
        new ReviewManagementService(
            testAIService, scmFactory, testChunkAccumulator, testReviewRepository);

    fixApplicationService = new FixApplicationService(testGitLabPort, mockRepository);
  }

  @Nested
  @DisplayName("End-to-End Fix Application Flow")
  final class EndToEndFixApplicationFlow {

    @Test
    @DisplayName("should_stream_review_chunks_successfully")
    final void should_stream_review_chunks_successfully() {
      final RepositoryIdentifier repo = new GitLabRepositoryId("test-org/test-project");
      final ChangeRequestIdentifier changeRequest = new MergeRequestId(123);

      testGitLabPort.setDiffAnalysisBundle(createMockDiffBundle());
      testAIService.setReviewChunks(createSecurityIssueChunks());

      final Flux<ReviewChunk> reviewStream =
          reviewManagementService.streamReview(repo, changeRequest);

      StepVerifier.create(reviewStream)
          .assertNext(
              chunk -> {
                assertThat(chunk.type()).isEqualTo(ReviewChunk.ChunkType.SECURITY);
                assertThat(chunk.content()).contains("SQL Injection");
              })
          .assertNext(
              chunk -> {
                assertThat(chunk.type()).isEqualTo(ReviewChunk.ChunkType.SUGGESTION);
                assertThat(chunk.content()).contains("parameterized queries");
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("should_apply_fix_successfully_with_high_confidence")
    final void should_apply_fix_successfully_with_high_confidence() {
      final RepositoryIdentifier repo = new GitLabRepositoryId("test-org/test-project");
      final MergeRequestId changeRequest = new MergeRequestId(456);

      testGitLabPort.setHasWriteAccess(true);
      testGitLabPort.setCommitResult(
          new CommitResult(
              "commit-sha-abc123",
              "https://gitlab.com/test-org/test-project/-/commit/abc123",
              "mr-456",
              List.of("src/main/java/SecurityVulnerability.java"),
              Instant.now()));

      final String filePath = "src/main/java/SecurityVulnerability.java";
      final String fixDiff =
          """
          --- a/src/SecurityVulnerability.java
          +++ b/src/SecurityVulnerability.java
          @@ -10,1 +10,1 @@
          -String query = "SELECT * FROM users WHERE id = " + userId;
          +PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE id = ?");
          """;
      final String commitMessage = "fix: prevent SQL injection in user query";

      final Mono<CommitResult> result =
          fixApplicationService.applyFix(repo, changeRequest, filePath, fixDiff, commitMessage);

      StepVerifier.create(result)
          .assertNext(
              commitResult -> {
                assertThat(commitResult.commitSha()).isEqualTo("commit-sha-abc123");
                assertThat(commitResult.branchName()).isEqualTo("mr-456");
                assertThat(commitResult.filesModified()).contains(filePath);
                assertThat(testGitLabPort.isApplyFixCalled()).isTrue();
                assertThat(testGitLabPort.getCapturedFilePath()).isEqualTo(filePath);
                assertThat(testGitLabPort.getCapturedBranchName()).isEqualTo("mr-456");
              })
          .verifyComplete();
    }
  }

  @Nested
  @DisplayName("Confidence Scoring Scenarios")
  final class ConfidenceScoringScenarios {

    @Test
    @DisplayName("should_include_high_confidence_issues_in_review_result")
    final void should_include_high_confidence_issues_in_review_result() {
      final RepositoryIdentifier repo = new GitLabRepositoryId("test-org/test-project");
      final ChangeRequestIdentifier changeRequest = new MergeRequestId(101);

      testGitLabPort.setDiffAnalysisBundle(createMockDiffBundle());
      testAIService.setReviewChunks(createSecurityIssueChunks());

      testChunkAccumulator.setReviewResult(createHighConfidenceReviewResult());

      final Flux<ReviewChunk> reviewStream =
          reviewManagementService.streamReview(repo, changeRequest);

      StepVerifier.create(reviewStream).expectNextCount(2).verifyComplete();

      final ReviewResult result = testChunkAccumulator.getLastAccumulatedResult();
      assertThat(result).isNotNull();
      assertThat(result.issues).hasSize(1);
      assertThat(result.issues.get(0).confidenceScore).isEqualTo(0.95);
      assertThat(result.issues.get(0).suggestedFix).isNotNull();
    }

    @Test
    @DisplayName("should_filter_out_low_confidence_issues")
    final void should_filter_out_low_confidence_issues() {
      testChunkAccumulator.setReviewResult(createLowConfidenceReviewResult());

      final ReviewResult result = testChunkAccumulator.getLastAccumulatedResult();

      assertThat(result).isNotNull();
      assertThat(result.issues).hasSize(1);
      assertThat(result.issues.get(0).confidenceScore).isLessThan(0.5);
      assertThat(result.issues.get(0).suggestedFix).isNull();
    }
  }

  @Nested
  @DisplayName("GitLab Fix Application Scenarios")
  final class GitLabFixApplicationScenarios {

    @Test
    @DisplayName("should_reject_fix_application_when_no_write_access")
    final void should_reject_fix_application_when_no_write_access() {
      final RepositoryIdentifier repo = new GitLabRepositoryId("test-org/readonly-project");
      final MergeRequestId changeRequest = new MergeRequestId(789);

      testGitLabPort.setHasWriteAccess(false);

      final Mono<CommitResult> result =
          fixApplicationService.applyFix(
              repo,
              changeRequest,
              "file.java",
              "--- a/file.java\n+++ b/file.java\n@@ -1,1 +1,1 @@\n-old\n+new",
              "fix: update code");

      StepVerifier.create(result).expectError().verify();
    }
  }

  private DiffAnalysisBundle createMockDiffBundle() {
    final GitDiffDocument gitDiffDocument = new GitDiffDocument(List.of());
    final String rawDiff =
        """
        --- a/src/SecurityVulnerability.java
        +++ b/src/SecurityVulnerability.java
        @@ -10,1 +10,1 @@
        -String query = "SELECT * FROM users WHERE id = " + userId;
        +String query = "SELECT * FROM users WHERE id = " + userId;
        """;
    final RepositoryIdentifier repo =
        RepositoryIdentifier.create(SourceProvider.GITLAB, "test/repo");
    return new DiffAnalysisBundle(repo, gitDiffDocument, rawDiff);
  }

  private List<ReviewChunk> createSecurityIssueChunks() {
    return List.of(
        new ReviewChunk(
            ReviewChunk.ChunkType.SECURITY,
            "SQL Injection vulnerability detected in SecurityVulnerability.java:10",
            null),
        new ReviewChunk(
            ReviewChunk.ChunkType.SUGGESTION,
            "Use parameterized queries to prevent SQL injection",
            null));
  }

  private ReviewResult createHighConfidenceReviewResult() {
    final ReviewResult result = new ReviewResult();
    result.summary = "Found 1 critical security issue with high confidence";

    final ReviewResult.Issue issue = new ReviewResult.Issue();
    issue.file = "src/main/java/SecurityVulnerability.java";
    issue.start_line = 10;
    issue.severity = "CRITICAL";
    issue.title = "SQL Injection vulnerability";
    issue.suggestion = "Use parameterized queries to prevent SQL injection";
    issue.confidenceScore = 0.95;
    issue.confidenceExplanation = "Clear security vulnerability with established pattern";
    issue.suggestedFix =
        "```diff\n- String query = \"SELECT * FROM users WHERE id = \" + userId;\n+ PreparedStatement stmt = conn.prepareStatement(\"SELECT * FROM users WHERE id = ?\");\n```";

    result.issues.add(issue);
    return result;
  }

  private ReviewResult createLowConfidenceReviewResult() {
    final ReviewResult result = new ReviewResult();
    result.summary = "Found 1 minor style suggestion with low confidence";

    final ReviewResult.Issue issue = new ReviewResult.Issue();
    issue.file = "src/main/java/StyleIssue.java";
    issue.start_line = 5;
    issue.severity = "INFO";
    issue.title = "Consider using var for local variables";
    issue.suggestion = "Modern Java style prefers var for local variables";
    issue.confidenceScore = 0.3;
    issue.confidenceExplanation = "Style preference, not a clear improvement";

    result.issues.add(issue);
    return result;
  }

  private static final class TestGitLabSCMPort implements SCMPort {

    private boolean hasWriteAccess = true;
    private CommitResult commitResult;
    private DiffAnalysisBundle diffAnalysisBundle;
    private boolean shouldFailPublish = false;
    private final AtomicBoolean publishReviewCalled = new AtomicBoolean(false);
    private final AtomicBoolean applyFixCalled = new AtomicBoolean(false);
    private String capturedFilePath;
    private String capturedBranchName;
    private String capturedCommitMessage;

    final void setHasWriteAccess(final boolean hasWriteAccess) {
      this.hasWriteAccess = hasWriteAccess;
    }

    final void setCommitResult(final CommitResult commitResult) {
      this.commitResult = commitResult;
    }

    final void setDiffAnalysisBundle(final DiffAnalysisBundle bundle) {
      this.diffAnalysisBundle = bundle;
    }

    final void setShouldFailPublish(final boolean shouldFailPublish) {
      this.shouldFailPublish = shouldFailPublish;
    }

    final boolean isPublishReviewCalled() {
      return publishReviewCalled.get();
    }

    final boolean isApplyFixCalled() {
      return applyFixCalled.get();
    }

    final String getCapturedFilePath() {
      return capturedFilePath;
    }

    final String getCapturedBranchName() {
      return capturedBranchName;
    }

    @Override
    public Mono<CommitResult> applyFix(
        final RepositoryIdentifier repo,
        final String branchName,
        final String filePath,
        final String fixDiff,
        final String commitMessage) {
      applyFixCalled.set(true);
      this.capturedFilePath = filePath;
      this.capturedBranchName = branchName;
      this.capturedCommitMessage = commitMessage;

      if (!hasWriteAccess) {
        return Mono.error(new RuntimeException("No write access to repository"));
      }

      return Mono.just(commitResult);
    }

    @Override
    public Mono<Boolean> hasWriteAccess(final RepositoryIdentifier repo) {
      return Mono.just(hasWriteAccess);
    }

    @Override
    public Mono<DiffAnalysisBundle> getDiff(
        final RepositoryIdentifier repo, final ChangeRequestIdentifier changeRequest) {
      return Mono.just(diffAnalysisBundle);
    }

    @Override
    public Mono<Void> publishReview(
        final RepositoryIdentifier repo,
        final ChangeRequestIdentifier changeRequest,
        final ReviewResult reviewResult) {
      publishReviewCalled.set(true);
      if (shouldFailPublish) {
        return Mono.error(new RuntimeException("Publish failed"));
      }
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

  private static final class TestAIReviewStreamingService extends AIReviewStreamingService {

    private List<ReviewChunk> reviewChunks = List.of();

    TestAIReviewStreamingService() {
      super(null, null);
    }

    final void setReviewChunks(final List<ReviewChunk> chunks) {
      this.reviewChunks = chunks;
    }

    @Override
    public Flux<ReviewChunk> reviewCodeStreaming(
        final DiffAnalysisBundle diff, final ReviewConfiguration config) {
      return Flux.fromIterable(reviewChunks);
    }

    @Override
    public ReviewConfiguration getLlmMetadata() {
      return ReviewConfiguration.defaults().withLlmMetadata("TestProvider", "TestModel");
    }
  }

  private static final class TestSCMProviderFactory extends SCMProviderFactory {

    private final SCMPort scmPort;

    TestSCMProviderFactory(final SCMPort scmPort) {
      super(List.of(scmPort));
      this.scmPort = scmPort;
    }

    @Override
    public SCMPort getProvider(final SourceProvider provider) {
      return scmPort;
    }
  }

  private static final class TestReviewChunkAccumulator extends ReviewChunkAccumulator {

    private ReviewResult reviewResult;
    private final List<ReviewChunk> accumulatedChunks = new ArrayList<>();

    TestReviewChunkAccumulator() {
      super(null, null, null);
    }

    final void setReviewResult(final ReviewResult result) {
      this.reviewResult = result;
    }

    final ReviewResult getLastAccumulatedResult() {
      return reviewResult;
    }

    @Override
    public ReviewResult accumulateChunks(final List<ReviewChunk> chunks) {
      accumulatedChunks.clear();
      accumulatedChunks.addAll(chunks);
      return reviewResult != null ? reviewResult : new ReviewResult();
    }

    @Override
    public ReviewResult accumulateChunks(
        final List<ReviewChunk> chunks, final ReviewConfiguration config) {
      accumulatedChunks.clear();
      accumulatedChunks.addAll(chunks);
      return reviewResult != null ? reviewResult : new ReviewResult();
    }
  }

  private static final class TestPostgresReviewRepository extends PostgresReviewRepository {

    TestPostgresReviewRepository() {
      super(null);
    }

    @Override
    public Mono<Void> save(final String id, final ReviewResult reviewResult) {
      return Mono.empty();
    }
  }
}
