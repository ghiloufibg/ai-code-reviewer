package com.ghiloufi.aicode.core.domain.model;

import java.util.List;
import java.util.Objects;

public record StructuredTicketAnalysis(
    String ticketId,
    String title,
    String fullDescription,
    String objective,
    List<String> acceptanceCriteria,
    List<String> businessRules,
    List<String> technicalNotes)
    implements TicketContext {

  public StructuredTicketAnalysis {
    Objects.requireNonNull(ticketId, "Ticket ID cannot be null");
    Objects.requireNonNull(title, "Title cannot be null");
    objective = objective != null ? objective : "";
    acceptanceCriteria = acceptanceCriteria != null ? List.copyOf(acceptanceCriteria) : List.of();
    businessRules = businessRules != null ? List.copyOf(businessRules) : List.of();
    technicalNotes = technicalNotes != null ? List.copyOf(technicalNotes) : List.of();
  }

  public static StructuredTicketAnalysis fromRawContext(
      final TicketBusinessContext raw,
      final String objective,
      final List<String> acceptanceCriteria,
      final List<String> businessRules,
      final List<String> technicalNotes) {
    return new StructuredTicketAnalysis(
        raw.ticketId(),
        raw.title(),
        raw.fullDescription(),
        objective,
        acceptanceCriteria,
        businessRules,
        technicalNotes);
  }

  @Override
  public boolean isEmpty() {
    return ticketId.isEmpty() && title.isEmpty();
  }

  @Override
  public boolean hasDescription() {
    return fullDescription != null && !fullDescription.isBlank();
  }

  public boolean hasAcceptanceCriteria() {
    return !acceptanceCriteria.isEmpty();
  }

  public boolean hasBusinessRules() {
    return !businessRules.isEmpty();
  }

  public boolean hasTechnicalNotes() {
    return !technicalNotes.isEmpty();
  }

  public boolean hasObjective() {
    return !objective.isBlank();
  }

  @Override
  public String formatForPrompt() {
    if (isEmpty()) {
      return "";
    }

    final StringBuilder sb = new StringBuilder();
    sb.append(
        """
        ═══════════════════════════════════════════════════════════════
        BUSINESS CONTEXT FROM TICKET: %s
        ═══════════════════════════════════════════════════════════════

        Feature/Fix: %s
        """
            .formatted(ticketId, title));

    if (hasObjective()) {
      sb.append("\nOBJECTIVE:\n").append(objective).append("\n");
    }

    if (hasAcceptanceCriteria()) {
      sb.append("\nACCEPTANCE CRITERIA:\n");
      for (final String criterion : acceptanceCriteria) {
        sb.append("  • ").append(criterion).append("\n");
      }
    }

    if (hasBusinessRules()) {
      sb.append("\nBUSINESS RULES:\n");
      for (final String rule : businessRules) {
        sb.append("  • ").append(rule).append("\n");
      }
    }

    if (hasTechnicalNotes()) {
      sb.append("\nTECHNICAL NOTES:\n");
      for (final String note : technicalNotes) {
        sb.append("  • ").append(note).append("\n");
      }
    }

    if (hasDescription()) {
      sb.append("\nORIGINAL DESCRIPTION:\n").append(fullDescription).append("\n");
    }

    sb.append(
        """

        ═══════════════════════════════════════════════════════════════
        """);

    return sb.toString();
  }
}
