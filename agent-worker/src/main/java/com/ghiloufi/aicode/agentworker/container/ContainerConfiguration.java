package com.ghiloufi.aicode.agentworker.container;

import java.util.List;
import java.util.Map;

public record ContainerConfiguration(
    String imageName,
    long memoryBytes,
    long nanoCpus,
    String workspaceVolume,
    List<String> command,
    Map<String, String> environment,
    boolean readOnly,
    boolean autoRemove,
    boolean noNewPrivileges) {

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String imageName;
    private long memoryBytes = 2147483648L;
    private long nanoCpus = 2000000000L;
    private String workspaceVolume;
    private List<String> command = List.of();
    private Map<String, String> environment = Map.of();
    private boolean readOnly = true;
    private boolean autoRemove = true;
    private boolean noNewPrivileges = true;

    public Builder imageName(String imageName) {
      this.imageName = imageName;
      return this;
    }

    public Builder memoryBytes(long memoryBytes) {
      this.memoryBytes = memoryBytes;
      return this;
    }

    public Builder nanoCpus(long nanoCpus) {
      this.nanoCpus = nanoCpus;
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

    public Builder readOnly(boolean readOnly) {
      this.readOnly = readOnly;
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

    public ContainerConfiguration build() {
      return new ContainerConfiguration(
          imageName,
          memoryBytes,
          nanoCpus,
          workspaceVolume,
          command,
          environment,
          readOnly,
          autoRemove,
          noNewPrivileges);
    }
  }
}
