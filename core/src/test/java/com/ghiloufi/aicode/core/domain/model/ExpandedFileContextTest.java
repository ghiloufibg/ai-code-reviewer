package com.ghiloufi.aicode.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

final class ExpandedFileContextTest {

  @Test
  void should_create_valid_expanded_file_context() {
    final var context = new ExpandedFileContext("src/Main.java", "public class Main {}", 1, false);

    assertThat(context.filePath()).isEqualTo("src/Main.java");
    assertThat(context.content()).isEqualTo("public class Main {}");
    assertThat(context.lineCount()).isEqualTo(1);
    assertThat(context.truncated()).isFalse();
  }

  @Test
  void should_throw_when_file_path_is_null() {
    assertThatThrownBy(() -> new ExpandedFileContext(null, "content", 1, false))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("File path cannot be null");
  }

  @Test
  void should_throw_when_file_path_is_blank() {
    assertThatThrownBy(() -> new ExpandedFileContext("  ", "content", 1, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("File path cannot be blank");
  }

  @Test
  void should_throw_when_line_count_is_negative() {
    assertThatThrownBy(() -> new ExpandedFileContext("path", "content", -1, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Line count cannot be negative");
  }

  @Test
  void should_create_empty_context() {
    final var context = ExpandedFileContext.empty("src/Empty.java");

    assertThat(context.filePath()).isEqualTo("src/Empty.java");
    assertThat(context.content()).isEmpty();
    assertThat(context.lineCount()).isEqualTo(0);
    assertThat(context.truncated()).isFalse();
    assertThat(context.hasContent()).isFalse();
  }

  @Test
  void should_create_truncated_context() {
    final var content = "line1\nline2\nline3";
    final var context = ExpandedFileContext.truncated("src/Large.java", content, 100);

    assertThat(context.filePath()).isEqualTo("src/Large.java");
    assertThat(context.content()).isEqualTo(content);
    assertThat(context.truncated()).isTrue();
  }

  @Test
  void should_create_context_from_content() {
    final var content = "line1\nline2\nline3";
    final var context = ExpandedFileContext.of("src/File.java", content);

    assertThat(context.filePath()).isEqualTo("src/File.java");
    assertThat(context.lineCount()).isEqualTo(3);
    assertThat(context.truncated()).isFalse();
    assertThat(context.hasContent()).isTrue();
  }

  @Test
  void should_return_content_length() {
    final var context = new ExpandedFileContext("path", "12345", 1, false);

    assertThat(context.contentLength()).isEqualTo(5);
  }

  @Test
  void should_return_zero_content_length_when_null() {
    final var context = new ExpandedFileContext("path", null, 0, false);

    assertThat(context.contentLength()).isEqualTo(0);
    assertThat(context.hasContent()).isFalse();
  }
}
