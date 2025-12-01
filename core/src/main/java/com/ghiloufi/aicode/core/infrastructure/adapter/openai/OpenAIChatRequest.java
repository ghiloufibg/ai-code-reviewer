package com.ghiloufi.aicode.core.infrastructure.adapter.openai;

import java.util.List;

public record OpenAIChatRequest(
    String model, List<ChatMessage> messages, boolean stream, double temperature) {

  public record ChatMessage(String role, String content) {

    public static ChatMessage system(final String content) {
      return new ChatMessage("system", content);
    }

    public static ChatMessage user(final String content) {
      return new ChatMessage("user", content);
    }
  }

  public static OpenAIChatRequest forCodeReview(
      final String model, final String systemPrompt, final String userPrompt) {
    final List<ChatMessage> messages =
        List.of(ChatMessage.system(systemPrompt), ChatMessage.user(userPrompt));
    return new OpenAIChatRequest(model, messages, true, 0.1);
  }

  public static OpenAIChatRequest forTicketAnalysis(
      final String model, final String systemPrompt, final String ticketContent) {
    final List<ChatMessage> messages =
        List.of(ChatMessage.system(systemPrompt), ChatMessage.user(ticketContent));
    return new OpenAIChatRequest(model, messages, true, 0.2);
  }
}
