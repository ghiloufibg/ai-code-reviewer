package com.ghiloufi.aicode.api.controller;

import com.ghiloufi.aicode.api.AnalysisApi;
import com.ghiloufi.aicode.api.model.*;
import com.ghiloufi.aicode.service.analysis.AnalysisService;
import com.ghiloufi.aicode.service.analysis.FileStorageService;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.Part;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST Controller implementing Analysis API for code analysis operations. Provides endpoints for
 * file upload, progress tracking, and results retrieval.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class AnalysisController implements AnalysisApi {

  private final AnalysisService analysisService;
  private final FileStorageService fileStorageService;

  /** Upload file for analysis POST /analysis/upload - Generated interface implementation */
  @Override
  public Mono<ResponseEntity<AnalysisResponse>> _uploadFileForAnalysis(
      @RequestPart(value = "file", required = true) Flux<Part> file,
      @Valid @RequestPart(value = "analysisOptions", required = false) String analysisOptions,
      ServerWebExchange exchange) {

    log.info("Received file upload request");

    // Generate unique analysis ID
    String analysisId = java.util.UUID.randomUUID().toString();

    // Check if file is provided
    if (file == null) {
      log.warn("No file uploaded for analysis");
      return Mono.just(
          ResponseEntity.badRequest().body(createErrorResponse("No file provided for analysis")));
    }

    // Parse analysis options
    final AnalysisOptions finalOptions = parseAnalysisOptions(analysisOptions);

    // Convert single Flux<Part> to List<Flux<Part>> for FileStorageService compatibility
    List<Flux<Part>> fileList = List.of(file);

    // Store uploaded file and extract content
    return fileStorageService
        .storeAndExtractFiles(analysisId, fileList)
        .flatMap(
            fileContents -> {
              if (fileContents.isEmpty()) {
                log.warn("No file uploaded for analysis");
                return Mono.just(
                    ResponseEntity.badRequest()
                        .body(createErrorResponse("No file provided for analysis")));
              }

              List<String> fileNames = List.copyOf(fileContents.keySet());

              // Validate file types and sizes
              if (!validateFiles(fileNames)) {
                return Mono.just(
                    ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid file types or sizes")));
              }

              // Start analysis - FULLY REACTIVE!
              return analysisService.startAnalysis(fileNames, finalOptions, analysisId)
                  .map(response -> {
                    log.info("Analysis started successfully: {}", response.getId());
                    return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
                  })
                  .onErrorResume(e -> {
                    log.error("Failed to start analysis", e);
                    return Mono.just(
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(createErrorResponse("Failed to start analysis: " + e.getMessage())));
                  });
            })
        .onErrorResume(
            error -> {
              log.error("Error processing file upload", error);
              return Mono.just(
                  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(createErrorResponse("File upload processing failed")));
            });
  }

  /** Get analysis status GET /analysis/{id}/status */
  @Override
  public Mono<ResponseEntity<AnalysisStatus>> _getAnalysisStatus(
      String id, ServerWebExchange exchange) {

    log.debug("Getting analysis status for: {}", id);

    return Mono.fromSupplier(
        () -> {
          Optional<AnalysisStatus> statusOpt = analysisService.getAnalysisStatus(id);

          if (statusOpt.isEmpty()) {
            log.warn("Analysis not found: {}", id);
            return ResponseEntity.notFound().build();
          }

          return ResponseEntity.ok(statusOpt.get());
        });
  }

  /** Stream real-time analysis progress (Server-Sent Events) GET /analysis/{id}/stream */
  @Override
  public Mono<ResponseEntity<String>> _streamAnalysisProgress(
      String id, ServerWebExchange exchange) {

    log.info("Starting SSE stream for analysis: {}", id);

    return Mono.fromSupplier(
        () -> {
          try {
            // Set SSE headers
            exchange.getResponse().getHeaders().setContentType(MediaType.TEXT_EVENT_STREAM);
            exchange.getResponse().getHeaders().set("Cache-Control", "no-cache");
            exchange.getResponse().getHeaders().set("Connection", "keep-alive");

            // Get progress stream
            Flux<String> progressStream = analysisService.getProgressStream(id);

            // Format as SSE events
            Flux<String> sseStream =
                progressStream
                    .map(data -> "data: " + data + "\n\n")
                    .delayElements(Duration.ofMillis(500)) // Throttle events
                    .doOnSubscribe(subscription -> log.debug("SSE stream subscribed for: {}", id))
                    .doOnComplete(() -> log.debug("SSE stream completed for: {}", id))
                    .doOnError(
                        error -> log.error("SSE stream error for {}: {}", id, error.getMessage()));

            // Convert to single string response (WebFlux SSE handling)
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body("SSE stream initiated");

          } catch (Exception e) {
            log.error("Failed to create SSE stream for analysis: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to create progress stream");
          }
        });
  }

  /** Get analysis results GET /analysis/{id}/results */
  @Override
  public Mono<ResponseEntity<AnalysisResults>> _getAnalysisResults(
      String id, String format, ServerWebExchange exchange) {

    log.debug("Getting analysis results for: {} in format: {}", id, format);

    return Mono.fromSupplier(
        () -> {
          Optional<AnalysisResults> resultsOpt = analysisService.getAnalysisResults(id);

          if (resultsOpt.isEmpty()) {
            // Check if analysis exists but not completed
            Optional<AnalysisStatus> statusOpt = analysisService.getAnalysisStatus(id);
            if (statusOpt.isPresent()) {
              AnalysisStatus status = statusOpt.get();
              if (status.getStatus() != AnalysisStatus.StatusEnum.COMPLETED) {
                log.warn("Analysis {} not completed yet, status: {}", id, status.getStatus());
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
              }
            }

            log.warn("Analysis results not found: {}", id);
            return ResponseEntity.notFound().build();
          }

          AnalysisResults results = resultsOpt.get();
          log.info(
              "Returning analysis results for: {} ({} issues, {} suggestions)",
              id,
              results.getIssues().size(),
              results.getSuggestions().size());

          return ResponseEntity.ok(results);
        });
  }

  /** Cancel analysis DELETE /analysis/{id} */
  @Override
  public Mono<ResponseEntity<Void>> _cancelAnalysis(String id, ServerWebExchange exchange) {

    log.info("Cancelling analysis: {}", id);

    return Mono.fromSupplier(
        () -> {
          boolean cancelled = analysisService.cancelAnalysis(id);

          if (!cancelled) {
            // Check if analysis exists
            Optional<AnalysisStatus> statusOpt = analysisService.getAnalysisStatus(id);
            if (statusOpt.isEmpty()) {
              log.warn("Analysis not found for cancellation: {}", id);
              return ResponseEntity.notFound().build();
            }

            // Analysis exists but cannot be cancelled
            log.warn("Cannot cancel analysis {} in current state", id);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
          }

          log.info("Analysis cancelled successfully: {}", id);
          return ResponseEntity.noContent().build();
        });
  }

  /** Validate uploaded files (basic validation) */
  private boolean validateFiles(List<String> fileNames) {
    if (fileNames.size() > 50) {
      log.warn("Too many files uploaded: {}", fileNames.size());
      return false;
    }

    for (String fileName : fileNames) {
      if (fileName == null || fileName.trim().isEmpty()) {
        log.warn("Invalid file name detected");
        return false;
      }

      // Check for supported file types
      if (!isSupportedFileType(fileName)) {
        log.warn("Unsupported file type: {}", fileName);
        return false;
      }
    }

    return true;
  }

  /** Check if file type is supported for analysis */
  private boolean isSupportedFileType(String fileName) {
    String lowercaseName = fileName.toLowerCase();
    return lowercaseName.endsWith(".java")
        || lowercaseName.endsWith(".js")
        || lowercaseName.endsWith(".ts")
        || lowercaseName.endsWith(".py")
        || lowercaseName.endsWith(".cpp")
        || lowercaseName.endsWith(".c")
        || lowercaseName.endsWith(".h")
        || lowercaseName.endsWith(".cs")
        || lowercaseName.endsWith(".go")
        || lowercaseName.endsWith(".rs")
        || lowercaseName.endsWith(".php")
        || lowercaseName.endsWith(".rb")
        || lowercaseName.endsWith(".swift")
        || lowercaseName.endsWith(".kt")
        || lowercaseName.endsWith(".scala")
        || lowercaseName.endsWith(".xml")
        || lowercaseName.endsWith(".json")
        || lowercaseName.endsWith(".yml")
        || lowercaseName.endsWith(".yaml")
        || lowercaseName.endsWith(".properties");
  }

  /** Create error response (helper method) */
  private AnalysisResponse createErrorResponse(String message) {
    AnalysisResponse response = new AnalysisResponse();
    response.setStatus(AnalysisResponse.StatusEnum.FAILED);
    response.setMessage(message);
    return response;
  }

  /** Parse analysis options from JSON string */
  private AnalysisOptions parseAnalysisOptions(String analysisOptions) {
    if (analysisOptions != null && !analysisOptions.trim().isEmpty()) {
      try {
        // TODO: Parse JSON string to AnalysisOptions object
        // For now, we'll return null which means default options
        log.debug("Analysis options provided: {}", analysisOptions);
        return null;
      } catch (Exception e) {
        log.warn("Invalid analysis options JSON: {}", e.getMessage());
        return null;
      }
    }
    return null;
  }
}
