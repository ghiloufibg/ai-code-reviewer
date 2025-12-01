package com.ghiloufi.aicode.core.infrastructure.adapter.openai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

final class OpenAIStreamResponseTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void should_parse_stream_response_with_content() throws Exception {
    final String json =
        """
        {
          "id": "chatcmpl-123",
          "object": "chat.completion.chunk",
          "choices": [
            {
              "index": 0,
              "delta": {
                "content": "Hello"
              },
              "finish_reason": null
            }
          ]
        }
        """;

    final OpenAIStreamResponse response = objectMapper.readValue(json, OpenAIStreamResponse.class);

    assertThat(response.id()).isEqualTo("chatcmpl-123");
    assertThat(response.object()).isEqualTo("chat.completion.chunk");
    assertThat(response.hasContent()).isTrue();
    assertThat(response.extractFirstContent()).contains("Hello");
  }

  @Test
  void should_parse_stream_response_with_role() throws Exception {
    final String json =
        """
        {
          "id": "chatcmpl-123",
          "object": "chat.completion.chunk",
          "choices": [
            {
              "index": 0,
              "delta": {
                "role": "assistant"
              },
              "finish_reason": null
            }
          ]
        }
        """;

    final OpenAIStreamResponse response = objectMapper.readValue(json, OpenAIStreamResponse.class);

    assertThat(response.hasContent()).isFalse();
    assertThat(response.extractFirstContent()).isEmpty();
    assertThat(response.choices().get(0).delta().role()).isEqualTo("assistant");
  }

  @Test
  void should_parse_stream_response_with_finish_reason() throws Exception {
    final String json =
        """
        {
          "id": "chatcmpl-123",
          "object": "chat.completion.chunk",
          "choices": [
            {
              "index": 0,
              "delta": {},
              "finish_reason": "stop"
            }
          ]
        }
        """;

    final OpenAIStreamResponse response = objectMapper.readValue(json, OpenAIStreamResponse.class);

    assertThat(response.hasContent()).isFalse();
    assertThat(response.choices().get(0).finishReason()).isEqualTo("stop");
  }

  @Test
  void should_handle_empty_choices() {
    final OpenAIStreamResponse response = new OpenAIStreamResponse("id", "object", List.of());

    assertThat(response.hasContent()).isFalse();
    assertThat(response.extractFirstContent()).isEmpty();
  }

  @Test
  void should_handle_null_choices() {
    final OpenAIStreamResponse response = new OpenAIStreamResponse("id", "object", null);

    assertThat(response.hasContent()).isFalse();
    assertThat(response.extractFirstContent()).isEmpty();
  }

  @Test
  void should_handle_null_delta() {
    final OpenAIStreamResponse.Choice choice = new OpenAIStreamResponse.Choice(0, null, null);
    final OpenAIStreamResponse response = new OpenAIStreamResponse("id", "object", List.of(choice));

    assertThat(response.hasContent()).isFalse();
    assertThat(response.extractFirstContent()).isEmpty();
  }

  @Test
  void should_handle_empty_content() {
    final OpenAIStreamResponse.Delta delta = new OpenAIStreamResponse.Delta("assistant", "");
    final OpenAIStreamResponse.Choice choice = new OpenAIStreamResponse.Choice(0, delta, null);
    final OpenAIStreamResponse response = new OpenAIStreamResponse("id", "object", List.of(choice));

    assertThat(response.hasContent()).isFalse();
    assertThat(response.extractFirstContent()).contains("");
  }

  @Test
  void should_ignore_unknown_fields() throws Exception {
    final String json =
        """
        {
          "id": "chatcmpl-123",
          "object": "chat.completion.chunk",
          "created": 1234567890,
          "model": "gpt-4",
          "system_fingerprint": "fp_abc123",
          "choices": [
            {
              "index": 0,
              "delta": {
                "content": "Test",
                "refusal": null
              },
              "logprobs": null,
              "finish_reason": null
            }
          ]
        }
        """;

    final OpenAIStreamResponse response = objectMapper.readValue(json, OpenAIStreamResponse.class);

    assertThat(response.hasContent()).isTrue();
    assertThat(response.extractFirstContent()).contains("Test");
  }

  @Test
  void should_parse_content_with_special_characters() throws Exception {
    final String json =
        """
        {
          "id": "chatcmpl-123",
          "object": "chat.completion.chunk",
          "choices": [
            {
              "index": 0,
              "delta": {
                "content": "Line1\\nLine2\\t\\"quoted\\""
              },
              "finish_reason": null
            }
          ]
        }
        """;

    final OpenAIStreamResponse response = objectMapper.readValue(json, OpenAIStreamResponse.class);

    assertThat(response.extractFirstContent()).contains("Line1\nLine2\t\"quoted\"");
  }

  @Test
  void should_extract_content_from_choice() {
    final OpenAIStreamResponse.Delta delta = new OpenAIStreamResponse.Delta(null, "content");
    final OpenAIStreamResponse.Choice choice = new OpenAIStreamResponse.Choice(0, delta, null);

    assertThat(choice.extractContent()).contains("content");
  }
}
