package com.ghiloufi.security.model;

import jakarta.validation.constraints.NotBlank;

public record DifferentialSecurityAnalysisRequest(
    @NotBlank(message = "Old code must not be blank") String oldCode,
    @NotBlank(message = "New code must not be blank") String newCode,
    @NotBlank(message = "Language must not be blank") String language,
    @NotBlank(message = "Filename must not be blank") String filename) {}
