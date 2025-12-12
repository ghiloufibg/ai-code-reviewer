package com.ghiloufi.aicode.gateway.formatter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghiloufi.aicode.core.domain.model.ReviewChunk;
import com.ghiloufi.aicode.core.domain.model.ReviewChunk.ChunkType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SSEFormatter Tests")
final class SSEFormatterTest {

  private SSEFormatter formatter;

  @BeforeEach
  void setUp() {
    formatter = new SSEFormatter(new ObjectMapper());
  }

  @Nested
  @DisplayName("formatReviewChunk")
  final class FormatReviewChunkTests {

    @Test
    @DisplayName("should_format_analysis_chunk_as_sse_data")
    void should_format_analysis_chunk_as_sse_data() {
      final ReviewChunk chunk =
          new ReviewChunk(ChunkType.ANALYSIS, "Security vulnerability found", "file.java:42");

      final String result = formatter.formatReviewChunk(chunk);

      assertThat(result).startsWith("data: ");
      assertThat(result).endsWith("\n\n");
      assertThat(result).contains("\"type\":\"ANALYSIS\"");
      assertThat(result).contains("\"content\":\"Security vulnerability found\"");
      assertThat(result).contains("\"metadata\":\"file.java:42\"");
      assertThat(result).contains("\"timestamp\":");
    }

    @Test
    @DisplayName("should_format_chunk_with_null_metadata_as_empty_string")
    void should_format_chunk_with_null_metadata_as_empty_string() {
      final ReviewChunk chunk = ReviewChunk.of(ChunkType.SUGGESTION, "Use var for local variables");

      final String result = formatter.formatReviewChunk(chunk);

      assertThat(result).startsWith("data: ");
      assertThat(result).contains("\"type\":\"SUGGESTION\"");
      assertThat(result).contains("\"content\":\"Use var for local variables\"");
      assertThat(result).contains("\"metadata\":\"\"");
    }

    @Test
    @DisplayName("should_format_security_chunk")
    void should_format_security_chunk() {
      final ReviewChunk chunk =
          new ReviewChunk(ChunkType.SECURITY, "SQL injection vulnerability", "UserDao.java:15");

      final String result = formatter.formatReviewChunk(chunk);

      assertThat(result).contains("\"type\":\"SECURITY\"");
      assertThat(result).contains("\"content\":\"SQL injection vulnerability\"");
    }

    @Test
    @DisplayName("should_format_performance_chunk")
    void should_format_performance_chunk() {
      final ReviewChunk chunk =
          new ReviewChunk(ChunkType.PERFORMANCE, "N+1 query detected", "Repository.java:30");

      final String result = formatter.formatReviewChunk(chunk);

      assertThat(result).contains("\"type\":\"PERFORMANCE\"");
      assertThat(result).contains("\"content\":\"N+1 query detected\"");
    }

    @Test
    @DisplayName("should_format_error_chunk")
    void should_format_error_chunk() {
      final ReviewChunk chunk = ReviewChunk.of(ChunkType.ERROR, "Failed to analyze file");

      final String result = formatter.formatReviewChunk(chunk);

      assertThat(result).contains("\"type\":\"ERROR\"");
      assertThat(result).contains("\"content\":\"Failed to analyze file\"");
    }

    @Test
    @DisplayName("should_format_commentary_chunk")
    void should_format_commentary_chunk() {
      final ReviewChunk chunk =
          ReviewChunk.of(ChunkType.COMMENTARY, "Overall code quality is good");

      final String result = formatter.formatReviewChunk(chunk);

      assertThat(result).contains("\"type\":\"COMMENTARY\"");
      assertThat(result).contains("\"content\":\"Overall code quality is good\"");
    }

    @Test
    @DisplayName("should_include_timestamp_in_chunk")
    void should_include_timestamp_in_chunk() {
      final ReviewChunk chunk = ReviewChunk.of(ChunkType.ANALYSIS, "Test content");

      final String result = formatter.formatReviewChunk(chunk);

      assertThat(result).containsPattern("\"timestamp\":\\d+");
    }
  }

  @Nested
  @DisplayName("formatDone")
  final class FormatDoneTests {

    @Test
    @DisplayName("should_format_done_message_as_sse_data")
    void should_format_done_message_as_sse_data() {
      final String result = formatter.formatDone();

      assertThat(result).startsWith("data: ");
      assertThat(result).endsWith("\n\n");
      assertThat(result).contains("\"type\":\"DONE\"");
      assertThat(result).contains("\"message\":\"Analysis stream completed\"");
      assertThat(result).contains("\"timestamp\":");
    }
  }

  @Nested
  @DisplayName("formatPublished")
  final class FormatPublishedTests {

    @Test
    @DisplayName("should_format_published_message_as_sse_data")
    void should_format_published_message_as_sse_data() {
      final String result = formatter.formatPublished();

      assertThat(result).startsWith("data: ");
      assertThat(result).endsWith("\n\n");
      assertThat(result).contains("\"type\":\"PUBLISHED\"");
      assertThat(result).contains("\"message\":\"Review published to repository\"");
      assertThat(result).contains("\"timestamp\":");
    }
  }

  @Nested
  @DisplayName("formatError")
  final class FormatErrorTests {

    @Test
    @DisplayName("should_format_error_with_message")
    void should_format_error_with_message() {
      final Throwable error = new RuntimeException("Connection timeout");

      final String result = formatter.formatError(error);

      assertThat(result).startsWith("data: ");
      assertThat(result).endsWith("\n\n");
      assertThat(result).contains("\"type\":\"ERROR\"");
      assertThat(result).contains("\"error\":\"Connection timeout\"");
      assertThat(result).contains("\"timestamp\":");
    }

    @Test
    @DisplayName("should_format_error_with_null_message_as_unknown")
    void should_format_error_with_null_message_as_unknown() {
      final Throwable error = new RuntimeException((String) null);

      final String result = formatter.formatError(error);

      assertThat(result).contains("\"type\":\"ERROR\"");
      assertThat(result).contains("\"error\":\"Unknown error\"");
    }

    @Test
    @DisplayName("should_format_nested_exception")
    void should_format_nested_exception() {
      final Throwable cause = new IllegalStateException("Root cause");
      final Throwable error = new RuntimeException("Wrapper exception", cause);

      final String result = formatter.formatError(error);

      assertThat(result).contains("\"error\":\"Wrapper exception\"");
    }
  }

  @Nested
  @DisplayName("SSE Format Compliance")
  final class SSEFormatComplianceTests {

    @Test
    @DisplayName("all_messages_should_end_with_double_newline")
    void all_messages_should_end_with_double_newline() {
      final ReviewChunk chunk = ReviewChunk.of(ChunkType.ANALYSIS, "content");

      assertThat(formatter.formatReviewChunk(chunk)).endsWith("\n\n");
      assertThat(formatter.formatDone()).endsWith("\n\n");
      assertThat(formatter.formatPublished()).endsWith("\n\n");
      assertThat(formatter.formatError(new RuntimeException("test"))).endsWith("\n\n");
    }

    @Test
    @DisplayName("all_messages_should_start_with_data_prefix")
    void all_messages_should_start_with_data_prefix() {
      final ReviewChunk chunk = ReviewChunk.of(ChunkType.ANALYSIS, "content");

      assertThat(formatter.formatReviewChunk(chunk)).startsWith("data: ");
      assertThat(formatter.formatDone()).startsWith("data: ");
      assertThat(formatter.formatPublished()).startsWith("data: ");
      assertThat(formatter.formatError(new RuntimeException("test"))).startsWith("data: ");
    }

    @Test
    @DisplayName("all_messages_should_contain_valid_json")
    void all_messages_should_contain_valid_json() {
      final ObjectMapper mapper = new ObjectMapper();
      final ReviewChunk chunk = ReviewChunk.of(ChunkType.ANALYSIS, "content");

      assertValidJson(mapper, formatter.formatReviewChunk(chunk));
      assertValidJson(mapper, formatter.formatDone());
      assertValidJson(mapper, formatter.formatPublished());
      assertValidJson(mapper, formatter.formatError(new RuntimeException("test")));
    }

    private void assertValidJson(final ObjectMapper mapper, final String sseMessage) {
      final String jsonPart = sseMessage.replace("data: ", "").trim();
      try {
        mapper.readTree(jsonPart);
      } catch (final Exception e) {
        throw new AssertionError("Invalid JSON in SSE message: " + jsonPart, e);
      }
    }
  }
}
