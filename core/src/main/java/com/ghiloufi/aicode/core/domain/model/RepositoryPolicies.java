package com.ghiloufi.aicode.core.domain.model;

import java.util.List;

public record RepositoryPolicies(List<PolicyDocument> documents) {

  public RepositoryPolicies {
    documents = documents != null ? List.copyOf(documents) : List.of();
  }

  public boolean hasPolicies() {
    return !documents.isEmpty();
  }

  public List<PolicyDocument> allPolicies() {
    return documents;
  }

  public int policyCount() {
    return documents.size();
  }

  public int totalContentLength() {
    return documents.stream().mapToInt(PolicyDocument::contentLength).sum();
  }

  public static RepositoryPolicies empty() {
    return new RepositoryPolicies(List.of());
  }
}
