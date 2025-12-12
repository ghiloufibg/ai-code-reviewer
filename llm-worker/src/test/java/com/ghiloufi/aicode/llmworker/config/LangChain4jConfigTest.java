package com.ghiloufi.aicode.llmworker.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.llmworker.config.ProviderProperties.AnthropicProperties;
import com.ghiloufi.aicode.llmworker.config.ProviderProperties.GeminiProperties;
import com.ghiloufi.aicode.llmworker.config.ProviderProperties.OllamaProperties;
import com.ghiloufi.aicode.llmworker.config.ProviderProperties.OpenAiProperties;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("LangChain4jConfig Tests")
final class LangChain4jConfigTest {

  @Nested
  @DisplayName("Provider Properties Access")
  final class ProviderPropertiesAccess {

    @Test
    @DisplayName("should_access_openai_properties")
    void should_access_openai_properties() {
      final OpenAiProperties openAi = new OpenAiProperties("test-key", "gpt-4o", null);
      final ProviderProperties props =
          new ProviderProperties(
              "openai",
              openAi,
              new AnthropicProperties(null, null),
              new GeminiProperties(null, null),
              new OllamaProperties(null, null),
              Duration.ofSeconds(60));

      assertThat(props.getOpenai().getApiKey()).isEqualTo("test-key");
      assertThat(props.getOpenai().getModel()).isEqualTo("gpt-4o");
    }

    @Test
    @DisplayName("should_access_anthropic_properties")
    void should_access_anthropic_properties() {
      final AnthropicProperties anthropic = new AnthropicProperties("claude-key", "claude-3-opus");
      final ProviderProperties props =
          new ProviderProperties(
              "anthropic",
              new OpenAiProperties(null, null, null),
              anthropic,
              new GeminiProperties(null, null),
              new OllamaProperties(null, null),
              Duration.ofSeconds(90));

      assertThat(props.getAnthropic().getApiKey()).isEqualTo("claude-key");
      assertThat(props.getAnthropic().getModel()).isEqualTo("claude-3-opus");
    }

    @Test
    @DisplayName("should_access_gemini_properties")
    void should_access_gemini_properties() {
      final GeminiProperties gemini = new GeminiProperties("gemini-key", "gemini-1.5-pro");
      final ProviderProperties props =
          new ProviderProperties(
              "gemini",
              new OpenAiProperties(null, null, null),
              new AnthropicProperties(null, null),
              gemini,
              new OllamaProperties(null, null),
              Duration.ofSeconds(120));

      assertThat(props.getGemini().getApiKey()).isEqualTo("gemini-key");
      assertThat(props.getGemini().getModel()).isEqualTo("gemini-1.5-pro");
    }

    @Test
    @DisplayName("should_access_ollama_properties")
    void should_access_ollama_properties() {
      final OllamaProperties ollama = new OllamaProperties("http://localhost:11434", "llama3");
      final ProviderProperties props =
          new ProviderProperties(
              "ollama",
              new OpenAiProperties(null, null, null),
              new AnthropicProperties(null, null),
              new GeminiProperties(null, null),
              ollama,
              Duration.ofSeconds(180));

      assertThat(props.getOllama().getBaseUrl()).isEqualTo("http://localhost:11434");
      assertThat(props.getOllama().getModel()).isEqualTo("llama3");
    }

    @Test
    @DisplayName("should_access_timeout")
    void should_access_timeout() {
      final ProviderProperties props =
          new ProviderProperties(
              "openai",
              new OpenAiProperties(null, null, null),
              new AnthropicProperties(null, null),
              new GeminiProperties(null, null),
              new OllamaProperties(null, null),
              Duration.ofSeconds(300));

      assertThat(props.getTimeout()).isEqualTo(Duration.ofSeconds(300));
    }

    @Test
    @DisplayName("should_access_openai_base_url")
    void should_access_openai_base_url() {
      final OpenAiProperties openAi =
          new OpenAiProperties("key", "gpt-4o", "https://custom.openai.com");
      final ProviderProperties props =
          new ProviderProperties(
              "openai",
              openAi,
              new AnthropicProperties(null, null),
              new GeminiProperties(null, null),
              new OllamaProperties(null, null),
              Duration.ofSeconds(60));

      assertThat(props.getOpenai().getBaseUrl()).isEqualTo("https://custom.openai.com");
    }
  }

  @Nested
  @DisplayName("Configuration Bean Creation")
  final class ConfigurationBeanCreation {

    @Test
    @DisplayName("should_create_config_instance")
    void should_create_config_instance() {
      final ProviderProperties props =
          new ProviderProperties(
              "openai",
              new OpenAiProperties("key", "model", null),
              new AnthropicProperties(null, null),
              new GeminiProperties(null, null),
              new OllamaProperties(null, null),
              Duration.ofSeconds(60));

      final LangChain4jConfig config = new LangChain4jConfig(props);

      assertThat(config).isNotNull();
    }
  }

  @Nested
  @DisplayName("Provider Selection")
  final class ProviderSelection {

    @Test
    @DisplayName("should_identify_openai_provider")
    void should_identify_openai_provider() {
      final ProviderProperties props =
          new ProviderProperties(
              "openai",
              new OpenAiProperties("key", "gpt-4o", null),
              new AnthropicProperties(null, null),
              new GeminiProperties(null, null),
              new OllamaProperties(null, null),
              Duration.ofSeconds(60));

      assertThat(props.getProvider()).isEqualTo("openai");
    }

    @Test
    @DisplayName("should_identify_anthropic_provider")
    void should_identify_anthropic_provider() {
      final ProviderProperties props =
          new ProviderProperties(
              "anthropic",
              new OpenAiProperties(null, null, null),
              new AnthropicProperties("key", "claude-3"),
              new GeminiProperties(null, null),
              new OllamaProperties(null, null),
              Duration.ofSeconds(60));

      assertThat(props.getProvider()).isEqualTo("anthropic");
    }

    @Test
    @DisplayName("should_identify_gemini_provider")
    void should_identify_gemini_provider() {
      final ProviderProperties props =
          new ProviderProperties(
              "gemini",
              new OpenAiProperties(null, null, null),
              new AnthropicProperties(null, null),
              new GeminiProperties("key", "gemini-pro"),
              new OllamaProperties(null, null),
              Duration.ofSeconds(60));

      assertThat(props.getProvider()).isEqualTo("gemini");
    }

    @Test
    @DisplayName("should_identify_ollama_provider")
    void should_identify_ollama_provider() {
      final ProviderProperties props =
          new ProviderProperties(
              "ollama",
              new OpenAiProperties(null, null, null),
              new AnthropicProperties(null, null),
              new GeminiProperties(null, null),
              new OllamaProperties("http://localhost:11434", "llama3"),
              Duration.ofSeconds(60));

      assertThat(props.getProvider()).isEqualTo("ollama");
    }
  }
}
