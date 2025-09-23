package com.ghiloufi.aicode.api.controller;

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
 * Temporary implementation while migrating to new SettingsApi structure.
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class ConfigurationController {

    private final ApplicationConfig applicationConfig;

    // TODO: Migrate to SettingsApi implementation
    // Original configuration logic will be moved to SettingsApi
}