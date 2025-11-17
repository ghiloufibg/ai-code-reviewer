package com.ghiloufi.security.controller;

import com.ghiloufi.security.model.DifferentialSecurityAnalysisRequest;
import com.ghiloufi.security.model.DifferentialSecurityAnalysisResponse;
import com.ghiloufi.security.model.SecurityAnalysisRequest;
import com.ghiloufi.security.model.SecurityAnalysisResponse;
import com.ghiloufi.security.service.DifferentialSecurityAnalyzer;
import com.ghiloufi.security.service.SecurityAnalysisOrchestrator;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/security")
public class SecurityAnalysisController {

  private static final Logger logger = LoggerFactory.getLogger(SecurityAnalysisController.class);

  private final SecurityAnalysisOrchestrator orchestrator;
  private final DifferentialSecurityAnalyzer differentialAnalyzer;

  public SecurityAnalysisController(
      final SecurityAnalysisOrchestrator orchestrator,
      final DifferentialSecurityAnalyzer differentialAnalyzer) {
    this.orchestrator = orchestrator;
    this.differentialAnalyzer = differentialAnalyzer;
  }

  @PostMapping("/analyze-diff")
  public ResponseEntity<DifferentialSecurityAnalysisResponse> analyzeDiff(
      @Valid @RequestBody final DifferentialSecurityAnalysisRequest request) {
    logger.info(
        "Received differential security analysis request for file: {} (language: {})",
        request.filename(),
        request.language());

    final DifferentialSecurityAnalysisResponse response = differentialAnalyzer.analyzeDiff(request);

    logger.info(
        "Returning differential analysis response: verdict={}, new={}, fixed={}, existing={} for file: {}",
        response.verdict(),
        response.newFindings().size(),
        response.fixedFindings().size(),
        response.existingFindings().size(),
        request.filename());

    return ResponseEntity.ok(response);
  }

  @Deprecated(forRemoval = true)
  @PostMapping("/analyze")
  public ResponseEntity<SecurityAnalysisResponse> analyze(
      @Valid @RequestBody final SecurityAnalysisRequest request) {
    logger.warn(
        "DEPRECATED: /api/security/analyze endpoint is deprecated. Please use /api/security/analyze-diff instead.");
    logger.info("Received security analysis request for file: {}", request.filename());

    final SecurityAnalysisResponse response = orchestrator.analyze(request);

    logger.info(
        "Returning security analysis response with {} findings for file: {}",
        response.findings().size(),
        request.filename());

    return ResponseEntity.ok(response);
  }
}
