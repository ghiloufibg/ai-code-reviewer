package com.ghiloufi.aicode.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.application.service.context.ContextOrchestrator;
import com.ghiloufi.aicode.core.config.SummaryCommentProperties;
import com.ghiloufi.aicode.core.domain.model.*;
import com.ghiloufi.aicode.core.domain.port.output.SCMPort;
import com.ghiloufi.aicode.core.domain.service.SummaryCommentFormatter;
import com.ghiloufi.aicode.core.infrastructure.factory.SCMProviderFactory;
import com.ghiloufi.aicode.core.infrastructure.persistence.PostgresReviewRepository;
import com.ghiloufi.aicode.core.infrastructure.resilience.Resilience;
import com.ghiloufi.aicode.core.service.accumulator.ReviewChunkAccumulator;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

final class ReviewManagementServiceSummaryCommentTest {

  private TestSCMPort testSCMPort;

  @BeforeEach
  void setUp() {
    testSCMPort = new TestSCMPort();
  }

  private ReviewManagementService createReviewManagementService(
      final SummaryCommentProperties summaryCommentProperties) {
    final SCMProviderFactory scmProviderFactory = new TestSCMProviderFactory(testSCMPort);
    final AIReviewStreamingService aiReviewStreamingService = new TestAIReviewStreamingService();
    final ReviewChunkAccumulator chunkAccumulator = new TestReviewChunkAccumulator();
    final PostgresReviewRepository reviewRepository = new TestPostgresReviewRepository();
    final ContextOrchestrator contextOrchestrator = new TestContextOrchestrator();
    final SummaryCommentFormatter summaryCommentFormatter =
        new SummaryCommentFormatter(summaryCommentProperties);

    final Resilience resilience = new Resilience(RetryRegistry.ofDefaults());

    return new ReviewManagementService(
        aiReviewStreamingService,
        scmProviderFactory,
        chunkAccumulator,
        reviewRepository,
        contextOrchestrator,
        summaryCommentProperties,
        summaryCommentFormatter,
        resilience);
  }

  @Test
  void should_publish_summary_comment_when_feature_enabled() {
    final SummaryCommentProperties summaryCommentProperties =
        new SummaryCommentProperties(true, true, true);
    final ReviewManagementService reviewManagementService =
        createReviewManagementService(summaryCommentProperties);

    final RepositoryIdentifier repository = new GitLabRepositoryId("test/repo");
    final ChangeRequestIdentifier changeRequest = new MergeRequestId(1);
    final ReviewResult reviewResult = createReviewResult();

    final Mono<Void> result =
        reviewManagementService.publishReview(repository, changeRequest, reviewResult);

    StepVerifier.create(result).expectComplete().verify();

    assertThat(testSCMPort.isPublishReviewCalled()).isTrue();
    assertThat(testSCMPort.isPublishSummaryCommentCalled()).isTrue();
    assertThat(testSCMPort.getCapturedSummaryComment()).isNotNull();
    assertThat(testSCMPort.getCapturedSummaryComment()).contains("## 📊 AI Code Review Summary");
  }

  @Test
  void should_not_publish_summary_comment_when_feature_disabled() {
    final SummaryCommentProperties summaryCommentProperties =
        new SummaryCommentProperties(false, true, true);
    final ReviewManagementService reviewManagementService =
        createReviewManagementService(summaryCommentProperties);

    final RepositoryIdentifier repository = new GitLabRepositoryId("test/repo");
    final ChangeRequestIdentifier changeRequest = new MergeRequestId(1);
    final ReviewResult reviewResult = createReviewResult();

    final Mono<Void> result =
        reviewManagementService.publishReview(repository, changeRequest, reviewResult);

    StepVerifier.create(result).expectComplete().verify();

    assertThat(testSCMPort.isPublishReviewCalled()).isTrue();
    assertThat(testSCMPort.isPublishSummaryCommentCalled()).isFalse();
  }

  @Test
  void should_continue_review_process_when_summary_comment_fails() {
    final SummaryCommentProperties summaryCommentProperties =
        new SummaryCommentProperties(true, true, true);
    final ReviewManagementService reviewManagementService =
        createReviewManagementService(summaryCommentProperties);
    testSCMPort.setShouldFailSummaryComment(true);

    final RepositoryIdentifier repository = new GitLabRepositoryId("test/repo");
    final ChangeRequestIdentifier changeRequest = new MergeRequestId(1);
    final ReviewResult reviewResult = createReviewResult();

    final Mono<Void> result =
        reviewManagementService.publishReview(repository, changeRequest, reviewResult);

    StepVerifier.create(result).expectComplete().verify();

    assertThat(testSCMPort.isPublishReviewCalled()).isTrue();
    assertThat(testSCMPort.isPublishSummaryCommentCalled()).isTrue();
  }

  @Test
  void should_include_statistics_in_summary_when_enabled() {
    final SummaryCommentProperties summaryCommentProperties =
        new SummaryCommentProperties(true, true, true);
    final ReviewManagementService reviewManagementService =
        createReviewManagementService(summaryCommentProperties);

    final RepositoryIdentifier repository = new GitLabRepositoryId("test/repo");
    final ChangeRequestIdentifier changeRequest = new MergeRequestId(1);
    final ReviewResult reviewResult = createReviewResult();

    final Mono<Void> result =
        reviewManagementService.publishReview(repository, changeRequest, reviewResult);

    StepVerifier.create(result).expectComplete().verify();

    final String summaryComment = testSCMPort.getCapturedSummaryComment();
    assertThat(summaryComment).contains("### 📈 Review Statistics");
    assertThat(summaryComment).contains("Issues Found");
  }

  @Test
  void should_exclude_statistics_in_summary_when_disabled() {
    final SummaryCommentProperties summaryCommentProperties =
        new SummaryCommentProperties(true, false, true);
    final ReviewManagementService reviewManagementService =
        createReviewManagementService(summaryCommentProperties);

    final RepositoryIdentifier repository = new GitLabRepositoryId("test/repo");
    final ChangeRequestIdentifier changeRequest = new MergeRequestId(1);
    final ReviewResult reviewResult = createReviewResult();

    final Mono<Void> result =
        reviewManagementService.publishReview(repository, changeRequest, reviewResult);

    StepVerifier.create(result).expectComplete().verify();

    final String summaryComment = testSCMPort.getCapturedSummaryComment();
    assertThat(summaryComment).doesNotContain("### 📈 Review Statistics");
  }

  @Test
  void should_include_severity_breakdown_when_enabled() {
    final SummaryCommentProperties summaryCommentProperties =
        new SummaryCommentProperties(true, true, true);
    final ReviewManagementService reviewManagementService =
        createReviewManagementService(summaryCommentProperties);

    final RepositoryIdentifier repository = new GitLabRepositoryId("test/repo");
    final ChangeRequestIdentifier changeRequest = new MergeRequestId(1);
    final ReviewResult reviewResult = createReviewResultWithSeverity();

    final Mono<Void> result =
        reviewManagementService.publishReview(repository, changeRequest, reviewResult);

    StepVerifier.create(result).expectComplete().verify();

    final String summaryComment = testSCMPort.getCapturedSummaryComment();
    assertThat(summaryComment).contains("### ⚠️ Severity Breakdown");
    assertThat(summaryComment).contains("HIGH");
  }

  private ReviewResult createReviewResult() {
    final ReviewResult.Issue issue =
        ReviewResult.Issue.issueBuilder()
            .file("Test.java")
            .startLine(10)
            .severity("MEDIUM")
            .title("Test issue")
            .build();

    return ReviewResult.builder().summary("Test review summary").issues(List.of(issue)).build();
  }

  private ReviewResult createReviewResultWithSeverity() {
    final ReviewResult.Issue highIssue =
        ReviewResult.Issue.issueBuilder()
            .file("Critical.java")
            .startLine(5)
            .severity("HIGH")
            .title("High severity issue")
            .build();

    final ReviewResult.Issue mediumIssue =
        ReviewResult.Issue.issueBuilder()
            .file("Warning.java")
            .startLine(15)
            .severity("MEDIUM")
            .title("Medium severity issue")
            .build();

    return ReviewResult.builder()
        .summary("Test review with severity")
        .issues(List.of(highIssue, mediumIssue))
        .build();
  }

  private static final class TestSCMPort implements SCMPort {
    private final AtomicBoolean publishReviewCalled = new AtomicBoolean(false);
    private final AtomicBoolean publishSummaryCommentCalled = new AtomicBoolean(false);
    private final AtomicReference<String> capturedSummaryComment = new AtomicReference<>();
    private boolean shouldFailSummaryComment = false;

    void setShouldFailSummaryComment(final boolean shouldFailSummaryComment) {
      this.shouldFailSummaryComment = shouldFailSummaryComment;
    }

    boolean isPublishReviewCalled() {
      return publishReviewCalled.get();
    }

    boolean isPublishSummaryCommentCalled() {
      return publishSummaryCommentCalled.get();
    }

    String getCapturedSummaryComment() {
      return capturedSummaryComment.get();
    }

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
      publishReviewCalled.set(true);
      return Mono.empty();
    }

    @Override
    public Mono<Void> publishSummaryComment(
        final RepositoryIdentifier repo,
        final ChangeRequestIdentifier changeRequest,
        final String summaryComment) {
      publishSummaryCommentCalled.set(true);
      capturedSummaryComment.set(summaryComment);

      if (shouldFailSummaryComment) {
        return Mono.error(new RuntimeException("Failed to publish summary comment"));
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
        final RepositoryIdentifier repo, final java.time.LocalDate since, final int maxResults) {
      return Flux.empty();
    }

    @Override
    public Mono<String> getFileContent(final RepositoryIdentifier repo, final String filePath) {
      return Mono.empty();
    }

    @Override
    public Mono<PrMetadata> getPullRequestMetadata(
        final RepositoryIdentifier repo, final ChangeRequestIdentifier changeRequest) {
      return Mono.empty();
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

  private static final class TestAIReviewStreamingService extends AIReviewStreamingService {
    TestAIReviewStreamingService() {
      super(null, null, null, null, null);
    }

    @Override
    public ReviewConfiguration getLlmMetadata() {
      return new ReviewConfiguration(
          null, null, false, null, null, "test-provider", "test-model", 0.0);
    }
  }

  private static final class TestReviewChunkAccumulator extends ReviewChunkAccumulator {
    TestReviewChunkAccumulator() {
      super(null, null, null);
    }
  }

  private static final class TestPostgresReviewRepository extends PostgresReviewRepository {
    TestPostgresReviewRepository() {
      super(null);
    }

    @Override
    public Mono<Void> save(final String reviewId, final ReviewResult reviewResult) {
      return Mono.empty();
    }
  }

  private static final class TestContextOrchestrator extends ContextOrchestrator {
    TestContextOrchestrator() {
      super(null, null, null);
    }

    @Override
    public Mono<EnrichedDiffAnalysisBundle> retrieveEnrichedContext(
        final DiffAnalysisBundle diffAnalysisBundle) {
      return Mono.just(
          new EnrichedDiffAnalysisBundle(
              diffAnalysisBundle.repositoryIdentifier(),
              diffAnalysisBundle.structuredDiff(),
              diffAnalysisBundle.rawDiffText(),
              ContextRetrievalResult.empty(),
              diffAnalysisBundle.prMetadata()));
    }
  }
}
