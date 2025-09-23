package com.ghiloufi.aicode.domain.value;

import com.ghiloufi.aicode.shared.exception.DomainException;

/**
 * Value object representing review configuration parameters.
 *
 * <p>Contains all the configuration needed to conduct a code review,
 * including LLM settings, analysis parameters, and quality thresholds.
 */
public record ReviewConfiguration(
    String model,
    String ollamaHost,
    int timeoutSeconds,
    int maxLinesPerChunk,
    int contextLines,
    String javaVersion,
    String buildSystem,
    boolean enableStaticAnalysis,
    boolean enableLlmAnalysis,
    boolean enableSecurityAnalysis
) {

    public ReviewConfiguration {
        if (model == null || model.trim().isEmpty()) {
            throw new DomainException("Model cannot be null or empty");
        }
        if (ollamaHost == null || ollamaHost.trim().isEmpty()) {
            throw new DomainException("Ollama host cannot be null or empty");
        }
        if (timeoutSeconds <= 0) {
            throw new DomainException("Timeout seconds must be positive");
        }
        if (maxLinesPerChunk <= 0) {
            throw new DomainException("Max lines per chunk must be positive");
        }
        if (contextLines < 0) {
            throw new DomainException("Context lines cannot be negative");
        }
        if (javaVersion == null || javaVersion.trim().isEmpty()) {
            throw new DomainException("Java version cannot be null or empty");
        }
        if (buildSystem == null || buildSystem.trim().isEmpty()) {
            throw new DomainException("Build system cannot be null or empty");
        }

        model = model.trim();
        ollamaHost = ollamaHost.trim();
        javaVersion = javaVersion.trim();
        buildSystem = buildSystem.trim();
    }

    /**
     * Gets the display name for this configuration.
     */
    public String getDisplayName() {
        return String.format("%s on %s (timeout: %ds, chunk: %d lines)",
                           model, ollamaHost, timeoutSeconds, maxLinesPerChunk);
    }

    /**
     * Creates a default configuration.
     */
    public static ReviewConfiguration defaultConfiguration() {
        return new ReviewConfiguration(
            "qwen2.5-coder:7b-instruct",
            "http://localhost:11434",
            45,
            1500,
            5,
            "17",
            "maven",
            true,
            true,
            false
        );
    }

    /**
     * Creates a configuration for security-focused review.
     */
    public static ReviewConfiguration securityFocused() {
        return new ReviewConfiguration(
            "qwen2.5-coder:7b-instruct",
            "http://localhost:11434",
            60,
            1000,
            5,
            "17",
            "maven",
            true,
            true,
            true
        );
    }

    /**
     * Creates a fast configuration for quick reviews.
     */
    public static ReviewConfiguration fastReview() {
        return new ReviewConfiguration(
            "qwen2.5-coder:7b-instruct",
            "http://localhost:11434",
            30,
            2000,
            3,
            "17",
            "maven",
            true,
            false,
            false
        );
    }

    /**
     * Checks if any analysis is enabled.
     */
    public boolean hasAnalysisEnabled() {
        return enableStaticAnalysis || enableLlmAnalysis || enableSecurityAnalysis;
    }
}