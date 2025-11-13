package com.ghiloufi.aicode.core.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.DiffHunkBlock;
import com.ghiloufi.aicode.core.domain.model.GitDiffDocument;
import com.ghiloufi.aicode.core.domain.model.GitFileModification;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DiffFormatter Tests")
class DiffFormatterTest {

  private DiffFormatter formatter;

  @BeforeEach
  void setUp() {
    formatter = new DiffFormatter();
  }

  @Test
  @DisplayName("should_calculate_absolute_line_numbers_correctly")
  void should_calculate_absolute_line_numbers_correctly() {
    final GitFileModification file = new GitFileModification("src/Test.java", "src/Test.java");
    final DiffHunkBlock hunk = new DiffHunkBlock(98, 3, 100, 4);
    hunk.lines = List.of("+added line", " context line", "+added line 2", " context line 2");
    file.diffHunkBlocks = List.of(hunk);

    final GitDiffDocument diff = new GitDiffDocument(List.of(file));

    final String formatted = formatter.formatDiff(diff);

    assertThat(formatted).contains("100  │ + added line");
    assertThat(formatted).contains("101  │   context line");
    assertThat(formatted).contains("102  │ + added line 2");
    assertThat(formatted).contains("103  │   context line 2");
  }

  @Test
  @DisplayName("should_handle_removed_lines_correctly")
  void should_handle_removed_lines_correctly() {
    final GitFileModification file = new GitFileModification("src/Test.java", "src/Test.java");
    final DiffHunkBlock hunk = new DiffHunkBlock(50, 4, 50, 2);
    hunk.lines = List.of(" context", "-removed line 1", "-removed line 2", " context");
    file.diffHunkBlocks = List.of(hunk);

    final GitDiffDocument diff = new GitDiffDocument(List.of(file));

    final String formatted = formatter.formatDiff(diff);

    assertThat(formatted).contains("50   │   context");
    assertThat(formatted).contains("│ - removed line 1");
    assertThat(formatted).contains("│ - removed line 2");
    assertThat(formatted).contains("51   │   context");
  }

  @Test
  @DisplayName("should_format_multiple_files_with_separators")
  void should_format_multiple_files_with_separators() {
    final GitFileModification file1 = new GitFileModification("src/File1.java", "src/File1.java");
    final DiffHunkBlock hunk1 = new DiffHunkBlock(10, 1, 10, 1);
    hunk1.lines = List.of(" line 10");
    file1.diffHunkBlocks = List.of(hunk1);

    final GitFileModification file2 = new GitFileModification("src/File2.java", "src/File2.java");
    final DiffHunkBlock hunk2 = new DiffHunkBlock(20, 1, 20, 1);
    hunk2.lines = List.of(" line 20");
    file2.diffHunkBlocks = List.of(hunk2);

    final GitFileModification file3 = new GitFileModification("src/File3.java", "src/File3.java");
    final DiffHunkBlock hunk3 = new DiffHunkBlock(30, 1, 30, 1);
    hunk3.lines = List.of(" line 30");
    file3.diffHunkBlocks = List.of(hunk3);

    final GitDiffDocument diff = new GitDiffDocument(List.of(file1, file2, file3));

    final String formatted = formatter.formatDiff(diff);

    assertThat(formatted).contains("FILE: src/File1.java");
    assertThat(formatted).contains("FILE: src/File2.java");
    assertThat(formatted).contains("FILE: src/File3.java");
    assertThat(formatted.split("={80}")).hasSizeGreaterThanOrEqualTo(6);
  }

  @Test
  @DisplayName("should_indicate_file_status")
  void should_indicate_file_status() {
    final GitFileModification newFile = new GitFileModification("/dev/null", "src/New.java");
    final DiffHunkBlock hunk1 = new DiffHunkBlock(0, 0, 1, 2);
    hunk1.lines = List.of("+new line 1", "+new line 2");
    newFile.diffHunkBlocks = List.of(hunk1);

    final GitFileModification modifiedFile =
        new GitFileModification("src/Modified.java", "src/Modified.java");
    final DiffHunkBlock hunk2 = new DiffHunkBlock(10, 2, 10, 2);
    hunk2.lines = List.of(" context", "+added");
    modifiedFile.diffHunkBlocks = List.of(hunk2);

    final GitFileModification renamedFile =
        new GitFileModification("src/OldName.java", "src/NewName.java");
    final DiffHunkBlock hunk3 = new DiffHunkBlock(5, 1, 5, 1);
    hunk3.lines = List.of(" line");
    renamedFile.diffHunkBlocks = List.of(hunk3);

    final GitDiffDocument diff = new GitDiffDocument(List.of(newFile, modifiedFile, renamedFile));

    final String formatted = formatter.formatDiff(diff);

    assertThat(formatted).contains("(NEW FILE)");
    assertThat(formatted).contains("(MODIFIED)");
    assertThat(formatted).contains("(RENAMED FROM src/OldName.java)");
  }

  @Test
  @DisplayName("should_handle_multiple_hunks_in_same_file")
  void should_handle_multiple_hunks_in_same_file() {
    final GitFileModification file = new GitFileModification("src/Test.java", "src/Test.java");

    final DiffHunkBlock hunk1 = new DiffHunkBlock(10, 2, 10, 3);
    hunk1.lines = List.of(" context", "+added line 11", " context");

    final DiffHunkBlock hunk2 = new DiffHunkBlock(50, 1, 52, 2);
    hunk2.lines = List.of(" context", "+added line 53");

    final DiffHunkBlock hunk3 = new DiffHunkBlock(100, 2, 103, 2);
    hunk3.lines = List.of(" line 103", " line 104");

    file.diffHunkBlocks = List.of(hunk1, hunk2, hunk3);

    final GitDiffDocument diff = new GitDiffDocument(List.of(file));

    final String formatted = formatter.formatDiff(diff);

    assertThat(formatted).contains("Hunk 1: Lines 10-12");
    assertThat(formatted).contains("Hunk 2: Lines 52-53");
    assertThat(formatted).contains("Hunk 3: Lines 103-104");
    assertThat(formatted).contains("10   │   context");
    assertThat(formatted).contains("11   │ + added line 11");
    assertThat(formatted).contains("52   │   context");
    assertThat(formatted).contains("53   │ + added line 53");
  }

  @Test
  @DisplayName("should_handle_empty_diff_gracefully")
  void should_handle_empty_diff_gracefully() {
    final GitDiffDocument emptyDiff = new GitDiffDocument(List.of());

    final String formatted = formatter.formatDiff(emptyDiff);

    assertThat(formatted).isNotNull();
    assertThat(formatted).contains("No changes");
  }

  @Test
  @DisplayName("should_strip_plus_minus_markers_from_line_content")
  void should_strip_plus_minus_markers_from_line_content() {
    final GitFileModification file = new GitFileModification("src/Test.java", "src/Test.java");
    final DiffHunkBlock hunk = new DiffHunkBlock(10, 3, 10, 3);
    hunk.lines = List.of(" normal line", "+added line", "-removed line");
    file.diffHunkBlocks = List.of(hunk);

    final GitDiffDocument diff = new GitDiffDocument(List.of(file));

    final String formatted = formatter.formatDiff(diff);

    assertThat(formatted).contains("│   normal line");
    assertThat(formatted).contains("│ + added line");
    assertThat(formatted).contains("│ - removed line");
    assertThat(formatted).doesNotContain("│  +added line");
    assertThat(formatted).doesNotContain("│  -removed line");
  }
}
