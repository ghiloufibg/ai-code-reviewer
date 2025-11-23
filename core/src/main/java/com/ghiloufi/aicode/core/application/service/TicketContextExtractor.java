package com.ghiloufi.aicode.core.application.service;

import com.ghiloufi.aicode.core.domain.model.TicketBusinessContext;
import com.ghiloufi.aicode.core.domain.port.output.TicketSystemPort;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public final class TicketContextExtractor {

  private static final Pattern BRACKETED_TICKET_ID_PATTERN = Pattern.compile("\\[([A-Z]+-\\d+)\\]");

  private final TicketSystemPort ticketSystemPort;

  public TicketContextExtractor(final TicketSystemPort ticketSystemPort) {
    this.ticketSystemPort = ticketSystemPort;
  }

  public Mono<TicketBusinessContext> extractFromMergeRequest(
      final String title, final String description) {

    final String ticketId = extractTicketId(title, description);

    if (ticketId == null) {
      log.debug("No ticket ID found in merge request (expected format: [TICKET-123])");
      return Mono.just(TicketBusinessContext.empty());
    }

    log.debug("Extracting ticket context for: {}", ticketId);
    return ticketSystemPort
        .getTicketContext(ticketId)
        .defaultIfEmpty(TicketBusinessContext.empty());
  }

  private String extractTicketId(final String title, final String description) {
    if (title != null && !title.isBlank()) {
      final Matcher matcher = BRACKETED_TICKET_ID_PATTERN.matcher(title);
      if (matcher.find()) {
        return matcher.group(1);
      }
    }

    if (description != null && !description.isBlank()) {
      final Matcher matcher = BRACKETED_TICKET_ID_PATTERN.matcher(description);
      if (matcher.find()) {
        return matcher.group(1);
      }
    }

    return null;
  }
}
