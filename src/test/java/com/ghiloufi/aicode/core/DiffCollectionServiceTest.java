package com.ghiloufi.aicode.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ghiloufi.aicode.domain.DiffAnalysisBundle;
import com.ghiloufi.aicode.domain.GitDiffDocument;
import com.ghiloufi.aicode.github.GithubClient;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiffCollectionService Tests")
public class DiffCollectionServiceTest {

  private final int contextLines = 3;
  // Sample diff content for testing
  private final String sampleDiff =
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
  @Mock private Process mockProcess;
  @Mock private ProcessBuilder mockProcessBuilder;
  private DiffCollectionService diffCollectionService;

  @BeforeEach
  void setUp() {
    diffCollectionService = new DiffCollectionService(contextLines);
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create service with specified context lines")
    void should_create_service_with_context_lines() {
      DiffCollectionService service = new DiffCollectionService(5);
      assertNotNull(service);
      // Context lines are used internally, we can't directly assert them
      // but we can verify they're used in the git command via integration tests
    }

    @Test
    @DisplayName("Should create service with zero context lines")
    void should_create_service_with_zero_context_lines() {
      assertDoesNotThrow(() -> new DiffCollectionService(0));
    }

    @Test
    @DisplayName("Should create service with large context lines")
    void should_create_service_with_large_context_lines() {
      assertDoesNotThrow(() -> new DiffCollectionService(1000));
    }
  }

  @Nested
  @DisplayName("GitHub Collection Tests")
  class GitHubCollectionTests {

    @Test
    @DisplayName("Should successfully collect diff from GitHub PR")
    void should_collect_diff_from_github_pr() {
      // Arrange
      int prNumber = 123;
      when(mockGithubClient.fetchPrUnifiedDiff(prNumber, contextLines)).thenReturn(sampleDiff);

      // Act
      DiffAnalysisBundle result =
          diffCollectionService.collectFromGitHub(mockGithubClient, prNumber);

      // Assert
      assertNotNull(result);
      assertEquals(sampleDiff, result.rawDiffText());
      assertNotNull(result.structuredDiff());
      assertEquals(1, result.structuredDiff().files.size());

      verify(mockGithubClient).fetchPrUnifiedDiff(prNumber, contextLines);
    }

    @Test
    @DisplayName("Should handle empty diff from GitHub")
    @Disabled("A voir si le texte brut du diff peut etre null ou pas.")
    void should_handle_empty_diff_from_github() {
      // Arrange
      int prNumber = 456;
      String emptyDiff = "";
      when(mockGithubClient.fetchPrUnifiedDiff(prNumber, contextLines)).thenReturn(emptyDiff);

      // Act
      DiffAnalysisBundle result =
          diffCollectionService.collectFromGitHub(mockGithubClient, prNumber);

      // Assert
      assertNotNull(result);
      assertEquals("", result.rawDiffText());
      assertNotNull(result.structuredDiff());
      assertEquals(0, result.structuredDiff().files.size());
    }

    @Test
    @DisplayName("Should handle large diff from GitHub")
    void should_handle_large_diff_from_github() {
      // Arrange
      int prNumber = 789;
      StringBuilder largeDiff = new StringBuilder();
      largeDiff.append("--- a/large_file.txt\n");
      largeDiff.append("+++ b/large_file.txt\n");
      largeDiff.append("@@ -1,1000 +1,1000 @@\n");
      for (int i = 1; i <= 1000; i++) {
        largeDiff.append(" line ").append(i).append("\n");
      }

      when(mockGithubClient.fetchPrUnifiedDiff(prNumber, contextLines))
          .thenReturn(largeDiff.toString());

      // Act
      DiffAnalysisBundle result =
          diffCollectionService.collectFromGitHub(mockGithubClient, prNumber);

      // Assert
      assertNotNull(result);
      assertEquals(largeDiff.toString(), result.rawDiffText());
      assertNotNull(result.structuredDiff());
    }

    @Test
    @DisplayName("Should throw RuntimeException when GitHub client fails")
    void should_throw_exception_when_github_client_fails() {
      // Arrange
      int prNumber = 999;
      when(mockGithubClient.fetchPrUnifiedDiff(prNumber, contextLines))
          .thenThrow(new RuntimeException("GitHub API error"));

      // Act & Assert
      RuntimeException exception =
          assertThrows(
              RuntimeException.class,
              () -> diffCollectionService.collectFromGitHub(mockGithubClient, prNumber));

      assertTrue(exception.getMessage().contains("Failed to fetch diff from GitHub PR #999"));
      assertTrue(exception.getCause().getMessage().contains("GitHub API error"));
    }

    @Test
    @DisplayName("Should throw RuntimeException when GitHub client throws IOException")
    void should_throw_exception_when_github_throws_io_exception() {
      // Arrange
      int prNumber = 888;
      when(mockGithubClient.fetchPrUnifiedDiff(prNumber, contextLines))
          .thenThrow(new RuntimeException(new IOException("Network error")));

      // Act & Assert
      RuntimeException exception =
          assertThrows(
              RuntimeException.class,
              () -> diffCollectionService.collectFromGitHub(mockGithubClient, prNumber));

      assertTrue(exception.getMessage().contains("Failed to fetch diff from GitHub PR #888"));
    }
  }

  @Nested
  @DisplayName("Local Git Collection Tests")
  @Disabled
  class LocalGitCollectionTests {

    @Test
    @DisplayName("Should successfully collect diff from local git")
    void should_collect_diff_from_local_git() throws Exception {
      // Arrange
      String base = "main";
      String head = "feature-branch";

      InputStream diffStream =
          new ByteArrayInputStream(sampleDiff.getBytes(StandardCharsets.UTF_8));
      when(mockProcess.getInputStream()).thenReturn(diffStream);
      when(mockProcess.waitFor()).thenReturn(0);

      try (MockedStatic<ProcessBuilder> processBuilderMock = mockStatic(ProcessBuilder.class)) {
        ProcessBuilder realProcessBuilder = mock(ProcessBuilder.class);
        when(realProcessBuilder.redirectErrorStream(true)).thenReturn(realProcessBuilder);
        when(realProcessBuilder.start()).thenReturn(mockProcess);

        processBuilderMock
            .when(() -> new ProcessBuilder("git", "diff", "--unified=" + contextLines, base, head))
            .thenReturn(realProcessBuilder);

        // Act
        DiffAnalysisBundle result = diffCollectionService.collectFromLocalGit(base, head);

        // Assert
        assertNotNull(result);
        assertEquals(sampleDiff, result.getUnifiedDiffString());
        assertNotNull(result.structuredDiff());
        assertEquals(1, result.structuredDiff().files.size());

        verify(mockProcess).waitFor();
      }
    }

    @Test
    @DisplayName("Should handle empty diff from local git")
    void should_handle_empty_diff_from_local_git() throws Exception {
      // Arrange
      String base = "main";
      String head = "main"; // Same commit, should produce empty diff
      String emptyDiff = "";

      InputStream emptyStream =
          new ByteArrayInputStream(emptyDiff.getBytes(StandardCharsets.UTF_8));
      when(mockProcess.getInputStream()).thenReturn(emptyStream);
      when(mockProcess.waitFor()).thenReturn(0);

      try (MockedStatic<ProcessBuilder> processBuilderMock = mockStatic(ProcessBuilder.class)) {
        ProcessBuilder realProcessBuilder = mock(ProcessBuilder.class);
        when(realProcessBuilder.redirectErrorStream(true)).thenReturn(realProcessBuilder);
        when(realProcessBuilder.start()).thenReturn(mockProcess);

        processBuilderMock
            .when(() -> new ProcessBuilder("git", "diff", "--unified=" + contextLines, base, head))
            .thenReturn(realProcessBuilder);

        // Act
        DiffAnalysisBundle result = diffCollectionService.collectFromLocalGit(base, head);

        // Assert
        assertNotNull(result);
        assertEquals("", result.getUnifiedDiffString());
        assertEquals(0, result.structuredDiff().files.size());
      }
    }

    @Test
    @DisplayName("Should handle git diff with multiple files")
    void should_handle_git_diff_with_multiple_files() throws Exception {
      // Arrange
      String base = "main";
      String head = "feature";
      String multiFileDiff =
          """
                --- a/file1.txt
                +++ b/file1.txt
                @@ -1,1 +1,2 @@
                 line 1
                +added line
                --- a/file2.txt
                +++ b/file2.txt
                @@ -1,1 +1,1 @@
                -old content
                +new content
                """;

      InputStream diffStream =
          new ByteArrayInputStream(multiFileDiff.getBytes(StandardCharsets.UTF_8));
      when(mockProcess.getInputStream()).thenReturn(diffStream);
      when(mockProcess.waitFor()).thenReturn(0);

      try (MockedStatic<ProcessBuilder> processBuilderMock = mockStatic(ProcessBuilder.class)) {
        ProcessBuilder realProcessBuilder = mock(ProcessBuilder.class);
        when(realProcessBuilder.redirectErrorStream(true)).thenReturn(realProcessBuilder);
        when(realProcessBuilder.start()).thenReturn(mockProcess);

        processBuilderMock
            .when(() -> new ProcessBuilder("git", "diff", "--unified=" + contextLines, base, head))
            .thenReturn(realProcessBuilder);

        // Act
        DiffAnalysisBundle result = diffCollectionService.collectFromLocalGit(base, head);

        // Assert
        assertNotNull(result);
        assertEquals(multiFileDiff, result.getUnifiedDiffString());
        assertEquals(2, result.structuredDiff().files.size());
      }
    }

    @Test
    @DisplayName("Should use correct context lines in git command")
    void should_use_correct_context_lines_in_git_command() throws Exception {
      // Arrange
      int customContextLines = 7;
      DiffCollectionService customService = new DiffCollectionService(customContextLines);
      String base = "main";
      String head = "feature";

      InputStream diffStream =
          new ByteArrayInputStream(sampleDiff.getBytes(StandardCharsets.UTF_8));
      when(mockProcess.getInputStream()).thenReturn(diffStream);
      when(mockProcess.waitFor()).thenReturn(0);

      try (MockedStatic<ProcessBuilder> processBuilderMock = mockStatic(ProcessBuilder.class)) {
        ProcessBuilder realProcessBuilder = mock(ProcessBuilder.class);
        when(realProcessBuilder.redirectErrorStream(true)).thenReturn(realProcessBuilder);
        when(realProcessBuilder.start()).thenReturn(mockProcess);

        processBuilderMock
            .when(
                () ->
                    new ProcessBuilder(
                        "git", "diff", "--unified=" + customContextLines, base, head))
            .thenReturn(realProcessBuilder);

        // Act
        customService.collectFromLocalGit(base, head);

        // Assert
        processBuilderMock.verify(
            () -> new ProcessBuilder("git", "diff", "--unified=" + customContextLines, base, head));
      }
    }

    @Test
    @DisplayName("Should throw RuntimeException when ProcessBuilder fails")
    void should_throw_exception_when_process_builder_fails() throws IOException {
      // Arrange
      String base = "main";
      String head = "feature";

      try (MockedStatic<ProcessBuilder> processBuilderMock = mockStatic(ProcessBuilder.class)) {
        ProcessBuilder realProcessBuilder = mock(ProcessBuilder.class);
        when(realProcessBuilder.redirectErrorStream(true)).thenReturn(realProcessBuilder);
        when(realProcessBuilder.start()).thenThrow(new IOException("Cannot start process"));

        processBuilderMock
            .when(() -> new ProcessBuilder("git", "diff", "--unified=" + contextLines, base, head))
            .thenReturn(realProcessBuilder);

        // Act & Assert
        RuntimeException exception =
            assertThrows(
                RuntimeException.class,
                () -> diffCollectionService.collectFromLocalGit(base, head));

        assertTrue(exception.getMessage().contains("Git diff command failed for main..feature"));
        assertTrue(exception.getCause() instanceof IOException);
      }
    }

    @Test
    @DisplayName("Should throw RuntimeException when process reading fails")
    void should_throw_exception_when_process_reading_fails() throws Exception {
      // Arrange
      String base = "main";
      String head = "feature";

      when(mockProcess.getInputStream()).thenThrow(new IOException("Cannot read process output"));

      try (MockedStatic<ProcessBuilder> processBuilderMock = mockStatic(ProcessBuilder.class)) {
        ProcessBuilder realProcessBuilder = mock(ProcessBuilder.class);
        when(realProcessBuilder.redirectErrorStream(true)).thenReturn(realProcessBuilder);
        when(realProcessBuilder.start()).thenReturn(mockProcess);

        processBuilderMock
            .when(() -> new ProcessBuilder("git", "diff", "--unified=" + contextLines, base, head))
            .thenReturn(realProcessBuilder);

        // Act & Assert
        RuntimeException exception =
            assertThrows(
                RuntimeException.class,
                () -> diffCollectionService.collectFromLocalGit(base, head));

        assertTrue(exception.getMessage().contains("Git diff command failed for main..feature"));
        assertTrue(exception.getCause() instanceof IOException);
      }
    }

    @Test
    @DisplayName("Should throw RuntimeException when process wait is interrupted")
    void should_throw_exception_when_process_wait_interrupted() throws Exception {
      // Arrange
      String base = "main";
      String head = "feature";

      InputStream diffStream =
          new ByteArrayInputStream(sampleDiff.getBytes(StandardCharsets.UTF_8));
      when(mockProcess.getInputStream()).thenReturn(diffStream);
      when(mockProcess.waitFor()).thenThrow(new InterruptedException("Wait interrupted"));

      try (MockedStatic<ProcessBuilder> processBuilderMock = mockStatic(ProcessBuilder.class)) {
        ProcessBuilder realProcessBuilder = mock(ProcessBuilder.class);
        when(realProcessBuilder.redirectErrorStream(true)).thenReturn(realProcessBuilder);
        when(realProcessBuilder.start()).thenReturn(mockProcess);

        processBuilderMock
            .when(() -> new ProcessBuilder("git", "diff", "--unified=" + contextLines, base, head))
            .thenReturn(realProcessBuilder);

        // Act & Assert
        RuntimeException exception =
            assertThrows(
                RuntimeException.class,
                () -> diffCollectionService.collectFromLocalGit(base, head));

        assertTrue(exception.getMessage().contains("Git diff command failed for main..feature"));
        assertTrue(exception.getCause() instanceof InterruptedException);
      }
    }
  }

  @Nested
  @DisplayName("DiffBundle Creation Tests")
  class DiffBundleCreationTests {

    @Test
    @DisplayName("Should create DiffBundle with parsed and raw diff")
    void should_create_diff_bundle_with_parsed_and_raw_diff() {
      // Arrange
      when(mockGithubClient.fetchPrUnifiedDiff(123, contextLines)).thenReturn(sampleDiff);

      // Act
      DiffAnalysisBundle result = diffCollectionService.collectFromGitHub(mockGithubClient, 123);

      // Assert
      assertNotNull(result);
      assertNotNull(result.structuredDiff()); // Parsed diff
      assertNotNull(result.rawDiffText()); // Raw diff
      assertEquals(sampleDiff, result.rawDiffText());

      // Verify parsing worked correctly
      GitDiffDocument parsed = result.structuredDiff();
      assertEquals(1, parsed.files.size());
      assertEquals("file.txt", parsed.files.get(0).oldPath);
      assertEquals("file.txt", parsed.files.get(0).newPath);
    }

    @Test
    @DisplayName("Should create DiffBundle with malformed diff")
    void should_create_diff_bundle_with_malformed_diff() {
      // Arrange
      String malformedDiff = "This is not a valid diff format";
      when(mockGithubClient.fetchPrUnifiedDiff(123, contextLines)).thenReturn(malformedDiff);

      // Act
      DiffAnalysisBundle result = diffCollectionService.collectFromGitHub(mockGithubClient, 123);

      // Assert
      assertNotNull(result);
      assertEquals(malformedDiff, result.rawDiffText());
      // Parser should handle malformed input gracefully
      assertNotNull(result.structuredDiff());
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should handle real-world GitHub diff scenario")
    void should_handle_real_world_github_scenario() {
      // Arrange
      String realWorldDiff =
          """
                diff --git a/src/main/java/Service.java b/src/main/java/Service.java
                index 1234567..abcdefg 100644
                --- a/src/main/java/Service.java
                +++ b/src/main/java/Service.java
                @@ -1,5 +1,6 @@
                 public class Service {
                     public void method() {
                -        System.out.println("old");
                +        System.out.println("new");
                +        System.out.println("added");
                     }
                 }
                """;

      when(mockGithubClient.fetchPrUnifiedDiff(456, contextLines)).thenReturn(realWorldDiff);

      // Act
      DiffAnalysisBundle result = diffCollectionService.collectFromGitHub(mockGithubClient, 456);

      // Assert
      assertNotNull(result);
      assertEquals(realWorldDiff, result.rawDiffText());
      assertEquals(1, result.structuredDiff().files.size());
      assertEquals("src/main/java/Service.java", result.structuredDiff().files.get(0).newPath);
      assertEquals(1, result.structuredDiff().files.get(0).diffHunkBlocks.size());
    }

    @Test
    @DisplayName("Should handle different context lines configurations")
    void should_handle_different_context_lines_configurations() {
      // Test with different context line values
      int[] contextLineValues = {0, 1, 3, 5, 10};

      for (int contextLines : contextLineValues) {
        DiffCollectionService service = new DiffCollectionService(contextLines);

        when(mockGithubClient.fetchPrUnifiedDiff(123, contextLines)).thenReturn(sampleDiff);

        DiffAnalysisBundle result = service.collectFromGitHub(mockGithubClient, 123);

        assertNotNull(result, "Should work with context lines: " + contextLines);
        assertEquals(sampleDiff, result.rawDiffText());

        verify(mockGithubClient).fetchPrUnifiedDiff(123, contextLines);
      }
    }
  }

  @Nested
  @DisplayName("Edge Cases Tests")
  class EdgeCasesTests {

    @Test
    @DisplayName("Should handle null or invalid commit references")
    @Disabled
    void should_handle_invalid_commit_references() throws Exception {
      // Arrange
      String base = "invalid-commit";
      String head = "another-invalid";
      String gitError = "fatal: bad revision 'invalid-commit'";

      InputStream errorStream = new ByteArrayInputStream(gitError.getBytes(StandardCharsets.UTF_8));
      when(mockProcess.getInputStream()).thenReturn(errorStream);
      when(mockProcess.waitFor()).thenReturn(128); // Git error exit code

      try (MockedStatic<ProcessBuilder> processBuilderMock = mockStatic(ProcessBuilder.class)) {
        ProcessBuilder realProcessBuilder = mock(ProcessBuilder.class);
        when(realProcessBuilder.redirectErrorStream(true)).thenReturn(realProcessBuilder);
        when(realProcessBuilder.start()).thenReturn(mockProcess);

        processBuilderMock
            .when(() -> new ProcessBuilder("git", "diff", "--unified=" + contextLines, base, head))
            .thenReturn(realProcessBuilder);

        // Act
        DiffAnalysisBundle result = diffCollectionService.collectFromLocalGit(base, head);

        // Assert
        assertNotNull(result);
        assertEquals(gitError, result.getUnifiedDiffString());
        // Parser should handle git error output gracefully
        assertNotNull(result.structuredDiff());
      }
    }

    @Test
    @DisplayName("Should handle very large PR numbers")
    void should_handle_large_pr_numbers() {
      // Arrange
      int largePrNumber = Integer.MAX_VALUE;
      when(mockGithubClient.fetchPrUnifiedDiff(largePrNumber, contextLines)).thenReturn(sampleDiff);

      // Act & Assert
      assertDoesNotThrow(
          () -> diffCollectionService.collectFromGitHub(mockGithubClient, largePrNumber));
    }

    @Test
    @DisplayName("Should handle binary file diffs")
    void should_handle_binary_file_diffs() {
      // Arrange
      String binaryDiff =
          """
                diff --git a/image.png b/image.png
                index 1234567..abcdefg 100644
                Binary files a/image.png and b/image.png differ
                """;

      when(mockGithubClient.fetchPrUnifiedDiff(789, contextLines)).thenReturn(binaryDiff);

      // Act
      DiffAnalysisBundle result = diffCollectionService.collectFromGitHub(mockGithubClient, 789);

      // Assert
      assertNotNull(result);
      assertEquals(binaryDiff, result.rawDiffText());
      assertNotNull(result.structuredDiff());
    }
  }
}
