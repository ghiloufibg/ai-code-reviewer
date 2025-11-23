package com.ghiloufi.aicode.core.domain.port.output;

import com.ghiloufi.aicode.core.domain.model.ContextRetrievalResult;
import com.ghiloufi.aicode.core.domain.model.DiffAnalysisBundle;
import reactor.core.publisher.Mono;

public interface ContextRetrievalStrategy {

  Mono<ContextRetrievalResult> retrieveContext(DiffAnalysisBundle diffBundle);

  String getStrategyName();

  int getPriority();
}
