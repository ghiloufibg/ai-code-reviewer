package com.ghiloufi.aicode.agentworker.repository;

import com.ghiloufi.aicode.agentworker.config.AgentWorkerProperties;
import com.ghiloufi.aicode.agentworker.container.ContainerConfiguration;
import com.ghiloufi.aicode.agentworker.container.DockerContainerManager;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RepositoryCloner {

  private final DockerContainerManager containerManager;
  private final AgentWorkerProperties properties;

  public CloneResult clone(CloneRequest request) {
    final var startTime = Instant.now();

    try {
      log.info(
          "Cloning repository {} branch {} to {}",
          sanitizeUrl(request.repositoryUrl()),
          request.branch(),
          request.targetDirectory());

      final var containerConfig = buildCloneContainerConfig(request);
      final var result = containerManager.executeInContainer(containerConfig);

      final var duration = Duration.between(startTime, Instant.now());

      if (result.isSuccess()) {
        final var commitHash = extractCommitHash(result.stdout());
        log.info(
            "Successfully cloned repository in {}ms, commit: {}", duration.toMillis(), commitHash);
        return CloneResult.success(Path.of(request.targetDirectory()), commitHash, duration);
      } else {
        log.error("Clone failed with exit code {}: {}", result.exitCode(), result.stderr());
        return CloneResult.failure(result.stderr(), duration);
      }

    } catch (Exception e) {
      log.error("Clone operation failed", e);
      return CloneResult.failure(e.getMessage(), Duration.between(startTime, Instant.now()));
    }
  }

  private ContainerConfiguration buildCloneContainerConfig(CloneRequest request) {
    final var authenticatedUrl = buildAuthenticatedUrl(request);
    final var cloneCommand = buildCloneCommand(request, authenticatedUrl);

    final Map<String, String> env = new HashMap<>();
    env.put("GIT_TERMINAL_PROMPT", "0");

    return ContainerConfiguration.builder()
        .imageName(properties.getDocker().getAnalysisImage())
        .memoryBytes(properties.getDocker().getResourceLimits().getMemoryBytes())
        .nanoCpus(properties.getDocker().getResourceLimits().getNanoCpus())
        .workspaceVolume(request.targetDirectory())
        .command(cloneCommand)
        .environment(env)
        .readOnly(false)
        .autoRemove(true)
        .noNewPrivileges(true)
        .build();
  }

  private List<String> buildCloneCommand(CloneRequest request, String authenticatedUrl) {
    return List.of(
        "/bin/sh",
        "-c",
        String.format(
            "git clone --depth=%d --branch=%s --single-branch %s /workspace/repo && "
                + "cd /workspace/repo && git rev-parse HEAD",
            request.depth(), request.branch(), authenticatedUrl));
  }

  private String buildAuthenticatedUrl(CloneRequest request) {
    if (request.authToken() == null || request.authToken().isBlank()) {
      return request.repositoryUrl();
    }

    final var url = request.repositoryUrl();
    if (url.startsWith("https://github.com/")) {
      return url.replace(
          "https://github.com/", "https://x-access-token:" + request.authToken() + "@github.com/");
    } else if (url.startsWith("https://gitlab.com/")) {
      return url.replace(
          "https://gitlab.com/", "https://oauth2:" + request.authToken() + "@gitlab.com/");
    }

    return url;
  }

  private String extractCommitHash(String output) {
    if (output == null || output.isBlank()) {
      return "unknown";
    }

    final var lines = output.trim().split("\n");
    for (int i = lines.length - 1; i >= 0; i--) {
      final var line = lines[i].trim();
      if (line.matches("[a-f0-9]{40}")) {
        return line;
      }
    }

    return "unknown";
  }

  private String sanitizeUrl(String url) {
    return url.replaceAll("://[^@]+@", "://***@");
  }
}
