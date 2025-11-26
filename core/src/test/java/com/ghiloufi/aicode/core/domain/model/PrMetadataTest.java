package com.ghiloufi.aicode.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class PrMetadataTest {

  @Test
  void should_create_valid_pr_metadata() {
    final var commits =
        List.of(new CommitInfo("abc123", "Fix bug", "author", Instant.now(), List.of()));
    final var metadata =
        new PrMetadata(
            "Fix authentication",
            "This PR fixes the auth bug",
            "john@example.com",
            "main",
            "feature/auth-fix",
            List.of("bug", "security"),
            commits,
            5);

    assertThat(metadata.title()).isEqualTo("Fix authentication");
    assertThat(metadata.description()).isEqualTo("This PR fixes the auth bug");
    assertThat(metadata.author()).isEqualTo("john@example.com");
    assertThat(metadata.baseBranch()).isEqualTo("main");
    assertThat(metadata.headBranch()).isEqualTo("feature/auth-fix");
    assertThat(metadata.labels()).containsExactly("bug", "security");
    assertThat(metadata.commits()).hasSize(1);
    assertThat(metadata.changedFilesCount()).isEqualTo(5);
  }

  @Test
  void should_create_empty_metadata() {
    final var metadata = PrMetadata.empty();

    assertThat(metadata.hasTitle()).isFalse();
    assertThat(metadata.hasDescription()).isFalse();
    assertThat(metadata.hasAuthor()).isFalse();
    assertThat(metadata.hasLabels()).isFalse();
    assertThat(metadata.hasCommits()).isFalse();
    assertThat(metadata.changedFilesCount()).isEqualTo(0);
  }

  @Test
  void should_handle_null_labels() {
    final var metadata = new PrMetadata("title", null, null, null, null, null, null, 0);

    assertThat(metadata.labels()).isEmpty();
    assertThat(metadata.hasLabels()).isFalse();
  }

  @Test
  void should_handle_null_commits() {
    final var metadata = new PrMetadata("title", null, null, null, null, null, null, 0);

    assertThat(metadata.commits()).isEmpty();
    assertThat(metadata.hasCommits()).isFalse();
  }

  @Test
  void should_throw_when_changed_files_count_is_negative() {
    assertThatThrownBy(
            () -> new PrMetadata("title", null, null, null, null, List.of(), List.of(), -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Changed files count cannot be negative");
  }

  @Test
  void should_detect_has_title() {
    final var withTitle = new PrMetadata("Title", null, null, null, null, null, null, 0);
    final var withoutTitle = new PrMetadata(null, null, null, null, null, null, null, 0);
    final var withBlankTitle = new PrMetadata("   ", null, null, null, null, null, null, 0);

    assertThat(withTitle.hasTitle()).isTrue();
    assertThat(withoutTitle.hasTitle()).isFalse();
    assertThat(withBlankTitle.hasTitle()).isFalse();
  }

  @Test
  void should_detect_has_description() {
    final var withDesc = new PrMetadata(null, "Description", null, null, null, null, null, 0);
    final var withoutDesc = new PrMetadata(null, null, null, null, null, null, null, 0);

    assertThat(withDesc.hasDescription()).isTrue();
    assertThat(withoutDesc.hasDescription()).isFalse();
  }

  @Test
  void should_detect_has_author() {
    final var withAuthor = new PrMetadata(null, null, "author", null, null, null, null, 0);
    final var withoutAuthor = new PrMetadata(null, null, null, null, null, null, null, 0);

    assertThat(withAuthor.hasAuthor()).isTrue();
    assertThat(withoutAuthor.hasAuthor()).isFalse();
  }

  @Test
  void should_return_branch_info() {
    final var metadata = new PrMetadata(null, null, null, "main", "feature/test", null, null, 0);

    assertThat(metadata.branchInfo()).isEqualTo("feature/test → main");
  }

  @Test
  void should_return_empty_branch_info_when_branches_null() {
    final var metadata = new PrMetadata(null, null, null, null, null, null, null, 0);

    assertThat(metadata.branchInfo()).isEmpty();
  }

  @Test
  void should_make_defensive_copy_of_labels() {
    final var mutableLabels = new ArrayList<>(List.of("label1"));
    final var metadata = new PrMetadata(null, null, null, null, null, mutableLabels, null, 0);

    mutableLabels.clear();

    assertThat(metadata.labels()).hasSize(1);
  }

  @Test
  void should_make_defensive_copy_of_commits() {
    final var commit = new CommitInfo("abc", "msg", null, null, List.of());
    final var mutableCommits = new ArrayList<>(List.of(commit));
    final var metadata = new PrMetadata(null, null, null, null, null, null, mutableCommits, 0);

    mutableCommits.clear();

    assertThat(metadata.commits()).hasSize(1);
  }
}
