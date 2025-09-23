package com.ghiloufi.aicode.application.port.output;

import com.ghiloufi.aicode.domain.entity.AnalysisResult;
import com.ghiloufi.aicode.domain.entity.DiffAnalysis;
import com.ghiloufi.aicode.domain.value.ReviewConfiguration;
import reactor.core.publisher.Mono;

/**
 * Port for LLM analysis operations.
 *
 * <p>This port defines the contract for LLM-based code analysis,
 * abstracting the specific LLM implementation details.
 */
public interface LlmAnalysisPort {

    /**
     * Analyzes the given diff using LLM and returns analysis results.
     *
     * @param diffAnalysis the diff to analyze
     * @param config review configuration
     * @return analysis result containing issues and recommendations
     */
    Mono<AnalysisResult> analyzeDiff(DiffAnalysis diffAnalysis, ReviewConfiguration config);
}