package com.ghiloufi.aicode.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "prompts.review")
public final class PromptProperties {

  private final String system;
  private final String fixGeneration;
  private final String confidence;
  private final String schema;
  private final String outputRequirements;

  public PromptProperties(
      @DefaultValue("") final String system,
      @DefaultValue("") final String fixGeneration,
      @DefaultValue("") final String confidence,
      @DefaultValue("") final String schema,
      @DefaultValue("") final String outputRequirements) {
    this.system = system;
    this.fixGeneration = fixGeneration;
    this.confidence = confidence;
    this.schema = schema;
    this.outputRequirements = outputRequirements;
  }

  public String getSystem() {
    return system;
  }

  public String getFixGeneration() {
    return fixGeneration;
  }

  public String getConfidence() {
    return confidence;
  }

  public String getSchema() {
    return schema;
  }

  public String getOutputRequirements() {
    return outputRequirements;
  }
}
