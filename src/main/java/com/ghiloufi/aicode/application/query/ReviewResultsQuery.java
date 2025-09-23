package com.ghiloufi.aicode.application.query;

import java.util.UUID;

/**
 * Query to get review results.
 */
public record ReviewResultsQuery(UUID reviewId) {
    public ReviewResultsQuery {
        if (reviewId == null) {
            throw new IllegalArgumentException("Review ID cannot be null");
        }
    }
}