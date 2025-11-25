package com.ghiloufi.aicode.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

final class CommitInfoTest {

  @Test
  void should_create_valid_commit_info() {
    final var commit =
        new CommitInfo(
            "abc123",
            "feat: add user service",
            "John Doe",
            Instant.now(),
            List.of("UserService.java", "UserRepository.java"));

    assertThat(commit.commitId()).isEqualTo("abc123");
    assertThat(commit.message()).isEqualTo("feat: add user service");
    assertThat(commit.author()).isEqualTo("John Doe");
    assertThat(commit.timestamp()).isNotNull();
    assertThat(commit.changedFiles()).hasSize(2);
  }

  @Test
  void should_return_changed_file_count() {
    final var commit =
        new CommitInfo(
            "abc123",
            "message",
            "author",
            Instant.now(),
            List.of("File1.java", "File2.java", "File3.java"));

    assertThat(commit.getChangedFileCount()).isEqualTo(3);
  }

  @Test
  void should_identify_touched_file() {
    final var commit =
        new CommitInfo(
            "abc123",
            "message",
            "author",
            Instant.now(),
            List.of("UserService.java", "UserRepository.java"));

    assertThat(commit.touchedFile("UserService.java")).isTrue();
    assertThat(commit.touchedFile("OtherFile.java")).isFalse();
  }

  @Test
  void should_throw_when_commit_id_is_null() {
    assertThatThrownBy(() -> new CommitInfo(null, "message", "author", Instant.now(), List.of()))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Commit ID cannot be null");
  }

  @Test
  void should_throw_when_commit_id_is_blank() {
    assertThatThrownBy(() -> new CommitInfo("", "message", "author", Instant.now(), List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Commit ID cannot be blank");
  }

  @Test
  void should_allow_null_message() {
    final var commit = new CommitInfo("abc123", null, "author", Instant.now(), List.of());

    assertThat(commit.message()).isNull();
    assertThat(commit.hasMessage()).isFalse();
  }

  @Test
  void should_allow_null_author() {
    final var commit = new CommitInfo("abc123", "message", null, Instant.now(), List.of());

    assertThat(commit.author()).isNull();
    assertThat(commit.hasAuthor()).isFalse();
  }

  @Test
  void should_allow_null_timestamp() {
    final var commit = new CommitInfo("abc123", "message", "author", null, List.of());

    assertThat(commit.timestamp()).isNull();
  }

  @Test
  void should_handle_null_changed_files() {
    final var commit = new CommitInfo("abc123", "message", "author", Instant.now(), null);

    assertThat(commit.changedFiles()).isEmpty();
    assertThat(commit.getChangedFileCount()).isZero();
  }

  @Test
  void should_accept_empty_changed_files_list() {
    final var commit = new CommitInfo("abc123", "message", "author", Instant.now(), List.of());

    assertThat(commit.changedFiles()).isEmpty();
    assertThat(commit.getChangedFileCount()).isZero();
  }

  @Test
  void should_return_short_id() {
    final var commit =
        new CommitInfo("abc123def456789", "message", "author", Instant.now(), List.of());

    assertThat(commit.shortId()).isEqualTo("abc123d");
  }

  @Test
  void should_return_full_id_when_short() {
    final var commit = new CommitInfo("abc", "message", "author", Instant.now(), List.of());

    assertThat(commit.shortId()).isEqualTo("abc");
  }

  @Test
  void should_return_first_line_of_message() {
    final var commit =
        new CommitInfo(
            "abc123", "First line\nSecond line\nThird line", "author", Instant.now(), List.of());

    assertThat(commit.firstLineOfMessage()).isEqualTo("First line");
  }

  @Test
  void should_return_full_message_when_single_line() {
    final var commit =
        new CommitInfo("abc123", "Single line message", "author", Instant.now(), List.of());

    assertThat(commit.firstLineOfMessage()).isEqualTo("Single line message");
  }

  @Test
  void should_return_empty_string_when_message_is_null() {
    final var commit = new CommitInfo("abc123", null, "author", Instant.now(), List.of());

    assertThat(commit.firstLineOfMessage()).isEmpty();
  }

  @Test
  void should_return_empty_string_when_message_is_blank() {
    final var commit = new CommitInfo("abc123", "   ", "author", Instant.now(), List.of());

    assertThat(commit.firstLineOfMessage()).isEmpty();
  }

  @Test
  void should_detect_has_message() {
    final var withMessage = new CommitInfo("abc123", "message", null, null, null);
    final var withoutMessage = new CommitInfo("abc123", null, null, null, null);
    final var withBlankMessage = new CommitInfo("abc123", "   ", null, null, null);

    assertThat(withMessage.hasMessage()).isTrue();
    assertThat(withoutMessage.hasMessage()).isFalse();
    assertThat(withBlankMessage.hasMessage()).isFalse();
  }

  @Test
  void should_detect_has_author() {
    final var withAuthor = new CommitInfo("abc123", null, "author", null, null);
    final var withoutAuthor = new CommitInfo("abc123", null, null, null, null);
    final var withBlankAuthor = new CommitInfo("abc123", null, "   ", null, null);

    assertThat(withAuthor.hasAuthor()).isTrue();
    assertThat(withoutAuthor.hasAuthor()).isFalse();
    assertThat(withBlankAuthor.hasAuthor()).isFalse();
  }

  @Test
  void should_make_defensive_copy_of_changed_files() {
    final var mutableFiles = new java.util.ArrayList<>(List.of("file1.java"));
    final var commit = new CommitInfo("abc123", "message", "author", Instant.now(), mutableFiles);

    mutableFiles.clear();

    assertThat(commit.changedFiles()).hasSize(1);
  }
}
