package com.ghiloufi.aicode.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

final class PolicyDocumentTest {

  @Test
  void should_create_valid_policy_document() {
    final var doc =
        new PolicyDocument(
            "CONTRIBUTING.md",
            ".github/CONTRIBUTING.md",
            "# Contributing\nPlease follow these guidelines.",
            false);

    assertThat(doc.name()).isEqualTo("CONTRIBUTING.md");
    assertThat(doc.path()).isEqualTo(".github/CONTRIBUTING.md");
    assertThat(doc.content()).contains("Contributing");
    assertThat(doc.truncated()).isFalse();
  }

  @Test
  void should_throw_when_name_is_null() {
    assertThatThrownBy(() -> new PolicyDocument(null, "path", "content", false))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Name cannot be null");
  }

  @Test
  void should_throw_when_name_is_blank() {
    assertThatThrownBy(() -> new PolicyDocument("  ", "path", "content", false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Name cannot be blank");
  }

  @Test
  void should_throw_when_path_is_null() {
    assertThatThrownBy(() -> new PolicyDocument("name", null, "content", false))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Path cannot be null");
  }

  @Test
  void should_throw_when_path_is_blank() {
    assertThatThrownBy(() -> new PolicyDocument("name", "  ", "content", false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Path cannot be blank");
  }

  @Test
  void should_return_true_when_has_content() {
    final var doc = new PolicyDocument("name", "path", "content", false);

    assertThat(doc.hasContent()).isTrue();
  }

  @Test
  void should_return_false_when_content_is_null() {
    final var doc = new PolicyDocument("name", "path", null, false);

    assertThat(doc.hasContent()).isFalse();
  }

  @Test
  void should_return_false_when_content_is_blank() {
    final var doc = new PolicyDocument("name", "path", "   ", false);

    assertThat(doc.hasContent()).isFalse();
  }

  @Test
  void should_return_content_length() {
    final var doc = new PolicyDocument("name", "path", "12345", false);

    assertThat(doc.contentLength()).isEqualTo(5);
  }

  @Test
  void should_return_zero_content_length_when_null() {
    final var doc = new PolicyDocument("name", "path", null, false);

    assertThat(doc.contentLength()).isEqualTo(0);
  }
}
