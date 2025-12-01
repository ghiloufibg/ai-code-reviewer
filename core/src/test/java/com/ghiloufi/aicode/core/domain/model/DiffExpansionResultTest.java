package com.ghiloufi.aicode.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class DiffExpansionResultTest {

  @Test
  void should_create_valid_result() {
    final var file1 = new ExpandedFileContext("src/A.java", "content1", 5, false);
    final var file2 = new ExpandedFileContext("src/B.java", "content2", 10, true);
    final var result = new DiffExpansionResult(List.of(file1, file2), 5, 2, 3, "max files");

    assertThat(result.expandedFiles()).hasSize(2);
    assertThat(result.totalFilesRequested()).isEqualTo(5);
    assertThat(result.filesExpanded()).isEqualTo(2);
    assertThat(result.filesSkipped()).isEqualTo(3);
    assertThat(result.skipReason()).isEqualTo("max files");
  }

  @Test
  void should_create_empty_result() {
    final var result = DiffExpansionResult.empty();

    assertThat(result.hasExpandedFiles()).isFalse();
    assertThat(result.expandedFiles()).isEmpty();
    assertThat(result.totalFilesRequested()).isEqualTo(0);
  }

  @Test
  void should_create_disabled_result() {
    final var result = DiffExpansionResult.disabled();

    assertThat(result.hasExpandedFiles()).isFalse();
    assertThat(result.skipReason()).isEqualTo("Feature disabled");
  }

  @Test
  void should_handle_null_expanded_files() {
    final var result = new DiffExpansionResult(null, 0, 0, 0, null);

    assertThat(result.expandedFiles()).isEmpty();
    assertThat(result.hasExpandedFiles()).isFalse();
  }

  @Test
  void should_throw_when_total_files_requested_is_negative() {
    assertThatThrownBy(() -> new DiffExpansionResult(List.of(), -1, 0, 0, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Total files requested cannot be negative");
  }

  @Test
  void should_throw_when_files_expanded_is_negative() {
    assertThatThrownBy(() -> new DiffExpansionResult(List.of(), 0, -1, 0, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Files expanded cannot be negative");
  }

  @Test
  void should_throw_when_files_skipped_is_negative() {
    assertThatThrownBy(() -> new DiffExpansionResult(List.of(), 0, 0, -1, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Files skipped cannot be negative");
  }

  @Test
  void should_calculate_total_content_length() {
    final var file1 = new ExpandedFileContext("a.java", "12345", 1, false);
    final var file2 = new ExpandedFileContext("b.java", "1234567890", 1, false);
    final var result = new DiffExpansionResult(List.of(file1, file2), 2, 2, 0, null);

    assertThat(result.totalContentLength()).isEqualTo(15);
  }

  @Test
  void should_calculate_total_line_count() {
    final var file1 = new ExpandedFileContext("a.java", "content", 10, false);
    final var file2 = new ExpandedFileContext("b.java", "content", 20, false);
    final var result = new DiffExpansionResult(List.of(file1, file2), 2, 2, 0, null);

    assertThat(result.totalLineCount()).isEqualTo(30);
  }

  @Test
  void should_count_truncated_files() {
    final var file1 = new ExpandedFileContext("a.java", "content", 10, false);
    final var file2 = new ExpandedFileContext("b.java", "content", 20, true);
    final var file3 = new ExpandedFileContext("c.java", "content", 30, true);
    final var result = new DiffExpansionResult(List.of(file1, file2, file3), 3, 3, 0, null);

    assertThat(result.truncatedFileCount()).isEqualTo(2);
  }

  @Test
  void should_make_defensive_copy_of_expanded_files() {
    final var file = new ExpandedFileContext("a.java", "content", 1, false);
    final var mutableList = new ArrayList<>(List.of(file));
    final var result = new DiffExpansionResult(mutableList, 1, 1, 0, null);

    mutableList.clear();

    assertThat(result.expandedFiles()).hasSize(1);
  }
}
