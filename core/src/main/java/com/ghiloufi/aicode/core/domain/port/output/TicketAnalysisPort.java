package com.ghiloufi.aicode.core.domain.port.output;

import com.ghiloufi.aicode.core.domain.model.StructuredTicketAnalysis;
import com.ghiloufi.aicode.core.domain.model.TicketBusinessContext;
import reactor.core.publisher.Mono;

public interface TicketAnalysisPort {

  Mono<StructuredTicketAnalysis> analyzeTicket(TicketBusinessContext rawContext);

  String getProviderName();

  String getModelName();
}
