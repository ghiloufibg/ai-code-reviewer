package com.ghiloufi.aicode.infrastructure.adapter.input.rest;

import com.ghiloufi.aicode.infrastructure.adapter.legacy.LegacyCodeReviewOrchestrator;
import com.ghiloufi.aicode.infrastructure.config.ApplicationConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Simple REST controller demonstrating the new Clean Architecture integration.
 *
 * <p>This controller provides a basic endpoint to test the new architecture
 * while maintaining compatibility with existing infrastructure.
 */
@RestController
@RequestMapping("/api/v1/simple")
@RequiredArgsConstructor
@Slf4j
public class SimpleReviewController {

    private final LegacyCodeReviewOrchestrator orchestrator;
    private final ApplicationConfig applicationConfig;

    /**
     * Simple endpoint to start a review using default configuration.
     */
    @PostMapping("/review")
    public Mono<ResponseEntity<Map<String, String>>> startSimpleReview() {
        log.info("Starting simple review with Clean Architecture");

        return orchestrator.executeCodeReview(applicationConfig)
            .then(Mono.just(ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Review started successfully using Clean Architecture",
                "architecture", "Clean Architecture with Hexagonal Ports and Adapters"
            ))))
            .onErrorResume(error -> {
                log.error("Review failed", error);
                return Mono.just(ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Review failed: " + error.getMessage()
                )));
            });
    }

    /**
     * Health check endpoint for the new architecture.
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        return Mono.just(ResponseEntity.ok(Map.of(
            "status", "UP",
            "architecture", "Clean Architecture",
            "layers", Map.of(
                "domain", "Domain entities and value objects",
                "application", "Use cases and ports",
                "infrastructure", "Adapters and framework code"
            )
        )));
    }
}