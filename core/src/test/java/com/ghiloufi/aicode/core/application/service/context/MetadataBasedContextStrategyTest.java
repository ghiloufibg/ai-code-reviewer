package com.ghiloufi.aicode.core.application.service.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ghiloufi.aicode.core.domain.model.ContextRetrievalResult;
import com.ghiloufi.aicode.core.domain.model.DiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.GitDiffDocument;
import com.ghiloufi.aicode.core.domain.model.GitFileModification;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import com.ghiloufi.aicode.core.domain.port.output.SCMPort;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("MetadataBasedContextStrategy Tests")
final class MetadataBasedContextStrategyTest {

  private MetadataBasedContextStrategy strategy;
  private SCMPort mockSCMPort;

  @BeforeEach
  final void setUp() {
    mockSCMPort = mock(SCMPort.class);
    strategy = new MetadataBasedContextStrategy(mockSCMPort);
  }

  @Nested
  @DisplayName("Strategy Metadata")
  final class StrategyMetadata {

    @Test
    @DisplayName("should_have_correct_strategy_name")
    final void should_have_correct_strategy_name() {
      assertThat(strategy.getStrategyName()).isEqualTo("metadata-based");
    }

    @Test
    @DisplayName("should_have_priority_of_10")
    final void should_have_priority_of_10() {
      assertThat(strategy.getPriority()).isEqualTo(10);
    }
  }

  @Nested
  @DisplayName("Context Retrieval")
  final class ContextRetrieval {

    @Test
    @DisplayName("should_combine_matches_from_all_analyzers")
    final void should_combine_matches_from_all_analyzers() {
      final String rawDiff =
          """
          diff --git a/src/service/UserService.java b/src/service/UserService.java
          index abc123..def456 100644
          --- a/src/service/UserService.java
          +++ b/src/service/UserService.java
          @@ -1,5 +1,6 @@
           package com.example.service;

          +import com.example.util.Helper;
           import java.util.List;

           public class UserService {
          """;

      final GitFileModification modification =
          new GitFileModification("src/service/UserService.java", "src/service/UserService.java");
      final GitDiffDocument gitDiff = new GitDiffDocument(List.of(modification));
      final RepositoryIdentifier repo =
          RepositoryIdentifier.create(SourceProvider.GITLAB, "test/repo");
      final DiffAnalysisBundle bundle = new DiffAnalysisBundle(repo, gitDiff, rawDiff, null);

      final List<String> repositoryFiles =
          List.of(
              "src/service/UserService.java",
              "src/service/UserRepository.java",
              "src/util/Helper.java");

      when(mockSCMPort.listRepositoryFiles()).thenReturn(Mono.just(repositoryFiles));

      final Mono<ContextRetrievalResult> result = strategy.retrieveContext(bundle);

      StepVerifier.create(result)
          .assertNext(
              contextResult -> {
                assertThat(contextResult.matches()).hasSizeGreaterThanOrEqualTo(2);
                assertThat(contextResult.matches())
                    .anyMatch(match -> match.filePath().contains("Helper.java"));
                assertThat(contextResult.matches())
                    .anyMatch(match -> match.filePath().contains("UserRepository.java"));
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("should_deduplicate_matches_with_same_file_path")
    final void should_deduplicate_matches_with_same_file_path() {
      final String rawDiff =
          """
          diff --git a/src/service/UserService.java b/src/service/UserService.java
          index abc123..def456 100644
          --- a/src/service/UserService.java
          +++ b/src/service/UserService.java
          @@ -1,5 +1,6 @@
           package com.example.service;

          +import com.example.service.UserRepository;
           import java.util.List;

           public class UserService {
          """;

      final GitFileModification modification =
          new GitFileModification("src/service/UserService.java", "src/service/UserService.java");
      final GitDiffDocument gitDiff = new GitDiffDocument(List.of(modification));
      final RepositoryIdentifier repo =
          RepositoryIdentifier.create(SourceProvider.GITLAB, "test/repo");
      final DiffAnalysisBundle bundle = new DiffAnalysisBundle(repo, gitDiff, rawDiff, null);

      final List<String> repositoryFiles =
          List.of("src/service/UserService.java", "src/service/UserRepository.java");

      when(mockSCMPort.listRepositoryFiles()).thenReturn(Mono.just(repositoryFiles));

      final Mono<ContextRetrievalResult> result = strategy.retrieveContext(bundle);

      StepVerifier.create(result)
          .assertNext(
              contextResult -> {
                assertThat(contextResult.matches()).isNotEmpty();
                final long userRepositoryMatches =
                    contextResult.matches().stream()
                        .filter(match -> match.filePath().contains("UserRepository.java"))
                        .count();
                assertThat(userRepositoryMatches).isGreaterThanOrEqualTo(1);
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("should_track_metadata_in_result")
    final void should_track_metadata_in_result() {
      final GitFileModification modification =
          new GitFileModification("src/service/UserService.java", "src/service/UserService.java");
      final GitDiffDocument gitDiff = new GitDiffDocument(List.of(modification));
      final RepositoryIdentifier repo =
          RepositoryIdentifier.create(SourceProvider.GITLAB, "test/repo");
      final DiffAnalysisBundle bundle =
          new DiffAnalysisBundle(repo, gitDiff, "raw diff", null);

      when(mockSCMPort.listRepositoryFiles()).thenReturn(Mono.just(List.of()));

      final Mono<ContextRetrievalResult> result = strategy.retrieveContext(bundle);

      StepVerifier.create(result)
          .assertNext(
              contextResult -> {
                assertThat(contextResult.metadata()).isNotNull();
                assertThat(contextResult.metadata().strategyName()).isEqualTo("metadata-based");
                assertThat(contextResult.metadata().totalCandidates()).isEqualTo(0);
                assertThat(contextResult.metadata().executionTime()).isNotNull();
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("should_handle_empty_repository_files_list")
    final void should_handle_empty_repository_files_list() {
      final GitFileModification modification =
          new GitFileModification("src/service/UserService.java", "src/service/UserService.java");
      final GitDiffDocument gitDiff = new GitDiffDocument(List.of(modification));
      final RepositoryIdentifier repo =
          RepositoryIdentifier.create(SourceProvider.GITLAB, "test/repo");
      final DiffAnalysisBundle bundle =
          new DiffAnalysisBundle(repo, gitDiff, "raw diff", null);

      when(mockSCMPort.listRepositoryFiles()).thenReturn(Mono.just(List.of()));

      final Mono<ContextRetrievalResult> result = strategy.retrieveContext(bundle);

      StepVerifier.create(result)
          .assertNext(
              contextResult -> {
                assertThat(contextResult.matches()).isEmpty();
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("should_handle_scm_port_error")
    final void should_handle_scm_port_error() {
      final GitFileModification modification =
          new GitFileModification("src/service/UserService.java", "src/service/UserService.java");
      final GitDiffDocument gitDiff = new GitDiffDocument(List.of(modification));
      final RepositoryIdentifier repo =
          RepositoryIdentifier.create(SourceProvider.GITLAB, "test/repo");
      final DiffAnalysisBundle bundle =
          new DiffAnalysisBundle(repo, gitDiff, "raw diff", null);

      when(mockSCMPort.listRepositoryFiles())
          .thenReturn(Mono.error(new RuntimeException("SCM error")));

      final Mono<ContextRetrievalResult> result = strategy.retrieveContext(bundle);

      StepVerifier.create(result).expectError(RuntimeException.class).verify();
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  final class IntegrationTests {

    @Test
    @DisplayName("should_integrate_all_analyzers_successfully")
    final void should_integrate_all_analyzers_successfully() {
      final String rawDiff =
          """
          diff --git a/src/main/java/com/example/service/UserService.java b/src/main/java/com/example/service/UserService.java
          index abc123..def456 100644
          --- a/src/main/java/com/example/service/UserService.java
          +++ b/src/main/java/com/example/service/UserService.java
          @@ -1,5 +1,6 @@
           package com.example.service;

          +import com.example.repository.UserRepository;
           import java.util.List;

           public class UserService {
          """;

      final GitFileModification modification =
          new GitFileModification(
              "src/main/java/com/example/service/UserService.java",
              "src/main/java/com/example/service/UserService.java");
      final GitDiffDocument gitDiff = new GitDiffDocument(List.of(modification));
      final RepositoryIdentifier repo =
          RepositoryIdentifier.create(SourceProvider.GITLAB, "test/repo");
      final DiffAnalysisBundle bundle = new DiffAnalysisBundle(repo, gitDiff, rawDiff, null);

      final List<String> repositoryFiles =
          List.of(
              "src/main/java/com/example/service/UserService.java",
              "src/main/java/com/example/service/OrderService.java",
              "src/main/java/com/example/repository/UserRepository.java",
              "src/test/java/com/example/service/UserServiceTest.java");

      when(mockSCMPort.listRepositoryFiles()).thenReturn(Mono.just(repositoryFiles));

      final Mono<ContextRetrievalResult> result = strategy.retrieveContext(bundle);

      StepVerifier.create(result)
          .assertNext(
              contextResult -> {
                assertThat(contextResult.matches()).isNotEmpty();
                assertThat(contextResult.metadata()).isNotNull();
                assertThat(contextResult.metadata().strategyName()).isEqualTo("metadata-based");
                assertThat(contextResult.metadata().totalCandidates())
                    .isEqualTo(contextResult.matches().size());
              })
          .verifyComplete();
    }
  }
}
