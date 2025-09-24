package com.ghiloufi.aicode.service.analysis;

import com.ghiloufi.aicode.api.model.*;
import com.ghiloufi.aicode.client.llm.LlmClient;
import com.ghiloufi.aicode.client.llm.PromptBuilder;
import com.ghiloufi.aicode.service.analysis.model.AnalysisEntity;
import com.ghiloufi.aicode.service.analysis.repository.AnalysisRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Service for managing code analysis lifecycle with real LLM integration. Handles file upload,
 * analysis processing with LLM, and real-time progress tracking via SSE events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisService {

  private final AnalysisRepository repository;
  private final DummyDataGenerator dummyDataGenerator;
  private final FileStorageService fileStorageService;
  private final LlmClient llmClient;
  private final PromptBuilder promptBuilder;
  private final LlmResponseParser llmResponseParser;

  // SSE sinks for real-time progress streaming
  private final Map<String, Sinks.Many<String>> progressSinks = new ConcurrentHashMap<>();

  /** Start a new analysis with uploaded files - Fully Reactive */
  public Mono<AnalysisResponse> startAnalysis(
      List<String> fileNames, AnalysisOptions options, String analysisId) {
    log.info("Starting analysis: {} with {} files", analysisId, fileNames.size());

    // Calculate total size (dummy calculation)
    long totalSize =
        fileNames.stream()
            .mapToLong(name -> 1000 + (long) (Math.random() * 5000)) // 1-6KB per file
            .sum();

    // Create analysis entity
    AnalysisEntity entity =
        AnalysisEntity.builder()
            .id(analysisId)
            .status(AnalysisStatus.StatusEnum.PENDING)
            .createdAt(LocalDateTime.now())
            .progress(new AtomicInteger(0))
            .currentStep("Initializing")
            .totalSteps(4)
            .stepsCompleted(0)
            .uploadedFiles(fileNames)
            .filesCount(fileNames.size())
            .totalSize(totalSize)
            .model(options != null ? options.getModel() : "deepseek-coder")
            .staticAnalysis(options != null ? options.getStaticAnalysis() : true)
            .securityScan(options != null ? options.getSecurityScan() : true)
            .performanceAnalysis(options != null ? options.getPerformanceAnalysis() : false)
            .estimatedTimeRemaining("3-5 minutes")
            .build();

    repository.save(entity);

    // Start reactive processing
    processAnalysisReactive(analysisId)
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe();

    // Return response
    AnalysisResponse response = new AnalysisResponse()
        .id(analysisId)
        .status(AnalysisResponse.StatusEnum.PENDING)
        .message("Analysis started successfully")
        .createdAt(OffsetDateTime.now())
        .estimatedDuration("5 minutes")
        .filesCount(fileNames.size())
        .totalSize((int) totalSize);

    return Mono.just(response);
  }

  /** Get current analysis status */
  public Optional<AnalysisStatus> getAnalysisStatus(String analysisId) {
    return repository.findById(analysisId).map(this::mapToAnalysisStatus);
  }

  /** Get analysis results (only for completed analyses) */
  public Optional<AnalysisResults> getAnalysisResults(String analysisId) {
    return repository
        .findById(analysisId)
        .filter(entity -> entity.getStatus() == AnalysisStatus.StatusEnum.COMPLETED)
        .map(
            entity -> {
              // Try to get real LLM results first, fallback to dummy if not available
              try {
                Map<String, String> fileContents = fileStorageService.getFileContents(analysisId);
                if (fileContents != null && !fileContents.isEmpty()) {
                  // Return real analysis results if available
                  return entity.getLlmResults();
                }
              } catch (Exception e) {
                log.warn("Failed to retrieve real analysis results, using dummy data", e);
              }
              return dummyDataGenerator.generateAnalysisResults(analysisId, entity);
            });
  }

  /** Cancel a running analysis */
  public boolean cancelAnalysis(String analysisId) {
    Optional<AnalysisEntity> entityOpt = repository.findById(analysisId);
    if (entityOpt.isEmpty()) {
      return false;
    }

    AnalysisEntity entity = entityOpt.get();
    if (!entity.canBeCancelled()) {
      log.warn("Cannot cancel analysis {} in status: {}", analysisId, entity.getStatus());
      return false;
    }

    entity.markAsCancelled();
    repository.update(entity);

    // Close SSE sink if exists
    Sinks.Many<String> sink = progressSinks.remove(analysisId);
    if (sink != null) {
      sink.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
    }

    log.info("Cancelled analysis: {}", analysisId);
    return true;
  }

  /** Create SSE stream for real-time progress updates */
  public Flux<String> getProgressStream(String analysisId) {
    Optional<AnalysisEntity> entityOpt = repository.findById(analysisId);
    if (entityOpt.isEmpty()) {
      return Flux.error(new IllegalArgumentException("Analysis not found: " + analysisId));
    }

    // Create or get existing sink
    Sinks.Many<String> sink =
        progressSinks.computeIfAbsent(
            analysisId, id -> Sinks.many().multicast().onBackpressureBuffer());

    // Send current status immediately
    AnalysisEntity entity = entityOpt.get();
    String currentEvent = createProgressEvent(entity);
    sink.emitNext(currentEvent, Sinks.EmitFailureHandler.FAIL_FAST);

    return sink.asFlux()
        .doOnCancel(
            () -> {
              log.debug("SSE stream cancelled for analysis: {}", analysisId);
              progressSinks.remove(analysisId);
            })
        .doOnComplete(
            () -> {
              log.debug("SSE stream completed for analysis: {}", analysisId);
              progressSinks.remove(analysisId);
            });
  }

  /** Process analysis reactively with real LLM integration - Fully Non-Blocking */
  public Mono<Void> processAnalysisReactive(String analysisId) {
    log.info("Starting reactive processing for analysis: {}", analysisId);

    return Mono.fromSupplier(() -> repository.findById(analysisId))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .switchIfEmpty(Mono.fromRunnable(() -> log.error("Analysis entity not found for processing: {}", analysisId)))
        .flatMap(entity -> {

          // Step 1: File Processing (25%)
          updateProgress(entity, 0, "File Processing", "Reading uploaded files...");
          return Mono.fromSupplier(() -> fileStorageService.getFileContents(analysisId))
              .filter(fileContents -> fileContents != null && !fileContents.isEmpty())
              .switchIfEmpty(Mono.error(new RuntimeException("No file contents found for analysis: " + analysisId)))
              .doOnNext(fileContents -> updateProgress(entity, 25, "File Processing", "Files processed successfully"))

              .flatMap(fileContents -> {
                // Step 2: Content Preparation (50%)
                updateProgress(entity, 25, "Content Preparation", "Preparing content for LLM analysis...");
                return Mono.fromSupplier(() -> fileStorageService.generateSimplifiedContent(fileContents))
                    .doOnNext(unifiedDiff -> updateProgress(entity, 50, "Content Preparation", "Content prepared for analysis"));

              })
              .flatMap(unifiedDiff -> {
                // Step 3: AI Analysis (75%)
                updateProgress(entity, 50, "AI Analysis", "Performing AI code review...");
                String systemPrompt = promptBuilder.getSystemPrompt();
                String userPrompt = promptBuilder.buildUserMessage(
                    "uploaded-files", // repository name
                    "main", // default branch
                    "17", // java version
                    "maven", // build system
                    unifiedDiff, // unified diff
                    null, // static analysis (not available for uploaded files)
                    null, // project config
                    null // test status
                );

                // FULLY REACTIVE LLM CALL - NO BLOCKING!
                return llmClient.review(systemPrompt, userPrompt)
                    .doOnError(error -> log.error("LLM review failed", error))
                    .onErrorReturn("Error occurred during LLM analysis")
                    .filter(response -> response != null && !response.trim().isEmpty())
                    .switchIfEmpty(Mono.error(new RuntimeException("Empty response from LLM")))
                    .doOnNext(response -> updateProgress(entity, 75, "AI Analysis", "AI analysis completed"));

              })
              .flatMap(llmResponse -> {
                // Step 4: Results Generation (100%)
                updateProgress(entity, 75, "Results Generation", "Parsing LLM response...");
                return Mono.fromSupplier(() -> llmResponseParser.parseAnalysisResults(analysisId, llmResponse, entity))
                    .doOnNext(results -> {
                      entity.setLlmResults(results);
                      repository.update(entity);
                      updateProgress(entity, 100, "Completed", "Analysis completed successfully");

                      // Close SSE stream
                      Sinks.Many<String> sink = progressSinks.remove(analysisId);
                      if (sink != null) {
                        String completedEvent = createCompletedEvent();
                        sink.emitNext(completedEvent, Sinks.EmitFailureHandler.FAIL_FAST);
                        sink.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
                      }

                      log.info("Analysis completed successfully: {}", analysisId);
                    })
                    .then();
              });
        })
        .onErrorResume(error -> {
          log.error("Analysis processing failed: {}", analysisId, error);

          return Mono.fromSupplier(() -> repository.findById(analysisId))
              .filter(Optional::isPresent)
              .map(Optional::get)
              .doOnNext(entity -> {
                entity.markAsFailed("Internal processing error: " + error.getMessage());
                repository.update(entity);

                // Send error event via SSE
                Sinks.Many<String> sink = progressSinks.remove(analysisId);
                if (sink != null) {
                  String errorEvent = createErrorEvent(error.getMessage());
                  sink.emitNext(errorEvent, Sinks.EmitFailureHandler.FAIL_FAST);
                  sink.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
                }
              })
              .then();
        })
        .then();
  }

  /** Update progress and send SSE event */
  private void updateProgress(AnalysisEntity entity, int progress, String step, String message) {
    entity.updateProgress(progress, step);
    entity.setStepsCompleted(progress / 25); // Each step is 25%
    entity.setEstimatedTimeRemaining(calculateEstimatedTime(progress));

    if (progress > 0) {
      entity.setStatus(AnalysisStatus.StatusEnum.IN_PROGRESS);
      if (entity.getStartedAt() == null) {
        entity.setStartedAt(LocalDateTime.now());
      }
    }

    repository.update(entity);

    // Send SSE event
    Sinks.Many<String> sink = progressSinks.get(entity.getId());
    if (sink != null) {
      String event = createProgressEvent(entity);
      sink.emitNext(event, Sinks.EmitFailureHandler.FAIL_FAST);
    }

    log.debug("Progress updated for {}: {}% - {}", entity.getId(), progress, step);
  }

  /** Map entity to AnalysisStatus DTO */
  private AnalysisStatus mapToAnalysisStatus(AnalysisEntity entity) {
    return new AnalysisStatus()
        .id(entity.getId())
        .status(entity.getStatus())
        .progress(entity.getCurrentProgress())
        .currentStep(entity.getCurrentStep())
        .stepsCompleted(entity.getStepsCompleted())
        .totalSteps(entity.getTotalSteps())
        .estimatedTimeRemaining(entity.getEstimatedTimeRemaining())
        .startedAt(
            entity.getStartedAt() != null
                ? entity.getStartedAt().atOffset(OffsetDateTime.now().getOffset())
                : null)
        .completedAt(
            entity.getCompletedAt() != null
                ? entity.getCompletedAt().atOffset(OffsetDateTime.now().getOffset())
                : null)
        .error(entity.getErrorMessage());
  }

  /** Create SSE progress event */
  private String createProgressEvent(AnalysisEntity entity) {
    return String.format(
        "{\"type\":\"progress\",\"progress\":%d,\"step\":\"%s\",\"status\":\"%s\",\"estimatedTimeRemaining\":\"%s\"}",
        entity.getCurrentProgress(),
        entity.getCurrentStep(),
        entity.getStatus().getValue(),
        entity.getEstimatedTimeRemaining());
  }

  /** Create SSE completion event */
  private String createCompletedEvent() {
    return "{\"type\":\"completed\",\"message\":\"Analysis completed successfully\"}";
  }

  /** Create SSE error event */
  private String createErrorEvent(String errorMessage) {
    return String.format(
        "{\"type\":\"error\",\"message\":\"%s\"}", errorMessage.replace("\"", "\\\""));
  }

  /** Calculate estimated time remaining based on progress */
  private String calculateEstimatedTime(int progress) {
    if (progress >= 100) return "Completed";
    if (progress >= 75) return "30 seconds";
    if (progress >= 50) return "2 minutes";
    if (progress >= 25) return "3 minutes";
    return "4-5 minutes";
  }
}
