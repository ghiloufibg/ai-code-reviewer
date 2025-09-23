package com.ghiloufi.aicode.api.controller;

import com.ghiloufi.aicode.api.model.*;
import com.ghiloufi.aicode.config.ApplicationConfig;
import com.ghiloufi.aicode.exception.ConfigurationException;
import com.ghiloufi.aicode.exception.ReviewNotFoundException;
import com.ghiloufi.aicode.orchestrator.CodeReviewOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * REST Controller for Code Review API endpoints.
 * Temporary implementation to maintain compatibility while migrating to new API structure.
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class CodeReviewController {

    private final CodeReviewOrchestrator orchestrator;
    private final ApplicationConfig applicationConfig;

    // TODO: Temporary implementation - needs to be migrated to new API structure
    // Original code review logic will be moved to AnalysisApi implementation
}