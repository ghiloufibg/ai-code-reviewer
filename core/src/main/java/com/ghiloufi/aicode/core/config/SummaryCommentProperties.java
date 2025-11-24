package com.ghiloufi.aicode.core.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@Getter
@ConfigurationProperties(prefix = "features.summary-comment")
public final class SummaryCommentProperties {

  private final boolean enabled;
  private final boolean includeStatistics;
  private final boolean includeSeverityBreakdown;

  public SummaryCommentProperties(
      @DefaultValue("false") final boolean enabled,
      @DefaultValue("true") final boolean includeStatistics,
      @DefaultValue("true") final boolean includeSeverityBreakdown) {
    this.enabled = enabled;
    this.includeStatistics = includeStatistics;
    this.includeSeverityBreakdown = includeSeverityBreakdown;
  }
}
