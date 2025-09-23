package com.ghiloufi.aicode.infrastructure.adapter.output.filesystem;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Adapter for local Git operations.
 *
 * <p>Provides functionality to collect diffs from local Git repository using Git commands.
 */
@Component
@Slf4j
public class LocalGitAdapter {

  /** Gets diff between two commits. */
  public Mono<String> getDiff(String fromCommit, String toCommit) {
    return Mono.fromCallable(() -> executeGitDiff(fromCommit, toCommit))
        .subscribeOn(Schedulers.boundedElastic())
        .doOnNext(diff -> log.debug("Collected diff: {} characters", diff.length()));
  }

  /** Executes git diff command. */
  private String executeGitDiff(String fromCommit, String toCommit) {
    try {
      String[] command = {
        "git",
        "diff",
        "--unified=5", // 5 lines of context
        fromCommit + ".." + toCommit
      };

      ProcessBuilder processBuilder = new ProcessBuilder(command);
      processBuilder.redirectErrorStream(true);
      Process process = processBuilder.start();

      StringBuilder output = new StringBuilder();
      try (BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          output.append(line).append("\n");
        }
      }

      int exitCode = process.waitFor();
      if (exitCode != 0) {
        throw new RuntimeException("Git diff command failed with exit code: " + exitCode);
      }

      return output.toString();

    } catch (Exception e) {
      log.error("Failed to execute git diff", e);
      throw new RuntimeException("Failed to collect local Git diff", e);
    }
  }
}
