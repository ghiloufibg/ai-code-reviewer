package com.ghiloufi.aicode.client.github;

import static org.junit.jupiter.api.Assertions.*;

import com.ghiloufi.aicode.domain.model.DiffHunkBlock;
import com.ghiloufi.aicode.domain.model.GitDiffDocument;
import com.ghiloufi.aicode.domain.model.GitFileModification;
import com.ghiloufi.aicode.client.github.GitHubDiffPositionMapper;
import com.ghiloufi.aicode.service.diff.UnifiedDiffParser;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GitHubDiffPositionMapper Tests")
public class GitHubDiffPositionMapperTest {

  private UnifiedDiffParser parser;

  @BeforeEach
  void setUp() {
    parser = new UnifiedDiffParser();
  }

  private GitDiffDocument createTestDiff() {
    GitDiffDocument diff = new GitDiffDocument();
    diff.files = new ArrayList<>();
    return diff;
  }

  private GitFileModification createFileDiff(String oldPath, String newPath) {
    GitFileModification file = new GitFileModification();
    file.oldPath = oldPath;
    file.newPath = newPath;
    file.diffHunkBlocks = new ArrayList<>();
    return file;
  }

  private DiffHunkBlock createHunk(
      int oldStart, int oldCount, int newStart, int newCount, String... lines) {
    DiffHunkBlock diffHunkBlock = new DiffHunkBlock();
    diffHunkBlock.oldStart = oldStart;
    diffHunkBlock.oldCount = oldCount;
    diffHunkBlock.newStart = newStart;
    diffHunkBlock.newCount = newCount;
    diffHunkBlock.lines = new ArrayList<>();
    for (String line : lines) {
      diffHunkBlock.lines.add(line);
    }
    return diffHunkBlock;
  }

  @Nested
  @DisplayName("Basic Position Mapping Tests")
  class BasicPositionMappingTests {

    @Test
    @DisplayName("Should find position for simple addition in single file")
    void should_find_position_for_simple_addition() {
      String diff =
          """
                --- a/file.txt
                +++ b/file.txt
                @@ -1,2 +1,3 @@
                 line 1
                +added line
                 line 2
                """;

      GitDiffDocument parsedDiff = parser.parse(diff);
      GitHubDiffPositionMapper mapper = new GitHubDiffPositionMapper(parsedDiff);

      // Position counting: hunk header (1) + line 1 (2) + added line (3)
      assertEquals(3, mapper.positionFor("file.txt", 2)); // The added line becomes line 2
    }

    @Test
    @DisplayName("Should find position for context line")
    void should_find_position_for_context_line() {
      String diff =
          """
                --- a/file.txt
                +++ b/file.txt
                @@ -1,3 +1,3 @@
                 line 1
                -old line
                +new line
                 line 3
                """;

      GitDiffDocument parsedDiff = parser.parse(diff);
      GitHubDiffPositionMapper mapper = new GitHubDiffPositionMapper(parsedDiff);

      // Position for line 1 (context): hunk header (1) + line 1 (2)
      assertEquals(2, mapper.positionFor("file.txt", 1));
      // Position for line 2 (replacement): hunk header (1) + line 1 (2) + old line (3) + new line
      // (4)
      assertEquals(4, mapper.positionFor("file.txt", 2));
      // Position for line 3 (context): hunk header (1) + ... + line 3 (5)
      assertEquals(5, mapper.positionFor("file.txt", 3));
    }

    @Test
    @DisplayName("Should find position for deletion context")
    void should_find_position_for_deletion_context() {
      String diff =
          """
                --- a/file.txt
                +++ b/file.txt
                @@ -1,3 +1,2 @@
                 line 1
                -deleted line
                 line 2
                """;

      GitDiffDocument parsedDiff = parser.parse(diff);
      GitHubDiffPositionMapper mapper = new GitHubDiffPositionMapper(parsedDiff);

      // Line 1: hunk header (1) + line 1 (2)
      assertEquals(2, mapper.positionFor("file.txt", 1));
      // Line 2 (was line 3): hunk header (1) + line 1 (2) + deleted line (3) + line 2 (4)
      assertEquals(4, mapper.positionFor("file.txt", 2));
    }
  }

  @Nested
  @DisplayName("Multiple Files Tests")
  class MultipleFilesTests {

    @Test
    @DisplayName("Should find position in second file")
    void should_find_position_in_second_file() {
      String diff =
          """
                --- a/file1.txt
                +++ b/file1.txt
                @@ -1,2 +1,2 @@
                 line 1
                -old line
                +new line
                --- a/file2.txt
                +++ b/file2.txt
                @@ -1,1 +1,2 @@
                 existing line
                +added line
                """;

      GitDiffDocument parsedDiff = parser.parse(diff);
      GitHubDiffPositionMapper mapper = new GitHubDiffPositionMapper(parsedDiff);

      // Position in file2.txt
      // Positions 1-4 are for file1.txt (hunk header + 3 lines)
      // Position 5: file2.txt hunk header
      // Position 6: existing line
      // Position 7: added line
      assertEquals(7, mapper.positionFor("file2.txt", 2)); // The added line
    }

    @Test
    @DisplayName("Should handle position in first file when multiple files exist")
    void should_handle_position_in_first_file() {
      String diff =
          """
                --- a/file1.txt
                +++ b/file1.txt
                @@ -1,1 +1,2 @@
                +added line
                 existing line
                --- a/file2.txt
                +++ b/file2.txt
                @@ -1,1 +1,1 @@
                 unchanged
                """;

      GitDiffDocument parsedDiff = parser.parse(diff);
      GitHubDiffPositionMapper mapper = new GitHubDiffPositionMapper(parsedDiff);

      // Position in file1.txt: hunk header (1) + added line (2)
      assertEquals(2, mapper.positionFor("file1.txt", 1));
    }

    @Test
    @DisplayName("Should skip files correctly when target file is not first")
    void should_skip_files_correctly() {
      String diff =
          """
                --- a/skip1.txt
                +++ b/skip1.txt
                @@ -1,3 +1,3 @@
                 line 1
                 line 2
                 line 3
                --- a/skip2.txt
                +++ b/skip2.txt
                @@ -1,2 +1,3 @@
                 line A
                +line B
                 line C
                --- a/target.txt
                +++ b/target.txt
                @@ -1,1 +1,2 @@
                 target line
                +target added
                """;

      GitDiffDocument parsedDiff = parser.parse(diff);
      GitHubDiffPositionMapper mapper = new GitHubDiffPositionMapper(parsedDiff);

      // Skip1: 4 positions (1 hunk header + 3 lines)
      // Skip2: 4 positions (1 hunk header + 3 lines)
      // Target: position 9 (hunk header) + 1 = 10 (target line) + 1 = 11 (target added)
      assertEquals(11, mapper.positionFor("target.txt", 2));
    }
  }

  @Nested
  @DisplayName("Multiple Hunks Tests")
  class MultipleHunksTests {

    @Test
    @DisplayName("Should find position in second hunk")
    void should_find_position_in_second_hunk() {
      String diff =
          """
                --- a/file.txt
                +++ b/file.txt
                @@ -1,2 +1,2 @@
                 line 1
                -old line 2
                +new line 2
                @@ -5,2 +5,3 @@
                 line 5
                +added line 6
                 line 6
                """;

      GitDiffDocument parsedDiff = parser.parse(diff);
      GitHubDiffPositionMapper mapper = new GitHubDiffPositionMapper(parsedDiff);

      // First hunk: 4 positions (header + 3 lines)
      // Second hunk: header (5) + line 5 (6) + added line 6 (7)
      assertEquals(7, mapper.positionFor("file.txt", 6)); // The added line
    }

    @Test
    @DisplayName("Should handle complex multiple hunks scenario")
    void should_handle_complex_multiple_hunks() {
      String diff =
          """
                --- a/script.py
                +++ b/script.py
                @@ -1,3 +1,3 @@
                 def hello():
                -    print("Hello")
                +    print("Hello, world!")

                @@ -4,5 +4,6 @@
                 def add(a, b):
                -    return a+b
                +    return a + b
                +
                 def multiply(a, b):
                     return a*b
                """;

      GitDiffDocument parsedDiff = parser.parse(diff);
      GitHubDiffPositionMapper mapper = new GitHubDiffPositionMapper(parsedDiff);

      // First hunk positions 1-4
      // Second hunk starts at position 5
      // Line 5 in new file: second hunk header (5) + def add (6) + return a + b (8)
      assertEquals(8, mapper.positionFor("script.py", 5)); // The modified return statement
    }
  }

  @Nested
  @DisplayName("Path Resolution Tests")
  class PathResolutionTests {

    @Test
    @DisplayName("Should use newPath when both paths exist")
    void should_use_new_path_when_both_exist() {
      GitDiffDocument diff = createTestDiff();
      GitFileModification file = createFileDiff("old/path.txt", "new/path.txt");
      file.diffHunkBlocks.add(createHunk(1, 1, 1, 2, " line 1", "+line 2"));
      diff.files.add(file);

      GitHubDiffPositionMapper mapper = new GitHubDiffPositionMapper(diff);

      // Should find using newPath
      assertEquals(3, mapper.positionFor("new/path.txt", 2));
      // Should not find using oldPath
      assertEquals(-1, mapper.positionFor("old/path.txt", 2));
    }

    @Test
    @DisplayName("Should fallback to oldPath when newPath is null")
    void should_fallback_to_old_path_when_new_path_null() {
      GitDiffDocument diff = createTestDiff();
      GitFileModification file = createFileDiff("deleted/file.txt", null);
      file.diffHunkBlocks.add(createHunk(1, 2, 0, 0, "-line 1", "-line 2"));
      diff.files.add(file);

      GitHubDiffPositionMapper mapper = new GitHubDiffPositionMapper(diff);

      // Should find using oldPath since newPath is null
      assertEquals(-1, mapper.positionFor("deleted/file.txt", 1)); // No new lines in deletion
    }

    @Test
    @DisplayName("Should handle file creation (oldPath is /dev/null)")
    void should_handle_file_creation() {
      String diff =
          """
                --- /dev/null
                +++ b/new_file.txt
                @@ -0,0 +1,3 @@
                +line 1
                +line 2
                +line 3
                """;

      GitDiffDocument parsedDiff = parser.parse(diff);
      GitHubDiffPositionMapper mapper = new GitHubDiffPositionMapper(parsedDiff);

      assertEquals(2, mapper.positionFor("new_file.txt", 1)); // First line
      assertEquals(3, mapper.positionFor("new_file.txt", 2)); // Second line
      assertEquals(4, mapper.positionFor("new_file.txt", 3)); // Third line
    }
  }

  @Nested
  @DisplayName("Edge Cases Tests")
  class EdgeCasesTests {

    @Test
    @DisplayName("Should return -1 for non-existent file")
    void should_return_minus_one_for_non_existent_file() {
      String diff =
          """
                --- a/existing.txt
                +++ b/existing.txt
                @@ -1,1 +1,2 @@
                 line 1
                +line 2
                """;

      GitDiffDocument parsedDiff = parser.parse(diff);
      GitHubDiffPositionMapper mapper = new GitHubDiffPositionMapper(parsedDiff);

      assertEquals(-1, mapper.positionFor("non_existent.txt", 1));
    }

    @Test
    @DisplayName("Should return -1 for non-existent line in existing file")
    void should_return_minus_one_for_non_existent_line() {
      String diff =
          """
                --- a/file.txt
                +++ b/file.txt
                @@ -1,2 +1,2 @@
                 line 1
                 line 2
                """;

      GitDiffDocument parsedDiff = parser.parse(diff);
      GitHubDiffPositionMapper mapper = new GitHubDiffPositionMapper(parsedDiff);

      assertEquals(-1, mapper.positionFor("file.txt", 99)); // Line doesn't exist
    }

    @Test
    @DisplayName("Should handle empty diff")
    void should_handle_empty_diff() {
      GitDiffDocument emptyDiff = createTestDiff();
      GitHubDiffPositionMapper mapper = new GitHubDiffPositionMapper(emptyDiff);

      assertEquals(-1, mapper.positionFor("any_file.txt", 1));
    }

    @Test
    @DisplayName("Should handle file with no hunks")
    void should_handle_file_with_no_hunks() {
      GitDiffDocument diff = createTestDiff();
      GitFileModification file = createFileDiff("a/file.txt", "b/file.txt");
      // No hunks added
      diff.files.add(file);

      GitHubDiffPositionMapper mapper = new GitHubDiffPositionMapper(diff);

      assertEquals(-1, mapper.positionFor("file.txt", 1));
    }

    @Test
    @DisplayName("Should handle hunk with only deletions")
    void should_handle_hunk_with_only_deletions() {
      String diff =
          """
                --- a/file.txt
                +++ b/file.txt
                @@ -1,3 +1,1 @@
                 remaining line
                -deleted line 1
                -deleted line 2
                """;

      GitDiffDocument parsedDiff = parser.parse(diff);
      GitHubDiffPositionMapper mapper = new GitHubDiffPositionMapper(parsedDiff);

      // Only the remaining line exists in the new file
      assertEquals(2, mapper.positionFor("file.txt", 1));
      assertEquals(-1, mapper.positionFor("file.txt", 2)); // No second line in new file
    }
  }

  @Nested
  @DisplayName("Line Number Tracking Tests")
  class LineNumberTrackingTests {

    @Test
    @DisplayName("Should correctly track line numbers with mixed operations")
    void should_track_line_numbers_with_mixed_operations() {
      String diff =
          """
                --- a/file.txt
                +++ b/file.txt
                @@ -1,6 +1,7 @@
                 context line 1
                +added line 2
                 context line 3
                -deleted line 4
                 context line 5
                +added line 6
                 context line 7
                """;

      GitDiffDocument parsedDiff = parser.parse(diff);
      GitHubDiffPositionMapper mapper = new GitHubDiffPositionMapper(parsedDiff);

      // Line tracking in new file:
      // 1: context line 1 (position 2)
      // 2: added line 2 (position 3)
      // 3: context line 3 (position 4)
      // 4: context line 5 (position 6, after skipping deleted line)
      // 5: added line 6 (position 7)
      // 6: context line 7 (position 8)

      assertEquals(2, mapper.positionFor("file.txt", 1)); // context line 1
      assertEquals(3, mapper.positionFor("file.txt", 2)); // added line 2
      assertEquals(4, mapper.positionFor("file.txt", 3)); // context line 3
      assertEquals(6, mapper.positionFor("file.txt", 4)); // context line 5 (skipped deletion)
      assertEquals(7, mapper.positionFor("file.txt", 5)); // added line 6
      assertEquals(8, mapper.positionFor("file.txt", 6)); // context line 7
    }

    @Test
    @DisplayName("Should handle line numbers starting from different values")
    void should_handle_different_starting_line_numbers() {
      String diff =
          """
                --- a/file.txt
                +++ b/file.txt
                @@ -10,3 +15,4 @@
                 line 15
                +line 16
                 line 17
                 line 18
                """;

      GitDiffDocument parsedDiff = parser.parse(diff);
      GitHubDiffPositionMapper mapper = new GitHubDiffPositionMapper(parsedDiff);

      assertEquals(2, mapper.positionFor("file.txt", 15)); // First line
      assertEquals(3, mapper.positionFor("file.txt", 16)); // Added line
      assertEquals(4, mapper.positionFor("file.txt", 17)); // Third line
      assertEquals(5, mapper.positionFor("file.txt", 18)); // Fourth line
    }
  }

  @Nested
  @DisplayName("Real-world Integration Tests")
  class RealWorldIntegrationTests {

    @Test
    @DisplayName("Should handle typical GitHub PR diff")
    void should_handle_typical_github_pr_diff() {
      String diff =
          """
                diff --git a/src/main/java/Service.java b/src/main/java/Service.java
                index 1234567..abcdefg 100644
                --- a/src/main/java/Service.java
                +++ b/src/main/java/Service.java
                @@ -1,10 +1,12 @@
                 public class Service {

                     public void method1() {
                -        System.out.println("old");
                +        System.out.println("new");
                +        System.out.println("added");
                     }

                     public void method2() {
                         System.out.println("unchanged");
                +        // Added comment
                     }
                 }
                """;

      GitDiffDocument parsedDiff = parser.parse(diff);
      GitHubDiffPositionMapper mapper = new GitHubDiffPositionMapper(parsedDiff);

      // Position counting:
      // 1: @@ hunk header
      // 2: " public class Service {"           -> new line 1
      // 3: "     "                            -> new line 2
      // 4: "     public void method1() {"     -> new line 3
      // 5: "-        System.out.println("old");" (not counted for new lines)
      // 6: "+        System.out.println("new");" -> new line 4
      // 7: "+        System.out.println("added");" -> new line 5
      // 8: "     }"                           -> new line 6
      // 9: "     "                            -> new line 7
      // 10: "     public void method2() {"    -> new line 8
      // 11: "         System.out.println("unchanged");" -> new line 9
      // 12: "+        // Added comment"       -> new line 10
      // 13: "     }"                         -> new line 11
      // 14: " }"                             -> new line 12

      assertEquals(
          2, mapper.positionFor("src/main/java/Service.java", 1)); // public class Service {
      assertEquals(
          6, mapper.positionFor("src/main/java/Service.java", 4)); // System.out.println("new");
      assertEquals(
          7, mapper.positionFor("src/main/java/Service.java", 5)); // System.out.println("added");
      assertEquals(12, mapper.positionFor("src/main/java/Service.java", 10)); // // Added comment
    }
  }
}
