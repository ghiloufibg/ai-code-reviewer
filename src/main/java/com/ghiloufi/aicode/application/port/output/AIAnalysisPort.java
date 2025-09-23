package com.ghiloufi.aicode.application.port.output;

import com.ghiloufi.aicode.domain.entity.AnalysisResult;
import com.ghiloufi.aicode.domain.entity.DiffAnalysis;
import com.ghiloufi.aicode.domain.value.ReviewConfiguration;
import reactor.core.publisher.Mono;

/**
 * Output port for AI-powered code analysis.
 *
 * <p>Abstracts the interaction with LLM services for
 * intelligent code review and analysis.
 */
public interface AIAnalysisPort {

    /**
     * Analyzes code changes using AI/LLM.
     */
    Mono<AnalysisResult> analyze(DiffAnalysis diffAnalysis, ReviewConfiguration configuration);

    /**
     * Checks if AI analysis is available.
     */
    boolean isAvailable();

    /**
     * Gets the current AI model information.
     */
    String getModelInfo();
}