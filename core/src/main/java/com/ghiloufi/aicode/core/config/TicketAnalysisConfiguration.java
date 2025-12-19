package com.ghiloufi.aicode.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghiloufi.aicode.core.application.service.AnalyzingTicketContextService;
import com.ghiloufi.aicode.core.application.service.TicketContextService;
import com.ghiloufi.aicode.core.domain.port.output.TicketAnalysisPort;
import com.ghiloufi.aicode.core.domain.port.output.TicketSystemPort;
import com.ghiloufi.aicode.core.infrastructure.adapter.OpenAITicketAnalysisAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TicketAnalysisPromptProperties.class)
public class TicketAnalysisConfiguration {

  @Bean
  @ConditionalOnProperty(name = "ticket-analysis.llm.enabled", havingValue = "true")
  public TicketAnalysisPort ticketAnalysisPort(
      @Value("${ticket-analysis.llm.base-url:http://localhost:1234/v1}") final String baseUrl,
      @Value("${ticket-analysis.llm.model:gpt-4o-mini}") final String model,
      @Value("${ticket-analysis.llm.api-key:not-required}") final String apiKey,
      final ObjectMapper objectMapper,
      final TicketAnalysisPromptProperties promptProperties) {
    return new OpenAITicketAnalysisAdapter(baseUrl, model, apiKey, objectMapper, promptProperties);
  }

  @Bean
  @ConditionalOnProperty(name = "ticket-analysis.llm.enabled", havingValue = "true")
  public TicketContextService analyzingTicketContextService(
      final TicketSystemPort ticketSystemPort, final TicketAnalysisPort ticketAnalysisPort) {
    return new AnalyzingTicketContextService(ticketSystemPort, ticketAnalysisPort);
  }
}
