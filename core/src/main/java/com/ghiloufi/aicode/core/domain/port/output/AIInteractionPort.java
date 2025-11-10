package com.ghiloufi.aicode.core.domain.port.output;

import reactor.core.publisher.Flux;

public interface AIInteractionPort {

  Flux<String> streamCompletion(String prompt);

  String getProviderName();

  String getModelName();
}
