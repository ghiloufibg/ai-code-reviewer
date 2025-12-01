package com.ghiloufi.aicode.core.domain.model;

public sealed interface TicketContext permits TicketBusinessContext, StructuredTicketAnalysis {

  String ticketId();

  String title();

  String fullDescription();

  boolean isEmpty();

  boolean hasDescription();

  String formatForPrompt();

  static TicketContext empty() {
    return TicketBusinessContext.empty();
  }
}
