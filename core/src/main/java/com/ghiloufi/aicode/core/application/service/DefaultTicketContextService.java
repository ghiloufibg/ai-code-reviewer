package com.ghiloufi.aicode.core.application.service;

import com.ghiloufi.aicode.core.domain.model.TicketBusinessContext;
import com.ghiloufi.aicode.core.domain.model.TicketContext;
import com.ghiloufi.aicode.core.domain.port.output.TicketSystemPort;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public final class DefaultTicketContextService implements TicketContextService {

  private static final Pattern BRACKETED_TICKET_ID_PATTERN = Pattern.compile("\\[([A-Z]+-\\d+)\\]");

  private final TicketSystemPort ticketSystemPort;

  @Override
  public Mono<TicketContext> extractFromMergeRequest(final String title, final String description) {
    final String ticketId = extractTicketId(title, description);

    if (ticketId == null) {
      log.debug("No ticket ID found in merge request (expected format: [TICKET-123])");
      return Mono.just(TicketBusinessContext.empty());
    }

    log.debug("Extracting ticket context for: {}", ticketId);
    return ticketSystemPort
        .getTicketContext(ticketId)
        .map(context -> (TicketContext) context)
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
