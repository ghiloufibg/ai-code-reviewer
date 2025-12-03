package com.ghiloufi.aicode.core.config;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "context-retrieval")
public record ContextRetrievalConfig(
    boolean enabled,
    int strategyTimeoutSeconds,
    List<String> enabledStrategies,
    RolloutConfig rollout,
    DiffExpansionConfig diffExpansion,
    PrMetadataConfig prMetadata,
    RepositoryPoliciesConfig policies) {

  public ContextRetrievalConfig {
    Objects.requireNonNull(enabledStrategies, "Enabled strategies cannot be null");
    Objects.requireNonNull(rollout, "Rollout config cannot be null");

    if (strategyTimeoutSeconds <= 0) {
      throw new IllegalArgumentException("Strategy timeout must be positive");
    }

    if (diffExpansion == null) {
      diffExpansion = DiffExpansionConfig.defaults();
    }
    if (prMetadata == null) {
      prMetadata = PrMetadataConfig.defaults();
    }
    if (policies == null) {
      policies = RepositoryPoliciesConfig.defaults();
    }
  }

  public record RolloutConfig(int percentage, boolean skipLargeDiffs, int maxDiffLines) {

    public RolloutConfig {
      if (percentage < 0 || percentage > 100) {
        throw new IllegalArgumentException("Percentage must be between 0 and 100");
      }

      if (maxDiffLines <= 0) {
        throw new IllegalArgumentException("Max diff lines must be positive");
      }
    }
  }

  public record DiffExpansionConfig(
      boolean enabled,
      int maxFileSizeKb,
      int maxLineCount,
      int maxFilesToExpand,
      Set<String> excludedExtensions) {

    public DiffExpansionConfig {
      if (maxFileSizeKb <= 0) {
        throw new IllegalArgumentException("Max file size must be positive");
      }
      if (maxLineCount <= 0) {
        throw new IllegalArgumentException("Max line count must be positive");
      }
      if (maxFilesToExpand <= 0) {
        throw new IllegalArgumentException("Max files to expand must be positive");
      }
      excludedExtensions = excludedExtensions != null ? Set.copyOf(excludedExtensions) : Set.of();
    }

    public static DiffExpansionConfig defaults() {
      return new DiffExpansionConfig(
          true, 100, 500, 10, Set.of(".lock", ".svg", ".png", ".jpg", ".gif", ".ico"));
    }

    public boolean shouldExpandFile(final String filePath, final int fileSizeBytes) {
      if (!enabled) {
        return false;
      }
      final int lastDot = filePath.lastIndexOf('.');
      if (lastDot > 0) {
        final String ext = filePath.substring(lastDot);
        if (excludedExtensions.contains(ext)) {
          return false;
        }
      }
      return fileSizeBytes <= maxFileSizeKb * 1024;
    }
  }

  public record PrMetadataConfig(
      boolean enabled,
      boolean includeLabels,
      boolean includeCommits,
      boolean includeAuthor,
      int maxCommitMessages) {

    public PrMetadataConfig {
      if (maxCommitMessages < 0) {
        throw new IllegalArgumentException("Max commit messages must be non-negative");
      }
    }

    public static PrMetadataConfig defaults() {
      return new PrMetadataConfig(true, true, true, true, 5);
    }
  }

  public record RepositoryPoliciesConfig(boolean enabled, int maxContentChars, List<String> files) {

    public RepositoryPoliciesConfig {
      if (maxContentChars <= 0) {
        throw new IllegalArgumentException("Max content chars must be positive");
      }
      files = files != null ? List.copyOf(files) : List.of();
    }

    public static RepositoryPoliciesConfig defaults() {
      return new RepositoryPoliciesConfig(true, 5000, List.of());
    }
  }

  public static ContextRetrievalConfig defaults() {
    return new ContextRetrievalConfig(
        true,
        5,
        List.of("metadata-based", "git-history"),
        new RolloutConfig(100, true, 5000),
        DiffExpansionConfig.defaults(),
        PrMetadataConfig.defaults(),
        RepositoryPoliciesConfig.defaults());
  }

  public boolean isStrategyEnabled(final String strategyName) {
    return enabled && enabledStrategies.contains(strategyName);
  }

  public boolean isDiffExpansionEnabled() {
    return enabled && diffExpansion != null && diffExpansion.enabled();
  }

  public boolean isPrMetadataEnabled() {
    return enabled && prMetadata != null && prMetadata.enabled();
  }

  public boolean isPoliciesEnabled() {
    return enabled && policies != null && policies.enabled();
  }
}
