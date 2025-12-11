package com.ghiloufi.aicode.core.infrastructure.resilience;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TimeoutProperties.class)
public class ResilienceConfiguration {}
