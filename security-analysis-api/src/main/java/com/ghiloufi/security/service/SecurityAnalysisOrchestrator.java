package com.ghiloufi.security.service;

import com.ghiloufi.security.exception.UnsupportedLanguageException;
import com.ghiloufi.security.model.SecurityAnalysisRequest;
import com.ghiloufi.security.model.SecurityAnalysisResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SecurityAnalysisOrchestrator {

  private static final Logger logger = LoggerFactory.getLogger(SecurityAnalysisOrchestrator.class);

  private static final String SUPPORTED_LANGUAGE = "java";

  private final SpotBugsAnalyzer spotBugsAnalyzer;
  private final MultiToolSecurityOrchestrator multiToolOrchestrator;

  public SecurityAnalysisOrchestrator(
      final SpotBugsAnalyzer spotBugsAnalyzer,
      @Autowired(required = false) final MultiToolSecurityOrchestrator multiToolOrchestrator) {
    this.spotBugsAnalyzer = spotBugsAnalyzer;
    this.multiToolOrchestrator = multiToolOrchestrator;

    if (multiToolOrchestrator != null) {
      logger.info("Multi-tool security analysis is ENABLED");
    } else {
      logger.info("Multi-tool security analysis is DISABLED, using SpotBugs only");
    }
  }

  public SecurityAnalysisResponse analyze(final SecurityAnalysisRequest request) {
    logger.info(
        "Starting security analysis for file: {}, language: {}",
        request.filename(),
        request.language());

    validateLanguage(request.language());

    final SecurityAnalysisResponse response;

    if (multiToolOrchestrator != null) {
      response = multiToolOrchestrator.analyzeWithAllTools(request);
    } else {
      response = spotBugsAnalyzer.analyze(request.code(), request.language(), request.filename());
    }

    logger.info(
        "Security analysis completed for file: {}. Found {} findings in {}ms",
        request.filename(),
        response.findings().size(),
        response.analysisTimeMs());

    return response;
  }

  private void validateLanguage(final String language) {
    if (!SUPPORTED_LANGUAGE.equalsIgnoreCase(language)) {
      logger.warn("Unsupported language requested: {}", language);
      throw new UnsupportedLanguageException(language);
    }
  }
}
