package com.ghiloufi.aicode.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "features.fix-application")
public class FixApplicationProperties {

  private boolean enabled = false;

  private double minimumConfidenceThreshold = 0.7;

  private int maxConcurrentApplications = 5;

  private int applicationTimeoutSeconds = 30;
}
