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

@DisplayName("ProviderProperties Tests")
final class ProviderPropertiesTest {

  @Nested
  @DisplayName("Constructor Defaults")
  final class ConstructorDefaultsTests {

    @Test
    @DisplayName("should_use_default_provider_when_null")
    final void should_use_default_provider_when_null() {
      final ProviderProperties properties =
          new ProviderProperties(null, null, null, null, null, null);

      assertThat(properties.getProvider()).isNull();
    }

    @Test
    @DisplayName("should_use_provided_provider")
    final void should_use_provided_provider() {
      final ProviderProperties properties =
          new ProviderProperties("anthropic", null, null, null, null, null);

      assertThat(properties.getProvider()).isEqualTo("anthropic");
    }

    @Test
    @DisplayName("should_use_default_timeout_when_null")
    final void should_use_default_timeout_when_null() {
      final ProviderProperties properties =
          new ProviderProperties("openai", null, null, null, null, null);

      assertThat(properties.getTimeout()).isNull();
    }

    @Test
    @DisplayName("should_use_provided_timeout")
    final void should_use_provided_timeout() {
      final Duration customTimeout = Duration.ofSeconds(300);
      final ProviderProperties properties =
          new ProviderProperties("openai", null, null, null, null, customTimeout);

      assertThat(properties.getTimeout()).isEqualTo(customTimeout);
    }

    @Test
    @DisplayName("should_create_default_openai_when_null")
    final void should_create_default_openai_when_null() {
      final ProviderProperties properties =
          new ProviderProperties("openai", null, null, null, null, null);

      assertThat(properties.getOpenai()).isNotNull();
      assertThat(properties.getOpenai().getModel()).isEqualTo("gpt-4o");
    }

    @Test
    @DisplayName("should_create_default_anthropic_when_null")
    final void should_create_default_anthropic_when_null() {
      final ProviderProperties properties =
          new ProviderProperties("anthropic", null, null, null, null, null);

      assertThat(properties.getAnthropic()).isNotNull();
      assertThat(properties.getAnthropic().getModel()).isEqualTo("claude-sonnet-4-20250514");
    }

    @Test
    @DisplayName("should_create_default_gemini_when_null")
    final void should_create_default_gemini_when_null() {
      final ProviderProperties properties =
          new ProviderProperties("gemini", null, null, null, null, null);

      assertThat(properties.getGemini()).isNotNull();
      assertThat(properties.getGemini().getModel()).isEqualTo("gemini-1.5-pro");
    }

    @Test
    @DisplayName("should_create_default_ollama_when_null")
    final void should_create_default_ollama_when_null() {
      final ProviderProperties properties =
          new ProviderProperties("ollama", null, null, null, null, null);

      assertThat(properties.getOllama()).isNotNull();
      assertThat(properties.getOllama().getBaseUrl()).isEqualTo("http://localhost:11434");
      assertThat(properties.getOllama().getModel()).isEqualTo("llama3");
    }
  }

  @Nested
  @DisplayName("OpenAiProperties")
  final class OpenAiPropertiesTests {

    @Test
    @DisplayName("should_store_all_openai_fields")
    final void should_store_all_openai_fields() {
      final OpenAiProperties openai =
          new OpenAiProperties("sk-test-key", "gpt-4o-mini", "https://api.custom.com");

      assertThat(openai.getApiKey()).isEqualTo("sk-test-key");
      assertThat(openai.getModel()).isEqualTo("gpt-4o-mini");
      assertThat(openai.getBaseUrl()).isEqualTo("https://api.custom.com");
    }

    @Test
    @DisplayName("should_allow_null_base_url")
    final void should_allow_null_base_url() {
      final OpenAiProperties openai = new OpenAiProperties("sk-test-key", "gpt-4o", null);

      assertThat(openai.getBaseUrl()).isNull();
    }
  }

  @Nested
  @DisplayName("AnthropicProperties")
  final class AnthropicPropertiesTests {

    @Test
    @DisplayName("should_store_anthropic_fields")
    final void should_store_anthropic_fields() {
      final AnthropicProperties anthropic =
          new AnthropicProperties("sk-ant-api-key", "claude-opus-4-20250514");

      assertThat(anthropic.getApiKey()).isEqualTo("sk-ant-api-key");
      assertThat(anthropic.getModel()).isEqualTo("claude-opus-4-20250514");
    }
  }

  @Nested
  @DisplayName("GeminiProperties")
  final class GeminiPropertiesTests {

    @Test
    @DisplayName("should_store_gemini_fields")
    final void should_store_gemini_fields() {
      final GeminiProperties gemini = new GeminiProperties("AIza-test-key", "gemini-2.0-flash");

      assertThat(gemini.getApiKey()).isEqualTo("AIza-test-key");
      assertThat(gemini.getModel()).isEqualTo("gemini-2.0-flash");
    }
  }

  @Nested
  @DisplayName("OllamaProperties")
  final class OllamaPropertiesTests {

    @Test
    @DisplayName("should_store_ollama_fields")
    final void should_store_ollama_fields() {
      final OllamaProperties ollama =
          new OllamaProperties("http://remote-server:11434", "codellama");

      assertThat(ollama.getBaseUrl()).isEqualTo("http://remote-server:11434");
      assertThat(ollama.getModel()).isEqualTo("codellama");
    }
  }

  @Nested
  @DisplayName("Full Configuration")
  final class FullConfigurationTests {

    @Test
    @DisplayName("should_preserve_all_provider_properties")
    final void should_preserve_all_provider_properties() {
      final OpenAiProperties openai = new OpenAiProperties("openai-key", "gpt-4o", null);
      final AnthropicProperties anthropic = new AnthropicProperties("anthropic-key", "claude-3");
      final GeminiProperties gemini = new GeminiProperties("gemini-key", "gemini-pro");
      final OllamaProperties ollama = new OllamaProperties("http://localhost:11434", "llama3");

      final ProviderProperties properties =
          new ProviderProperties(
              "openai", openai, anthropic, gemini, ollama, Duration.ofMinutes(2));

      assertThat(properties.getProvider()).isEqualTo("openai");
      assertThat(properties.getOpenai().getApiKey()).isEqualTo("openai-key");
      assertThat(properties.getAnthropic().getApiKey()).isEqualTo("anthropic-key");
      assertThat(properties.getGemini().getApiKey()).isEqualTo("gemini-key");
      assertThat(properties.getOllama().getBaseUrl()).isEqualTo("http://localhost:11434");
      assertThat(properties.getTimeout()).isEqualTo(Duration.ofMinutes(2));
    }
  }
}
