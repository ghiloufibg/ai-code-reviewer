package com.ghiloufi.aicode.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "features.summary-comment")
public class SummaryCommentProperties {

  private boolean enabled = false;

  private boolean includeStatistics = true;

  private boolean includeSeverityBreakdown = true;
}
