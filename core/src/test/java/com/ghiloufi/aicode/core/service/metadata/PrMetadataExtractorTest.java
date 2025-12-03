package com.ghiloufi.aicode.core.service.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.config.ContextRetrievalConfig;
import com.ghiloufi.aicode.core.domain.model.ChangeRequestIdentifier;
import com.ghiloufi.aicode.core.domain.model.CommitInfo;
import com.ghiloufi.aicode.core.domain.model.DiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.GitHubRepositoryId;
import com.ghiloufi.aicode.core.domain.model.MergeRequestSummary;
import com.ghiloufi.aicode.core.domain.model.PrMetadata;
import com.ghiloufi.aicode.core.domain.model.PullRequestId;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.model.RepositoryInfo;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import com.ghiloufi.aicode.core.domain.port.output.SCMPort;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

final class PrMetadataExtractorTest {

  private TestSCMPort scmPort;
  private PrMetadataExtractor extractor;

  @BeforeEach
  void setUp() {
    scmPort = new TestSCMPort();
  }

  @Nested
  class WhenDisabled {

    @Test
    void should_return_empty_metadata_when_feature_disabled() {
      final var config = createConfig(false, true, true, true, 5);
      extractor = new PrMetadataExtractor(scmPort, config);

      final var result =
          extractor
              .extractMetadata(new GitHubRepositoryId("owner", "repo"), new PullRequestId(1))
              .block();

      assertThat(result.hasTitle()).isFalse();
      assertThat(result.hasCommits()).isFalse();
    }
  }

  @Nested
  class WhenEnabled {

    @Test
    void should_extract_full_metadata() {
      final var config = createConfig(true, true, true, true, 5);
      extractor = new PrMetadataExtractor(scmPort, config);
      scmPort.setPrMetadata(createFullMetadata());

      final var result =
          extractor
              .extractMetadata(new GitHubRepositoryId("owner", "repo"), new PullRequestId(1))
              .block();

      assertThat(result.title()).isEqualTo("Fix bug");
      assertThat(result.description()).isEqualTo("Description");
      assertThat(result.author()).isEqualTo("john");
      assertThat(result.labels()).containsExactly("bug", "urgent");
      assertThat(result.commits()).hasSize(2);
    }

    @Test
    void should_exclude_labels_when_disabled() {
      final var config = createConfig(true, false, true, true, 5);
      extractor = new PrMetadataExtractor(scmPort, config);
      scmPort.setPrMetadata(createFullMetadata());

      final var result =
          extractor
              .extractMetadata(new GitHubRepositoryId("owner", "repo"), new PullRequestId(1))
              .block();

      assertThat(result.labels()).isEmpty();
    }

    @Test
    void should_exclude_commits_when_disabled() {
      final var config = createConfig(true, true, false, true, 5);
      extractor = new PrMetadataExtractor(scmPort, config);
      scmPort.setPrMetadata(createFullMetadata());

      final var result =
          extractor
              .extractMetadata(new GitHubRepositoryId("owner", "repo"), new PullRequestId(1))
              .block();

      assertThat(result.commits()).isEmpty();
    }

    @Test
    void should_exclude_author_when_disabled() {
      final var config = createConfig(true, true, true, false, 5);
      extractor = new PrMetadataExtractor(scmPort, config);
      scmPort.setPrMetadata(createFullMetadata());

      final var result =
          extractor
              .extractMetadata(new GitHubRepositoryId("owner", "repo"), new PullRequestId(1))
              .block();

      assertThat(result.author()).isNull();
    }

    @Test
    void should_limit_commits_to_max() {
      final var config = createConfig(true, true, true, true, 1);
      extractor = new PrMetadataExtractor(scmPort, config);
      scmPort.setPrMetadata(createFullMetadata());

      final var result =
          extractor
              .extractMetadata(new GitHubRepositoryId("owner", "repo"), new PullRequestId(1))
              .block();

      assertThat(result.commits()).hasSize(1);
    }

    @Test
    void should_return_empty_metadata_on_error() {
      final var config = createConfig(true, true, true, true, 5);
      extractor = new PrMetadataExtractor(scmPort, config);
      scmPort.setError(new RuntimeException("API error"));

      final var result =
          extractor
              .extractMetadata(new GitHubRepositoryId("owner", "repo"), new PullRequestId(1))
              .block();

      assertThat(result.hasTitle()).isFalse();
    }
  }

  private PrMetadata createFullMetadata() {
    final var commits =
        List.of(
            new CommitInfo("abc123", "First commit", "john", Instant.now(), List.of()),
            new CommitInfo("def456", "Second commit", "john", Instant.now(), List.of()));
    return new PrMetadata(
        "Fix bug",
        "Description",
        "john",
        "main",
        "feature/fix",
        List.of("bug", "urgent"),
        commits,
        3);
  }

  private ContextRetrievalConfig createConfig(
      final boolean enabled,
      final boolean includeLabels,
      final boolean includeCommits,
      final boolean includeAuthor,
      final int maxCommitMessages) {
    return new ContextRetrievalConfig(
        true,
        5,
        List.of(),
        new ContextRetrievalConfig.RolloutConfig(100, true, 5000),
        ContextRetrievalConfig.DiffExpansionConfig.defaults(),
        new ContextRetrievalConfig.PrMetadataConfig(
            enabled, includeLabels, includeCommits, includeAuthor, maxCommitMessages),
        ContextRetrievalConfig.RepositoryPoliciesConfig.defaults());
  }

  private static class TestSCMPort implements SCMPort {
    private PrMetadata prMetadata;
    private Throwable error;

    void setPrMetadata(final PrMetadata metadata) {
      this.prMetadata = metadata;
    }

    void setError(final Throwable error) {
      this.error = error;
    }

    @Override
    public Mono<PrMetadata> getPullRequestMetadata(
        final RepositoryIdentifier repo, final ChangeRequestIdentifier changeRequest) {
      if (error != null) {
        return Mono.error(error);
      }
      return prMetadata != null ? Mono.just(prMetadata) : Mono.just(PrMetadata.empty());
    }

    @Override
    public Mono<String> getFileContent(final RepositoryIdentifier r, final String f) {
      return Mono.empty();
    }

    @Override
    public Mono<DiffAnalysisBundle> getDiff(
        final RepositoryIdentifier r, final ChangeRequestIdentifier c) {
      return Mono.empty();
    }

    @Override
    public Mono<Void> publishReview(
        final RepositoryIdentifier r, final ChangeRequestIdentifier c, final ReviewResult res) {
      return Mono.empty();
    }

    @Override
    public Mono<Void> publishSummaryComment(
        final RepositoryIdentifier r, final ChangeRequestIdentifier c, final String s) {
      return Mono.empty();
    }

    @Override
    public Mono<Boolean> isChangeRequestOpen(
        final RepositoryIdentifier r, final ChangeRequestIdentifier c) {
      return Mono.just(true);
    }

    @Override
    public Mono<RepositoryInfo> getRepository(final RepositoryIdentifier r) {
      return Mono.empty();
    }

    @Override
    public Flux<MergeRequestSummary> getOpenChangeRequests(final RepositoryIdentifier r) {
      return Flux.empty();
    }

    @Override
    public Flux<RepositoryInfo> getAllRepositories() {
      return Flux.empty();
    }

    @Override
    public SourceProvider getProviderType() {
      return SourceProvider.GITHUB;
    }

    @Override
    public Mono<List<String>> listRepositoryFiles() {
      return Mono.just(List.of());
    }

    @Override
    public Flux<CommitInfo> getCommitsFor(
        final RepositoryIdentifier r, final String f, final LocalDate s, final int m) {
      return Flux.empty();
    }

    @Override
    public Flux<CommitInfo> getCommitsSince(
        final RepositoryIdentifier r, final LocalDate s, final int m) {
      return Flux.empty();
    }
  }
}
