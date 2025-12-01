package com.ghiloufi.aicode.core.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

final class OpenAIAdapterTest {

  private MockWebServer mockServer;
  private OpenAIAdapter adapter;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() throws Exception {
    mockServer = new MockWebServer();
    mockServer.start();
    adapter =
        new OpenAIAdapter(
            mockServer.url("/v1").toString(), "gpt-4o-mini", "test-api-key", objectMapper);
  }

  @AfterEach
  void tearDown() throws Exception {
    mockServer.shutdown();
  }

  @Nested
  final class StreamCompletionTests {

    @Test
    void should_parse_single_chunk_response() {
      final String sseResponse =
          """
          data: {"id":"chatcmpl-1","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"Hello"},"finish_reason":null}]}

          data: [DONE]

          """;

      mockServer.enqueue(
          new MockResponse().setHeader("Content-Type", "text/event-stream").setBody(sseResponse));

      StepVerifier.create(adapter.streamCompletion("System prompt", "Test prompt"))
          .expectNext("Hello")
          .verifyComplete();
    }

    @Test
    void should_parse_multiple_chunk_responses() {
      final String sseResponse =
          """
          data: {"id":"chatcmpl-1","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"Hello"},"finish_reason":null}]}

          data: {"id":"chatcmpl-1","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":" World"},"finish_reason":null}]}

          data: [DONE]

          """;

      mockServer.enqueue(
          new MockResponse().setHeader("Content-Type", "text/event-stream").setBody(sseResponse));

      StepVerifier.create(adapter.streamCompletion("System prompt", "Test prompt"))
          .expectNext("Hello")
          .expectNext(" World")
          .verifyComplete();
    }

    @Test
    void should_filter_empty_content_chunks() {
      final String sseResponse =
          """
          data: {"id":"chatcmpl-1","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"role":"assistant"},"finish_reason":null}]}

          data: {"id":"chatcmpl-1","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"Hello"},"finish_reason":null}]}

          data: {"id":"chatcmpl-1","object":"chat.completion.chunk","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}

          data: [DONE]

          """;

      mockServer.enqueue(
          new MockResponse().setHeader("Content-Type", "text/event-stream").setBody(sseResponse));

      StepVerifier.create(adapter.streamCompletion("System prompt", "Test prompt"))
          .expectNext("Hello")
          .verifyComplete();
    }

    @Test
    void should_filter_done_marker() {
      final String sseResponse =
          """
          data: {"id":"chatcmpl-1","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"Test"},"finish_reason":null}]}

          data: [DONE]

          """;

      mockServer.enqueue(
          new MockResponse().setHeader("Content-Type", "text/event-stream").setBody(sseResponse));

      StepVerifier.create(adapter.streamCompletion("System prompt", "Test prompt"))
          .expectNext("Test")
          .verifyComplete();
    }

    @Test
    void should_handle_special_characters_in_response() {
      final String sseResponse =
          """
          data: {"id":"chatcmpl-1","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"Line1\\nLine2"},"finish_reason":null}]}

          data: [DONE]

          """;

      mockServer.enqueue(
          new MockResponse().setHeader("Content-Type", "text/event-stream").setBody(sseResponse));

      StepVerifier.create(adapter.streamCompletion("System prompt", "Test prompt"))
          .expectNext("Line1\nLine2")
          .verifyComplete();
    }

    @Test
    void should_handle_empty_response() {
      final String sseResponse = "data: [DONE]\n\n";

      mockServer.enqueue(
          new MockResponse().setHeader("Content-Type", "text/event-stream").setBody(sseResponse));

      StepVerifier.create(adapter.streamCompletion("System prompt", "Test prompt")).verifyComplete();
    }

    @Test
    void should_gracefully_handle_malformed_json() {
      final String sseResponse =
          """
          data: not-valid-json

          data: {"id":"chatcmpl-1","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"Valid"},"finish_reason":null}]}

          data: [DONE]

          """;

      mockServer.enqueue(
          new MockResponse().setHeader("Content-Type", "text/event-stream").setBody(sseResponse));

      StepVerifier.create(adapter.streamCompletion("System prompt", "Test prompt"))
          .expectNext("Valid")
          .verifyComplete();
    }
  }

  @Nested
  final class RequestFormattingTests {

    @Test
    void should_send_correct_request_format() throws Exception {
      final String sseResponse = "data: [DONE]\n\n";
      mockServer.enqueue(
          new MockResponse().setHeader("Content-Type", "text/event-stream").setBody(sseResponse));

      adapter.streamCompletion("System prompt", "Test prompt").blockLast();

      final RecordedRequest request = mockServer.takeRequest();
      final String body = request.getBody().readUtf8();

      assertThat(body).contains("\"model\":\"gpt-4o-mini\"");
      assertThat(body).contains("\"stream\":true");
      assertThat(body).contains("\"temperature\":0.1");
      assertThat(body).contains("\"messages\":");
      assertThat(body).contains("\"role\":\"system\"");
      assertThat(body).contains("\"role\":\"user\"");
      assertThat(body).contains("Test prompt");
    }

    @Test
    void should_send_authorization_header() throws Exception {
      final String sseResponse = "data: [DONE]\n\n";
      mockServer.enqueue(
          new MockResponse().setHeader("Content-Type", "text/event-stream").setBody(sseResponse));

      adapter.streamCompletion("System prompt", "Test prompt").blockLast();

      final RecordedRequest request = mockServer.takeRequest();
      assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test-api-key");
    }

    @Test
    void should_send_content_type_header() throws Exception {
      final String sseResponse = "data: [DONE]\n\n";
      mockServer.enqueue(
          new MockResponse().setHeader("Content-Type", "text/event-stream").setBody(sseResponse));

      adapter.streamCompletion("System prompt", "Test prompt").blockLast();

      final RecordedRequest request = mockServer.takeRequest();
      assertThat(request.getHeader("Content-Type")).isEqualTo("application/json");
    }

    @Test
    void should_escape_special_characters_in_prompt() throws Exception {
      final String sseResponse = "data: [DONE]\n\n";
      mockServer.enqueue(
          new MockResponse().setHeader("Content-Type", "text/event-stream").setBody(sseResponse));

      adapter.streamCompletion("System prompt", "Test \"prompt\" with\nnewline").blockLast();

      final RecordedRequest request = mockServer.takeRequest();
      final String body = request.getBody().readUtf8();

      assertThat(body).doesNotContain("Test \"prompt\"");
      assertThat(body).contains("\\\"prompt\\\"");
      assertThat(body).contains("\\n");
    }
  }

  @Nested
  final class ProviderInfoTests {

    @Test
    void should_return_provider_name() {
      assertThat(adapter.getProviderName()).isEqualTo("openai");
    }

    @Test
    void should_return_model_name() {
      assertThat(adapter.getModelName()).isEqualTo("gpt-4o-mini");
    }
  }
}
