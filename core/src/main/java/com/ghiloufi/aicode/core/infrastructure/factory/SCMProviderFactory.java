package com.ghiloufi.aicode.core.infrastructure.factory;

import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import com.ghiloufi.aicode.core.domain.port.output.SCMPort;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SCMProviderFactory {

  private final Map<SourceProvider, SCMPort> providers;

  public SCMProviderFactory(final List<SCMPort> adapters) {
    this.providers =
        adapters.stream().collect(Collectors.toMap(SCMPort::getProviderType, adapter -> adapter));

    log.info("Registered SCM providers: {}", providers.keySet());
  }

  public SCMPort getProvider(final SourceProvider provider) {
    return Optional.ofNullable(providers.get(provider))
        .orElseThrow(
            () -> new UnsupportedOperationException("Provider not registered: " + provider));
  }
}
