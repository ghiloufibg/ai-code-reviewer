package com.ghiloufi.aicode.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommitInfoTest {

  @Test
  void should_create_valid_commit_info() {
    final CommitInfo commit =
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
    final CommitInfo commit =
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
    final CommitInfo commit =
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
  void should_throw_when_message_is_null() {
    assertThatThrownBy(() -> new CommitInfo("abc123", null, "author", Instant.now(), List.of()))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Message cannot be null");
  }

  @Test
  void should_throw_when_author_is_null() {
    assertThatThrownBy(() -> new CommitInfo("abc123", "message", null, Instant.now(), List.of()))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Author cannot be null");
  }

  @Test
  void should_throw_when_author_is_blank() {
    assertThatThrownBy(() -> new CommitInfo("abc123", "message", "", Instant.now(), List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Author cannot be blank");
  }

  @Test
  void should_throw_when_timestamp_is_null() {
    assertThatThrownBy(() -> new CommitInfo("abc123", "message", "author", null, List.of()))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Timestamp cannot be null");
  }

  @Test
  void should_throw_when_changed_files_is_null() {
    assertThatThrownBy(() -> new CommitInfo("abc123", "message", "author", Instant.now(), null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Changed files cannot be null");
  }

  @Test
  void should_accept_empty_changed_files_list() {
    final CommitInfo commit =
        new CommitInfo("abc123", "message", "author", Instant.now(), List.of());

    assertThat(commit.changedFiles()).isEmpty();
    assertThat(commit.getChangedFileCount()).isZero();
  }
}
