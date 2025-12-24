package com.ghiloufi.aicode.agentworker.container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ContainerConfiguration")
final class ContainerConfigurationTest {

  @Nested
  @DisplayName("builder")
  final class BuilderTests {

    @Test
    void should_build_configuration_with_all_fields() {
      final var config =
          ContainerConfiguration.builder()
              .imageName("analysis-image:latest")
              .memoryLimitBytes(4294967296L)
              .cpuNanoCores(4000000000L)
              .timeout(Duration.ofMinutes(15))
              .workingDirectory("/app")
              .workspaceVolume("/tmp/workspace")
              .command(List.of("/bin/sh", "-c", "echo hello"))
              .environment(Map.of("CI", "true", "DEBUG", "1"))
              .readOnlyRootFilesystem(false)
              .autoRemove(true)
              .noNewPrivileges(true)
              .privileged(false)
              .networkDisabled(true)
              .build();

      assertThat(config.imageName()).isEqualTo("analysis-image:latest");
      assertThat(config.memoryLimitBytes()).isEqualTo(4294967296L);
      assertThat(config.cpuNanoCores()).isEqualTo(4000000000L);
      assertThat(config.timeout()).isEqualTo(Duration.ofMinutes(15));
      assertThat(config.workingDirectory()).isEqualTo("/app");
      assertThat(config.workspaceVolume()).isEqualTo("/tmp/workspace");
      assertThat(config.command()).containsExactly("/bin/sh", "-c", "echo hello");
      assertThat(config.environment()).containsEntry("CI", "true").containsEntry("DEBUG", "1");
      assertThat(config.readOnlyRootFilesystem()).isFalse();
      assertThat(config.autoRemove()).isTrue();
      assertThat(config.noNewPrivileges()).isTrue();
      assertThat(config.privileged()).isFalse();
      assertThat(config.networkDisabled()).isTrue();
    }

    @Test
    void should_use_secure_defaults() {
      final var config = ContainerConfiguration.builder().imageName("test-image").build();

      assertThat(config.autoRemove()).isTrue();
      assertThat(config.noNewPrivileges()).isTrue();
      assertThat(config.privileged()).isFalse();
    }

    @Test
    void should_use_default_resource_limits() {
      final var config = ContainerConfiguration.builder().imageName("test-image").build();

      assertThat(config.memoryLimitBytes()).isEqualTo(2147483648L);
      assertThat(config.cpuNanoCores()).isEqualTo(2000000000L);
      assertThat(config.timeout()).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void should_use_default_working_directory() {
      final var config = ContainerConfiguration.builder().imageName("test-image").build();

      assertThat(config.workingDirectory()).isEqualTo("/workspace");
    }

    @Test
    void should_use_empty_defaults_for_command_and_environment() {
      final var config = ContainerConfiguration.builder().imageName("test-image").build();

      assertThat(config.command()).isEmpty();
      assertThat(config.environment()).isEmpty();
    }

    @Test
    void should_allow_disabling_security_features() {
      final var config =
          ContainerConfiguration.builder()
              .imageName("test-image")
              .readOnlyRootFilesystem(false)
              .autoRemove(false)
              .noNewPrivileges(false)
              .privileged(true)
              .build();

      assertThat(config.readOnlyRootFilesystem()).isFalse();
      assertThat(config.autoRemove()).isFalse();
      assertThat(config.noNewPrivileges()).isFalse();
      assertThat(config.privileged()).isTrue();
    }

    @Test
    void should_require_image_name() {
      assertThatThrownBy(() -> ContainerConfiguration.builder().build())
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("imageName");
    }

    @Test
    void should_reject_negative_memory_limit() {
      assertThatThrownBy(
              () ->
                  ContainerConfiguration.builder().imageName("test").memoryLimitBytes(-1L).build())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("memoryLimitBytes");
    }

    @Test
    void should_reject_zero_cpu_limit() {
      assertThatThrownBy(
              () -> ContainerConfiguration.builder().imageName("test").cpuNanoCores(0L).build())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cpuNanoCores");
    }
  }

  @Nested
  @DisplayName("record accessors")
  final class AccessorTests {

    @Test
    void should_provide_all_record_accessors() {
      final var config =
          new ContainerConfiguration(
              "my-image",
              1073741824L,
              1000000000L,
              Duration.ofMinutes(5),
              "/app",
              "/data",
              List.of("run"),
              Map.of("KEY", "VALUE"),
              true,
              false,
              true,
              false,
              true);

      assertThat(config.imageName()).isEqualTo("my-image");
      assertThat(config.memoryLimitBytes()).isEqualTo(1073741824L);
      assertThat(config.cpuNanoCores()).isEqualTo(1000000000L);
      assertThat(config.timeout()).isEqualTo(Duration.ofMinutes(5));
      assertThat(config.workingDirectory()).isEqualTo("/app");
      assertThat(config.workspaceVolume()).isEqualTo("/data");
      assertThat(config.command()).containsExactly("run");
      assertThat(config.environment()).containsEntry("KEY", "VALUE");
      assertThat(config.readOnlyRootFilesystem()).isTrue();
      assertThat(config.autoRemove()).isFalse();
      assertThat(config.noNewPrivileges()).isTrue();
      assertThat(config.privileged()).isFalse();
      assertThat(config.networkDisabled()).isTrue();
    }
  }

  @Nested
  @DisplayName("resource limits")
  final class ResourceLimitTests {

    @Test
    void should_accept_2gb_memory_limit() {
      final var config =
          ContainerConfiguration.builder().imageName("test").memoryLimitBytes(2147483648L).build();

      assertThat(config.memoryLimitBytes()).isEqualTo(2147483648L);
    }

    @Test
    void should_accept_4gb_memory_limit() {
      final var config =
          ContainerConfiguration.builder().imageName("test").memoryLimitBytes(4294967296L).build();

      assertThat(config.memoryLimitBytes()).isEqualTo(4294967296L);
    }

    @Test
    void should_accept_custom_cpu_limit() {
      final var config =
          ContainerConfiguration.builder().imageName("test").cpuNanoCores(500000000L).build();

      assertThat(config.cpuNanoCores()).isEqualTo(500000000L);
    }

    @Test
    void should_accept_custom_timeout() {
      final var config =
          ContainerConfiguration.builder()
              .imageName("test")
              .timeout(Duration.ofMinutes(30))
              .build();

      assertThat(config.timeout()).isEqualTo(Duration.ofMinutes(30));
    }
  }

  @Nested
  @DisplayName("factory methods")
  final class FactoryMethodTests {

    @Test
    void should_create_secure_defaults_configuration() {
      final var config = ContainerConfiguration.secureDefaults("analysis:latest");

      assertThat(config.imageName()).isEqualTo("analysis:latest");
      assertThat(config.memoryLimitBytes()).isEqualTo(2147483648L);
      assertThat(config.cpuNanoCores()).isEqualTo(2000000000L);
      assertThat(config.timeout()).isEqualTo(Duration.ofMinutes(10));
      assertThat(config.autoRemove()).isTrue();
      assertThat(config.privileged()).isFalse();
      assertThat(config.readOnlyRootFilesystem()).isTrue();
      assertThat(config.noNewPrivileges()).isTrue();
      assertThat(config.networkDisabled()).isFalse();
    }

    @Test
    void should_create_isolated_defaults_configuration() {
      final var config = ContainerConfiguration.isolatedDefaults("sandbox:latest");

      assertThat(config.imageName()).isEqualTo("sandbox:latest");
      assertThat(config.memoryLimitBytes()).isEqualTo(2147483648L);
      assertThat(config.cpuNanoCores()).isEqualTo(2000000000L);
      assertThat(config.timeout()).isEqualTo(Duration.ofMinutes(10));
      assertThat(config.autoRemove()).isTrue();
      assertThat(config.privileged()).isFalse();
      assertThat(config.readOnlyRootFilesystem()).isTrue();
      assertThat(config.noNewPrivileges()).isTrue();
      assertThat(config.networkDisabled()).isTrue();
    }
  }
}
