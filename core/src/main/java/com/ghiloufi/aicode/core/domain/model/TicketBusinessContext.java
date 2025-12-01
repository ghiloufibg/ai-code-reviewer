package com.ghiloufi.aicode.core.domain.model;

import java.util.Objects;

public record TicketBusinessContext(String ticketId, String title, String fullDescription)
    implements TicketContext {

  private static final TicketBusinessContext EMPTY = new TicketBusinessContext("", "", null);

  public TicketBusinessContext {
    Objects.requireNonNull(ticketId, "Ticket ID cannot be null");
    Objects.requireNonNull(title, "Title cannot be null");
  }

  public static TicketBusinessContext empty() {
    return EMPTY;
  }

  public boolean isEmpty() {
    return ticketId.isEmpty() && title.isEmpty();
  }

  public boolean hasDescription() {
    return fullDescription != null && !fullDescription.isBlank();
  }

  public String formatForPrompt() {
    if (isEmpty() || !hasDescription()) {
      return "";
    }

    return """
        ═══════════════════════════════════════════════════════════════
        BUSINESS CONTEXT FROM TICKET: %s
        ═══════════════════════════════════════════════════════════════

        Feature/Fix: %s

        TICKET DESCRIPTION:
        %s

        ═══════════════════════════════════════════════════════════════
        """
        .formatted(ticketId, title, fullDescription);
  }
}
