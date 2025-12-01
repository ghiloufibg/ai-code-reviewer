package com.ghiloufi.aicode.core.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@Getter
@ConfigurationProperties(prefix = "prompts.ticket-analysis")
public final class TicketAnalysisPromptProperties {

  private final String system;

  public TicketAnalysisPromptProperties(@DefaultValue("") final String system) {
    this.system = system;
  }
}
