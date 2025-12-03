package com.ghiloufi.aicode.core.service.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.config.ContextRetrievalConfig;
import com.ghiloufi.aicode.core.domain.model.ChangeRequestIdentifier;
import com.ghiloufi.aicode.core.domain.model.CommitInfo;
import com.ghiloufi.aicode.core.domain.model.DiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.GitHubRepositoryId;
import com.ghiloufi.aicode.core.domain.model.MergeRequestSummary;
import com.ghiloufi.aicode.core.domain.model.PrMetadata;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.model.RepositoryInfo;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import com.ghiloufi.aicode.core.domain.port.output.SCMPort;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

final class RepositoryPolicyProviderTest {

  private TestSCMPort scmPort;
  private RepositoryPolicyProvider provider;

  @BeforeEach
  void setUp() {
    scmPort = new TestSCMPort();
  }

  @Nested
  class WhenDisabled {

    @Test
    void should_return_empty_policies_when_feature_disabled() {
      final var config = createConfig(false, 5000, List.of("CONTRIBUTING.md"));
      provider = new RepositoryPolicyProvider(scmPort, config);

      final var result = provider.getPolicies(new GitHubRepositoryId("owner", "repo")).block();

      assertThat(result.hasPolicies()).isFalse();
    }
  }

  @Nested
  class WhenEnabled {

    @Test
    void should_fetch_configured_file() {
      final var config = createConfig(true, 5000, List.of("CONTRIBUTING.md"));
      provider = new RepositoryPolicyProvider(scmPort, config);
      scmPort.setFileContent("CONTRIBUTING.md", "# Contributing\nFollow these rules.");

      final var result = provider.getPolicies(new GitHubRepositoryId("owner", "repo")).block();

      assertThat(result.hasPolicies()).isTrue();
      assertThat(result.allPolicies()).hasSize(1);
      assertThat(result.allPolicies().get(0).name()).isEqualTo("CONTRIBUTING.md");
      assertThat(result.allPolicies().get(0).content()).contains("Contributing");
    }

    @Test
    void should_fetch_multiple_files() {
      final var config =
          createConfig(true, 5000, List.of("CONTRIBUTING.md", "SECURITY.md", "README.md"));
      provider = new RepositoryPolicyProvider(scmPort, config);
      scmPort.setFileContent("CONTRIBUTING.md", "# Contributing");
      scmPort.setFileContent("SECURITY.md", "# Security");
      scmPort.setFileContent("README.md", "# Readme");

      final var result = provider.getPolicies(new GitHubRepositoryId("owner", "repo")).block();

      assertThat(result.allPolicies()).hasSize(3);
    }

    @Test
    void should_truncate_large_content() {
      final var config = createConfig(true, 50, List.of("CONTRIBUTING.md"));
      provider = new RepositoryPolicyProvider(scmPort, config);
      scmPort.setFileContent("CONTRIBUTING.md", "A".repeat(100));

      final var result = provider.getPolicies(new GitHubRepositoryId("owner", "repo")).block();

      assertThat(result.allPolicies().get(0).truncated()).isTrue();
      assertThat(result.allPolicies().get(0).content()).contains("(truncated)");
      assertThat(result.allPolicies().get(0).content().length()).isLessThan(100);
    }

    @Test
    void should_skip_missing_files() {
      final var config =
          createConfig(true, 5000, List.of("CONTRIBUTING.md", "NONEXISTENT.md", "SECURITY.md"));
      provider = new RepositoryPolicyProvider(scmPort, config);
      scmPort.setFileContent("CONTRIBUTING.md", "# Contributing");
      scmPort.setFileContent("SECURITY.md", "# Security");

      final var result = provider.getPolicies(new GitHubRepositoryId("owner", "repo")).block();

      assertThat(result.allPolicies()).hasSize(2);
    }

    @Test
    void should_return_empty_when_no_files_configured() {
      final var config = createConfig(true, 5000, List.of());
      provider = new RepositoryPolicyProvider(scmPort, config);

      final var result = provider.getPolicies(new GitHubRepositoryId("owner", "repo")).block();

      assertThat(result.hasPolicies()).isFalse();
    }

    @Test
    void should_return_empty_when_all_files_missing() {
      final var config = createConfig(true, 5000, List.of("MISSING1.md", "MISSING2.md"));
      provider = new RepositoryPolicyProvider(scmPort, config);

      final var result = provider.getPolicies(new GitHubRepositoryId("owner", "repo")).block();

      assertThat(result.hasPolicies()).isFalse();
    }

    @Test
    void should_extract_file_name_from_path() {
      final var config = createConfig(true, 5000, List.of(".github/CONTRIBUTING.md"));
      provider = new RepositoryPolicyProvider(scmPort, config);
      scmPort.setFileContent(".github/CONTRIBUTING.md", "# Content");

      final var result = provider.getPolicies(new GitHubRepositoryId("owner", "repo")).block();

      assertThat(result.allPolicies().get(0).name()).isEqualTo("CONTRIBUTING.md");
      assertThat(result.allPolicies().get(0).path()).isEqualTo(".github/CONTRIBUTING.md");
    }
  }

  private ContextRetrievalConfig createConfig(
      final boolean enabled, final int maxContentChars, final List<String> files) {
    return new ContextRetrievalConfig(
        true,
        5,
        List.of(),
        new ContextRetrievalConfig.RolloutConfig(100, true, 5000),
        ContextRetrievalConfig.DiffExpansionConfig.defaults(),
        ContextRetrievalConfig.PrMetadataConfig.defaults(),
        new ContextRetrievalConfig.RepositoryPoliciesConfig(enabled, maxContentChars, files));
  }

  private static class TestSCMPort implements SCMPort {
    private final Map<String, String> fileContents = new HashMap<>();

    void setFileContent(final String path, final String content) {
      fileContents.put(path, content);
    }

    @Override
    public Mono<String> getFileContent(final RepositoryIdentifier repo, final String filePath) {
      final String content = fileContents.get(filePath);
      return content != null ? Mono.just(content) : Mono.error(new RuntimeException("Not found"));
    }

    @Override
    public Mono<PrMetadata> getPullRequestMetadata(
        final RepositoryIdentifier r, final ChangeRequestIdentifier c) {
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
