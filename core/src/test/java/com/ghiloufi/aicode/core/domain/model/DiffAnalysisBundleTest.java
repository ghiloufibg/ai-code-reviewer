package com.ghiloufi.aicode.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DiffAnalysisBundle Tests")
final class DiffAnalysisBundleTest {

  @Nested
  @DisplayName("Construction")
  final class Construction {

    @Test
    @DisplayName("should_create_bundle_with_repository_identifier")
    final void should_create_bundle_with_repository_identifier() {
      final RepositoryIdentifier repo =
          RepositoryIdentifier.create(SourceProvider.GITLAB, "test-owner/test-repo");
      final GitFileModification modification =
          new GitFileModification("src/Test.java", "src/Test.java");
      final GitDiffDocument gitDiff = new GitDiffDocument(List.of(modification));
      final String rawDiff = "diff --git a/src/Test.java b/src/Test.java";

      final DiffAnalysisBundle bundle = new DiffAnalysisBundle(repo, gitDiff, rawDiff, null, null);

      assertThat(bundle.repositoryIdentifier()).isEqualTo(repo);
      assertThat(bundle.structuredDiff()).isEqualTo(gitDiff);
      assertThat(bundle.rawDiffText()).isEqualTo(rawDiff);
    }

    @Test
    @DisplayName("should_reject_null_repository_identifier")
    final void should_reject_null_repository_identifier() {
      final GitFileModification modification =
          new GitFileModification("src/Test.java", "src/Test.java");
      final GitDiffDocument gitDiff = new GitDiffDocument(List.of(modification));
      final String rawDiff = "diff --git a/src/Test.java b/src/Test.java";

      assertThatThrownBy(() -> new DiffAnalysisBundle(null, gitDiff, rawDiff, null, null))
          .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("should_reject_null_structured_diff")
    final void should_reject_null_structured_diff() {
      final RepositoryIdentifier repo =
          RepositoryIdentifier.create(SourceProvider.GITLAB, "test-owner/test-repo");
      final String rawDiff = "diff --git a/src/Test.java b/src/Test.java";

      assertThatThrownBy(() -> new DiffAnalysisBundle(repo, null, rawDiff, null, null))
          .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("should_reject_null_raw_diff")
    final void should_reject_null_raw_diff() {
      final RepositoryIdentifier repo =
          RepositoryIdentifier.create(SourceProvider.GITLAB, "test-owner/test-repo");
      final GitFileModification modification =
          new GitFileModification("src/Test.java", "src/Test.java");
      final GitDiffDocument gitDiff = new GitDiffDocument(List.of(modification));

      assertThatThrownBy(() -> new DiffAnalysisBundle(repo, gitDiff, null, null, null))
          .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("should_reject_empty_raw_diff")
    final void should_reject_empty_raw_diff() {
      final RepositoryIdentifier repo =
          RepositoryIdentifier.create(SourceProvider.GITLAB, "test-owner/test-repo");
      final GitFileModification modification =
          new GitFileModification("src/Test.java", "src/Test.java");
      final GitDiffDocument gitDiff = new GitDiffDocument(List.of(modification));

      assertThatThrownBy(() -> new DiffAnalysisBundle(repo, gitDiff, "", null, null))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should_reject_whitespace_only_raw_diff")
    final void should_reject_whitespace_only_raw_diff() {
      final RepositoryIdentifier repo =
          RepositoryIdentifier.create(SourceProvider.GITLAB, "test-owner/test-repo");
      final GitFileModification modification =
          new GitFileModification("src/Test.java", "src/Test.java");
      final GitDiffDocument gitDiff = new GitDiffDocument(List.of(modification));

      assertThatThrownBy(() -> new DiffAnalysisBundle(repo, gitDiff, "   ", null, null))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("Business Logic")
  final class BusinessLogic {

    @Test
    @DisplayName("should_calculate_total_line_count")
    final void should_calculate_total_line_count() {
      final RepositoryIdentifier repo =
          RepositoryIdentifier.create(SourceProvider.GITLAB, "test-owner/test-repo");
      final GitFileModification modification =
          new GitFileModification("src/Test.java", "src/Test.java");
      final DiffHunkBlock hunk = new DiffHunkBlock();
      hunk.lines.add("+new line");
      hunk.lines.add("-old line");
      modification.diffHunkBlocks.add(hunk);

      final GitDiffDocument gitDiff = new GitDiffDocument(List.of(modification));
      final String rawDiff = "diff --git a/src/Test.java b/src/Test.java";

      final DiffAnalysisBundle bundle = new DiffAnalysisBundle(repo, gitDiff, rawDiff, null, null);

      assertThat(bundle.getTotalLineCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("should_calculate_modified_file_count")
    final void should_calculate_modified_file_count() {
      final RepositoryIdentifier repo =
          RepositoryIdentifier.create(SourceProvider.GITLAB, "test-owner/test-repo");
      final GitFileModification mod1 = new GitFileModification("src/Test1.java", "src/Test1.java");
      final GitFileModification mod2 = new GitFileModification("src/Test2.java", "src/Test2.java");

      final GitDiffDocument gitDiff = new GitDiffDocument(List.of(mod1, mod2));
      final String rawDiff = "diff --git a/src/Test.java b/src/Test.java";

      final DiffAnalysisBundle bundle = new DiffAnalysisBundle(repo, gitDiff, rawDiff, null, null);

      assertThat(bundle.getModifiedFileCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("should_generate_summary")
    final void should_generate_summary() {
      final RepositoryIdentifier repo =
          RepositoryIdentifier.create(SourceProvider.GITLAB, "test-owner/test-repo");
      final GitFileModification modification =
          new GitFileModification("src/Test.java", "src/Test.java");
      final DiffHunkBlock hunk = new DiffHunkBlock();
      hunk.lines.add("+new line");
      modification.diffHunkBlocks.add(hunk);

      final GitDiffDocument gitDiff = new GitDiffDocument(List.of(modification));
      final String rawDiff = "diff --git a/src/Test.java b/src/Test.java";

      final DiffAnalysisBundle bundle = new DiffAnalysisBundle(repo, gitDiff, rawDiff, null, null);

      assertThat(bundle.getSummary()).contains("1 fichier(s)");
      assertThat(bundle.getSummary()).contains("1 ligne(s)");
    }
  }
}
