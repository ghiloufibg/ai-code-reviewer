package com.ghiloufi.aicode.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.ChangeRequestIdentifier;
import com.ghiloufi.aicode.core.domain.model.CommitInfo;
import com.ghiloufi.aicode.core.domain.model.CommitResult;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import com.ghiloufi.aicode.core.domain.port.output.SCMPort;
import com.ghiloufi.aicode.core.infrastructure.persistence.repository.ReviewIssueRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("FixApplicationService Unit Tests")
final class FixApplicationServiceTest {

  private FixApplicationService fixApplicationService;
  private TestSCMPort testSCMPort;
  private ReviewIssueRepository mockRepository;

  @BeforeEach
  void setUp() {
    testSCMPort = new TestSCMPort();
    mockRepository = Mockito.mock(ReviewIssueRepository.class);
    fixApplicationService = new FixApplicationService(testSCMPort, mockRepository);
  }

  @Test
  @DisplayName("should_apply_fix_successfully_when_user_has_write_access")
  void should_apply_fix_successfully_when_user_has_write_access() {
    final RepositoryIdentifier repo =
        RepositoryIdentifier.create(SourceProvider.GITLAB, "test-org/test-repo");
    final ChangeRequestIdentifier changeRequest =
        ChangeRequestIdentifier.create(SourceProvider.GITLAB, 123);

    testSCMPort.setHasWriteAccess(true);
    testSCMPort.setCommitResult(
        new CommitResult(
            "abc123",
            "https://gitlab.com/test-org/test-repo/-/commit/abc123",
            "mr-123",
            List.of("src/Test.java"),
            Instant.now()));

    final Mono<CommitResult> result =
        fixApplicationService.applyFix(
            repo,
            changeRequest,
            "src/Test.java",
            "--- a/Test.java\n+++ b/Test.java\n@@ -1,1 +1,1 @@\n-old\n+new\n",
            "Fix security vulnerability");

    StepVerifier.create(result)
        .assertNext(
            commitResult -> {
              assertThat(commitResult.commitSha()).isEqualTo("abc123");
              assertThat(commitResult.branchName()).isEqualTo("mr-123");
              assertThat(commitResult.filesModified()).containsExactly("src/Test.java");
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("should_fail_when_user_has_no_write_access")
  void should_fail_when_user_has_no_write_access() {
    final RepositoryIdentifier repo =
        RepositoryIdentifier.create(SourceProvider.GITLAB, "test-org/test-repo");
    final ChangeRequestIdentifier changeRequest =
        ChangeRequestIdentifier.create(SourceProvider.GITLAB, 123);

    testSCMPort.setHasWriteAccess(false);

    final Mono<CommitResult> result =
        fixApplicationService.applyFix(
            repo, changeRequest, "src/Test.java", "diff content", "Fix issue");

    StepVerifier.create(result)
        .expectErrorMatches(
            error ->
                error instanceof IllegalStateException
                    && error.getMessage().contains("No write access"))
        .verify();
  }

  @Test
  @DisplayName("should_derive_branch_name_from_merge_request_number")
  void should_derive_branch_name_from_merge_request_number() {
    final RepositoryIdentifier repo =
        RepositoryIdentifier.create(SourceProvider.GITLAB, "test-org/test-repo");
    final ChangeRequestIdentifier changeRequest =
        ChangeRequestIdentifier.create(SourceProvider.GITLAB, 456);

    testSCMPort.setHasWriteAccess(true);
    testSCMPort.setCommitResult(
        new CommitResult(
            "def456",
            "https://gitlab.com/commit/def456",
            "mr-456",
            List.of("Test.java"),
            Instant.now()));

    final Mono<CommitResult> result =
        fixApplicationService.applyFix(repo, changeRequest, "Test.java", "diff", "Fix bug");

    StepVerifier.create(result)
        .assertNext(commitResult -> assertThat(commitResult.branchName()).isEqualTo("mr-456"))
        .verifyComplete();
  }

  private static final class TestSCMPort implements SCMPort {

    private boolean hasWriteAccess = true;
    private CommitResult commitResult;

    void setHasWriteAccess(final boolean hasWriteAccess) {
      this.hasWriteAccess = hasWriteAccess;
    }

    void setCommitResult(final CommitResult commitResult) {
      this.commitResult = commitResult;
    }

    @Override
    public Mono<CommitResult> applyFix(
        final RepositoryIdentifier repo,
        final String branchName,
        final String filePath,
        final String fixDiff,
        final String commitMessage) {
      return Mono.just(commitResult);
    }

    @Override
    public Mono<Boolean> hasWriteAccess(final RepositoryIdentifier repo) {
      return Mono.just(hasWriteAccess);
    }

    @Override
    public Mono<com.ghiloufi.aicode.core.domain.model.DiffAnalysisBundle> getDiff(
        final RepositoryIdentifier repo, final ChangeRequestIdentifier changeRequest) {
      return Mono.empty();
    }

    @Override
    public Mono<Void> publishReview(
        final RepositoryIdentifier repo,
        final ChangeRequestIdentifier changeRequest,
        final com.ghiloufi.aicode.core.domain.model.ReviewResult reviewResult) {
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
      return Mono.empty();
    }

    @Override
    public Mono<com.ghiloufi.aicode.core.domain.model.RepositoryInfo> getRepository(
        final RepositoryIdentifier repo) {
      return Mono.empty();
    }

    @Override
    public reactor.core.publisher.Flux<com.ghiloufi.aicode.core.domain.model.MergeRequestSummary>
        getOpenChangeRequests(final RepositoryIdentifier repo) {
      return reactor.core.publisher.Flux.empty();
    }

    @Override
    public reactor.core.publisher.Flux<com.ghiloufi.aicode.core.domain.model.RepositoryInfo>
        getAllRepositories() {
      return reactor.core.publisher.Flux.empty();
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
  }
}
