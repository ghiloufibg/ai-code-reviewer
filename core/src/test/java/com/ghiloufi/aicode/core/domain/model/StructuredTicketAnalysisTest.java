package com.ghiloufi.aicode.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

final class StructuredTicketAnalysisTest {

  @Test
  void should_create_structured_analysis_with_all_fields() {
    final StructuredTicketAnalysis analysis =
        new StructuredTicketAnalysis(
            "TM-123",
            "Add user authentication",
            "Full description here",
            "Implement secure authentication",
            List.of("Users can login", "Users can logout"),
            List.of("Passwords must be hashed"),
            List.of("Use JWT tokens"));

    assertThat(analysis.ticketId()).isEqualTo("TM-123");
    assertThat(analysis.title()).isEqualTo("Add user authentication");
    assertThat(analysis.fullDescription()).isEqualTo("Full description here");
    assertThat(analysis.objective()).isEqualTo("Implement secure authentication");
    assertThat(analysis.acceptanceCriteria())
        .containsExactly("Users can login", "Users can logout");
    assertThat(analysis.businessRules()).containsExactly("Passwords must be hashed");
    assertThat(analysis.technicalNotes()).containsExactly("Use JWT tokens");
  }

  @Test
  void should_throw_exception_when_ticket_id_is_null() {
    assertThatThrownBy(
            () ->
                new StructuredTicketAnalysis(
                    null, "Title", "Description", "Objective", List.of(), List.of(), List.of()))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Ticket ID cannot be null");
  }

  @Test
  void should_throw_exception_when_title_is_null() {
    assertThatThrownBy(
            () ->
                new StructuredTicketAnalysis(
                    "TM-1", null, "Description", "Objective", List.of(), List.of(), List.of()))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Title cannot be null");
  }

  @Test
  void should_default_null_objective_to_empty_string() {
    final StructuredTicketAnalysis analysis =
        new StructuredTicketAnalysis(
            "TM-1", "Title", "Description", null, List.of(), List.of(), List.of());

    assertThat(analysis.objective()).isEmpty();
  }

  @Test
  void should_default_null_lists_to_empty_lists() {
    final StructuredTicketAnalysis analysis =
        new StructuredTicketAnalysis("TM-1", "Title", "Description", "Objective", null, null, null);

    assertThat(analysis.acceptanceCriteria()).isEmpty();
    assertThat(analysis.businessRules()).isEmpty();
    assertThat(analysis.technicalNotes()).isEmpty();
  }

  @Test
  void should_make_defensive_copies_of_mutable_lists() {
    final List<String> criteria = new java.util.ArrayList<>(List.of("Criterion 1"));
    final List<String> rules = new java.util.ArrayList<>(List.of("Rule 1"));
    final List<String> notes = new java.util.ArrayList<>(List.of("Note 1"));

    final StructuredTicketAnalysis analysis =
        new StructuredTicketAnalysis(
            "TM-1", "Title", "Description", "Objective", criteria, rules, notes);

    assertThat(analysis.acceptanceCriteria()).isNotSameAs(criteria);
    assertThat(analysis.businessRules()).isNotSameAs(rules);
    assertThat(analysis.technicalNotes()).isNotSameAs(notes);

    criteria.add("New Criterion");
    rules.add("New Rule");
    notes.add("New Note");

    assertThat(analysis.acceptanceCriteria()).hasSize(1);
    assertThat(analysis.businessRules()).hasSize(1);
    assertThat(analysis.technicalNotes()).hasSize(1);
  }

  @Test
  void should_create_from_raw_context() {
    final TicketBusinessContext raw =
        new TicketBusinessContext("TM-456", "Feature title", "Raw description");

    final StructuredTicketAnalysis analysis =
        StructuredTicketAnalysis.fromRawContext(
            raw,
            "Parsed objective",
            List.of("AC-1", "AC-2"),
            List.of("BR-1"),
            List.of("TN-1", "TN-2", "TN-3"));

    assertThat(analysis.ticketId()).isEqualTo("TM-456");
    assertThat(analysis.title()).isEqualTo("Feature title");
    assertThat(analysis.fullDescription()).isEqualTo("Raw description");
    assertThat(analysis.objective()).isEqualTo("Parsed objective");
    assertThat(analysis.acceptanceCriteria()).hasSize(2);
    assertThat(analysis.businessRules()).hasSize(1);
    assertThat(analysis.technicalNotes()).hasSize(3);
  }

  @Test
  void should_identify_empty_analysis() {
    final StructuredTicketAnalysis empty =
        new StructuredTicketAnalysis("", "", null, "", List.of(), List.of(), List.of());

    assertThat(empty.isEmpty()).isTrue();
  }

  @Test
  void should_identify_non_empty_analysis() {
    final StructuredTicketAnalysis analysis =
        new StructuredTicketAnalysis(
            "TM-1", "Title", "Description", "Objective", List.of(), List.of(), List.of());

    assertThat(analysis.isEmpty()).isFalse();
  }

  @Test
  void should_check_has_description() {
    final StructuredTicketAnalysis withDescription =
        new StructuredTicketAnalysis(
            "TM-1", "Title", "Description", "Objective", List.of(), List.of(), List.of());
    final StructuredTicketAnalysis withoutDescription =
        new StructuredTicketAnalysis(
            "TM-1", "Title", null, "Objective", List.of(), List.of(), List.of());
    final StructuredTicketAnalysis withBlankDescription =
        new StructuredTicketAnalysis(
            "TM-1", "Title", "   ", "Objective", List.of(), List.of(), List.of());

    assertThat(withDescription.hasDescription()).isTrue();
    assertThat(withoutDescription.hasDescription()).isFalse();
    assertThat(withBlankDescription.hasDescription()).isFalse();
  }

  @Test
  void should_check_has_acceptance_criteria() {
    final StructuredTicketAnalysis with =
        new StructuredTicketAnalysis(
            "TM-1", "Title", "Desc", "Obj", List.of("AC-1"), List.of(), List.of());
    final StructuredTicketAnalysis without =
        new StructuredTicketAnalysis(
            "TM-1", "Title", "Desc", "Obj", List.of(), List.of(), List.of());

    assertThat(with.hasAcceptanceCriteria()).isTrue();
    assertThat(without.hasAcceptanceCriteria()).isFalse();
  }

  @Test
  void should_check_has_business_rules() {
    final StructuredTicketAnalysis with =
        new StructuredTicketAnalysis(
            "TM-1", "Title", "Desc", "Obj", List.of(), List.of("BR-1"), List.of());
    final StructuredTicketAnalysis without =
        new StructuredTicketAnalysis(
            "TM-1", "Title", "Desc", "Obj", List.of(), List.of(), List.of());

    assertThat(with.hasBusinessRules()).isTrue();
    assertThat(without.hasBusinessRules()).isFalse();
  }

  @Test
  void should_check_has_technical_notes() {
    final StructuredTicketAnalysis with =
        new StructuredTicketAnalysis(
            "TM-1", "Title", "Desc", "Obj", List.of(), List.of(), List.of("TN-1"));
    final StructuredTicketAnalysis without =
        new StructuredTicketAnalysis(
            "TM-1", "Title", "Desc", "Obj", List.of(), List.of(), List.of());

    assertThat(with.hasTechnicalNotes()).isTrue();
    assertThat(without.hasTechnicalNotes()).isFalse();
  }

  @Test
  void should_check_has_objective() {
    final StructuredTicketAnalysis with =
        new StructuredTicketAnalysis(
            "TM-1", "Title", "Desc", "Objective", List.of(), List.of(), List.of());
    final StructuredTicketAnalysis without =
        new StructuredTicketAnalysis("TM-1", "Title", "Desc", "", List.of(), List.of(), List.of());
    final StructuredTicketAnalysis withBlank =
        new StructuredTicketAnalysis(
            "TM-1", "Title", "Desc", "   ", List.of(), List.of(), List.of());

    assertThat(with.hasObjective()).isTrue();
    assertThat(without.hasObjective()).isFalse();
    assertThat(withBlank.hasObjective()).isFalse();
  }

  @Test
  void should_format_complete_analysis_for_prompt() {
    final StructuredTicketAnalysis analysis =
        new StructuredTicketAnalysis(
            "TM-123",
            "Add authentication",
            "Detailed description",
            "Implement OAuth2 authentication",
            List.of("Users can login via Google", "Sessions persist"),
            List.of("Max 3 login attempts", "Lock for 15 minutes"),
            List.of("Use Spring Security", "Store tokens in Redis"));

    final String formatted = analysis.formatForPrompt();

    assertThat(formatted).contains("BUSINESS CONTEXT FROM TICKET: TM-123");
    assertThat(formatted).contains("Feature/Fix: Add authentication");
    assertThat(formatted).contains("OBJECTIVE:");
    assertThat(formatted).contains("Implement OAuth2 authentication");
    assertThat(formatted).contains("ACCEPTANCE CRITERIA:");
    assertThat(formatted).contains("Users can login via Google");
    assertThat(formatted).contains("Sessions persist");
    assertThat(formatted).contains("BUSINESS RULES:");
    assertThat(formatted).contains("Max 3 login attempts");
    assertThat(formatted).contains("TECHNICAL NOTES:");
    assertThat(formatted).contains("Use Spring Security");
    assertThat(formatted).contains("ORIGINAL DESCRIPTION:");
    assertThat(formatted).contains("Detailed description");
  }

  @Test
  void should_format_partial_analysis_for_prompt() {
    final StructuredTicketAnalysis analysis =
        new StructuredTicketAnalysis(
            "TM-456",
            "Bug fix",
            null,
            "Fix login issue",
            List.of("Login works"),
            List.of(),
            List.of());

    final String formatted = analysis.formatForPrompt();

    assertThat(formatted).contains("BUSINESS CONTEXT FROM TICKET: TM-456");
    assertThat(formatted).contains("OBJECTIVE:");
    assertThat(formatted).contains("ACCEPTANCE CRITERIA:");
    assertThat(formatted).doesNotContain("BUSINESS RULES:");
    assertThat(formatted).doesNotContain("TECHNICAL NOTES:");
    assertThat(formatted).doesNotContain("ORIGINAL DESCRIPTION:");
  }

  @Test
  void should_return_empty_string_when_formatting_empty_analysis() {
    final StructuredTicketAnalysis empty =
        new StructuredTicketAnalysis("", "", null, "", List.of(), List.of(), List.of());

    final String formatted = empty.formatForPrompt();

    assertThat(formatted).isEmpty();
  }
}
