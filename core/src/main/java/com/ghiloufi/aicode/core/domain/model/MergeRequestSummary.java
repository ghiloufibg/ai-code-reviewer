package com.ghiloufi.aicode.core.domain.model;

import java.time.Instant;

public record MergeRequestSummary(
    int iid,
    String title,
    String description,
    String state,
    String author,
    String sourceBranch,
    String targetBranch,
    Instant createdAt,
    Instant updatedAt,
    String webUrl) {}
