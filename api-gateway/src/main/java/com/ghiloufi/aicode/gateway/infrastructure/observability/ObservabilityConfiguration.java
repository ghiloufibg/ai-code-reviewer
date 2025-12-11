package com.ghiloufi.aicode.gateway.infrastructure.observability;

import com.ghiloufi.aicode.core.infrastructure.observability.MdcContextLifter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ObservabilityConfiguration {

  @PostConstruct
  void registerMdcContextLifter() {
    MdcContextLifter.registerMdcContextLifter();
    log.info("MDC context lifter registered for reactive streams");
  }

  @PreDestroy
  void unregisterMdcContextLifter() {
    MdcContextLifter.unregisterMdcContextLifter();
    log.info("MDC context lifter unregistered");
  }
}
