package com.ghiloufi.aicode.llmworker.config;

import java.time.Duration;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Getter
@Validated
@ConfigurationProperties(prefix = "llm")
public class ProviderProperties {

  private final String provider;
  private final OpenAiProperties openai;
  private final AnthropicProperties anthropic;
  private final GeminiProperties gemini;
  private final OllamaProperties ollama;
  private final Duration timeout;

  public ProviderProperties(
      @DefaultValue("openai") String provider,
      OpenAiProperties openai,
      AnthropicProperties anthropic,
      GeminiProperties gemini,
      OllamaProperties ollama,
      @DefaultValue("120s") Duration timeout) {
    this.provider = provider;
    this.openai = openai != null ? openai : new OpenAiProperties(null, "gpt-4o", null);
    this.anthropic =
        anthropic != null ? anthropic : new AnthropicProperties(null, "claude-sonnet-4-20250514");
    this.gemini = gemini != null ? gemini : new GeminiProperties(null, "gemini-1.5-pro");
    this.ollama =
        ollama != null ? ollama : new OllamaProperties("http://localhost:11434", "llama3");
    this.timeout = timeout;
  }

  @Getter
  public static class OpenAiProperties {
    private final String apiKey;
    private final String model;
    private final String baseUrl;

    public OpenAiProperties(String apiKey, @DefaultValue("gpt-4o") String model, String baseUrl) {
      this.apiKey = apiKey;
      this.model = model;
      this.baseUrl = baseUrl;
    }
  }

  @Getter
  public static class AnthropicProperties {
    private final String apiKey;
    private final String model;

    public AnthropicProperties(
        String apiKey, @DefaultValue("claude-sonnet-4-20250514") String model) {
      this.apiKey = apiKey;
      this.model = model;
    }
  }

  @Getter
  public static class GeminiProperties {
    private final String apiKey;
    private final String model;

    public GeminiProperties(String apiKey, @DefaultValue("gemini-1.5-pro") String model) {
      this.apiKey = apiKey;
      this.model = model;
    }
  }

  @Getter
  public static class OllamaProperties {
    private final String baseUrl;
    private final String model;

    public OllamaProperties(
        @DefaultValue("http://localhost:11434") String baseUrl,
        @DefaultValue("llama3") String model) {
      this.baseUrl = baseUrl;
      this.model = model;
    }
  }
}
