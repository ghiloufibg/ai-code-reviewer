package com.ghiloufi.aicode.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.StructuredTicketAnalysis;
import com.ghiloufi.aicode.core.domain.model.TicketBusinessContext;
import com.ghiloufi.aicode.core.domain.model.TicketContext;
import com.ghiloufi.aicode.core.domain.port.output.TicketAnalysisPort;
import com.ghiloufi.aicode.core.domain.port.output.TicketSystemPort;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

final class AnalyzingTicketContextServiceTest {

  private static final String SAMPLE_TICKET_ID = "TM-123";
  private static final String SAMPLE_TITLE = "Add authentication";
  private static final String SAMPLE_DESCRIPTION = "Implement OAuth2 authentication flow";

  @Test
  void should_extract_and_analyze_ticket_from_title() {
    final TicketSystemPort ticketSystem = createMockTicketSystem();
    final TicketAnalysisPort analysisPort = createSuccessfulAnalysisPort();
    final TicketContextService service =
        new AnalyzingTicketContextService(ticketSystem, analysisPort);

    final Mono<TicketContext> result =
        service.extractFromMergeRequest("[TM-123] : Fix authentication", null);

    StepVerifier.create(result)
        .assertNext(
            context -> {
              assertThat(context).isInstanceOf(StructuredTicketAnalysis.class);
              final StructuredTicketAnalysis analysis = (StructuredTicketAnalysis) context;
              assertThat(analysis.ticketId()).isEqualTo(SAMPLE_TICKET_ID);
              assertThat(analysis.objective()).isEqualTo("Implement secure login");
              assertThat(analysis.acceptanceCriteria()).containsExactly("Users can login");
            })
        .verifyComplete();
  }

  @Test
  void should_extract_and_analyze_ticket_from_description() {
    final TicketSystemPort ticketSystem = createMockTicketSystem();
    final TicketAnalysisPort analysisPort = createSuccessfulAnalysisPort();
    final TicketContextService service =
        new AnalyzingTicketContextService(ticketSystem, analysisPort);

    final Mono<TicketContext> result =
        service.extractFromMergeRequest("Fix authentication", "This implements [TM-123]");

    StepVerifier.create(result)
        .assertNext(
            context -> {
              assertThat(context).isInstanceOf(StructuredTicketAnalysis.class);
              assertThat(context.ticketId()).isEqualTo(SAMPLE_TICKET_ID);
            })
        .verifyComplete();
  }

  @Test
  void should_return_empty_when_no_ticket_id_found() {
    final TicketSystemPort ticketSystem = createMockTicketSystem();
    final TicketAnalysisPort analysisPort = createSuccessfulAnalysisPort();
    final TicketContextService service =
        new AnalyzingTicketContextService(ticketSystem, analysisPort);

    final Mono<TicketContext> result =
        service.extractFromMergeRequest("Fix authentication", "No ticket reference");

    StepVerifier.create(result)
        .assertNext(context -> assertThat(context.isEmpty()).isTrue())
        .verifyComplete();
  }

  @Test
  void should_return_empty_when_title_and_description_are_null() {
    final TicketSystemPort ticketSystem = createMockTicketSystem();
    final TicketAnalysisPort analysisPort = createSuccessfulAnalysisPort();
    final TicketContextService service =
        new AnalyzingTicketContextService(ticketSystem, analysisPort);

    final Mono<TicketContext> result = service.extractFromMergeRequest(null, null);

    StepVerifier.create(result)
        .assertNext(context -> assertThat(context.isEmpty()).isTrue())
        .verifyComplete();
  }

  @Test
  void should_fallback_to_raw_context_when_analysis_fails() {
    final TicketSystemPort ticketSystem = createMockTicketSystem();
    final TicketAnalysisPort analysisPort = createFailingAnalysisPort();
    final TicketContextService service =
        new AnalyzingTicketContextService(ticketSystem, analysisPort);

    final Mono<TicketContext> result = service.extractFromMergeRequest("[TM-123] : Feature", null);

    StepVerifier.create(result)
        .assertNext(
            context -> {
              assertThat(context).isInstanceOf(TicketBusinessContext.class);
              assertThat(context.ticketId()).isEqualTo(SAMPLE_TICKET_ID);
              assertThat(context.title()).isEqualTo(SAMPLE_TITLE);
            })
        .verifyComplete();
  }

  @Test
  void should_skip_analysis_when_ticket_has_no_description() {
    final TicketSystemPort ticketSystem =
        ticketId -> Mono.just(new TicketBusinessContext(ticketId, "Title only", null));
    final TicketAnalysisPort analysisPort = createSuccessfulAnalysisPort();
    final TicketContextService service =
        new AnalyzingTicketContextService(ticketSystem, analysisPort);

    final Mono<TicketContext> result = service.extractFromMergeRequest("[TM-123] : Feature", null);

    StepVerifier.create(result)
        .assertNext(
            context -> {
              assertThat(context).isInstanceOf(TicketBusinessContext.class);
              assertThat(context.ticketId()).isEqualTo(SAMPLE_TICKET_ID);
            })
        .verifyComplete();
  }

  @Test
  void should_skip_analysis_when_ticket_is_empty() {
    final TicketSystemPort ticketSystem = ticketId -> Mono.just(TicketBusinessContext.empty());
    final TicketAnalysisPort analysisPort = createSuccessfulAnalysisPort();
    final TicketContextService service =
        new AnalyzingTicketContextService(ticketSystem, analysisPort);

    final Mono<TicketContext> result = service.extractFromMergeRequest("[TM-123] : Feature", null);

    StepVerifier.create(result)
        .assertNext(context -> assertThat(context.isEmpty()).isTrue())
        .verifyComplete();
  }

  @Test
  void should_return_empty_when_ticket_system_returns_empty() {
    final TicketSystemPort ticketSystem = ticketId -> Mono.empty();
    final TicketAnalysisPort analysisPort = createSuccessfulAnalysisPort();
    final TicketContextService service =
        new AnalyzingTicketContextService(ticketSystem, analysisPort);

    final Mono<TicketContext> result =
        service.extractFromMergeRequest("[TM-999] : Unknown ticket", null);

    StepVerifier.create(result)
        .assertNext(context -> assertThat(context.isEmpty()).isTrue())
        .verifyComplete();
  }

  @Test
  void should_prioritize_title_over_description_for_ticket_id() {
    final TicketSystemPort ticketSystem = createMockTicketSystem();
    final TicketAnalysisPort analysisPort = createSuccessfulAnalysisPort();
    final TicketContextService service =
        new AnalyzingTicketContextService(ticketSystem, analysisPort);

    final Mono<TicketContext> result =
        service.extractFromMergeRequest("[TM-123] : Fix", "Related to [TM-999]");

    StepVerifier.create(result)
        .assertNext(context -> assertThat(context.ticketId()).isEqualTo(SAMPLE_TICKET_ID))
        .verifyComplete();
  }

  private TicketSystemPort createMockTicketSystem() {
    return ticketId -> {
      if (SAMPLE_TICKET_ID.equals(ticketId) || ticketId.matches("[A-Z]+-\\d+")) {
        return Mono.just(new TicketBusinessContext(ticketId, SAMPLE_TITLE, SAMPLE_DESCRIPTION));
      }
      return Mono.empty();
    };
  }

  private TicketAnalysisPort createSuccessfulAnalysisPort() {
    return new TicketAnalysisPort() {
      @Override
      public Mono<StructuredTicketAnalysis> analyzeTicket(final TicketBusinessContext rawContext) {
        return Mono.just(
            StructuredTicketAnalysis.fromRawContext(
                rawContext,
                "Implement secure login",
                List.of("Users can login"),
                List.of("Hash passwords"),
                List.of("Use JWT")));
      }

      @Override
      public String getProviderName() {
        return "test-provider";
      }

      @Override
      public String getModelName() {
        return "test-model";
      }
    };
  }

  private TicketAnalysisPort createFailingAnalysisPort() {
    return new TicketAnalysisPort() {
      @Override
      public Mono<StructuredTicketAnalysis> analyzeTicket(final TicketBusinessContext rawContext) {
        return Mono.error(new RuntimeException("LLM analysis failed"));
      }

      @Override
      public String getProviderName() {
        return "failing-provider";
      }

      @Override
      public String getModelName() {
        return "failing-model";
      }
    };
  }
}
