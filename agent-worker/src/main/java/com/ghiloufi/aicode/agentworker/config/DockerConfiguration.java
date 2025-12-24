package com.ghiloufi.aicode.agentworker.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DockerConfiguration {

  private final AgentWorkerProperties properties;

  @Bean
  public DockerClient dockerClient() {
    final var dockerConfig =
        DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost(properties.getDocker().getHost())
            .build();

    final var httpClient =
        new ApacheDockerHttpClient.Builder()
            .dockerHost(dockerConfig.getDockerHost())
            .sslConfig(dockerConfig.getSSLConfig())
            .maxConnections(100)
            .connectionTimeout(Duration.ofSeconds(30))
            .responseTimeout(properties.getDocker().getTimeout())
            .build();

    log.info("Docker client configured with host: {}", properties.getDocker().getHost());

    return DockerClientImpl.getInstance(dockerConfig, httpClient);
  }
}
