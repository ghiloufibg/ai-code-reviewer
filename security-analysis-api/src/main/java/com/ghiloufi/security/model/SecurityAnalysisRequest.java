package com.ghiloufi.security.model;

import jakarta.validation.constraints.NotBlank;

public record SecurityAnalysisRequest(
    @NotBlank(message = "Code must not be blank") String code,
    @NotBlank(message = "Language must not be blank") String language,
    @NotBlank(message = "Filename must not be blank") String filename) {}
