package com.ghiloufi.aicode.core.infrastructure.adapter.openai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghiloufi.aicode.core.infrastructure.adapter.openai.OpenAIChatRequest.ChatMessage;
import org.junit.jupiter.api.Test;

final class OpenAIChatRequestTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void should_create_system_message() {
    final ChatMessage message = ChatMessage.system("You are a helpful assistant");

    assertThat(message.role()).isEqualTo("system");
    assertThat(message.content()).isEqualTo("You are a helpful assistant");
  }

  @Test
  void should_create_user_message() {
    final ChatMessage message = ChatMessage.user("Review this code");

    assertThat(message.role()).isEqualTo("user");
    assertThat(message.content()).isEqualTo("Review this code");
  }

  @Test
  void should_create_code_review_request() {
    final OpenAIChatRequest request = OpenAIChatRequest.forCodeReview("gpt-4", "Review this PR");

    assertThat(request.model()).isEqualTo("gpt-4");
    assertThat(request.stream()).isTrue();
    assertThat(request.temperature()).isEqualTo(0.1);
    assertThat(request.messages()).hasSize(2);
    assertThat(request.messages().get(0).role()).isEqualTo("system");
    assertThat(request.messages().get(1).role()).isEqualTo("user");
    assertThat(request.messages().get(1).content()).isEqualTo("Review this PR");
  }

  @Test
  void should_serialize_to_valid_json() throws Exception {
    final OpenAIChatRequest request = OpenAIChatRequest.forCodeReview("gpt-4o-mini", "Test prompt");

    final String json = objectMapper.writeValueAsString(request);

    assertThat(json).contains("\"model\":\"gpt-4o-mini\"");
    assertThat(json).contains("\"stream\":true");
    assertThat(json).contains("\"temperature\":0.1");
    assertThat(json).contains("\"messages\":");
    assertThat(json).contains("\"role\":\"system\"");
    assertThat(json).contains("\"role\":\"user\"");
  }

  @Test
  void should_handle_special_characters_in_prompt() throws Exception {
    final String prompt = "Review this:\n```java\nString s = \"test\";\n```";
    final OpenAIChatRequest request = OpenAIChatRequest.forCodeReview("gpt-4", prompt);

    final String json = objectMapper.writeValueAsString(request);

    assertThat(json).contains("\\n");
    assertThat(json).contains("\\\"test\\\"");
  }

  @Test
  void should_handle_unicode_in_prompt() throws Exception {
    final String prompt = "Review: émoji 🔍 中文";
    final OpenAIChatRequest request = OpenAIChatRequest.forCodeReview("gpt-4", prompt);

    final String json = objectMapper.writeValueAsString(request);
    final OpenAIChatRequest deserialized = objectMapper.readValue(json, OpenAIChatRequest.class);

    assertThat(deserialized.messages().get(1).content()).isEqualTo(prompt);
  }
}
