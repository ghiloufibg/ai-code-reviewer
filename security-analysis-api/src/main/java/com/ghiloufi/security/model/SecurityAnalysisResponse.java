package com.ghiloufi.security.model;

import java.util.List;

public record SecurityAnalysisResponse(
    List<SecurityFinding> findings, String tool, String toolVersion, long analysisTimeMs) {}
