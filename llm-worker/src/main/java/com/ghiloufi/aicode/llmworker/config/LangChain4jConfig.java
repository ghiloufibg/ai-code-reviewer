package com.ghiloufi.aicode.llmworker.config;

import static dev.langchain4j.model.chat.Capability.RESPONSE_FORMAT_JSON_SCHEMA;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class LangChain4jConfig {

  private final ProviderProperties props;

  @Bean
  @ConditionalOnProperty(name = "llm.provider", havingValue = "openai", matchIfMissing = true)
  public ChatModel openAiModel() {
    log.info("Configuring OpenAI provider with model: {}", props.getOpenai().getModel());

    OpenAiChatModel.OpenAiChatModelBuilder builder =
        OpenAiChatModel.builder()
            .apiKey(props.getOpenai().getApiKey())
            .modelName(props.getOpenai().getModel())
            .supportedCapabilities(RESPONSE_FORMAT_JSON_SCHEMA)
            .strictJsonSchema(true)
            .temperature(0.1)
            .timeout(props.getTimeout());

    if (props.getOpenai().getBaseUrl() != null) {
      builder.baseUrl(props.getOpenai().getBaseUrl());
    }

    return builder.build();
  }

  @Bean
  @ConditionalOnProperty(name = "llm.provider", havingValue = "anthropic")
  public ChatModel anthropicModel() {
    log.info("Configuring Anthropic provider with model: {}", props.getAnthropic().getModel());

    return AnthropicChatModel.builder()
        .apiKey(props.getAnthropic().getApiKey())
        .modelName(props.getAnthropic().getModel())
        .temperature(0.1)
        .timeout(props.getTimeout())
        .build();
  }

  @Bean
  @ConditionalOnProperty(name = "llm.provider", havingValue = "gemini")
  public ChatModel geminiModel() {
    log.info("Configuring Gemini provider with model: {}", props.getGemini().getModel());

    return GoogleAiGeminiChatModel.builder()
        .apiKey(props.getGemini().getApiKey())
        .modelName(props.getGemini().getModel())
        .supportedCapabilities(RESPONSE_FORMAT_JSON_SCHEMA)
        .temperature(0.1)
        .build();
  }

  @Bean
  @ConditionalOnProperty(name = "llm.provider", havingValue = "ollama")
  public ChatModel ollamaModel() {
    log.info(
        "Configuring Ollama provider at {} with model: {}",
        props.getOllama().getBaseUrl(),
        props.getOllama().getModel());

    return OllamaChatModel.builder()
        .baseUrl(props.getOllama().getBaseUrl())
        .modelName(props.getOllama().getModel())
        .supportedCapabilities(RESPONSE_FORMAT_JSON_SCHEMA)
        .temperature(0.1)
        .timeout(props.getTimeout())
        .build();
  }
}
