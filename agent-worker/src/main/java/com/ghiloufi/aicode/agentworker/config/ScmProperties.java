package com.ghiloufi.aicode.agentworker.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Getter
@Validated
@ConfigurationProperties(prefix = "scm")
public final class ScmProperties {

  private final GitHubProperties github;
  private final GitLabProperties gitlab;

  public ScmProperties(
      @DefaultValue GitHubProperties github, @DefaultValue GitLabProperties gitlab) {
    this.github = github;
    this.gitlab = gitlab;
  }

  @Getter
  public static final class GitHubProperties {

    private final String token;
    private final String baseUrl;

    public GitHubProperties(
        @DefaultValue("") String token, @DefaultValue("https://api.github.com") String baseUrl) {
      this.token = token;
      this.baseUrl = baseUrl;
    }
  }

  @Getter
  public static final class GitLabProperties {

    private final String token;
    private final String baseUrl;

    public GitLabProperties(
        @DefaultValue("") String token, @DefaultValue("https://gitlab.com") String baseUrl) {
      this.token = token;
      this.baseUrl = baseUrl;
    }
  }
}
