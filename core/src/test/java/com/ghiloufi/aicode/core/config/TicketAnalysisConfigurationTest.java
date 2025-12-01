package com.ghiloufi.aicode.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.application.service.DefaultTicketContextService;
import com.ghiloufi.aicode.core.application.service.TicketContextService;
import com.ghiloufi.aicode.core.domain.model.TicketBusinessContext;
import com.ghiloufi.aicode.core.domain.port.output.TicketSystemPort;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Mono;

final class TicketAnalysisConfigurationTest {

  @Nested
  @SpringBootTest(
      classes = {TicketAnalysisConfiguration.class, TestTicketSystemConfig.class},
      properties = {"ticket-analysis.llm.enabled=false"})
  final class WhenFeatureDisabled {

    @Autowired private ApplicationContext context;

    @Test
    void should_create_default_ticket_context_service() {
      final TicketContextService service = context.getBean(TicketContextService.class);
      assertThat(service).isInstanceOf(DefaultTicketContextService.class);
    }

    @Test
    void should_not_create_ticket_analysis_port() {
      assertThat(
              context.getBeanNamesForType(
                  com.ghiloufi.aicode.core.domain.port.output.TicketAnalysisPort.class))
          .isEmpty();
    }
  }

  @Nested
  @SpringBootTest(classes = {TicketAnalysisConfiguration.class, TestTicketSystemConfig.class})
  final class WhenFeatureNotConfigured {

    @Autowired private ApplicationContext context;

    @Test
    void should_default_to_disabled_and_create_default_service() {
      final TicketContextService service = context.getBean(TicketContextService.class);
      assertThat(service).isInstanceOf(DefaultTicketContextService.class);
    }
  }

  static class TestTicketSystemConfig {

    @org.springframework.context.annotation.Bean
    TicketSystemPort ticketSystemPort() {
      return ticketId -> Mono.just(new TicketBusinessContext(ticketId, "Test", "Description"));
    }
  }
}
