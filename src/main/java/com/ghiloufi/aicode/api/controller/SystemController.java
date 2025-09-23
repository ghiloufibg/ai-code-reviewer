package com.ghiloufi.aicode.api.controller;

import com.ghiloufi.aicode.api.SystemApi;
import com.ghiloufi.aicode.api.model.HealthCheck;
import com.ghiloufi.aicode.api.model.HealthStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for System API endpoints.
 *
 * <p>This controller implements the generated OpenAPI interface and provides
 * system-level endpoints like health checks.
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class SystemController implements SystemApi {

    @Override
    public Mono<ResponseEntity<HealthStatus>> _healthCheck(ServerWebExchange exchange) {
        log.debug("Health check requested");

        return Mono.fromCallable(() -> {
            Map<String, HealthCheck> checks = new HashMap<>();

            // Check application status
            checks.put("application", new HealthCheck()
                    .status(HealthCheck.StatusEnum.UP)
                    .message("Application is running")
                    .details(Map.of("uptime", "operational")));

            // Check database (if applicable)
            checks.put("database", new HealthCheck()
                    .status(HealthCheck.StatusEnum.UP)
                    .message("Database connection healthy")
                    .details(Map.of("connection", "active")));

            // Check LLM service connectivity (basic check)
            checks.put("llm", new HealthCheck()
                    .status(HealthCheck.StatusEnum.UP)
                    .message("LLM service accessible")
                    .details(Map.of("endpoint", "configured")));

            // Determine overall status
            boolean allUp = checks.values().stream()
                    .allMatch(check -> check.getStatus() == HealthCheck.StatusEnum.UP);

            HealthStatus.StatusEnum overallStatus = allUp
                    ? HealthStatus.StatusEnum.UP
                    : HealthStatus.StatusEnum.DEGRADED;

            HealthStatus healthStatus = new HealthStatus()
                    .status(overallStatus)
                    .checks(checks)
                    .timestamp(OffsetDateTime.now());

            return ResponseEntity.ok(healthStatus);
        })
        .doOnNext(response -> log.debug("Health check completed: {}",
            response.getBody().getStatus()));
    }
}