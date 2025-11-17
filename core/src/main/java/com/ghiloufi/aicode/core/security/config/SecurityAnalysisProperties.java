package com.ghiloufi.aicode.core.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "security.analysis")
public class SecurityAnalysisProperties {

  private boolean enabled = true;
  private boolean astAnalysisEnabled = true;
  private boolean patternAnalysisEnabled = true;
  private double minimumConfidenceThreshold = 0.7;
  private boolean includeInfoSeverity = false;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isAstAnalysisEnabled() {
    return astAnalysisEnabled;
  }

  public void setAstAnalysisEnabled(final boolean astAnalysisEnabled) {
    this.astAnalysisEnabled = astAnalysisEnabled;
  }

  public boolean isPatternAnalysisEnabled() {
    return patternAnalysisEnabled;
  }

  public void setPatternAnalysisEnabled(final boolean patternAnalysisEnabled) {
    this.patternAnalysisEnabled = patternAnalysisEnabled;
  }

  public double getMinimumConfidenceThreshold() {
    return minimumConfidenceThreshold;
  }

  public void setMinimumConfidenceThreshold(final double minimumConfidenceThreshold) {
    this.minimumConfidenceThreshold = minimumConfidenceThreshold;
  }

  public boolean isIncludeInfoSeverity() {
    return includeInfoSeverity;
  }

  public void setIncludeInfoSeverity(final boolean includeInfoSeverity) {
    this.includeInfoSeverity = includeInfoSeverity;
  }
}
