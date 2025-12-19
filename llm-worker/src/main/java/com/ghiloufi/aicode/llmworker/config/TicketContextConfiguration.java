package com.ghiloufi.aicode.llmworker.config;

import com.ghiloufi.aicode.core.application.service.TicketContextService;
import com.ghiloufi.aicode.core.domain.port.output.TicketSystemPort;
import com.ghiloufi.aicode.llmworker.service.DefaultTicketContextService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TicketContextConfiguration {

  @Bean
  @ConditionalOnProperty(
      name = "ticket-analysis.llm.enabled",
      havingValue = "false",
      matchIfMissing = true)
  public TicketContextService defaultTicketContextService(final TicketSystemPort ticketSystemPort) {
    return new DefaultTicketContextService(ticketSystemPort);
  }
}
