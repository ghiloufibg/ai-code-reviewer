package com.ghiloufi.aicode.agentworker.container;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ContainerConfiguration(
    String imageName,
    long memoryLimitBytes,
    long cpuNanoCores,
    Duration timeout,
    String workingDirectory,
    String workspaceVolume,
    List<String> command,
    Map<String, String> environment,
    boolean readOnlyRootFilesystem,
    boolean autoRemove,
    boolean noNewPrivileges,
    boolean privileged,
    boolean networkDisabled) {

  private static final long DEFAULT_MEMORY_BYTES = 2147483648L;
  private static final long DEFAULT_CPU_NANOCORES = 2000000000L;
  private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(10);
  private static final String DEFAULT_WORKSPACE = "/workspace";

  public static ContainerConfiguration secureDefaults(String imageName) {
    return builder()
        .imageName(imageName)
        .memoryLimitBytes(DEFAULT_MEMORY_BYTES)
        .cpuNanoCores(DEFAULT_CPU_NANOCORES)
        .timeout(DEFAULT_TIMEOUT)
        .autoRemove(true)
        .privileged(false)
        .readOnlyRootFilesystem(true)
        .noNewPrivileges(true)
        .networkDisabled(false)
        .build();
  }

  public static ContainerConfiguration isolatedDefaults(String imageName) {
    return builder()
        .imageName(imageName)
        .memoryLimitBytes(DEFAULT_MEMORY_BYTES)
        .cpuNanoCores(DEFAULT_CPU_NANOCORES)
        .timeout(DEFAULT_TIMEOUT)
        .autoRemove(true)
        .privileged(false)
        .readOnlyRootFilesystem(true)
        .noNewPrivileges(true)
        .networkDisabled(true)
        .build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String imageName;
    private long memoryLimitBytes = DEFAULT_MEMORY_BYTES;
    private long cpuNanoCores = DEFAULT_CPU_NANOCORES;
    private Duration timeout = DEFAULT_TIMEOUT;
    private String workingDirectory = DEFAULT_WORKSPACE;
    private String workspaceVolume;
    private List<String> command = List.of();
    private Map<String, String> environment = Map.of();
    private boolean readOnlyRootFilesystem = false;
    private boolean autoRemove = true;
    private boolean noNewPrivileges = true;
    private boolean privileged = false;
    private boolean networkDisabled = false;

    public Builder imageName(String imageName) {
      this.imageName = Objects.requireNonNull(imageName, "imageName");
      return this;
    }

    public Builder memoryLimitBytes(long memoryLimitBytes) {
      if (memoryLimitBytes <= 0) {
        throw new IllegalArgumentException("memoryLimitBytes must be positive");
      }
      this.memoryLimitBytes = memoryLimitBytes;
      return this;
    }

    public Builder cpuNanoCores(long cpuNanoCores) {
      if (cpuNanoCores <= 0) {
        throw new IllegalArgumentException("cpuNanoCores must be positive");
      }
      this.cpuNanoCores = cpuNanoCores;
      return this;
    }

    public Builder timeout(Duration timeout) {
      this.timeout = Objects.requireNonNull(timeout, "timeout");
      return this;
    }

    public Builder workingDirectory(String workingDirectory) {
      this.workingDirectory = workingDirectory;
      return this;
    }

    public Builder workspaceVolume(String workspaceVolume) {
      this.workspaceVolume = workspaceVolume;
      return this;
    }

    public Builder command(List<String> command) {
      this.command = command;
      return this;
    }

    public Builder environment(Map<String, String> environment) {
      this.environment = environment;
      return this;
    }

    public Builder readOnlyRootFilesystem(boolean readOnlyRootFilesystem) {
      this.readOnlyRootFilesystem = readOnlyRootFilesystem;
      return this;
    }

    public Builder autoRemove(boolean autoRemove) {
      this.autoRemove = autoRemove;
      return this;
    }

    public Builder noNewPrivileges(boolean noNewPrivileges) {
      this.noNewPrivileges = noNewPrivileges;
      return this;
    }

    public Builder privileged(boolean privileged) {
      this.privileged = privileged;
      return this;
    }

    public Builder networkDisabled(boolean networkDisabled) {
      this.networkDisabled = networkDisabled;
      return this;
    }

    public ContainerConfiguration build() {
      Objects.requireNonNull(imageName, "imageName is required");
      return new ContainerConfiguration(
          imageName,
          memoryLimitBytes,
          cpuNanoCores,
          timeout,
          workingDirectory,
          workspaceVolume,
          command,
          environment,
          readOnlyRootFilesystem,
          autoRemove,
          noNewPrivileges,
          privileged,
          networkDisabled);
    }
  }
}
