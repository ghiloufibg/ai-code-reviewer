package com.ghiloufi.aicode.core;

import static org.junit.jupiter.api.Assertions.*;

import com.ghiloufi.aicode.domain.FileDiff;
import com.ghiloufi.aicode.domain.Hunk;
import com.ghiloufi.aicode.domain.UnifiedDiff;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("UnifiedDiffParser Tests")
public class UnifiedDiffParserTest {

  private UnifiedDiffParser parser;

  @BeforeEach
  void setUp() {
    parser = new UnifiedDiffParser();
  }

  @Nested
  @DisplayName("Basic Parsing Tests")
  class BasicParsingTests {

    @Test
    @DisplayName("Should parse simple single file diff")
    void should_parse_simple_single_file_diff() {
      String diff =
          """
                --- a/script.py
                +++ b/script.py
                @@ -1,3 +1,3 @@
                 def hello():
                -    print("Hello")
                +    print("Hello, world!")
                """;

      UnifiedDiff result = parser.parse(diff);

      assertEquals(1, result.files.size());
      FileDiff file = result.files.get(0);
      assertEquals("script.py", file.oldPath);
      assertEquals("script.py", file.newPath);
      assertEquals(1, file.hunks.size());

      Hunk hunk = file.hunks.get(0);
      assertEquals(1, hunk.oldStart);
      assertEquals(3, hunk.oldCount);
      assertEquals(1, hunk.newStart);
      assertEquals(3, hunk.newCount);
      assertEquals(3, hunk.lines.size());
    }

    @Test
    @DisplayName("Should parse multiple files diff")
    void should_parse_multiple_files_diff() {
      String diff =
          """
                --- a/file1.txt
                +++ b/file1.txt
                @@ -1,2 +1,2 @@
                 line1
                -old line
                +new line
                --- a/file2.txt
                +++ b/file2.txt
                @@ -1,1 +1,2 @@
                 existing line
                +added line
                """;

      UnifiedDiff result = parser.parse(diff);

      assertEquals(2, result.files.size());
      assertEquals("file1.txt", result.files.get(0).oldPath);
      assertEquals("file2.txt", result.files.get(1).oldPath);
    }

    @Test
    @DisplayName("Should parse multiple hunks in single file")
    void should_parse_multiple_hunks_single_file() {
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

      UnifiedDiff result = parser.parse(diff);

      assertEquals(1, result.files.size());
      assertEquals(2, result.files.get(0).hunks.size());
    }
  }

  @Nested
  @DisplayName("Path Handling Tests")
  class PathHandlingTests {

    @Test
    @DisplayName("Should handle paths with a/ and b/ prefixes")
    void should_handle_paths_with_prefixes() {
      String diff =
          """
                --- a/src/main/java/MyClass.java
                +++ b/src/main/java/MyClass.java
                @@ -1,1 +1,1 @@
                -old content
                +new content
                """;

      UnifiedDiff result = parser.parse(diff);

      FileDiff file = result.files.get(0);
      assertEquals("src/main/java/MyClass.java", file.oldPath);
      assertEquals("src/main/java/MyClass.java", file.newPath);
    }

    @Test
    @DisplayName("Should handle paths without prefixes")
    void should_handle_paths_without_prefixes() {
      String diff =
          """
                --- MyClass.java
                +++ MyClass.java
                @@ -1,1 +1,1 @@
                -old content
                +new content
                """;

      UnifiedDiff result = parser.parse(diff);

      FileDiff file = result.files.get(0);
      assertEquals("MyClass.java", file.oldPath);
      assertEquals("MyClass.java", file.newPath);
    }

    @Test
    @DisplayName("Should handle new file creation")
    void should_handle_new_file_creation() {
      String diff =
          """
                --- /dev/null
                +++ b/new_file.txt
                @@ -0,0 +1,3 @@
                +This is a new file
                +with multiple lines
                +of content
                """;

      UnifiedDiff result = parser.parse(diff);

      FileDiff file = result.files.get(0);
      assertEquals("/dev/null", file.oldPath);
      assertEquals("new_file.txt", file.newPath);
    }

    @Test
    @DisplayName("Should handle file deletion")
    void should_handle_file_deletion() {
      String diff =
          """
                --- a/deleted_file.txt
                +++ /dev/null
                @@ -1,3 +0,0 @@
                -This file will be deleted
                -along with all its content
                -goodbye file
                """;

      UnifiedDiff result = parser.parse(diff);

      FileDiff file = result.files.get(0);
      assertEquals("deleted_file.txt", file.oldPath);
      assertEquals("/dev/null", file.newPath);
    }
  }

  @Nested
  @DisplayName("Hunk Header Parsing Tests")
  class HunkHeaderParsingTests {

    @Test
    @DisplayName("Should parse hunk header with single line counts")
    void should_parse_single_line_counts() {
      String diff =
          """
                --- a/file.txt
                +++ b/file.txt
                @@ -5 +5 @@
                -old line
                +new line
                """;

      UnifiedDiff result = parser.parse(diff);

      Hunk hunk = result.files.get(0).hunks.get(0);
      assertEquals(5, hunk.oldStart);
      assertEquals(1, hunk.oldCount);
      assertEquals(5, hunk.newStart);
      assertEquals(1, hunk.newCount);
    }

    @Test
    @DisplayName("Should parse hunk header with explicit line counts")
    void should_parse_explicit_line_counts() {
      String diff =
          """
                --- a/file.txt
                +++ b/file.txt
                @@ -10,5 +12,7 @@
                 context line
                -removed line 1
                -removed line 2
                +added line 1
                +added line 2
                +added line 3
                 context line
                """;

      UnifiedDiff result = parser.parse(diff);

      Hunk hunk = result.files.get(0).hunks.get(0);
      assertEquals(10, hunk.oldStart);
      assertEquals(5, hunk.oldCount);
      assertEquals(12, hunk.newStart);
      assertEquals(7, hunk.newCount);
    }

    @Test
    @DisplayName("Should parse hunk header with zero line count")
    void should_parse_zero_line_count() {
      String diff =
          """
                --- a/file.txt
                +++ b/file.txt
                @@ -0,0 +1,2 @@
                +new line 1
                +new line 2
                """;

      UnifiedDiff result = parser.parse(diff);

      Hunk hunk = result.files.get(0).hunks.get(0);
      assertEquals(0, hunk.oldStart);
      assertEquals(0, hunk.oldCount);
      assertEquals(1, hunk.newStart);
      assertEquals(2, hunk.newCount);
    }
  }

  @Nested
  @DisplayName("Line Type Tests")
  class LineTypeTests {

    @Test
    @DisplayName("Should handle addition lines")
    void should_handle_addition_lines() {
      String diff =
          """
                --- a/file.txt
                +++ b/file.txt
                @@ -1,1 +1,3 @@
                 existing line
                +added line 1
                +added line 2
                """;

      UnifiedDiff result = parser.parse(diff);

      Hunk hunk = result.files.get(0).hunks.get(0);
      assertEquals(3, hunk.lines.size());
      assertEquals(" existing line", hunk.lines.get(0));
      assertEquals("+added line 1", hunk.lines.get(1));
      assertEquals("+added line 2", hunk.lines.get(2));
    }

    @Test
    @DisplayName("Should handle deletion lines")
    void should_handle_deletion_lines() {
      String diff =
          """
                --- a/file.txt
                +++ b/file.txt
                @@ -1,3 +1,1 @@
                 existing line
                -deleted line 1
                -deleted line 2
                """;

      UnifiedDiff result = parser.parse(diff);

      Hunk hunk = result.files.get(0).hunks.get(0);
      assertEquals(3, hunk.lines.size());
      assertEquals(" existing line", hunk.lines.get(0));
      assertEquals("-deleted line 1", hunk.lines.get(1));
      assertEquals("-deleted line 2", hunk.lines.get(2));
    }

    @Test
    @DisplayName("Should handle context lines")
    void should_handle_context_lines() {
      String diff =
          """
                --- a/file.txt
                +++ b/file.txt
                @@ -1,5 +1,5 @@
                 context line 1
                 context line 2
                -old line
                +new line
                 context line 3
                """;

      UnifiedDiff result = parser.parse(diff);

      Hunk hunk = result.files.get(0).hunks.get(0);
      assertEquals(5, hunk.lines.size());
      assertEquals(" context line 1", hunk.lines.get(0));
      assertEquals(" context line 2", hunk.lines.get(1));
      assertEquals("-old line", hunk.lines.get(2));
      assertEquals("+new line", hunk.lines.get(3));
      assertEquals(" context line 3", hunk.lines.get(4));
    }

    @Test
    @DisplayName("Should handle continuation lines")
    void should_handle_continuation_lines() {
      String diff =
          """
                --- a/file.txt
                +++ b/file.txt
                @@ -1,2 +1,2 @@
                 line without newline
                \\ No newline at end of file
                """;

      UnifiedDiff result = parser.parse(diff);

      Hunk hunk = result.files.get(0).hunks.get(0);
      assertEquals(2, hunk.lines.size());
      assertEquals(" line without newline", hunk.lines.get(0));
      assertEquals("\\ No newline at end of file", hunk.lines.get(1));
    }
  }

  @Nested
  @DisplayName("Edge Cases Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should handle empty diff")
    void should_handle_empty_diff() {
      String diff = "";

      UnifiedDiff result = parser.parse(diff);

      assertEquals(0, result.files.size());
    }

    @Test
    @DisplayName("Should handle diff with only headers")
    void should_handle_diff_with_only_headers() {
      String diff =
          """
                --- a/file.txt
                +++ b/file.txt
                """;

      UnifiedDiff result = parser.parse(diff);

      assertEquals(1, result.files.size());
      assertEquals(0, result.files.get(0).hunks.size());
    }

    @Test
    @DisplayName("Should handle hunk without file headers")
    void should_handle_hunk_without_file_headers() {
      String diff =
          """
                @@ -1,2 +1,3 @@
                 existing line
                +added line
                """;

      UnifiedDiff result = parser.parse(diff);

      assertEquals(1, result.files.size());
      FileDiff file = result.files.get(0);
      assertNull(file.oldPath);
      assertNull(file.newPath);
      assertEquals(1, file.hunks.size());
    }

    @Test
    @DisplayName("Should handle only new file header")
    void should_handle_only_new_file_header() {
      String diff =
          """
                +++ b/file.txt
                @@ -0,0 +1,1 @@
                +new content
                """;

      UnifiedDiff result = parser.parse(diff);

      assertEquals(1, result.files.size());
      FileDiff file = result.files.get(0);
      assertNull(file.oldPath);
      assertEquals("file.txt", file.newPath);
    }

    @Test
    @DisplayName("Should ignore non-diff lines")
    void should_ignore_non_diff_lines() {
      String diff =
          """
                diff --git a/file.txt b/file.txt
                index 1234567..abcdefg 100644
                --- a/file.txt
                +++ b/file.txt
                @@ -1,1 +1,1 @@
                -old line
                +new line
                """;

      UnifiedDiff result = parser.parse(diff);

      assertEquals(1, result.files.size());
      assertEquals(1, result.files.get(0).hunks.size());
      assertEquals(2, result.files.get(0).hunks.get(0).lines.size());
    }

    @Test
    @DisplayName("Should handle large line numbers")
    void should_handle_large_line_numbers() {
      String diff =
          """
                --- a/large_file.txt
                +++ b/large_file.txt
                @@ -99999,5 +100001,3 @@
                 context
                -removed
                -removed
                +added
                 context
                """;

      UnifiedDiff result = parser.parse(diff);

      Hunk hunk = result.files.get(0).hunks.get(0);
      assertEquals(99999, hunk.oldStart);
      assertEquals(5, hunk.oldCount);
      assertEquals(100001, hunk.newStart);
      assertEquals(3, hunk.newCount);
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should handle malformed hunk header gracefully")
    void should_handle_malformed_hunk_header() {
      String diff =
          """
                --- a/file.txt
                +++ b/file.txt
                @@ invalid header @@
                +some content
                """;

      assertThrows(NumberFormatException.class, () -> parser.parse(diff));
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should parse real git diff output")
    void should_parse_real_git_diff_output() {
      String diff =
          """
                diff --git a/script.py b/script.py
                index 5d41402..7d79303 100644
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
                +    print("Multiplication done!")
                @@ -10,4 +11,4 @@
                 def goodbye():
                -    print("Bye")
                +    print("Goodbye!")
                """;

      UnifiedDiff result = parser.parse(diff);

      assertEquals(1, result.files.size());
      FileDiff file = result.files.get(0);
      assertEquals("script.py", file.oldPath);
      assertEquals("script.py", file.newPath);
      assertEquals(3, file.hunks.size());

      Hunk firstHunk = file.hunks.get(0);
      assertEquals(1, firstHunk.oldStart);
      assertEquals(3, firstHunk.oldCount);
      assertEquals(1, firstHunk.newStart);
      assertEquals(3, firstHunk.newCount);

      Hunk secondHunk = file.hunks.get(1);
      assertEquals(4, secondHunk.oldStart);
      assertEquals(5, secondHunk.oldCount);
      assertEquals(4, secondHunk.newStart);
      assertEquals(6, secondHunk.newCount);

      Hunk thirdHunk = file.hunks.get(2);
      assertEquals(10, thirdHunk.oldStart);
      assertEquals(4, thirdHunk.oldCount);
      assertEquals(11, thirdHunk.newStart);
      assertEquals(4, thirdHunk.newCount);
    }
  }
}
