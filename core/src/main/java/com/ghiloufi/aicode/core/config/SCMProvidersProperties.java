package com.ghiloufi.aicode.core.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.Optional;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "scm")
public class SCMProvidersProperties {

  @Valid
  @NotNull
  @Size(min = 1, message = "At least one SCM provider must be configured")
  private Map<String, ProviderConfig> providers;

  @Data
  public static class ProviderConfig {
    private boolean enabled = false;
    private String apiUrl;
    private String token;

    public void validate(final String providerName) {
      Optional.ofNullable(apiUrl)
          .filter(url -> !url.isBlank())
          .orElseThrow(
              () ->
                  new IllegalStateException(
                      String.format(
                          "SCM provider '%s' is enabled but 'scm.providers.%s.api-url' is not configured",
                          providerName, providerName)));

      Optional.ofNullable(token)
          .filter(tkn -> !tkn.isBlank())
          .orElseThrow(
              () ->
                  new IllegalStateException(
                      String.format(
                          "SCM provider '%s' is enabled but 'scm.providers.%s.token' is not configured",
                          providerName, providerName)));
    }
  }

  public ProviderConfig getGithub() {
    return providers.get("github");
  }

  public ProviderConfig getGitlab() {
    return providers.get("gitlab");
  }

  public void validateAllProviders() {
    Optional.ofNullable(providers)
        .filter(p -> !p.isEmpty())
        .orElseThrow(
            () -> new IllegalStateException("No SCM providers configured under 'scm.providers'"));

    final long enabledCount = providers.values().stream().filter(ProviderConfig::isEnabled).count();

    if (enabledCount == 0) {
      throw new IllegalStateException("At least one SCM provider must be enabled in configuration");
    }

    providers.entrySet().stream()
        .filter(entry -> entry.getValue().isEnabled())
        .forEach(entry -> entry.getValue().validate(entry.getKey()));
  }
}
