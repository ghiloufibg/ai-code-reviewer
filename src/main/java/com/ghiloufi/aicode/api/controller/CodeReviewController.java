package com.ghiloufi.aicode.api.controller;

import com.ghiloufi.aicode.api.model.*;
import com.ghiloufi.aicode.config.ApplicationConfig;
import com.ghiloufi.aicode.orchestrator.CodeReviewOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Code Review API endpoints. Temporary implementation to maintain compatibility
 * while migrating to new API structure.
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
