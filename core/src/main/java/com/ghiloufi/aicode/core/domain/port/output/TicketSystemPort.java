package com.ghiloufi.aicode.core.domain.port.output;

import com.ghiloufi.aicode.core.domain.model.TicketBusinessContext;
import reactor.core.publisher.Mono;

public interface TicketSystemPort {

  Mono<TicketBusinessContext> getTicketContext(String ticketId);

  default boolean supportsTicketId(final String ticketId) {
    return ticketId != null && !ticketId.isBlank();
  }
}
