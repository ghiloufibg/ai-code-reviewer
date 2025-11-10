package com.ghiloufi.aicode.core.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(SCMProvidersProperties.class)
@RequiredArgsConstructor
public class SCMConfigurationValidator {

  private final SCMProvidersProperties scmProvidersProperties;

  @PostConstruct
  public void validateConfiguration() {
    log.info("Validating SCM provider configurations...");

    scmProvidersProperties.validateAllProviders();

    final long enabledCount =
        scmProvidersProperties.getProviders().values().stream()
            .filter(SCMProvidersProperties.ProviderConfig::isEnabled)
            .count();

    log.info("SCM configuration validation successful - {} provider(s) enabled", enabledCount);

    scmProvidersProperties
        .getProviders()
        .forEach(
            (name, config) -> {
              if (config.isEnabled()) {
                log.info(
                    "{} provider enabled: API URL = {}", name.toUpperCase(), config.getApiUrl());
              } else {
                log.debug("{} provider disabled", name.toUpperCase());
              }
            });
  }
}
