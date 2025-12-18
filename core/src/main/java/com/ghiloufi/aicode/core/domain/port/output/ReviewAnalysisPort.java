package com.ghiloufi.aicode.core.domain.port.output;

import com.ghiloufi.aicode.core.domain.model.EnrichedDiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.ReviewChunk;
import com.ghiloufi.aicode.core.domain.model.ReviewConfiguration;
import reactor.core.publisher.Flux;

public interface ReviewAnalysisPort {

  Flux<ReviewChunk> analyzeCode(
      EnrichedDiffAnalysisBundle enrichedBundle, ReviewConfiguration configuration);

  String getAnalysisMethod();

  String getProviderName();

  String getModelName();

  default boolean supportsStreaming() {
    return true;
  }
}
