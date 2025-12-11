package com.ghiloufi.aicode.llmworker.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(ProviderProperties.class)
@RequiredArgsConstructor
public class LlmConfigurationValidator {

  private final ProviderProperties providerProperties;

  @PostConstruct
  public void validateConfiguration() {
    log.info("Validating LLM provider configuration...");

    final String provider = providerProperties.getProvider();

    switch (provider.toLowerCase()) {
      case "openai" -> validateOpenAi();
      case "anthropic" -> validateAnthropic();
      case "gemini" -> validateGemini();
      case "ollama" -> validateOllama();
      default ->
          throw new IllegalStateException(
              String.format(
                  "Unknown LLM provider: '%s'. Supported providers: openai, anthropic, gemini, ollama",
                  provider));
    }

    log.info(
        "LLM configuration validation successful - provider: {}, timeout: {}",
        provider,
        providerProperties.getTimeout());
  }

  private void validateOpenAi() {
    final ProviderProperties.OpenAiProperties openai = providerProperties.getOpenai();

    validateNotBlank(openai.getApiKey(), "llm.openai.api-key");
    validateNotBlank(openai.getModel(), "llm.openai.model");

    log.info(
        "OpenAI provider configured: model={}, baseUrl={}",
        openai.getModel(),
        openai.getBaseUrl() != null ? openai.getBaseUrl() : "default");
  }

  private void validateAnthropic() {
    final ProviderProperties.AnthropicProperties anthropic = providerProperties.getAnthropic();

    validateNotBlank(anthropic.getApiKey(), "llm.anthropic.api-key");
    validateNotBlank(anthropic.getModel(), "llm.anthropic.model");

    log.info("Anthropic provider configured: model={}", anthropic.getModel());
  }

  private void validateGemini() {
    final ProviderProperties.GeminiProperties gemini = providerProperties.getGemini();

    validateNotBlank(gemini.getApiKey(), "llm.gemini.api-key");
    validateNotBlank(gemini.getModel(), "llm.gemini.model");

    log.info("Gemini provider configured: model={}", gemini.getModel());
  }

  private void validateOllama() {
    final ProviderProperties.OllamaProperties ollama = providerProperties.getOllama();

    validateNotBlank(ollama.getBaseUrl(), "llm.ollama.base-url");
    validateNotBlank(ollama.getModel(), "llm.ollama.model");

    log.info(
        "Ollama provider configured: baseUrl={}, model={}", ollama.getBaseUrl(), ollama.getModel());
  }

  private void validateNotBlank(final String value, final String propertyName) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(
          String.format(
              "LLM provider '%s' is selected but '%s' is not configured. "
                  + "Please set the '%s' property in your configuration.",
              providerProperties.getProvider(), propertyName, propertyName));
    }
  }
}
