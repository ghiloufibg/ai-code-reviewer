package com.ghiloufi.aicode.agentworker.repository;

import java.nio.file.Path;
import java.time.Duration;

public record CloneResult(
    boolean success, Path clonedPath, String commitHash, Duration duration, String errorMessage) {

  public static CloneResult success(Path clonedPath, String commitHash, Duration duration) {
    return new CloneResult(true, clonedPath, commitHash, duration, null);
  }

  public static CloneResult failure(String errorMessage, Duration duration) {
    return new CloneResult(false, null, null, duration, errorMessage);
  }
}
