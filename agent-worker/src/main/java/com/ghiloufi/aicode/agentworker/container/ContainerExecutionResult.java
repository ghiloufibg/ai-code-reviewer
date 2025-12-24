package com.ghiloufi.aicode.agentworker.container;

import java.time.Duration;
import java.time.Instant;

public record ContainerExecutionResult(
    String containerId,
    int exitCode,
    String stdout,
    String stderr,
    Instant startTime,
    Instant endTime) {

  public boolean isSuccess() {
    return exitCode == 0;
  }

  public Duration duration() {
    return Duration.between(startTime, endTime);
  }

  public static ContainerExecutionResult success(
      String containerId, String stdout, Instant startTime, Instant endTime) {
    return new ContainerExecutionResult(containerId, 0, stdout, "", startTime, endTime);
  }

  public static ContainerExecutionResult failure(
      String containerId, int exitCode, String stderr, Instant startTime, Instant endTime) {
    return new ContainerExecutionResult(containerId, exitCode, "", stderr, startTime, endTime);
  }
}
