package com.ghiloufi.aicode.gateway.webhook.config;

import java.time.Duration;
import java.util.Set;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@Getter
@ConfigurationProperties(prefix = "webhook")
public final class WebhookProperties {

  private final Set<String> apiKeys;
  private final Duration idempotencyTtl;
  private final int rateLimitPerMinute;
  private final Set<String> ipWhitelist;
  private final boolean enabled;

  public WebhookProperties(
      @DefaultValue Set<String> apiKeys,
      @DefaultValue("PT24H") Duration idempotencyTtl,
      @DefaultValue("60") int rateLimitPerMinute,
      @DefaultValue Set<String> ipWhitelist,
      @DefaultValue("true") boolean enabled) {
    this.apiKeys = apiKeys;
    this.idempotencyTtl = idempotencyTtl;
    this.rateLimitPerMinute = rateLimitPerMinute;
    this.ipWhitelist = ipWhitelist;
    this.enabled = enabled;
  }

  public boolean hasApiKeys() {
    return apiKeys != null && !apiKeys.isEmpty();
  }
}
