package com.ghiloufi.aicode.core.service.expansion;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.config.ContextRetrievalConfig;
import com.ghiloufi.aicode.core.domain.model.ChangeRequestIdentifier;
import com.ghiloufi.aicode.core.domain.model.CommitInfo;
import com.ghiloufi.aicode.core.domain.model.DiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.DiffHunkBlock;
import com.ghiloufi.aicode.core.domain.model.GitDiffDocument;
import com.ghiloufi.aicode.core.domain.model.GitFileModification;
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
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

final class DiffExpansionServiceTest {

  private TestSCMPort scmPort;
  private DiffExpansionService service;

  @BeforeEach
  void setUp() {
    scmPort = new TestSCMPort();
  }

  @Nested
  class WhenDisabled {

    @Test
    void should_return_disabled_result_when_feature_disabled() {
      final var config = createConfig(false, 100, 500, 10, Set.of());
      service = new DiffExpansionService(scmPort, config);
      final var bundle = createBundle(List.of("src/Main.java"));

      final var result = service.expandDiff(bundle).block();

      assertThat(result.hasExpandedFiles()).isFalse();
      assertThat(result.skipReason()).isEqualTo("Feature disabled");
    }
  }

  @Nested
  class WhenEnabled {

    @Test
    void should_return_empty_result_when_no_files_modified() {
      final var config = createConfig(true, 100, 500, 10, Set.of());
      service = new DiffExpansionService(scmPort, config);
      final var bundle = createBundle(List.of());

      final var result = service.expandDiff(bundle).block();

      assertThat(result.hasExpandedFiles()).isFalse();
      assertThat(result.totalFilesRequested()).isZero();
    }

    @Test
    void should_expand_single_file() {
      final var config = createConfig(true, 100, 500, 10, Set.of());
      service = new DiffExpansionService(scmPort, config);
      scmPort.setFileContent("src/Main.java", "public class Main {}");
      final var bundle = createBundle(List.of("src/Main.java"));

      final var result = service.expandDiff(bundle).block();

      assertThat(result.hasExpandedFiles()).isTrue();
      assertThat(result.expandedFiles()).hasSize(1);
      assertThat(result.expandedFiles().getFirst().filePath()).isEqualTo("src/Main.java");
      assertThat(result.expandedFiles().getFirst().content()).isEqualTo("public class Main {}");
    }

    @Test
    void should_expand_multiple_files() {
      final var config = createConfig(true, 100, 500, 10, Set.of());
      service = new DiffExpansionService(scmPort, config);
      scmPort.setFileContent("src/A.java", "class A {}");
      scmPort.setFileContent("src/B.java", "class B {}");
      final var bundle = createBundle(List.of("src/A.java", "src/B.java"));

      final var result = service.expandDiff(bundle).block();

      assertThat(result.expandedFiles()).hasSize(2);
      assertThat(result.filesExpanded()).isEqualTo(2);
      assertThat(result.filesSkipped()).isZero();
    }

    @Test
    void should_respect_max_files_limit() {
      final var config = createConfig(true, 100, 500, 2, Set.of());
      service = new DiffExpansionService(scmPort, config);
      scmPort.setFileContent("src/A.java", "class A {}");
      scmPort.setFileContent("src/B.java", "class B {}");
      scmPort.setFileContent("src/C.java", "class C {}");
      final var bundle = createBundle(List.of("src/A.java", "src/B.java", "src/C.java"));

      final var result = service.expandDiff(bundle).block();

      assertThat(result.expandedFiles()).hasSize(2);
      assertThat(result.totalFilesRequested()).isEqualTo(3);
      assertThat(result.filesSkipped()).isEqualTo(1);
      assertThat(result.skipReason()).isEqualTo("max files limit");
    }

    @Test
    void should_exclude_files_with_excluded_extensions() {
      final var config = createConfig(true, 100, 500, 10, Set.of(".lock", ".svg"));
      service = new DiffExpansionService(scmPort, config);
      scmPort.setFileContent("src/Main.java", "class Main {}");
      final var bundle = createBundle(List.of("src/Main.java", "yarn.lock", "icon.svg"));

      final var result = service.expandDiff(bundle).block();

      assertThat(result.expandedFiles()).hasSize(1);
      assertThat(result.expandedFiles().getFirst().filePath()).isEqualTo("src/Main.java");
    }

    @Test
    void should_truncate_large_files() {
      final var config = createConfig(true, 100, 3, 10, Set.of());
      service = new DiffExpansionService(scmPort, config);
      scmPort.setFileContent("src/Large.java", "line1\nline2\nline3\nline4\nline5");
      final var bundle = createBundle(List.of("src/Large.java"));

      final var result = service.expandDiff(bundle).block();

      assertThat(result.expandedFiles()).hasSize(1);
      assertThat(result.expandedFiles().getFirst().truncated()).isTrue();
      assertThat(result.expandedFiles().getFirst().lineCount()).isEqualTo(5);
    }

    @Test
    void should_return_empty_context_when_file_fetch_fails() {
      final var config = createConfig(true, 100, 500, 10, Set.of());
      service = new DiffExpansionService(scmPort, config);
      scmPort.setFileError("src/Missing.java", new RuntimeException("File not found"));
      final var bundle = createBundle(List.of("src/Missing.java"));

      final var result = service.expandDiff(bundle).block();

      assertThat(result.expandedFiles()).hasSize(1);
      assertThat(result.expandedFiles().getFirst().hasContent()).isFalse();
    }

    @Test
    void should_skip_deleted_files() {
      final var config = createConfig(true, 100, 500, 10, Set.of());
      service = new DiffExpansionService(scmPort, config);
      final var deletedFile = new GitFileModification("src/Deleted.java", "/dev/null");
      final var modifiedFile = new GitFileModification("src/Old.java", "src/Main.java");
      scmPort.setFileContent("src/Main.java", "class Main {}");
      final var bundle = createBundleWithFiles(List.of(deletedFile, modifiedFile));

      final var result = service.expandDiff(bundle).block();

      assertThat(result.expandedFiles()).hasSize(1);
      assertThat(result.expandedFiles().getFirst().filePath()).isEqualTo("src/Main.java");
    }
  }

  private ContextRetrievalConfig createConfig(
      final boolean enabled,
      final int maxFileSizeKb,
      final int maxLineCount,
      final int maxFilesToExpand,
      final Set<String> excludedExtensions) {
    return new ContextRetrievalConfig(
        true,
        5,
        List.of(),
        new ContextRetrievalConfig.RolloutConfig(100, true, 5000),
        new ContextRetrievalConfig.DiffExpansionConfig(
            enabled, maxFileSizeKb, maxLineCount, maxFilesToExpand, excludedExtensions),
        ContextRetrievalConfig.PrMetadataConfig.defaults(),
        ContextRetrievalConfig.RepositoryPoliciesConfig.defaults());
  }

  private DiffAnalysisBundle createBundle(final List<String> filePaths) {
    final List<GitFileModification> files =
        filePaths.stream().map(path -> new GitFileModification(path, path)).toList();
    return createBundleWithFiles(files);
  }

  private DiffAnalysisBundle createBundleWithFiles(final List<GitFileModification> files) {
    files.forEach(f -> f.diffHunkBlocks.add(new DiffHunkBlock()));
    final var doc = new GitDiffDocument(files);
    return new DiffAnalysisBundle(
        new GitHubRepositoryId("owner", "repo"), doc, "diff content", null);
  }

  private static class TestSCMPort implements SCMPort {
    private final Map<String, String> fileContents = new HashMap<>();
    private final Map<String, Throwable> fileErrors = new HashMap<>();

    void setFileContent(final String path, final String content) {
      fileContents.put(path, content);
    }

    void setFileError(final String path, final Throwable error) {
      fileErrors.put(path, error);
    }

    @Override
    public Mono<String> getFileContent(final RepositoryIdentifier repo, final String filePath) {
      if (fileErrors.containsKey(filePath)) {
        return Mono.error(fileErrors.get(filePath));
      }
      final String content = fileContents.get(filePath);
      return content != null ? Mono.just(content) : Mono.error(new RuntimeException("Not found"));
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

    @Override
    public Mono<PrMetadata> getPullRequestMetadata(
        final RepositoryIdentifier r, final ChangeRequestIdentifier c) {
      return Mono.empty();
    }
  }
}
