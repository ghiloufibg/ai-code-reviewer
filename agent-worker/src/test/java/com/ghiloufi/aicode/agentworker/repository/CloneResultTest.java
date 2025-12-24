package com.ghiloufi.aicode.agentworker.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CloneResult")
final class CloneResultTest {

  @Nested
  @DisplayName("success factory method")
  final class SuccessTests {

    @Test
    void should_create_success_result_with_all_fields() {
      final var path = Path.of("/workspace/repo");
      final var commitHash = "abc123def456";
      final var duration = Duration.ofSeconds(5);

      final var result = CloneResult.success(path, commitHash, duration);

      assertThat(result.success()).isTrue();
      assertThat(result.clonedPath()).isEqualTo(path);
      assertThat(result.commitHash()).isEqualTo(commitHash);
      assertThat(result.duration()).isEqualTo(duration);
      assertThat(result.errorMessage()).isNull();
    }

    @Test
    void should_accept_any_commit_hash_format() {
      final var shortHash = "abc123";
      final var result = CloneResult.success(Path.of("/tmp"), shortHash, Duration.ZERO);

      assertThat(result.commitHash()).isEqualTo(shortHash);
    }
  }

  @Nested
  @DisplayName("failure factory method")
  final class FailureTests {

    @Test
    void should_create_failure_result_with_error_message() {
      final var errorMessage = "Repository not found";
      final var duration = Duration.ofSeconds(2);

      final var result = CloneResult.failure(errorMessage, duration);

      assertThat(result.success()).isFalse();
      assertThat(result.clonedPath()).isNull();
      assertThat(result.commitHash()).isNull();
      assertThat(result.duration()).isEqualTo(duration);
      assertThat(result.errorMessage()).isEqualTo(errorMessage);
    }

    @Test
    void should_preserve_error_message_details() {
      final var detailedError =
          "fatal: could not read Username for 'https://github.com': terminal prompts disabled";
      final var result = CloneResult.failure(detailedError, Duration.ofMillis(500));

      assertThat(result.errorMessage()).isEqualTo(detailedError);
    }
  }

  @Nested
  @DisplayName("record accessors")
  final class AccessorTests {

    @Test
    void should_provide_all_record_accessors_for_success() {
      final var result =
          new CloneResult(true, Path.of("/path"), "commit123", Duration.ofSeconds(10), null);

      assertThat(result.success()).isTrue();
      assertThat(result.clonedPath()).isEqualTo(Path.of("/path"));
      assertThat(result.commitHash()).isEqualTo("commit123");
      assertThat(result.duration()).isEqualTo(Duration.ofSeconds(10));
      assertThat(result.errorMessage()).isNull();
    }

    @Test
    void should_provide_all_record_accessors_for_failure() {
      final var result = new CloneResult(false, null, null, Duration.ofSeconds(1), "error message");

      assertThat(result.success()).isFalse();
      assertThat(result.clonedPath()).isNull();
      assertThat(result.commitHash()).isNull();
      assertThat(result.duration()).isEqualTo(Duration.ofSeconds(1));
      assertThat(result.errorMessage()).isEqualTo("error message");
    }
  }
}
