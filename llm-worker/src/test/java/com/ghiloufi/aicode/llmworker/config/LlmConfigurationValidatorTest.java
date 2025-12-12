package com.ghiloufi.aicode.llmworker.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ghiloufi.aicode.llmworker.config.ProviderProperties.AnthropicProperties;
import com.ghiloufi.aicode.llmworker.config.ProviderProperties.GeminiProperties;
import com.ghiloufi.aicode.llmworker.config.ProviderProperties.OllamaProperties;
import com.ghiloufi.aicode.llmworker.config.ProviderProperties.OpenAiProperties;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("LlmConfigurationValidator Tests")
final class LlmConfigurationValidatorTest {

  @Nested
  @DisplayName("OpenAI Provider Validation")
  final class OpenAiValidationTests {

    @Test
    @DisplayName("should_validate_valid_openai_configuration")
    final void should_validate_valid_openai_configuration() {
      final ProviderProperties properties =
          createProperties(
              "openai", new OpenAiProperties("sk-valid-api-key", "gpt-4o", null), null, null, null);

      final LlmConfigurationValidator validator = new LlmConfigurationValidator(properties);

      assertThatCode(validator::validateConfiguration).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should_validate_openai_with_custom_base_url")
    final void should_validate_openai_with_custom_base_url() {
      final ProviderProperties properties =
          createProperties(
              "openai",
              new OpenAiProperties("sk-valid-api-key", "gpt-4o", "https://custom.openai.azure.com"),
              null,
              null,
              null);

      final LlmConfigurationValidator validator = new LlmConfigurationValidator(properties);

      assertThatCode(validator::validateConfiguration).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should_reject_openai_with_null_api_key")
    final void should_reject_openai_with_null_api_key() {
      final ProviderProperties properties =
          createProperties("openai", new OpenAiProperties(null, "gpt-4o", null), null, null, null);

      final LlmConfigurationValidator validator = new LlmConfigurationValidator(properties);

      assertThatThrownBy(validator::validateConfiguration)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("llm.openai.api-key")
          .hasMessageContaining("not configured");
    }

    @Test
    @DisplayName("should_reject_openai_with_blank_api_key")
    final void should_reject_openai_with_blank_api_key() {
      final ProviderProperties properties =
          createProperties("openai", new OpenAiProperties("   ", "gpt-4o", null), null, null, null);

      final LlmConfigurationValidator validator = new LlmConfigurationValidator(properties);

      assertThatThrownBy(validator::validateConfiguration)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("llm.openai.api-key");
    }

    @Test
    @DisplayName("should_reject_openai_with_null_model")
    final void should_reject_openai_with_null_model() {
      final ProviderProperties properties =
          createProperties(
              "openai", new OpenAiProperties("sk-valid-key", null, null), null, null, null);

      final LlmConfigurationValidator validator = new LlmConfigurationValidator(properties);

      assertThatThrownBy(validator::validateConfiguration)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("llm.openai.model");
    }

    @Test
    @DisplayName("should_reject_openai_with_empty_model")
    final void should_reject_openai_with_empty_model() {
      final ProviderProperties properties =
          createProperties(
              "openai", new OpenAiProperties("sk-valid-key", "", null), null, null, null);

      final LlmConfigurationValidator validator = new LlmConfigurationValidator(properties);

      assertThatThrownBy(validator::validateConfiguration)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("llm.openai.model");
    }
  }

  @Nested
  @DisplayName("Anthropic Provider Validation")
  final class AnthropicValidationTests {

    @Test
    @DisplayName("should_validate_valid_anthropic_configuration")
    final void should_validate_valid_anthropic_configuration() {
      final ProviderProperties properties =
          createProperties(
              "anthropic",
              null,
              new AnthropicProperties("sk-ant-valid-key", "claude-sonnet-4-20250514"),
              null,
              null);

      final LlmConfigurationValidator validator = new LlmConfigurationValidator(properties);

      assertThatCode(validator::validateConfiguration).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should_reject_anthropic_with_null_api_key")
    final void should_reject_anthropic_with_null_api_key() {
      final ProviderProperties properties =
          createProperties(
              "anthropic",
              null,
              new AnthropicProperties(null, "claude-sonnet-4-20250514"),
              null,
              null);

      final LlmConfigurationValidator validator = new LlmConfigurationValidator(properties);

      assertThatThrownBy(validator::validateConfiguration)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("llm.anthropic.api-key");
    }

    @Test
    @DisplayName("should_reject_anthropic_with_blank_model")
    final void should_reject_anthropic_with_blank_model() {
      final ProviderProperties properties =
          createProperties(
              "anthropic", null, new AnthropicProperties("sk-ant-valid-key", "   "), null, null);

      final LlmConfigurationValidator validator = new LlmConfigurationValidator(properties);

      assertThatThrownBy(validator::validateConfiguration)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("llm.anthropic.model");
    }
  }

  @Nested
  @DisplayName("Gemini Provider Validation")
  final class GeminiValidationTests {

    @Test
    @DisplayName("should_validate_valid_gemini_configuration")
    final void should_validate_valid_gemini_configuration() {
      final ProviderProperties properties =
          createProperties(
              "gemini", null, null, new GeminiProperties("AIza-valid-key", "gemini-1.5-pro"), null);

      final LlmConfigurationValidator validator = new LlmConfigurationValidator(properties);

      assertThatCode(validator::validateConfiguration).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should_reject_gemini_with_null_api_key")
    final void should_reject_gemini_with_null_api_key() {
      final ProviderProperties properties =
          createProperties(
              "gemini", null, null, new GeminiProperties(null, "gemini-1.5-pro"), null);

      final LlmConfigurationValidator validator = new LlmConfigurationValidator(properties);

      assertThatThrownBy(validator::validateConfiguration)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("llm.gemini.api-key");
    }

    @Test
    @DisplayName("should_reject_gemini_with_empty_model")
    final void should_reject_gemini_with_empty_model() {
      final ProviderProperties properties =
          createProperties("gemini", null, null, new GeminiProperties("AIza-valid-key", ""), null);

      final LlmConfigurationValidator validator = new LlmConfigurationValidator(properties);

      assertThatThrownBy(validator::validateConfiguration)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("llm.gemini.model");
    }
  }

  @Nested
  @DisplayName("Ollama Provider Validation")
  final class OllamaValidationTests {

    @Test
    @DisplayName("should_validate_valid_ollama_configuration")
    final void should_validate_valid_ollama_configuration() {
      final ProviderProperties properties =
          createProperties(
              "ollama", null, null, null, new OllamaProperties("http://localhost:11434", "llama3"));

      final LlmConfigurationValidator validator = new LlmConfigurationValidator(properties);

      assertThatCode(validator::validateConfiguration).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should_reject_ollama_with_null_base_url")
    final void should_reject_ollama_with_null_base_url() {
      final ProviderProperties properties =
          createProperties("ollama", null, null, null, new OllamaProperties(null, "llama3"));

      final LlmConfigurationValidator validator = new LlmConfigurationValidator(properties);

      assertThatThrownBy(validator::validateConfiguration)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("llm.ollama.base-url");
    }

    @Test
    @DisplayName("should_reject_ollama_with_blank_model")
    final void should_reject_ollama_with_blank_model() {
      final ProviderProperties properties =
          createProperties(
              "ollama", null, null, null, new OllamaProperties("http://localhost:11434", "   "));

      final LlmConfigurationValidator validator = new LlmConfigurationValidator(properties);

      assertThatThrownBy(validator::validateConfiguration)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("llm.ollama.model");
    }
  }

  @Nested
  @DisplayName("Unknown Provider Validation")
  final class UnknownProviderTests {

    @Test
    @DisplayName("should_reject_unknown_provider")
    final void should_reject_unknown_provider() {
      final ProviderProperties properties =
          createProperties("unknown-provider", null, null, null, null);

      final LlmConfigurationValidator validator = new LlmConfigurationValidator(properties);

      assertThatThrownBy(validator::validateConfiguration)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Unknown LLM provider")
          .hasMessageContaining("unknown-provider")
          .hasMessageContaining("Supported providers: openai, anthropic, gemini, ollama");
    }

    @Test
    @DisplayName("should_reject_empty_provider")
    final void should_reject_empty_provider() {
      final ProviderProperties properties = createProperties("", null, null, null, null);

      final LlmConfigurationValidator validator = new LlmConfigurationValidator(properties);

      assertThatThrownBy(validator::validateConfiguration)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Unknown LLM provider");
    }
  }

  @Nested
  @DisplayName("Case Insensitivity")
  final class CaseInsensitivityTests {

    @Test
    @DisplayName("should_validate_openai_with_uppercase")
    final void should_validate_openai_with_uppercase() {
      final ProviderProperties properties =
          createProperties(
              "OPENAI", new OpenAiProperties("sk-valid-key", "gpt-4o", null), null, null, null);

      final LlmConfigurationValidator validator = new LlmConfigurationValidator(properties);

      assertThatCode(validator::validateConfiguration).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should_validate_anthropic_with_mixed_case")
    final void should_validate_anthropic_with_mixed_case() {
      final ProviderProperties properties =
          createProperties(
              "AnThRoPiC", null, new AnthropicProperties("sk-ant-key", "claude-3"), null, null);

      final LlmConfigurationValidator validator = new LlmConfigurationValidator(properties);

      assertThatCode(validator::validateConfiguration).doesNotThrowAnyException();
    }
  }

  private ProviderProperties createProperties(
      final String provider,
      final OpenAiProperties openai,
      final AnthropicProperties anthropic,
      final GeminiProperties gemini,
      final OllamaProperties ollama) {
    return new ProviderProperties(
        provider, openai, anthropic, gemini, ollama, Duration.ofSeconds(120));
  }
}
