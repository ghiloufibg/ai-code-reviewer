package com.ghiloufi.aicode.application.query;

import java.util.UUID;

/**
 * Query to get review status.
 */
public record ReviewStatusQuery(UUID reviewId) {
    public ReviewStatusQuery {
        if (reviewId == null) {
            throw new IllegalArgumentException("Review ID cannot be null");
        }
    }
}