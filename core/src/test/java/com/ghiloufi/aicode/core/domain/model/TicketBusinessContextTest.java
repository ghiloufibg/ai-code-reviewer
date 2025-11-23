package com.ghiloufi.aicode.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

final class TicketBusinessContextTest {

  @Test
  void should_format_ticket_with_description_for_prompt() {
    final TicketBusinessContext context =
        new TicketBusinessContext(
            "TM-1",
            "Add commenting capability",
            """
        h2. Business Requirements
        REQ-1: Users must be able to comment on tasks

        h2. Acceptance Criteria
        GIVEN task exists
        WHEN user adds comment
        THEN comment is saved
        """);

    final String formatted = context.formatForPrompt();

    assertThat(formatted).contains("BUSINESS CONTEXT FROM TICKET: TM-1");
    assertThat(formatted).contains("Feature/Fix: Add commenting capability");
    assertThat(formatted).contains("TICKET DESCRIPTION:");
    assertThat(formatted).contains("h2. Business Requirements");
    assertThat(formatted).contains("REQ-1: Users must be able to comment");
  }

  @Test
  void should_return_empty_string_when_no_description() {
    final TicketBusinessContext context = new TicketBusinessContext("TM-2", "Fix bug", null);

    final String formatted = context.formatForPrompt();

    assertThat(formatted).isEmpty();
  }

  @Test
  void should_return_empty_string_when_description_is_blank() {
    final TicketBusinessContext context = new TicketBusinessContext("TM-3", "Update docs", "   ");

    final String formatted = context.formatForPrompt();

    assertThat(formatted).isEmpty();
  }

  @Test
  void should_check_if_has_description() {
    final TicketBusinessContext withDescription =
        new TicketBusinessContext("TM-1", "Title", "Description");
    final TicketBusinessContext withoutDescription =
        new TicketBusinessContext("TM-2", "Title", null);
    final TicketBusinessContext withBlankDescription =
        new TicketBusinessContext("TM-3", "Title", "  ");

    assertThat(withDescription.hasDescription()).isTrue();
    assertThat(withoutDescription.hasDescription()).isFalse();
    assertThat(withBlankDescription.hasDescription()).isFalse();
  }

  @Test
  void should_throw_exception_when_ticket_id_is_null() {
    assertThatThrownBy(() -> new TicketBusinessContext(null, "Title", "Description"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Ticket ID cannot be null");
  }

  @Test
  void should_throw_exception_when_title_is_null() {
    assertThatThrownBy(() -> new TicketBusinessContext("TM-1", null, "Description"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Title cannot be null");
  }

  @Test
  void should_create_ticket_context_with_all_fields() {
    final TicketBusinessContext context = new TicketBusinessContext("TM-1", "Title", "Description");

    assertThat(context.ticketId()).isEqualTo("TM-1");
    assertThat(context.title()).isEqualTo("Title");
    assertThat(context.fullDescription()).isEqualTo("Description");
  }

  @Test
  void should_create_empty_ticket_context() {
    final TicketBusinessContext emptyContext = TicketBusinessContext.empty();

    assertThat(emptyContext.isEmpty()).isTrue();
    assertThat(emptyContext.ticketId()).isEmpty();
    assertThat(emptyContext.title()).isEmpty();
    assertThat(emptyContext.fullDescription()).isNull();
    assertThat(emptyContext.hasDescription()).isFalse();
  }

  @Test
  void should_return_empty_string_when_formatting_empty_context() {
    final TicketBusinessContext emptyContext = TicketBusinessContext.empty();

    final String formatted = emptyContext.formatForPrompt();

    assertThat(formatted).isEmpty();
  }

  @Test
  void should_identify_non_empty_context() {
    final TicketBusinessContext context = new TicketBusinessContext("TM-1", "Title", "Description");

    assertThat(context.isEmpty()).isFalse();
  }
}
