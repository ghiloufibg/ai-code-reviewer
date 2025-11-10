package com.ghiloufi.aicode.core.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.gitlab4j.api.models.Diff;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GitLabDiffBuilder")
final class GitLabDiffBuilderTest {

  private GitLabDiffBuilder diffBuilder;

  @BeforeEach
  final void setUp() {
    diffBuilder = new GitLabDiffBuilder();
  }

  @Nested
  @DisplayName("when building raw diff from GitLab diffs")
  final class BuildRawDiff {

    @Test
    @DisplayName("should_build_empty_diff_when_list_is_empty")
    final void should_build_empty_diff_when_list_is_empty() {
      final List<Diff> diffs = List.of();

      final String result = diffBuilder.buildRawDiff(diffs);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should_build_diff_with_single_file")
    final void should_build_diff_with_single_file() {
      final Diff diff = createDiff("old/path.java", "new/path.java", "@@ -1,3 +1,4 @@\n-old\n+new");

      final String result = diffBuilder.buildRawDiff(List.of(diff));

      assertThat(result).contains("diff --git a/old/path.java b/new/path.java");
      assertThat(result).contains("--- a/old/path.java");
      assertThat(result).contains("+++ b/new/path.java");
      assertThat(result).contains("@@ -1,3 +1,4 @@");
      assertThat(result).contains("-old");
      assertThat(result).contains("+new");
    }

    @Test
    @DisplayName("should_build_diff_with_multiple_files")
    final void should_build_diff_with_multiple_files() {
      final Diff diff1 = createDiff("file1.java", "file1.java", "@@ -1 +1 @@\n-old1\n+new1");
      final Diff diff2 = createDiff("file2.java", "file2.java", "@@ -1 +1 @@\n-old2\n+new2");

      final String result = diffBuilder.buildRawDiff(List.of(diff1, diff2));

      assertThat(result).contains("diff --git a/file1.java b/file1.java");
      assertThat(result).contains("diff --git a/file2.java b/file2.java");
      assertThat(result).contains("-old1");
      assertThat(result).contains("+new1");
      assertThat(result).contains("-old2");
      assertThat(result).contains("+new2");
    }

    @Test
    @DisplayName("should_handle_diff_with_null_content")
    final void should_handle_diff_with_null_content() {
      final Diff diff = createDiff("path.java", "path.java", null);

      final String result = diffBuilder.buildRawDiff(List.of(diff));

      assertThat(result).contains("diff --git a/path.java b/path.java");
      assertThat(result).contains("--- a/path.java");
      assertThat(result).contains("+++ b/path.java");
      assertThat(result).doesNotContain("@@");
    }

    @Test
    @DisplayName("should_handle_renamed_file")
    final void should_handle_renamed_file() {
      final Diff diff =
          createDiff("old/OldName.java", "new/NewName.java", "@@ -1 +1 @@\n class renamed");

      final String result = diffBuilder.buildRawDiff(List.of(diff));

      assertThat(result).contains("diff --git a/old/OldName.java b/new/NewName.java");
      assertThat(result).contains("--- a/old/OldName.java");
      assertThat(result).contains("+++ b/new/NewName.java");
    }

    private Diff createDiff(final String oldPath, final String newPath, final String diffContent) {
      final Diff diff = new Diff();
      diff.setOldPath(oldPath);
      diff.setNewPath(newPath);
      diff.setDiff(diffContent);
      return diff;
    }
  }
}
