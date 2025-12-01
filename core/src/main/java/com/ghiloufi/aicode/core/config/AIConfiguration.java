package com.ghiloufi.aicode.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghiloufi.aicode.core.domain.port.output.AIInteractionPort;
import com.ghiloufi.aicode.core.infrastructure.adapter.OpenAIAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfiguration {

  @Bean
  public AIInteractionPort aiInteractionPort(
      @Value("${llm.providers.openai.base-url:http://localhost:1234/v1}") final String baseUrl,
      @Value("${llm.providers.openai.default-model:gpt-4o-mini}") final String model,
      @Value("${llm.providers.openai.api-key:not-required}") final String apiKey,
      final ObjectMapper objectMapper) {
    return new OpenAIAdapter(baseUrl, model, apiKey, objectMapper);
  }
}
