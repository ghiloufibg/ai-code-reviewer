package com.ghiloufi.aicode.api.controller;

import com.ghiloufi.aicode.api.CodeReviewApi;
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
 *
 * <p>This controller implements the generated OpenAPI interface and provides
 * endpoints for managing code reviews, including starting new reviews,
 * checking status, and retrieving results.
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class CodeReviewController implements CodeReviewApi {

    private final CodeReviewOrchestrator orchestrator;
    private final ApplicationConfig applicationConfig;

    @Override
    public Mono<ResponseEntity<CodeReviewResponse>> _startCodeReview(
            Mono<CodeReviewRequest> codeReviewRequest,
            ServerWebExchange exchange) {

        return codeReviewRequest
                .doOnNext(request -> log.info("Starting code review: mode={}, repo={}",
                    request.getMode(), request.getRepository()))
                .flatMap(this::validateAndMapRequest)
                .flatMap(config -> {
                    // Start the review asynchronously
                    String reviewId = UUID.randomUUID().toString();

                    // Execute the review in the background
                    orchestrator.executeCodeReview(config)
                        .doOnSuccess(unused -> log.info("Code review completed: {}", reviewId))
                        .doOnError(error -> log.error("Code review failed: {}", reviewId, error))
                        .subscribe();

                    // Return immediate response
                    CodeReviewResponse response = new CodeReviewResponse()
                            .reviewId(UUID.fromString(reviewId))
                            .status(ReviewStatus.PENDING)
                            .message("Code review started successfully")
                            .createdAt(OffsetDateTime.now())
                            .estimatedDuration(300); // 5 minutes estimated

                    return Mono.just(ResponseEntity.status(HttpStatus.ACCEPTED).body(response));
                });
    }

    @Override
    public Mono<ResponseEntity<CodeReviewListResponse>> _listCodeReviews(
            ReviewStatus status,
            String mode,
            Integer page,
            Integer size,
            ServerWebExchange exchange) {

        log.info("Listing code reviews: status={}, mode={}, page={}, size={}",
            status, mode, page, size);

        // For now, return empty list - in real implementation,
        // this would query a database or storage service
        CodeReviewListResponse response = new CodeReviewListResponse()
                .content(java.util.List.of())
                .page(page != null ? page : 0)
                .size(size != null ? size : 20)
                .totalElements(0)
                .totalPages(0);

        return Mono.just(ResponseEntity.ok(response));
    }

    @Override
    public Mono<ResponseEntity<CodeReviewDetails>> _getCodeReview(
            UUID reviewId,
            ServerWebExchange exchange) {

        log.info("Getting code review details: {}", reviewId);

        // For now, throw exception - in real implementation,
        // this would query the review from storage
        return Mono.error(new ReviewNotFoundException(reviewId.toString()));
    }

    @Override
    public Mono<ResponseEntity<ReviewStatusResponse>> _getReviewStatus(
            UUID reviewId,
            ServerWebExchange exchange) {

        log.info("Getting review status: {}", reviewId);

        // For now, throw exception - in real implementation,
        // this would query the review status from storage
        return Mono.error(new ReviewNotFoundException(reviewId.toString()));
    }

    @Override
    public Mono<ResponseEntity<ReviewResult>> _getReviewResults(
            UUID reviewId,
            ServerWebExchange exchange) {

        log.info("Getting review results: {}", reviewId);

        // For now, throw exception - in real implementation,
        // this would query the review results from storage
        return Mono.error(new ReviewNotFoundException(reviewId.toString()));
    }

    /**
     * Validates and maps CodeReviewRequest to ApplicationConfig.
     */
    private Mono<ApplicationConfig> validateAndMapRequest(CodeReviewRequest request) {
        return Mono.fromCallable(() -> {
            try {
                // Create a copy of the current config
                ApplicationConfig config = new ApplicationConfig();

                // Map from request
                config.setMode(request.getMode().getValue());

                if (request.getRepository() != null) {
                    config.setRepository(request.getRepository());
                }

                if (request.getPullRequestNumber() != null) {
                    config.setPullRequestNumber(request.getPullRequestNumber());
                }

                if (request.getFromCommit() != null) {
                    config.setFromCommit(request.getFromCommit());
                }

                if (request.getToCommit() != null) {
                    config.setToCommit(request.getToCommit());
                }

                if (request.getModel() != null) {
                    config.setModel(request.getModel());
                } else {
                    config.setModel(applicationConfig.getModel());
                }

                if (request.getMaxLinesPerChunk() != null) {
                    config.setMaxLinesPerChunk(request.getMaxLinesPerChunk());
                } else {
                    config.setMaxLinesPerChunk(applicationConfig.getMaxLinesPerChunk());
                }

                if (request.getContextLines() != null) {
                    config.setContextLines(request.getContextLines());
                } else {
                    config.setContextLines(applicationConfig.getContextLines());
                }

                // Copy other settings from application config
                config.setOllamaHost(applicationConfig.getOllamaHost());
                config.setTimeoutSeconds(applicationConfig.getTimeoutSeconds());
                config.setDefaultBranch(applicationConfig.getDefaultBranch());
                config.setJavaVersion(applicationConfig.getJavaVersion());
                config.setBuildSystem(applicationConfig.getBuildSystem());

                // Initialize GitHub token
                config.initializeGithubToken();

                // Validate GitHub mode requirements
                config.validateGithubMode();

                return config;
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException("Invalid configuration: " + e.getMessage(), e);
            }
        });
    }
}