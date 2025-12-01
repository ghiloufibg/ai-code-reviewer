package com.ghiloufi.aicode.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.TicketBusinessContext;
import com.ghiloufi.aicode.core.domain.model.TicketContext;
import com.ghiloufi.aicode.core.domain.port.output.TicketSystemPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

final class DefaultTicketContextServiceTest {

  private static final String SAMPLE_TICKET_ID = "TM-123";
  private static final String SAMPLE_TITLE = "Fix authentication bug";
  private static final String SAMPLE_DESCRIPTION = "Detailed description";

  @Test
  void should_extract_bracketed_ticket_id_from_title() {
    final TicketSystemPort ticketSystem = createMockTicketSystem();
    final TicketContextService service = new DefaultTicketContextService(ticketSystem);

    final Mono<TicketContext> result =
        service.extractFromMergeRequest("[TM-123] : Fix authentication", null);

    StepVerifier.create(result)
        .assertNext(
            context -> {
              assertThat(context.ticketId()).isEqualTo(SAMPLE_TICKET_ID);
              assertThat(context.title()).isEqualTo(SAMPLE_TITLE);
            })
        .verifyComplete();
  }

  @Test
  void should_extract_bracketed_ticket_id_from_description_when_not_in_title() {
    final TicketSystemPort ticketSystem = createMockTicketSystem();
    final TicketContextService service = new DefaultTicketContextService(ticketSystem);

    final Mono<TicketContext> result =
        service.extractFromMergeRequest(
            "Fix authentication", "This PR implements [TM-123] requirements");

    StepVerifier.create(result)
        .assertNext(context -> assertThat(context.ticketId()).isEqualTo(SAMPLE_TICKET_ID))
        .verifyComplete();
  }

  @Test
  void should_prioritize_title_over_description() {
    final TicketSystemPort ticketSystem = createMockTicketSystem();
    final TicketContextService service = new DefaultTicketContextService(ticketSystem);

    final Mono<TicketContext> result =
        service.extractFromMergeRequest("[TM-123] : Fix", "Related to [TM-999]");

    StepVerifier.create(result)
        .assertNext(context -> assertThat(context.ticketId()).isEqualTo(SAMPLE_TICKET_ID))
        .verifyComplete();
  }

  @Test
  void should_return_empty_when_no_ticket_id_found() {
    final TicketSystemPort ticketSystem = createMockTicketSystem();
    final TicketContextService service = new DefaultTicketContextService(ticketSystem);

    final Mono<TicketContext> result =
        service.extractFromMergeRequest("Fix authentication", "No ticket reference");

    StepVerifier.create(result)
        .assertNext(context -> assertThat(context.isEmpty()).isTrue())
        .verifyComplete();
  }

  @Test
  void should_return_empty_when_title_and_description_are_null() {
    final TicketSystemPort ticketSystem = createMockTicketSystem();
    final TicketContextService service = new DefaultTicketContextService(ticketSystem);

    final Mono<TicketContext> result = service.extractFromMergeRequest(null, null);

    StepVerifier.create(result)
        .assertNext(context -> assertThat(context.isEmpty()).isTrue())
        .verifyComplete();
  }

  @Test
  void should_return_empty_when_title_and_description_are_blank() {
    final TicketSystemPort ticketSystem = createMockTicketSystem();
    final TicketContextService service = new DefaultTicketContextService(ticketSystem);

    final Mono<TicketContext> result = service.extractFromMergeRequest("   ", "  ");

    StepVerifier.create(result)
        .assertNext(context -> assertThat(context.isEmpty()).isTrue())
        .verifyComplete();
  }

  @Test
  void should_not_extract_unbracketed_ticket_id() {
    final TicketSystemPort ticketSystem = createMockTicketSystem();
    final TicketContextService service = new DefaultTicketContextService(ticketSystem);

    final Mono<TicketContext> result =
        service.extractFromMergeRequest("TM-123: Fix authentication", null);

    StepVerifier.create(result)
        .assertNext(context -> assertThat(context.isEmpty()).isTrue())
        .verifyComplete();
  }

  @Test
  void should_match_various_bracketed_formats() {
    final TicketSystemPort ticketSystem = createMockTicketSystem();
    final TicketContextService service = new DefaultTicketContextService(ticketSystem);

    final Mono<TicketContext> result1 =
        service.extractFromMergeRequest("[PROJ-456] : Feature implementation", null);
    final Mono<TicketContext> result2 = service.extractFromMergeRequest("[ABC-1]: Quick fix", null);
    final Mono<TicketContext> result3 =
        service.extractFromMergeRequest("[LONGNAME-9999] Update API", null);

    StepVerifier.create(result1)
        .assertNext(context -> assertThat(context.ticketId()).isEqualTo("PROJ-456"))
        .verifyComplete();

    StepVerifier.create(result2)
        .assertNext(context -> assertThat(context.ticketId()).isEqualTo("ABC-1"))
        .verifyComplete();

    StepVerifier.create(result3)
        .assertNext(context -> assertThat(context.ticketId()).isEqualTo("LONGNAME-9999"))
        .verifyComplete();
  }

  @Test
  void should_extract_first_bracketed_ticket_when_multiple_present() {
    final TicketSystemPort ticketSystem = createMockTicketSystem();
    final TicketContextService service = new DefaultTicketContextService(ticketSystem);

    final Mono<TicketContext> result =
        service.extractFromMergeRequest("[TM-123] : Related to [TM-456]", null);

    StepVerifier.create(result)
        .assertNext(context -> assertThat(context.ticketId()).isEqualTo("TM-123"))
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
}
