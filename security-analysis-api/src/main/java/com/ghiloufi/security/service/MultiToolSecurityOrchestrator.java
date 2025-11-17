package com.ghiloufi.security.service;

import com.ghiloufi.security.adapter.SecurityToolAdapter;
import com.ghiloufi.security.model.SecurityAnalysisRequest;
import com.ghiloufi.security.model.SecurityAnalysisResponse;
import com.ghiloufi.security.model.SecurityFinding;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
    name = "security.analysis.multi-tool.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class MultiToolSecurityOrchestrator {

  private static final Logger logger = LoggerFactory.getLogger(MultiToolSecurityOrchestrator.class);

  private final SpotBugsAnalyzer spotBugsAnalyzer;
  private final List<SecurityToolAdapter> additionalAdapters;
  private final ExecutorService executorService;

  public MultiToolSecurityOrchestrator(
      final SpotBugsAnalyzer spotBugsAnalyzer,
      final List<SecurityToolAdapter> securityToolAdapters) {
    this.spotBugsAnalyzer = spotBugsAnalyzer;
    this.additionalAdapters = securityToolAdapters;
    this.executorService = Executors.newFixedThreadPool(4);

    logger.info(
        "Multi-Tool Security Orchestrator initialized with {} tools",
        1 + additionalAdapters.size());
    logAvailableTools();
  }

  public SecurityAnalysisResponse analyzeWithAllTools(final SecurityAnalysisRequest request) {
    final long startTime = System.currentTimeMillis();

    logger.info(
        "Starting multi-tool security analysis for file: {}, language: {}",
        request.filename(),
        request.language());

    final List<Future<ToolAnalysisResult>> futures = new ArrayList<>();

    futures.add(
        CompletableFuture.supplyAsync(
                () -> {
                  try {
                    final SecurityAnalysisResponse response =
                        spotBugsAnalyzer.analyze(
                            request.code(), request.language(), request.filename());
                    return new ToolAnalysisResult("SpotBugs", response.findings());
                  } catch (final Exception e) {
                    logger.error("SpotBugs analysis failed", e);
                    return new ToolAnalysisResult("SpotBugs", List.of());
                  }
                },
                executorService)
            .orTimeout(30, TimeUnit.SECONDS)
            .exceptionally(
                ex -> {
                  if (ex.getCause() instanceof TimeoutException) {
                    logger.warn("SpotBugs timed out after 30 seconds");
                  } else {
                    logger.error("SpotBugs analysis failed", ex);
                  }
                  return new ToolAnalysisResult("SpotBugs", List.of());
                })
            .toCompletableFuture());

    for (final SecurityToolAdapter adapter : additionalAdapters) {
      if (!adapter.isAvailable()) {
        logger.warn("Tool {} is not available, skipping", adapter.getToolName());
        continue;
      }

      final int timeoutSeconds = adapter.getTimeoutSeconds();
      futures.add(
          CompletableFuture.supplyAsync(
                  () -> {
                    try {
                      final List<SecurityFinding> findings =
                          adapter.analyze(request.code(), request.filename());
                      return new ToolAnalysisResult(adapter.getToolName(), findings);
                    } catch (final Exception e) {
                      logger.error("Analysis failed for tool: {}", adapter.getToolName(), e);
                      return new ToolAnalysisResult(adapter.getToolName(), List.of());
                    }
                  },
                  executorService)
              .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
              .exceptionally(
                  ex -> {
                    if (ex.getCause() instanceof TimeoutException) {
                      logger.warn(
                          "{} timed out after {} seconds", adapter.getToolName(), timeoutSeconds);
                    } else {
                      logger.error("Analysis failed for tool: {}", adapter.getToolName(), ex);
                    }
                    return new ToolAnalysisResult(adapter.getToolName(), List.of());
                  })
              .toCompletableFuture());
    }

    final List<SecurityFinding> allFindings = new ArrayList<>();
    final List<String> successfulTools = new ArrayList<>();

    for (final Future<ToolAnalysisResult> future : futures) {
      try {
        final ToolAnalysisResult result = future.get();
        allFindings.addAll(result.findings());
        successfulTools.add(result.toolName());

        logger.debug(
            "Tool {} completed with {} findings", result.toolName(), result.findings().size());

      } catch (final Exception e) {
        logger.debug("Tool analysis interrupted or failed", e);
      }
    }

    final List<SecurityFinding> deduplicated = deduplicateFindings(allFindings);
    final List<SecurityFinding> sorted = sortBySeverity(deduplicated);

    final long analysisTime = System.currentTimeMillis() - startTime;

    final String toolNames = String.join(", ", successfulTools);
    final String toolVersions = getToolVersions(successfulTools);

    logger.info(
        "Multi-tool analysis completed. Tools: {}, Findings: {} (deduplicated from {}), Time: {}ms",
        toolNames,
        sorted.size(),
        allFindings.size(),
        analysisTime);

    return new SecurityAnalysisResponse(sorted, toolNames, toolVersions, analysisTime);
  }

  private List<SecurityFinding> deduplicateFindings(final List<SecurityFinding> findings) {
    return findings.stream()
        .collect(
            Collectors.toMap(
                finding -> finding.line() + ":" + finding.type() + ":" + finding.message(),
                Function.identity(),
                (f1, f2) -> {
                  final int severity1 = getSeverityScore(f1.severity());
                  final int severity2 = getSeverityScore(f2.severity());
                  return severity1 > severity2 ? f1 : f2;
                }))
        .values()
        .stream()
        .toList();
  }

  private List<SecurityFinding> sortBySeverity(final List<SecurityFinding> findings) {
    return findings.stream()
        .sorted(Comparator.comparingInt(f -> getSeverityScore(f.severity())))
        .toList();
  }

  private int getSeverityScore(final String severity) {
    return switch (severity.toUpperCase()) {
      case "CRITICAL" -> 0;
      case "HIGH" -> 1;
      case "MEDIUM" -> 2;
      case "LOW" -> 3;
      default -> 999;
    };
  }

  private String getToolVersions(final List<String> toolNames) {
    final List<String> versions = new ArrayList<>();

    if (toolNames.contains("SpotBugs")) {
      versions.add("SpotBugs:4.8.3");
    }

    for (final SecurityToolAdapter adapter : additionalAdapters) {
      if (toolNames.contains(adapter.getToolName())) {
        versions.add(adapter.getToolName() + ":" + adapter.getToolVersion());
      }
    }

    return String.join(", ", versions);
  }

  private void logAvailableTools() {
    logger.info("Available security tools:");
    logger.info("  - SpotBugs 4.8.3 (always enabled)");

    for (final SecurityToolAdapter adapter : additionalAdapters) {
      final String status = adapter.isAvailable() ? "available" : "NOT AVAILABLE";
      logger.info("  - {} {} ({})", adapter.getToolName(), adapter.getToolVersion(), status);
    }
  }

  private record ToolAnalysisResult(String toolName, List<SecurityFinding> findings) {}
}
