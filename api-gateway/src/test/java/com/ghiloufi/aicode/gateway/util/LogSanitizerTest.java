package com.ghiloufi.aicode.gateway.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("LogSanitizer")
final class LogSanitizerTest {

  @Nested
  @DisplayName("sanitize()")
  final class SanitizeTests {

    @Test
    @DisplayName("should_return_null_when_input_is_null")
    void should_return_null_when_input_is_null() {
      final String result = LogSanitizer.sanitize(null);

      assertThat(result).isNull();
    }

    @Test
    @DisplayName("should_return_unchanged_string_when_no_crlf_present")
    void should_return_unchanged_string_when_no_crlf_present() {
      final String input = "normal-log-message-123";

      final String result = LogSanitizer.sanitize(input);

      assertThat(result).isEqualTo(input);
    }

    @Test
    @DisplayName("should_replace_carriage_return_with_space")
    void should_replace_carriage_return_with_space() {
      final String input = "line1\rline2";

      final String result = LogSanitizer.sanitize(input);

      assertThat(result).isEqualTo("line1 line2");
    }

    @Test
    @DisplayName("should_replace_newline_with_space")
    void should_replace_newline_with_space() {
      final String input = "line1\nline2";

      final String result = LogSanitizer.sanitize(input);

      assertThat(result).isEqualTo("line1 line2");
    }

    @Test
    @DisplayName("should_replace_crlf_sequence_with_spaces")
    void should_replace_crlf_sequence_with_spaces() {
      final String input = "line1\r\nline2";

      final String result = LogSanitizer.sanitize(input);

      assertThat(result).isEqualTo("line1  line2");
    }

    @Test
    @DisplayName("should_handle_multiple_crlf_characters")
    void should_handle_multiple_crlf_characters() {
      final String input = "a\nb\rc\r\nd";

      final String result = LogSanitizer.sanitize(input);

      assertThat(result).isEqualTo("a b c  d");
    }

    @Test
    @DisplayName("should_handle_empty_string")
    void should_handle_empty_string() {
      final String result = LogSanitizer.sanitize("");

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should_handle_log_injection_attack_string")
    void should_handle_log_injection_attack_string() {
      final String maliciousInput = "request-id\n[ERROR] Fake error message injected";

      final String result = LogSanitizer.sanitize(maliciousInput);

      assertThat(result).isEqualTo("request-id [ERROR] Fake error message injected");
      assertThat(result).doesNotContain("\n");
      assertThat(result).doesNotContain("\r");
    }
  }
}
