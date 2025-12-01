package com.ghiloufi.aicode.core.application.service;

import com.ghiloufi.aicode.core.domain.model.TicketContext;
import reactor.core.publisher.Mono;

public interface TicketContextService {

  Mono<TicketContext> extractFromMergeRequest(String title, String description);
}
