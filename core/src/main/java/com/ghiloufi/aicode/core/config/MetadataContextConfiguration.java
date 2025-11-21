package com.ghiloufi.aicode.core.config;

import com.ghiloufi.aicode.core.application.service.context.MetadataBasedContextStrategy;
import com.ghiloufi.aicode.core.domain.port.output.ContextRetrievalStrategy;
import com.ghiloufi.aicode.core.domain.port.output.SCMPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetadataContextConfiguration {

  @Bean
  public ContextRetrievalStrategy metadataBasedContextStrategy(final SCMPort scmPort) {
    return new MetadataBasedContextStrategy(scmPort);
  }
}
