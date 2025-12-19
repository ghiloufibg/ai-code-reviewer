package com.ghiloufi.aicode.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.application.service.AnalyzingTicketContextService;
import com.ghiloufi.aicode.core.application.service.TicketContextService;
import com.ghiloufi.aicode.core.domain.model.TicketBusinessContext;
import com.ghiloufi.aicode.core.domain.port.output.TicketAnalysisPort;
import com.ghiloufi.aicode.core.domain.port.output.TicketSystemPort;
import com.ghiloufi.aicode.core.infrastructure.adapter.OpenAITicketAnalysisAdapter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Mono;

final class TicketAnalysisConfigurationTest {

  @Nested
  @SpringBootTest(
      classes = {TicketAnalysisConfiguration.class, TestConfig.class},
      properties = {"ticket-analysis.llm.enabled=false"})
  final class WhenFeatureDisabled {

    @Autowired private ApplicationContext context;

    @Test
    void should_not_create_ticket_analysis_port() {
      assertThat(context.getBeanNamesForType(TicketAnalysisPort.class)).isEmpty();
    }

    @Test
    void should_not_create_analyzing_ticket_context_service() {
      assertThat(context.getBeanNamesForType(TicketContextService.class)).isEmpty();
    }
  }

  @Nested
  @SpringBootTest(classes = {TicketAnalysisConfiguration.class, TestConfig.class})
  final class WhenFeatureNotConfigured {

    @Autowired private ApplicationContext context;

    @Test
    void should_default_to_disabled_and_not_create_analysis_port() {
      assertThat(context.getBeanNamesForType(TicketAnalysisPort.class)).isEmpty();
    }
  }

  @Nested
  @SpringBootTest(
      classes = {TicketAnalysisConfiguration.class, TestConfig.class},
      properties = {
        "ticket-analysis.llm.enabled=true",
        "ticket-analysis.llm.base-url=http://localhost:1234/v1",
        "ticket-analysis.llm.model=gpt-4o-mini",
        "ticket-analysis.llm.api-key=test-key"
      })
  final class WhenFeatureEnabled {

    @Autowired private ApplicationContext context;

    @Test
    void should_create_ticket_analysis_port() {
      final TicketAnalysisPort port = context.getBean(TicketAnalysisPort.class);
      assertThat(port).isInstanceOf(OpenAITicketAnalysisAdapter.class);
    }

    @Test
    void should_create_analyzing_ticket_context_service() {
      final TicketContextService service = context.getBean(TicketContextService.class);
      assertThat(service).isInstanceOf(AnalyzingTicketContextService.class);
    }
  }

  @org.springframework.boot.test.context.TestConfiguration
  static class TestConfig {

    @org.springframework.context.annotation.Bean
    TicketSystemPort ticketSystemPort() {
      return ticketId -> Mono.just(new TicketBusinessContext(ticketId, "Test", "Description"));
    }

    @org.springframework.context.annotation.Bean
    com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
      return new com.fasterxml.jackson.databind.ObjectMapper();
    }
  }
}
