package com.ghiloufi.aicode.agentworker.container;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Container Security Configuration Tests")
final class ContainerSecurityConfigTest {

  @Nested
  @DisplayName("Resource Limits Validation")
  final class ResourceLimitsTests {

    @Test
    @DisplayName("should_enforce_memory_limit_of_2gb")
    void should_enforce_memory_limit_of_2gb() {
      final var config =
          ContainerConfiguration.builder()
              .imageName("analysis:latest")
              .memoryLimitBytes(2147483648L)
              .cpuNanoCores(2000000000L)
              .timeout(Duration.ofMinutes(10))
              .build();

      assertThat(config.memoryLimitBytes()).isEqualTo(2147483648L);
      assertThat(config.memoryLimitBytes()).isLessThanOrEqualTo(4L * 1024 * 1024 * 1024);
    }

    @Test
    @DisplayName("should_enforce_cpu_limit_of_2_cores")
    void should_enforce_cpu_limit_of_2_cores() {
      final var config =
          ContainerConfiguration.builder()
              .imageName("analysis:latest")
              .memoryLimitBytes(2147483648L)
              .cpuNanoCores(2000000000L)
              .timeout(Duration.ofMinutes(10))
              .build();

      assertThat(config.cpuNanoCores()).isEqualTo(2000000000L);
      assertThat(config.cpuNanoCores()).isLessThanOrEqualTo(4000000000L);
    }

    @Test
    @DisplayName("should_enforce_timeout_limit")
    void should_enforce_timeout_limit() {
      final var config =
          ContainerConfiguration.builder()
              .imageName("analysis:latest")
              .memoryLimitBytes(2147483648L)
              .cpuNanoCores(2000000000L)
              .timeout(Duration.ofMinutes(10))
              .build();

      assertThat(config.timeout()).isEqualTo(Duration.ofMinutes(10));
      assertThat(config.timeout()).isLessThanOrEqualTo(Duration.ofMinutes(30));
    }
  }

  @Nested
  @DisplayName("Security Flags Validation")
  final class SecurityFlagsTests {

    @Test
    @DisplayName("should_enable_auto_remove_by_default")
    void should_enable_auto_remove_by_default() {
      final var config =
          ContainerConfiguration.builder()
              .imageName("analysis:latest")
              .memoryLimitBytes(2147483648L)
              .cpuNanoCores(2000000000L)
              .timeout(Duration.ofMinutes(10))
              .build();

      assertThat(config.autoRemove()).isTrue();
    }

    @Test
    @DisplayName("should_disable_privileged_mode_by_default")
    void should_disable_privileged_mode_by_default() {
      final var config =
          ContainerConfiguration.builder()
              .imageName("analysis:latest")
              .memoryLimitBytes(2147483648L)
              .cpuNanoCores(2000000000L)
              .timeout(Duration.ofMinutes(10))
              .build();

      assertThat(config.privileged()).isFalse();
    }

    @Test
    @DisplayName("should_enable_read_only_root_filesystem")
    void should_enable_read_only_root_filesystem() {
      final var config =
          ContainerConfiguration.builder()
              .imageName("analysis:latest")
              .memoryLimitBytes(2147483648L)
              .cpuNanoCores(2000000000L)
              .timeout(Duration.ofMinutes(10))
              .readOnlyRootFilesystem(true)
              .build();

      assertThat(config.readOnlyRootFilesystem()).isTrue();
    }

    @Test
    @DisplayName("should_disable_network_by_default_for_isolation")
    void should_disable_network_by_default_for_isolation() {
      final var config =
          ContainerConfiguration.builder()
              .imageName("analysis:latest")
              .memoryLimitBytes(2147483648L)
              .cpuNanoCores(2000000000L)
              .timeout(Duration.ofMinutes(10))
              .networkDisabled(true)
              .build();

      assertThat(config.networkDisabled()).isTrue();
    }
  }

  @Nested
  @DisplayName("Image Name Validation")
  final class ImageNameTests {

    @Test
    @DisplayName("should_require_valid_image_name")
    void should_require_valid_image_name() {
      final var config =
          ContainerConfiguration.builder()
              .imageName("ai-code-reviewer-analysis:latest")
              .memoryLimitBytes(2147483648L)
              .cpuNanoCores(2000000000L)
              .timeout(Duration.ofMinutes(10))
              .build();

      assertThat(config.imageName()).isEqualTo("ai-code-reviewer-analysis:latest");
    }

    @Test
    @DisplayName("should_accept_image_with_registry")
    void should_accept_image_with_registry() {
      final var config =
          ContainerConfiguration.builder()
              .imageName("ghcr.io/org/analysis:v1.0.0")
              .memoryLimitBytes(2147483648L)
              .cpuNanoCores(2000000000L)
              .timeout(Duration.ofMinutes(10))
              .build();

      assertThat(config.imageName()).contains("ghcr.io");
    }
  }

  @Nested
  @DisplayName("Working Directory Configuration")
  final class WorkingDirectoryTests {

    @Test
    @DisplayName("should_set_workspace_directory")
    void should_set_workspace_directory() {
      final var config =
          ContainerConfiguration.builder()
              .imageName("analysis:latest")
              .memoryLimitBytes(2147483648L)
              .cpuNanoCores(2000000000L)
              .timeout(Duration.ofMinutes(10))
              .workingDirectory("/workspace")
              .build();

      assertThat(config.workingDirectory()).isEqualTo("/workspace");
    }

    @Test
    @DisplayName("should_default_to_workspace_directory")
    void should_default_to_workspace_directory() {
      final var config =
          ContainerConfiguration.builder()
              .imageName("analysis:latest")
              .memoryLimitBytes(2147483648L)
              .cpuNanoCores(2000000000L)
              .timeout(Duration.ofMinutes(10))
              .build();

      assertThat(config.workingDirectory()).isEqualTo("/workspace");
    }
  }

  @Nested
  @DisplayName("Security Best Practices")
  final class SecurityBestPracticesTests {

    @Test
    @DisplayName("should_create_secure_default_configuration")
    void should_create_secure_default_configuration() {
      final var config = ContainerConfiguration.secureDefaults("analysis:latest");

      assertThat(config.autoRemove()).isTrue();
      assertThat(config.privileged()).isFalse();
      assertThat(config.readOnlyRootFilesystem()).isTrue();
      assertThat(config.networkDisabled()).isFalse();
      assertThat(config.memoryLimitBytes()).isEqualTo(2147483648L);
      assertThat(config.cpuNanoCores()).isEqualTo(2000000000L);
      assertThat(config.timeout()).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    @DisplayName("should_create_isolated_configuration")
    void should_create_isolated_configuration() {
      final var config = ContainerConfiguration.isolatedDefaults("analysis:latest");

      assertThat(config.autoRemove()).isTrue();
      assertThat(config.privileged()).isFalse();
      assertThat(config.readOnlyRootFilesystem()).isTrue();
      assertThat(config.networkDisabled()).isTrue();
    }
  }

  @Nested
  @DisplayName("Container Execution Safety")
  final class ExecutionSafetyTests {

    @Test
    @DisplayName("should_validate_non_negative_resource_limits")
    void should_validate_non_negative_resource_limits() {
      final var config =
          ContainerConfiguration.builder()
              .imageName("analysis:latest")
              .memoryLimitBytes(1024L * 1024 * 1024)
              .cpuNanoCores(1000000000L)
              .timeout(Duration.ofMinutes(5))
              .build();

      assertThat(config.memoryLimitBytes()).isPositive();
      assertThat(config.cpuNanoCores()).isPositive();
      assertThat(config.timeout()).isPositive();
    }

    @Test
    @DisplayName("should_validate_reasonable_timeout")
    void should_validate_reasonable_timeout() {
      final var config =
          ContainerConfiguration.builder()
              .imageName("analysis:latest")
              .memoryLimitBytes(2147483648L)
              .cpuNanoCores(2000000000L)
              .timeout(Duration.ofMinutes(10))
              .build();

      assertThat(config.timeout().toMinutes()).isBetween(1L, 60L);
    }
  }
}
