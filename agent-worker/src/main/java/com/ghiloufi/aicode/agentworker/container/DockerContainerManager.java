package com.ghiloufi.aicode.agentworker.container;

import com.ghiloufi.aicode.agentworker.config.AgentWorkerProperties;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DockerContainerManager {

  private final DockerClient dockerClient;
  private final AgentWorkerProperties properties;

  public ContainerExecutionResult executeInContainer(ContainerConfiguration config) {
    final var startTime = Instant.now();
    String containerId = null;

    try {
      containerId = createContainer(config);
      log.info("Created container: {}", containerId);

      dockerClient.startContainerCmd(containerId).exec();
      log.debug("Started container: {}", containerId);

      final var waitResult = waitForCompletion(containerId);
      final var endTime = Instant.now();

      final var logs = collectLogs(containerId);

      if (waitResult == 0) {
        log.info(
            "Container {} completed successfully in {}ms",
            containerId,
            endTime.toEpochMilli() - startTime.toEpochMilli());
        return ContainerExecutionResult.success(containerId, logs, startTime, endTime);
      } else {
        log.warn("Container {} exited with code {}", containerId, waitResult);
        return ContainerExecutionResult.failure(containerId, waitResult, logs, startTime, endTime);
      }

    } catch (Exception e) {
      log.error("Container execution failed", e);
      return ContainerExecutionResult.failure(
          containerId != null ? containerId : "unknown",
          -1,
          e.getMessage(),
          startTime,
          Instant.now());
    } finally {
      if (containerId != null && !config.autoRemove()) {
        removeContainer(containerId);
      }
    }
  }

  private String createContainer(ContainerConfiguration config) {
    final var hostConfig = createHostConfig(config);

    final var envList = new ArrayList<String>();
    config.environment().forEach((k, v) -> envList.add(k + "=" + v));

    final CreateContainerResponse container =
        dockerClient
            .createContainerCmd(config.imageName())
            .withHostConfig(hostConfig)
            .withCmd(config.command())
            .withEnv(envList)
            .withWorkingDir("/workspace")
            .exec();

    return container.getId();
  }

  private HostConfig createHostConfig(ContainerConfiguration config) {
    final var hostConfig =
        HostConfig.newHostConfig()
            .withMemory(config.memoryBytes())
            .withNanoCPUs(config.nanoCpus())
            .withAutoRemove(config.autoRemove())
            .withReadonlyRootfs(config.readOnly());

    if (config.workspaceVolume() != null) {
      final var workspaceVolume = new Volume("/workspace");
      hostConfig.withBinds(Bind.parse(config.workspaceVolume() + ":/workspace:rw"));
    }

    if (config.noNewPrivileges()) {
      hostConfig.withSecurityOpts(List.of("no-new-privileges"));
    }

    return hostConfig;
  }

  private int waitForCompletion(String containerId) throws InterruptedException {
    final var timeout = properties.getDocker().getTimeout().toSeconds();

    return dockerClient
        .waitContainerCmd(containerId)
        .exec(new WaitContainerResultCallback())
        .awaitStatusCode((int) timeout, TimeUnit.SECONDS);
  }

  private String collectLogs(String containerId) {
    final var stdout = new StringBuilder();
    final var stderr = new StringBuilder();

    try {
      dockerClient
          .logContainerCmd(containerId)
          .withStdOut(true)
          .withStdErr(true)
          .withFollowStream(false)
          .exec(new LogCollector(stdout, stderr))
          .awaitCompletion(30, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("Interrupted while collecting logs for container {}", containerId);
    }

    if (!stderr.isEmpty()) {
      return stderr.toString();
    }
    return stdout.toString();
  }

  private void removeContainer(String containerId) {
    try {
      dockerClient.removeContainerCmd(containerId).withForce(true).exec();
      log.debug("Removed container: {}", containerId);
    } catch (Exception e) {
      log.warn("Failed to remove container {}: {}", containerId, e.getMessage());
    }
  }

  public void pullImageIfNeeded(String imageName) {
    if (!properties.getDocker().isAutoPull()) {
      return;
    }

    try {
      dockerClient.inspectImageCmd(imageName).exec();
      log.debug("Image {} already exists locally", imageName);
    } catch (Exception e) {
      log.info("Pulling image: {}", imageName);
      try {
        dockerClient.pullImageCmd(imageName).start().awaitCompletion(5, TimeUnit.MINUTES);
        log.info("Successfully pulled image: {}", imageName);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Interrupted while pulling image: " + imageName, ie);
      }
    }
  }

  private static final class LogCollector extends ResultCallback.Adapter<Frame> {

    private final StringBuilder stdout;
    private final StringBuilder stderr;

    LogCollector(StringBuilder stdout, StringBuilder stderr) {
      this.stdout = stdout;
      this.stderr = stderr;
    }

    @Override
    public void onNext(Frame frame) {
      final var content = new String(frame.getPayload(), StandardCharsets.UTF_8);
      switch (frame.getStreamType()) {
        case STDOUT, RAW -> stdout.append(content);
        case STDERR -> stderr.append(content);
        default -> log.trace("Unknown stream type: {}", frame.getStreamType());
      }
    }
  }
}
