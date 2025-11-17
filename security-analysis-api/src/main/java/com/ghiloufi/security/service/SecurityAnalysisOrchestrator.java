package com.ghiloufi.security.service;

import com.ghiloufi.security.exception.UnsupportedLanguageException;
import com.ghiloufi.security.model.SecurityAnalysisRequest;
import com.ghiloufi.security.model.SecurityAnalysisResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;
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
  private final Cache<String, SecurityAnalysisResponse> analysisCache;

  public SecurityAnalysisOrchestrator(
      final SpotBugsAnalyzer spotBugsAnalyzer,
      @Autowired(required = false) final MultiToolSecurityOrchestrator multiToolOrchestrator) {
    this.spotBugsAnalyzer = spotBugsAnalyzer;
    this.multiToolOrchestrator = multiToolOrchestrator;

    this.analysisCache =
        Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .recordStats()
            .build();

    if (multiToolOrchestrator != null) {
      logger.info("Multi-tool security analysis is ENABLED with result caching");
    } else {
      logger.info("Multi-tool security analysis is DISABLED, using SpotBugs only with caching");
    }
  }

  public SecurityAnalysisResponse analyze(final SecurityAnalysisRequest request) {
    logger.info(
        "Starting security analysis for file: {}, language: {}",
        request.filename(),
        request.language());

    validateLanguage(request.language());

    final String cacheKey =
        generateCacheKey(request.code(), request.language(), request.filename());
    final SecurityAnalysisResponse cachedResponse = analysisCache.getIfPresent(cacheKey);

    if (cachedResponse != null) {
      logger.info(
          "Cache HIT for file: {} (saved {}ms)",
          request.filename(),
          cachedResponse.analysisTimeMs());
      return cachedResponse;
    }

    logger.debug("Cache MISS for file: {}, performing analysis", request.filename());

    final SecurityAnalysisResponse response;

    if (multiToolOrchestrator != null) {
      response = multiToolOrchestrator.analyzeWithAllTools(request);
    } else {
      response = spotBugsAnalyzer.analyze(request.code(), request.language(), request.filename());
    }

    analysisCache.put(cacheKey, response);

    logger.info(
        "Security analysis completed for file: {}. Found {} findings in {}ms (cached for reuse)",
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

  private String generateCacheKey(final String code, final String language, final String filename) {
    try {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final String combined = code + language + filename;
      final byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));

      final StringBuilder hexString = new StringBuilder();
      for (final byte b : hash) {
        final String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (final NoSuchAlgorithmException e) {
      logger.error("SHA-256 algorithm not available, falling back to simple concatenation", e);
      return code.hashCode() + "_" + language + "_" + filename;
    }
  }
}
