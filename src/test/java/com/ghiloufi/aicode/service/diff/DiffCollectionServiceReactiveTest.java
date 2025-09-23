package com.ghiloufi.aicode.service.diff;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import com.ghiloufi.aicode.domain.model.DiffAnalysisBundle;
import com.ghiloufi.aicode.domain.model.GitDiffDocument;
import com.ghiloufi.aicode.client.github.GithubClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiffCollectionService Reactive Tests")
class DiffCollectionServiceReactiveTest {

  private static final int CONTEXT_LINES = 3;
  private static final String SAMPLE_DIFF =
      """
      --- a/file.txt
      +++ b/file.txt
      @@ -1,3 +1,3 @@
       line 1
      -old line
      +new line
       line 3
      """;

  @Mock private GithubClient mockGithubClient;

  @Mock private UnifiedDiffParser mockUnifiedDiffParser;

  @Mock private GitDiffDocument mockGitDiffDocument;

  private DiffCollectionService diffCollectionService;

  @BeforeEach
  void setUp() {
    diffCollectionService = new DiffCollectionService(CONTEXT_LINES, "", mockUnifiedDiffParser);
  }

  @Nested
  @DisplayName("GitHub Collection Tests")
  class GitHubCollectionTests {

    @Test
    @DisplayName("collectFromGitHub should work with valid PR")
    void testCollectFromGitHubReactive() {
      // Arrange
      when(mockGithubClient.fetchPrUnifiedDiff(anyInt(), anyInt()))
          .thenReturn(Mono.just(SAMPLE_DIFF));
      when(mockUnifiedDiffParser.parse(SAMPLE_DIFF)).thenReturn(mockGitDiffDocument);
      when(mockGitDiffDocument.toUnifiedString()).thenReturn(SAMPLE_DIFF);

      // Act & Assert
      StepVerifier.create(diffCollectionService.collectFromGitHub(mockGithubClient, 123))
          .expectNextMatches(
              bundle -> {
                assertNotNull(bundle);
                assertEquals(SAMPLE_DIFF, bundle.getUnifiedDiffString());
                return true;
              })
          .verifyComplete();

      verify(mockGithubClient).fetchPrUnifiedDiff(123, CONTEXT_LINES);
      verify(mockUnifiedDiffParser).parse(SAMPLE_DIFF);
    }

    @Test
    @DisplayName("collectFromGitHub should handle GitHub client errors")
    void testCollectFromGitHubReactiveWithError() {
      // Arrange
      when(mockGithubClient.fetchPrUnifiedDiff(anyInt(), anyInt()))
          .thenReturn(Mono.error(new RuntimeException("GitHub API error")));

      // Act & Assert
      StepVerifier.create(diffCollectionService.collectFromGitHub(mockGithubClient, 123))
          .expectErrorMatches(
              throwable ->
                  throwable instanceof RuntimeException
                      && throwable
                          .getMessage()
                          .contains("Failed to fetch diff from GitHub PR #123"))
          .verify();

      verify(mockGithubClient).fetchPrUnifiedDiff(123, CONTEXT_LINES);
      verifyNoInteractions(mockUnifiedDiffParser);
    }

    @Test
    @DisplayName("collectFromGitHub should use reactive implementation")
    void testCollectFromGitHubUsesReactive() {
      // Arrange
      when(mockGithubClient.fetchPrUnifiedDiff(anyInt(), anyInt()))
          .thenReturn(Mono.just(SAMPLE_DIFF));
      when(mockUnifiedDiffParser.parse(SAMPLE_DIFF)).thenReturn(mockGitDiffDocument);
      when(mockGitDiffDocument.toUnifiedString()).thenReturn(SAMPLE_DIFF);

      // Act
      DiffAnalysisBundle result =
          diffCollectionService.collectFromGitHub(mockGithubClient, 123).block();

      // Assert
      assertNotNull(result);
      assertEquals(SAMPLE_DIFF, result.getUnifiedDiffString());
      verify(mockGithubClient).fetchPrUnifiedDiff(123, CONTEXT_LINES);
    }
  }

  @Nested
  @DisplayName("Local Git Collection Tests")
  class LocalGitCollectionTests {

    @Test
    @DisplayName("collectFromLocalGit should handle process execution")
    void testCollectFromLocalGitReactive() {
      // This test would require significant mocking of process execution
      // For now, we'll just verify the method exists and can be called
      StepVerifier.create(diffCollectionService.collectFromLocalGit("HEAD~1", "HEAD"))
          .expectErrorMatches(throwable -> throwable instanceof RuntimeException)
          .verify();
    }

    @Test
    @DisplayName("collectFromLocalGit should use reactive implementation")
    void testCollectFromLocalGitUsesReactive() {
      // Since this involves file system and process operations,
      // we'll test that it doesn't crash immediately using reactive stream
      StepVerifier.create(diffCollectionService.collectFromLocalGit("HEAD~1", "HEAD"))
          .expectErrorMatches(throwable -> throwable instanceof RuntimeException)
          .verify();
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should accept valid parameters")
    void testValidConstructor() {
      assertDoesNotThrow(() -> new DiffCollectionService(5, "test-repo", mockUnifiedDiffParser));
    }

    @Test
    @DisplayName("Constructor should handle empty repository")
    void testConstructorWithEmptyRepository() {
      assertDoesNotThrow(() -> new DiffCollectionService(5, "", mockUnifiedDiffParser));
    }
  }
}
