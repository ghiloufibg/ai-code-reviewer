package com.ghiloufi.security.service;

import com.ghiloufi.security.model.DifferentialSecurityAnalysisRequest;
import com.ghiloufi.security.model.DifferentialSecurityAnalysisResponse;
import com.ghiloufi.security.model.SecurityAnalysisRequest;
import com.ghiloufi.security.model.SecurityAnalysisResponse;
import com.ghiloufi.security.model.SecurityFinding;
import com.ghiloufi.security.model.SecurityVerdict;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DifferentialSecurityAnalyzer {

  private static final Logger logger = LoggerFactory.getLogger(DifferentialSecurityAnalyzer.class);
  private static final int LINE_TOLERANCE = 5;

  private final SecurityAnalysisOrchestrator orchestrator;

  public DifferentialSecurityAnalyzer(final SecurityAnalysisOrchestrator orchestrator) {
    this.orchestrator = orchestrator;
  }

  public DifferentialSecurityAnalysisResponse analyzeDiff(
      final DifferentialSecurityAnalysisRequest request) {
    final long startTime = System.currentTimeMillis();

    logger.info(
        "Starting differential analysis for file: {} (language: {})",
        request.filename(),
        request.language());

    final SecurityAnalysisResponse oldAnalysis =
        orchestrator.analyze(
            new SecurityAnalysisRequest(request.oldCode(), request.language(), request.filename()));

    final SecurityAnalysisResponse newAnalysis =
        orchestrator.analyze(
            new SecurityAnalysisRequest(request.newCode(), request.language(), request.filename()));

    final List<SecurityFinding> oldFindings = oldAnalysis.findings();
    final List<SecurityFinding> newFindings = newAnalysis.findings();

    final List<SecurityFinding> introducedFindings = new ArrayList<>();
    final List<SecurityFinding> fixedFindings = new ArrayList<>();
    final List<SecurityFinding> existingFindings = new ArrayList<>();

    for (final SecurityFinding newFinding : newFindings) {
      if (!isSimilarFindingIn(newFinding, oldFindings)) {
        introducedFindings.add(newFinding);
      } else {
        existingFindings.add(newFinding);
      }
    }

    for (final SecurityFinding oldFinding : oldFindings) {
      if (!isSimilarFindingIn(oldFinding, newFindings)) {
        fixedFindings.add(oldFinding);
      }
    }

    final SecurityVerdict verdict = determineVerdict(introducedFindings, fixedFindings);

    final long analysisTimeMs = System.currentTimeMillis() - startTime;

    logger.info(
        "Differential analysis complete: verdict={}, new={}, fixed={}, existing={}, time={}ms",
        verdict,
        introducedFindings.size(),
        fixedFindings.size(),
        existingFindings.size(),
        analysisTimeMs);

    return new DifferentialSecurityAnalysisResponse(
        introducedFindings,
        fixedFindings,
        existingFindings,
        verdict,
        newAnalysis.tool(),
        newAnalysis.toolVersion(),
        analysisTimeMs);
  }

  private boolean isSimilarFindingIn(
      final SecurityFinding finding, final List<SecurityFinding> findings) {
    return findings.stream().anyMatch(other -> areSimilarFindings(finding, other));
  }

  private boolean areSimilarFindings(
      final SecurityFinding finding1, final SecurityFinding finding2) {
    final boolean typeMatch = finding1.type().equals(finding2.type());
    final boolean messageMatch =
        normalizeMessage(finding1.message()).equals(normalizeMessage(finding2.message()));
    final boolean lineClose = Math.abs(finding1.line() - finding2.line()) <= LINE_TOLERANCE;

    return typeMatch && messageMatch && lineClose;
  }

  private String normalizeMessage(final String message) {
    return message
        .replaceAll("line \\d+", "line X")
        .replaceAll("\\d+", "N")
        .replaceAll("\\s+", " ")
        .trim()
        .toLowerCase();
  }

  private SecurityVerdict determineVerdict(
      final List<SecurityFinding> newFindings, final List<SecurityFinding> fixedFindings) {
    if (!newFindings.isEmpty()) {
      return SecurityVerdict.UNSAFE;
    }
    if (!fixedFindings.isEmpty()) {
      return SecurityVerdict.IMPROVED;
    }
    return SecurityVerdict.SAFE;
  }
}
