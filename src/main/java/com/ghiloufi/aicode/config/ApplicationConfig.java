package com.ghiloufi.aicode.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Application configuration for the AI Code Reviewer.
 *
 * <p>This class holds all configuration properties and replaces the inner ApplicationConfig class
 * in CodeReviewOrchestrator to follow Spring Boot best practices.
 */
@Component
@ConfigurationProperties(prefix = "app")
@Validated
public class ApplicationConfig {

  /** Execution mode: "local" for Git analysis or "github" for PR analysis. */
  @Pattern(regexp = "^(local|github)$", message = "Mode must be either 'local' or 'github'")
  public String mode = "github";

  /** GitHub repository in format "owner/repo". */
  public String repository = "";

  /** Pull Request number for GitHub mode. */
  public int pullRequestNumber = 0;

  /** GitHub token from environment variable. */
  public String githubToken;

  /** Git commit range for local mode. */
  public String fromCommit = "HEAD~1";

  public String toCommit = "HEAD";

  /** LLM model name (legacy field for compatibility). */
  public String model = "deepseek-coder-6.7b-instruct";

  /** Ollama host URL (legacy field for compatibility). */
  public String ollamaHost = "http://localhost:1234";

  /** Timeout for LLM requests in seconds (legacy field for compatibility). */
  public int timeoutSeconds = 45;

  /** Maximum lines per diff chunk. */
  @Positive(message = "Max lines per chunk must be positive")
  public int maxLinesPerChunk = 1500;

  /** Context lines around modifications. */
  public int contextLines = 5;

  /** Default branch name. */
  @NotBlank(message = "Default branch cannot be blank")
  public String defaultBranch = "main";

  /** Java version for the project. */
  @NotBlank(message = "Java version cannot be blank")
  public String javaVersion = "17";

  /** Build system (maven, gradle, etc.). */
  @NotBlank(message = "Build system cannot be blank")
  public String buildSystem = "maven";

  /** Initializes the GitHub token from environment variable. */
  public void initializeGithubToken() {
    this.githubToken = System.getenv("GITHUB_TOKEN");
  }

  /**
   * Validates GitHub-specific configuration requirements. This method is called after property
   * binding to validate complex rules.
   *
   * @throws ConstraintViolationException if GitHub mode requirements are not met
   */
  public void validateGithubMode() {
    if ("github".equals(mode)) {
      if (repository == null || repository.trim().isEmpty()) {
        throw new IllegalArgumentException(
            "Repository is required for GitHub mode (set app.repository or GITHUB_REPOSITORY)");
      }
      if (pullRequestNumber <= 0) {
        throw new IllegalArgumentException(
            "Pull Request number is required for GitHub mode (set app.pullRequestNumber or PR_NUMBER)");
      }
      if (githubToken == null || githubToken.trim().isEmpty()) {
        throw new IllegalArgumentException(
            "GitHub token is required for GitHub mode (set GITHUB_TOKEN environment variable)");
      }
    }
  }

  // Getters and setters for Spring Boot property binding
  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  public String getRepository() {
    return repository;
  }

  public void setRepository(String repository) {
    this.repository = repository;
  }

  public int getPullRequestNumber() {
    return pullRequestNumber;
  }

  public void setPullRequestNumber(int pullRequestNumber) {
    this.pullRequestNumber = pullRequestNumber;
  }

  public String getGithubToken() {
    return githubToken;
  }

  public void setGithubToken(String githubToken) {
    this.githubToken = githubToken;
  }

  public String getFromCommit() {
    return fromCommit;
  }

  public void setFromCommit(String fromCommit) {
    this.fromCommit = fromCommit;
  }

  public String getToCommit() {
    return toCommit;
  }

  public void setToCommit(String toCommit) {
    this.toCommit = toCommit;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public String getOllamaHost() {
    return ollamaHost;
  }

  public void setOllamaHost(String ollamaHost) {
    this.ollamaHost = ollamaHost;
  }

  public int getTimeoutSeconds() {
    return timeoutSeconds;
  }

  public void setTimeoutSeconds(int timeoutSeconds) {
    this.timeoutSeconds = timeoutSeconds;
  }

  public int getMaxLinesPerChunk() {
    return maxLinesPerChunk;
  }

  public void setMaxLinesPerChunk(int maxLinesPerChunk) {
    this.maxLinesPerChunk = maxLinesPerChunk;
  }

  public int getContextLines() {
    return contextLines;
  }

  public void setContextLines(int contextLines) {
    this.contextLines = contextLines;
  }

  public String getDefaultBranch() {
    return defaultBranch;
  }

  public void setDefaultBranch(String defaultBranch) {
    this.defaultBranch = defaultBranch;
  }

  public String getJavaVersion() {
    return javaVersion;
  }

  public void setJavaVersion(String javaVersion) {
    this.javaVersion = javaVersion;
  }

  public String getBuildSystem() {
    return buildSystem;
  }

  public void setBuildSystem(String buildSystem) {
    this.buildSystem = buildSystem;
  }
}
