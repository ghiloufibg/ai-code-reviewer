package com.ghiloufi.aicode.core.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@Getter
@ConfigurationProperties(prefix = "prompts")
public final class PromptVariantProperties {

  public enum Variant {
    CURRENT,
    OPTIMIZED
  }

  private final Variant variant;

  public PromptVariantProperties(@DefaultValue("OPTIMIZED") final Variant variant) {
    this.variant = variant;
  }
}
