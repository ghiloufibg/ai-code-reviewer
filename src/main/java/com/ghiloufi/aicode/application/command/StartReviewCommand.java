package com.ghiloufi.aicode.application.command;

import com.ghiloufi.aicode.domain.value.RepositoryInfo;
import com.ghiloufi.aicode.domain.value.ReviewConfiguration;

/**
 * Command to start a new code review.
 */
public record StartReviewCommand(
    RepositoryInfo repositoryInfo,
    ReviewConfiguration configuration
) {
    public StartReviewCommand {
        if (repositoryInfo == null) {
            throw new IllegalArgumentException("Repository info cannot be null");
        }
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
    }
}