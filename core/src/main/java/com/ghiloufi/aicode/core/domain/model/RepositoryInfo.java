package com.ghiloufi.aicode.core.domain.model;

public record RepositoryInfo(
    String fullName,
    String name,
    String owner,
    String description,
    String language,
    boolean hasIssues,
    boolean hasPullRequests,
    String defaultBranch,
    long id,
    boolean isPrivate,
    String htmlUrl) {}
