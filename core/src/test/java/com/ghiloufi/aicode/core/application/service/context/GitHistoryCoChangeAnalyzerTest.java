package com.ghiloufi.aicode.core.application.service.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ghiloufi.aicode.core.domain.model.CoChangeAnalysisResult;
import com.ghiloufi.aicode.core.domain.model.CommitInfo;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import com.ghiloufi.aicode.core.domain.port.output.SCMPort;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("GitHistoryCoChangeAnalyzer Tests")
final class GitHistoryCoChangeAnalyzerTest {

  private GitHistoryCoChangeAnalyzer analyzer;
  private SCMPort mockSCMPort;

  @BeforeEach
  final void setUp() {
    mockSCMPort = mock(SCMPort.class);
    analyzer = new GitHistoryCoChangeAnalyzer(mockSCMPort, 90, 100);
  }

  @Test
  @DisplayName("should_analyze_cochange_patterns_for_target_file")
  final void should_analyze_cochange_patterns_for_target_file() {
    final RepositoryIdentifier repo =
        RepositoryIdentifier.create(SourceProvider.GITLAB, "test-owner/test-repo");
    final String targetFile = "src/UserService.java";

    final List<CommitInfo> commits =
        List.of(
            new CommitInfo(
                "commit1",
                "msg",
                "author",
                Instant.now(),
                List.of("src/UserService.java", "src/UserRepository.java")),
            new CommitInfo(
                "commit2",
                "msg",
                "author",
                Instant.now(),
                List.of(
                    "src/UserService.java", "src/UserRepository.java", "src/UserController.java")),
            new CommitInfo(
                "commit3",
                "msg",
                "author",
                Instant.now(),
                List.of("src/UserService.java", "src/UserMapper.java")));

    when(mockSCMPort.getCommitsFor(eq(repo), eq(targetFile), any(LocalDate.class), anyInt()))
        .thenReturn(Flux.fromIterable(commits));

    final Mono<CoChangeAnalysisResult> result = analyzer.analyzeCoChanges(repo, targetFile);

    StepVerifier.create(result)
        .assertNext(
            analysisResult -> {
              assertThat(analysisResult.targetFile()).isEqualTo(targetFile);
              assertThat(analysisResult.hasMatches()).isTrue();
              assertThat(analysisResult.getTotalAnalyzedFiles()).isEqualTo(3);

              assertThat(analysisResult.rawFrequencyMap())
                  .containsEntry("src/UserRepository.java", 2);
              assertThat(analysisResult.rawFrequencyMap())
                  .containsEntry("src/UserController.java", 1);
              assertThat(analysisResult.rawFrequencyMap()).containsEntry("src/UserMapper.java", 1);

              assertThat(analysisResult.maxFrequency()).isEqualTo(2);
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("should_return_empty_result_when_no_commits_found")
  final void should_return_empty_result_when_no_commits_found() {
    final RepositoryIdentifier repo =
        RepositoryIdentifier.create(SourceProvider.GITLAB, "test-owner/test-repo");
    final String targetFile = "src/UserService.java";

    when(mockSCMPort.getCommitsFor(eq(repo), eq(targetFile), any(LocalDate.class), anyInt()))
        .thenReturn(Flux.empty());

    final Mono<CoChangeAnalysisResult> result = analyzer.analyzeCoChanges(repo, targetFile);

    StepVerifier.create(result)
        .assertNext(
            analysisResult -> {
              assertThat(analysisResult.targetFile()).isEqualTo(targetFile);
              assertThat(analysisResult.hasMatches()).isFalse();
              assertThat(analysisResult.getTotalAnalyzedFiles()).isZero();
              assertThat(analysisResult.maxFrequency()).isZero();
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("should_use_configured_lookback_days")
  final void should_use_configured_lookback_days() {
    final GitHistoryCoChangeAnalyzer customAnalyzer =
        new GitHistoryCoChangeAnalyzer(mockSCMPort, 30, 50);

    final RepositoryIdentifier repo =
        RepositoryIdentifier.create(SourceProvider.GITLAB, "test-owner/test-repo");
    final String targetFile = "src/UserService.java";

    when(mockSCMPort.getCommitsFor(eq(repo), eq(targetFile), any(LocalDate.class), anyInt()))
        .thenReturn(Flux.empty());

    StepVerifier.create(customAnalyzer.analyzeCoChanges(repo, targetFile))
        .assertNext(
            analysisResult -> {
              assertThat(analysisResult.targetFile()).isEqualTo(targetFile);
              assertThat(analysisResult.hasMatches()).isFalse();
            })
        .verifyComplete();
  }
}
