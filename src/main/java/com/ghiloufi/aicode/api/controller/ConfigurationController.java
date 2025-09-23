package com.ghiloufi.aicode.api.controller;

import com.ghiloufi.aicode.api.ConfigurationApi;
import com.ghiloufi.aicode.api.model.ApplicationConfiguration;
import com.ghiloufi.aicode.config.ApplicationConfig;
import com.ghiloufi.aicode.exception.ConfigurationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

/**
 * REST Controller for Configuration API endpoints.
 *
 * <p>This controller implements the generated OpenAPI interface and provides
 * endpoints for getting and updating application configuration.
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class ConfigurationController implements ConfigurationApi {

    private final ApplicationConfig applicationConfig;

    @Override
    public Mono<ResponseEntity<ApplicationConfiguration>> _getConfiguration(ServerWebExchange exchange) {
        log.info("Getting current configuration");

        ApplicationConfiguration config = new ApplicationConfiguration()
                .mode(ApplicationConfiguration.ModeEnum.fromValue(applicationConfig.getMode()))
                .model(applicationConfig.getModel())
                .ollamaHost(applicationConfig.getOllamaHost())
                .timeoutSeconds(applicationConfig.getTimeoutSeconds())
                .maxLinesPerChunk(applicationConfig.getMaxLinesPerChunk())
                .contextLines(applicationConfig.getContextLines())
                .defaultBranch(applicationConfig.getDefaultBranch())
                .javaVersion(applicationConfig.getJavaVersion())
                .buildSystem(applicationConfig.getBuildSystem());

        return Mono.just(ResponseEntity.ok(config));
    }

    @Override
    public Mono<ResponseEntity<ApplicationConfiguration>> _updateConfiguration(
            Mono<ApplicationConfiguration> applicationConfiguration,
            ServerWebExchange exchange) {

        return applicationConfiguration
                .doOnNext(config -> log.info("Updating configuration: mode={}, model={}",
                    config.getMode(), config.getModel()))
                .flatMap(this::validateAndUpdateConfig)
                .map(ResponseEntity::ok);
    }

    /**
     * Validates and updates the application configuration.
     */
    private Mono<ApplicationConfiguration> validateAndUpdateConfig(ApplicationConfiguration newConfig) {
        return Mono.fromCallable(() -> {
            try {
                // Validate the new configuration
                if (newConfig.getMode() != null) {
                    applicationConfig.setMode(newConfig.getMode().getValue());
                }

                if (newConfig.getModel() != null && !newConfig.getModel().trim().isEmpty()) {
                    applicationConfig.setModel(newConfig.getModel());
                }

                if (newConfig.getOllamaHost() != null && !newConfig.getOllamaHost().trim().isEmpty()) {
                    applicationConfig.setOllamaHost(newConfig.getOllamaHost());
                }

                if (newConfig.getTimeoutSeconds() != null) {
                    if (newConfig.getTimeoutSeconds() <= 0) {
                        throw new ConfigurationException("Timeout seconds must be positive");
                    }
                    applicationConfig.setTimeoutSeconds(newConfig.getTimeoutSeconds());
                }

                if (newConfig.getMaxLinesPerChunk() != null) {
                    if (newConfig.getMaxLinesPerChunk() <= 0) {
                        throw new ConfigurationException("Max lines per chunk must be positive");
                    }
                    applicationConfig.setMaxLinesPerChunk(newConfig.getMaxLinesPerChunk());
                }

                if (newConfig.getContextLines() != null) {
                    if (newConfig.getContextLines() < 0) {
                        throw new ConfigurationException("Context lines cannot be negative");
                    }
                    applicationConfig.setContextLines(newConfig.getContextLines());
                }

                if (newConfig.getDefaultBranch() != null && !newConfig.getDefaultBranch().trim().isEmpty()) {
                    applicationConfig.setDefaultBranch(newConfig.getDefaultBranch());
                }

                if (newConfig.getJavaVersion() != null && !newConfig.getJavaVersion().trim().isEmpty()) {
                    applicationConfig.setJavaVersion(newConfig.getJavaVersion());
                }

                if (newConfig.getBuildSystem() != null && !newConfig.getBuildSystem().trim().isEmpty()) {
                    applicationConfig.setBuildSystem(newConfig.getBuildSystem());
                }

                // Return the updated configuration
                return new ApplicationConfiguration()
                        .mode(ApplicationConfiguration.ModeEnum.fromValue(applicationConfig.getMode()))
                        .model(applicationConfig.getModel())
                        .ollamaHost(applicationConfig.getOllamaHost())
                        .timeoutSeconds(applicationConfig.getTimeoutSeconds())
                        .maxLinesPerChunk(applicationConfig.getMaxLinesPerChunk())
                        .contextLines(applicationConfig.getContextLines())
                        .defaultBranch(applicationConfig.getDefaultBranch())
                        .javaVersion(applicationConfig.getJavaVersion())
                        .buildSystem(applicationConfig.getBuildSystem());
            } catch (Exception e) {
                throw new ConfigurationException("Configuration update failed: " + e.getMessage(), e);
            }
        });
    }
}