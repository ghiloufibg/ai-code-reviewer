package com.ghiloufi.aicode.agentworker.container;

import static org.assertj.core.api.Assertions.assertThat;

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
              .memoryBytes(4294967296L)
              .nanoCpus(4000000000L)
              .workspaceVolume("/tmp/workspace")
              .command(List.of("/bin/sh", "-c", "echo hello"))
              .environment(Map.of("CI", "true", "DEBUG", "1"))
              .readOnly(false)
              .autoRemove(true)
              .noNewPrivileges(true)
              .build();

      assertThat(config.imageName()).isEqualTo("analysis-image:latest");
      assertThat(config.memoryBytes()).isEqualTo(4294967296L);
      assertThat(config.nanoCpus()).isEqualTo(4000000000L);
      assertThat(config.workspaceVolume()).isEqualTo("/tmp/workspace");
      assertThat(config.command()).containsExactly("/bin/sh", "-c", "echo hello");
      assertThat(config.environment()).containsEntry("CI", "true").containsEntry("DEBUG", "1");
      assertThat(config.readOnly()).isFalse();
      assertThat(config.autoRemove()).isTrue();
      assertThat(config.noNewPrivileges()).isTrue();
    }

    @Test
    void should_use_secure_defaults() {
      final var config =
          ContainerConfiguration.builder().imageName("test-image").workspaceVolume("/tmp").build();

      assertThat(config.readOnly()).isTrue();
      assertThat(config.autoRemove()).isTrue();
      assertThat(config.noNewPrivileges()).isTrue();
    }

    @Test
    void should_use_default_resource_limits() {
      final var config =
          ContainerConfiguration.builder().imageName("test-image").workspaceVolume("/tmp").build();

      assertThat(config.memoryBytes()).isEqualTo(2147483648L);
      assertThat(config.nanoCpus()).isEqualTo(2000000000L);
    }

    @Test
    void should_use_empty_defaults_for_command_and_environment() {
      final var config =
          ContainerConfiguration.builder().imageName("test-image").workspaceVolume("/tmp").build();

      assertThat(config.command()).isEmpty();
      assertThat(config.environment()).isEmpty();
    }

    @Test
    void should_allow_disabling_security_features() {
      final var config =
          ContainerConfiguration.builder()
              .imageName("test-image")
              .workspaceVolume("/tmp")
              .readOnly(false)
              .autoRemove(false)
              .noNewPrivileges(false)
              .build();

      assertThat(config.readOnly()).isFalse();
      assertThat(config.autoRemove()).isFalse();
      assertThat(config.noNewPrivileges()).isFalse();
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
              "/data",
              List.of("run"),
              Map.of("KEY", "VALUE"),
              true,
              false,
              true);

      assertThat(config.imageName()).isEqualTo("my-image");
      assertThat(config.memoryBytes()).isEqualTo(1073741824L);
      assertThat(config.nanoCpus()).isEqualTo(1000000000L);
      assertThat(config.workspaceVolume()).isEqualTo("/data");
      assertThat(config.command()).containsExactly("run");
      assertThat(config.environment()).containsEntry("KEY", "VALUE");
      assertThat(config.readOnly()).isTrue();
      assertThat(config.autoRemove()).isFalse();
      assertThat(config.noNewPrivileges()).isTrue();
    }
  }

  @Nested
  @DisplayName("resource limits")
  final class ResourceLimitTests {

    @Test
    void should_accept_2gb_memory_limit() {
      final var config =
          ContainerConfiguration.builder()
              .imageName("test")
              .workspaceVolume("/tmp")
              .memoryBytes(2147483648L)
              .build();

      assertThat(config.memoryBytes()).isEqualTo(2147483648L);
    }

    @Test
    void should_accept_4gb_memory_limit() {
      final var config =
          ContainerConfiguration.builder()
              .imageName("test")
              .workspaceVolume("/tmp")
              .memoryBytes(4294967296L)
              .build();

      assertThat(config.memoryBytes()).isEqualTo(4294967296L);
    }

    @Test
    void should_accept_custom_cpu_limit() {
      final var config =
          ContainerConfiguration.builder()
              .imageName("test")
              .workspaceVolume("/tmp")
              .nanoCpus(500000000L)
              .build();

      assertThat(config.nanoCpus()).isEqualTo(500000000L);
    }
  }
}
