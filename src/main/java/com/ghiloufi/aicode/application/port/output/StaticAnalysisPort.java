package com.ghiloufi.aicode.application.port.output;

import com.ghiloufi.aicode.domain.entity.AnalysisResult;
import com.ghiloufi.aicode.domain.entity.DiffAnalysis;
import reactor.core.publisher.Flux;

/**
 * Output port for static analysis operations.
 *
 * <p>Abstracts the execution of static analysis tools
 * (Checkstyle, PMD, SpotBugs, etc.) on code changes.
 */
public interface StaticAnalysisPort {

    /**
     * Runs static analysis on the provided diff.
     */
    Flux<AnalysisResult> analyze(DiffAnalysis diffAnalysis);

    /**
     * Checks if static analysis tools are available.
     */
    boolean isAvailable();

    /**
     * Gets the available analysis tools.
     */
    Flux<String> getAvailableTools();
}