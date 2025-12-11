package com.ghiloufi.aicode.gateway.webhook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record WebhookRequest(
    @NotBlank(message = "Provider is required")
        @Pattern(regexp = "^(github|gitlab)$", message = "Provider must be 'github' or 'gitlab'")
        String provider,
    @NotBlank(message = "Repository ID is required") String repositoryId,
    @NotNull(message = "Change request ID is required")
        @Positive(message = "Change request ID must be positive")
        Integer changeRequestId,
    String triggerSource) {}
