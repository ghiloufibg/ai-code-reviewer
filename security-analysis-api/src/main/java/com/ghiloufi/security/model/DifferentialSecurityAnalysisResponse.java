package com.ghiloufi.security.model;

import java.util.List;

public record DifferentialSecurityAnalysisResponse(
    List<SecurityFinding> newFindings,
    List<SecurityFinding> fixedFindings,
    List<SecurityFinding> existingFindings,
    SecurityVerdict verdict,
    String tool,
    String toolVersion,
    long analysisTimeMs) {}
