package com.ghiloufi.aicode.core.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHPullRequestFileDetail;

@DisplayName("GitHubDiffBuilder")
final class GitHubDiffBuilderTest {

  private GitHubDiffBuilder diffBuilder;

  @BeforeEach
  final void setUp() {
    diffBuilder = new GitHubDiffBuilder();
  }

  @Nested
  @DisplayName("when building raw diff from GitHub files")
  final class BuildRawDiff {

    @Test
    @DisplayName("should_build_empty_diff_when_list_is_empty")
    final void should_build_empty_diff_when_list_is_empty() {
      final List<GHPullRequestFileDetail> files = List.of();

      final String result = diffBuilder.buildRawDiff(files);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should_build_diff_with_single_file")
    final void should_build_diff_with_single_file() {
      final GHPullRequestFileDetail file =
          createFile("src/Main.java", null, "@@ -1,3 +1,4 @@\n-old\n+new");

      final String result = diffBuilder.buildRawDiff(List.of(file));

      assertThat(result).contains("diff --git a/src/Main.java b/src/Main.java");
      assertThat(result).contains("--- a/src/Main.java");
      assertThat(result).contains("+++ b/src/Main.java");
      assertThat(result).contains("@@ -1,3 +1,4 @@");
      assertThat(result).contains("-old");
      assertThat(result).contains("+new");
    }

    @Test
    @DisplayName("should_build_diff_with_multiple_files")
    final void should_build_diff_with_multiple_files() {
      final GHPullRequestFileDetail file1 =
          createFile("file1.java", null, "@@ -1 +1 @@\n-old1\n+new1");
      final GHPullRequestFileDetail file2 =
          createFile("file2.java", null, "@@ -1 +1 @@\n-old2\n+new2");

      final String result = diffBuilder.buildRawDiff(List.of(file1, file2));

      assertThat(result).contains("diff --git a/file1.java b/file1.java");
      assertThat(result).contains("diff --git a/file2.java b/file2.java");
      assertThat(result).contains("-old1");
      assertThat(result).contains("+new1");
      assertThat(result).contains("-old2");
      assertThat(result).contains("+new2");
    }

    @Test
    @DisplayName("should_handle_file_with_null_patch")
    final void should_handle_file_with_null_patch() {
      final GHPullRequestFileDetail file = createFile("binary.png", null, null);

      final String result = diffBuilder.buildRawDiff(List.of(file));

      assertThat(result).contains("diff --git a/binary.png b/binary.png");
      assertThat(result).contains("--- a/binary.png");
      assertThat(result).contains("+++ b/binary.png");
      assertThat(result).doesNotContain("@@");
    }

    @Test
    @DisplayName("should_handle_renamed_file")
    final void should_handle_renamed_file() {
      final GHPullRequestFileDetail file =
          createFile("NewName.java", "OldName.java", "@@ -1 +1 @@\n class renamed");

      final String result = diffBuilder.buildRawDiff(List.of(file));

      assertThat(result).contains("diff --git a/NewName.java b/NewName.java");
      assertThat(result).contains("--- a/OldName.java");
      assertThat(result).contains("+++ b/NewName.java");
    }

    @Test
    @DisplayName("should_use_current_filename_when_previous_filename_is_null")
    final void should_use_current_filename_when_previous_filename_is_null() {
      final GHPullRequestFileDetail file = createFile("NewFile.java", null, "@@ -0,0 +1 @@\n+new");

      final String result = diffBuilder.buildRawDiff(List.of(file));

      assertThat(result).contains("--- a/NewFile.java");
      assertThat(result).contains("+++ b/NewFile.java");
    }

    private GHPullRequestFileDetail createFile(
        final String filename, final String previousFilename, final String patch) {
      final GHPullRequestFileDetail file = mock(GHPullRequestFileDetail.class);
      when(file.getFilename()).thenReturn(filename);
      when(file.getPreviousFilename()).thenReturn(previousFilename);
      when(file.getPatch()).thenReturn(patch);
      return file;
    }
  }
}
