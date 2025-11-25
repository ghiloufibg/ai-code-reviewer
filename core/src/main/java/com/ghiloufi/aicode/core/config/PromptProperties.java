package com.ghiloufi.aicode.core.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.context.properties.bind.Name;

@Getter
@ConfigurationProperties(prefix = "prompts.review")
public final class PromptProperties {

  private final String system;
  private final String fixGeneration;
  private final String confidence;
  private final String schema;
  private final String outputRequirements;

  public PromptProperties(
      @DefaultValue("") final String system,
      @Name("fix-generation") @DefaultValue("") final String fixGeneration,
      @DefaultValue("") final String confidence,
      @DefaultValue("") final String schema,
      @Name("output-requirements") @DefaultValue("") final String outputRequirements) {
    this.system = system;
    this.fixGeneration = fixGeneration;
    this.confidence = confidence;
    this.schema = schema;
    this.outputRequirements = outputRequirements;
  }
}
