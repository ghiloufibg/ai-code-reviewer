package com.ghiloufi.aicode.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ghiloufi.aicode.core.config.ContextRetrievalConfig.DiffExpansionConfig;
import com.ghiloufi.aicode.core.config.ContextRetrievalConfig.PrMetadataConfig;
import com.ghiloufi.aicode.core.config.ContextRetrievalConfig.RepositoryPoliciesConfig;
import com.ghiloufi.aicode.core.config.ContextRetrievalConfig.RolloutConfig;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class ContextRetrievalConfigTest {

  @Nested
  final class MainConfigTests {

    @Test
    void should_create_valid_config_with_all_parameters() {
      final var config =
          new ContextRetrievalConfig(
              true,
              10,
              List.of("metadata-based"),
              new RolloutConfig(50, true, 1000),
              DiffExpansionConfig.defaults(),
              PrMetadataConfig.defaults(),
              RepositoryPoliciesConfig.defaults());

      assertThat(config.enabled()).isTrue();
      assertThat(config.strategyTimeoutSeconds()).isEqualTo(10);
      assertThat(config.enabledStrategies()).containsExactly("metadata-based");
    }

    @Test
    void should_apply_defaults_for_null_nested_configs() {
      final var config =
          new ContextRetrievalConfig(
              true,
              5,
              List.of("git-history"),
              new RolloutConfig(100, true, 5000),
              null,
              null,
              null);

      assertThat(config.diffExpansion()).isNotNull();
      assertThat(config.prMetadata()).isNotNull();
      assertThat(config.policies()).isNotNull();
    }

    @Test
    void should_throw_when_enabled_strategies_is_null() {
      assertThatThrownBy(
              () ->
                  new ContextRetrievalConfig(
                      true, 5, null, new RolloutConfig(100, true, 5000), null, null, null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Enabled strategies cannot be null");
    }

    @Test
    void should_throw_when_rollout_config_is_null() {
      assertThatThrownBy(
              () ->
                  new ContextRetrievalConfig(
                      true, 5, List.of("metadata-based"), null, null, null, null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Rollout config cannot be null");
    }

    @Test
    void should_throw_when_strategy_timeout_is_zero() {
      assertThatThrownBy(
              () ->
                  new ContextRetrievalConfig(
                      true,
                      0,
                      List.of("metadata-based"),
                      new RolloutConfig(100, true, 5000),
                      null,
                      null,
                      null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Strategy timeout must be positive");
    }

    @Test
    void should_throw_when_strategy_timeout_is_negative() {
      assertThatThrownBy(
              () ->
                  new ContextRetrievalConfig(
                      true,
                      -1,
                      List.of("metadata-based"),
                      new RolloutConfig(100, true, 5000),
                      null,
                      null,
                      null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Strategy timeout must be positive");
    }

    @Test
    void should_return_true_for_enabled_strategy() {
      final var config = ContextRetrievalConfig.defaults();

      assertThat(config.isStrategyEnabled("metadata-based")).isTrue();
      assertThat(config.isStrategyEnabled("git-history")).isTrue();
    }

    @Test
    void should_return_false_for_disabled_strategy() {
      final var config = ContextRetrievalConfig.defaults();

      assertThat(config.isStrategyEnabled("unknown-strategy")).isFalse();
    }

    @Test
    void should_return_false_for_strategy_when_config_disabled() {
      final var config =
          new ContextRetrievalConfig(
              false,
              5,
              List.of("metadata-based"),
              new RolloutConfig(100, true, 5000),
              null,
              null,
              null);

      assertThat(config.isStrategyEnabled("metadata-based")).isFalse();
    }

    @Test
    void should_return_correct_feature_enabled_status() {
      final var config = ContextRetrievalConfig.defaults();

      assertThat(config.isDiffExpansionEnabled()).isTrue();
      assertThat(config.isPrMetadataEnabled()).isTrue();
      assertThat(config.isPoliciesEnabled()).isTrue();
    }

    @Test
    void should_return_false_for_features_when_main_config_disabled() {
      final var config =
          new ContextRetrievalConfig(
              false,
              5,
              List.of("metadata-based"),
              new RolloutConfig(100, true, 5000),
              DiffExpansionConfig.defaults(),
              PrMetadataConfig.defaults(),
              RepositoryPoliciesConfig.defaults());

      assertThat(config.isDiffExpansionEnabled()).isFalse();
      assertThat(config.isPrMetadataEnabled()).isFalse();
      assertThat(config.isPoliciesEnabled()).isFalse();
    }
  }

  @Nested
  final class RolloutConfigTests {

    @Test
    void should_create_valid_rollout_config() {
      final var rollout = new RolloutConfig(75, true, 3000);

      assertThat(rollout.percentage()).isEqualTo(75);
      assertThat(rollout.skipLargeDiffs()).isTrue();
      assertThat(rollout.maxDiffLines()).isEqualTo(3000);
    }

    @Test
    void should_throw_when_percentage_is_negative() {
      assertThatThrownBy(() -> new RolloutConfig(-1, true, 1000))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Percentage must be between 0 and 100");
    }

    @Test
    void should_throw_when_percentage_exceeds_100() {
      assertThatThrownBy(() -> new RolloutConfig(101, true, 1000))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Percentage must be between 0 and 100");
    }

    @Test
    void should_throw_when_max_diff_lines_is_zero() {
      assertThatThrownBy(() -> new RolloutConfig(50, true, 0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Max diff lines must be positive");
    }

    @Test
    void should_throw_when_max_diff_lines_is_negative() {
      assertThatThrownBy(() -> new RolloutConfig(50, true, -1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Max diff lines must be positive");
    }

    @Test
    void should_accept_boundary_percentage_values() {
      final var zeroPercent = new RolloutConfig(0, true, 1000);
      final var hundredPercent = new RolloutConfig(100, true, 1000);

      assertThat(zeroPercent.percentage()).isEqualTo(0);
      assertThat(hundredPercent.percentage()).isEqualTo(100);
    }
  }

  @Nested
  final class DiffExpansionConfigTests {

    @Test
    void should_create_valid_diff_expansion_config() {
      final var config = new DiffExpansionConfig(true, 200, 1000, 15, Set.of(".json", ".xml"));

      assertThat(config.enabled()).isTrue();
      assertThat(config.maxFileSizeKb()).isEqualTo(200);
      assertThat(config.maxLineCount()).isEqualTo(1000);
      assertThat(config.maxFilesToExpand()).isEqualTo(15);
      assertThat(config.excludedExtensions()).containsExactlyInAnyOrder(".json", ".xml");
    }

    @Test
    void should_create_defaults_with_expected_values() {
      final var config = DiffExpansionConfig.defaults();

      assertThat(config.enabled()).isTrue();
      assertThat(config.maxFileSizeKb()).isEqualTo(100);
      assertThat(config.maxLineCount()).isEqualTo(500);
      assertThat(config.maxFilesToExpand()).isEqualTo(10);
      assertThat(config.excludedExtensions())
          .containsExactlyInAnyOrder(".lock", ".svg", ".png", ".jpg", ".gif", ".ico");
    }

    @Test
    void should_handle_null_excluded_extensions() {
      final var config = new DiffExpansionConfig(true, 100, 500, 10, null);

      assertThat(config.excludedExtensions()).isEmpty();
    }

    @Test
    void should_throw_when_max_file_size_is_zero() {
      assertThatThrownBy(() -> new DiffExpansionConfig(true, 0, 500, 10, Set.of()))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Max file size must be positive");
    }

    @Test
    void should_throw_when_max_line_count_is_zero() {
      assertThatThrownBy(() -> new DiffExpansionConfig(true, 100, 0, 10, Set.of()))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Max line count must be positive");
    }

    @Test
    void should_throw_when_max_files_to_expand_is_zero() {
      assertThatThrownBy(() -> new DiffExpansionConfig(true, 100, 500, 0, Set.of()))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Max files to expand must be positive");
    }

    @Test
    void should_expand_file_within_size_limit() {
      final var config = DiffExpansionConfig.defaults();

      assertThat(config.shouldExpandFile("src/Main.java", 50 * 1024)).isTrue();
    }

    @Test
    void should_not_expand_file_exceeding_size_limit() {
      final var config = DiffExpansionConfig.defaults();

      assertThat(config.shouldExpandFile("src/Main.java", 150 * 1024)).isFalse();
    }

    @Test
    void should_not_expand_excluded_extension() {
      final var config = DiffExpansionConfig.defaults();

      assertThat(config.shouldExpandFile("package.lock", 10 * 1024)).isFalse();
      assertThat(config.shouldExpandFile("icon.svg", 1024)).isFalse();
      assertThat(config.shouldExpandFile("logo.png", 1024)).isFalse();
      assertThat(config.shouldExpandFile("photo.jpg", 1024)).isFalse();
      assertThat(config.shouldExpandFile("animation.gif", 1024)).isFalse();
      assertThat(config.shouldExpandFile("favicon.ico", 1024)).isFalse();
    }

    @Test
    void should_not_expand_when_disabled() {
      final var config = new DiffExpansionConfig(false, 100, 500, 10, Set.of());

      assertThat(config.shouldExpandFile("src/Main.java", 10 * 1024)).isFalse();
    }

    @Test
    void should_expand_file_without_extension() {
      final var config = DiffExpansionConfig.defaults();

      assertThat(config.shouldExpandFile("Makefile", 10 * 1024)).isTrue();
    }
  }

  @Nested
  final class PrMetadataConfigTests {

    @Test
    void should_create_valid_pr_metadata_config() {
      final var config = new PrMetadataConfig(true, false, true, false, 10);

      assertThat(config.enabled()).isTrue();
      assertThat(config.includeLabels()).isFalse();
      assertThat(config.includeCommits()).isTrue();
      assertThat(config.includeAuthor()).isFalse();
      assertThat(config.maxCommitMessages()).isEqualTo(10);
    }

    @Test
    void should_create_defaults_with_expected_values() {
      final var config = PrMetadataConfig.defaults();

      assertThat(config.enabled()).isTrue();
      assertThat(config.includeLabels()).isTrue();
      assertThat(config.includeCommits()).isTrue();
      assertThat(config.includeAuthor()).isTrue();
      assertThat(config.maxCommitMessages()).isEqualTo(5);
    }

    @Test
    void should_accept_zero_max_commit_messages() {
      final var config = new PrMetadataConfig(true, true, true, true, 0);

      assertThat(config.maxCommitMessages()).isEqualTo(0);
    }

    @Test
    void should_throw_when_max_commit_messages_is_negative() {
      assertThatThrownBy(() -> new PrMetadataConfig(true, true, true, true, -1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Max commit messages must be non-negative");
    }
  }

  @Nested
  final class RepositoryPoliciesConfigTests {

    @Test
    void should_create_valid_policies_config() {
      final var config = new RepositoryPoliciesConfig(true, 10000, true, true, false, true);

      assertThat(config.enabled()).isTrue();
      assertThat(config.maxContentChars()).isEqualTo(10000);
      assertThat(config.includeContributing()).isTrue();
      assertThat(config.includeCodeOfConduct()).isTrue();
      assertThat(config.includePrTemplate()).isFalse();
      assertThat(config.includeSecurity()).isTrue();
    }

    @Test
    void should_create_defaults_with_expected_values() {
      final var config = RepositoryPoliciesConfig.defaults();

      assertThat(config.enabled()).isTrue();
      assertThat(config.maxContentChars()).isEqualTo(5000);
      assertThat(config.includeContributing()).isTrue();
      assertThat(config.includeCodeOfConduct()).isFalse();
      assertThat(config.includePrTemplate()).isTrue();
      assertThat(config.includeSecurity()).isTrue();
    }

    @Test
    void should_throw_when_max_content_chars_is_zero() {
      assertThatThrownBy(() -> new RepositoryPoliciesConfig(true, 0, true, true, true, true))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Max content chars must be positive");
    }

    @Test
    void should_throw_when_max_content_chars_is_negative() {
      assertThatThrownBy(() -> new RepositoryPoliciesConfig(true, -1, true, true, true, true))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Max content chars must be positive");
    }

    @Test
    void should_return_contributing_paths() {
      final var config = RepositoryPoliciesConfig.defaults();

      assertThat(config.getContributingPaths())
          .containsExactly("CONTRIBUTING.md", ".github/CONTRIBUTING.md", "docs/CONTRIBUTING.md");
    }

    @Test
    void should_return_code_of_conduct_paths() {
      final var config = RepositoryPoliciesConfig.defaults();

      assertThat(config.getCodeOfConductPaths())
          .containsExactly("CODE_OF_CONDUCT.md", ".github/CODE_OF_CONDUCT.md");
    }

    @Test
    void should_return_pr_template_paths() {
      final var config = RepositoryPoliciesConfig.defaults();

      assertThat(config.getPrTemplatePaths())
          .containsExactly(
              ".github/PULL_REQUEST_TEMPLATE.md",
              ".github/pull_request_template.md",
              "PULL_REQUEST_TEMPLATE.md");
    }

    @Test
    void should_return_security_paths() {
      final var config = RepositoryPoliciesConfig.defaults();

      assertThat(config.getSecurityPaths()).containsExactly("SECURITY.md", ".github/SECURITY.md");
    }
  }

  @Nested
  final class DefaultsTests {

    @Test
    void should_create_full_defaults_config() {
      final var config = ContextRetrievalConfig.defaults();

      assertThat(config.enabled()).isTrue();
      assertThat(config.strategyTimeoutSeconds()).isEqualTo(5);
      assertThat(config.enabledStrategies()).containsExactly("metadata-based", "git-history");
      assertThat(config.rollout().percentage()).isEqualTo(100);
      assertThat(config.rollout().skipLargeDiffs()).isTrue();
      assertThat(config.rollout().maxDiffLines()).isEqualTo(5000);
      assertThat(config.diffExpansion()).isNotNull();
      assertThat(config.prMetadata()).isNotNull();
      assertThat(config.policies()).isNotNull();
    }
  }
}
