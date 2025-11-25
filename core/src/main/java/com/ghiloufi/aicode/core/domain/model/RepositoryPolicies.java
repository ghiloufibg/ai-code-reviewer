package com.ghiloufi.aicode.core.domain.model;

import java.util.ArrayList;
import java.util.List;

public record RepositoryPolicies(
    PolicyDocument contributingGuide,
    PolicyDocument codeOfConduct,
    PolicyDocument prTemplate,
    PolicyDocument securityPolicy) {

  public boolean hasPolicies() {
    return contributingGuide != null
        || codeOfConduct != null
        || prTemplate != null
        || securityPolicy != null;
  }

  public List<PolicyDocument> allPolicies() {
    final List<PolicyDocument> all = new ArrayList<>();
    if (contributingGuide != null) {
      all.add(contributingGuide);
    }
    if (codeOfConduct != null) {
      all.add(codeOfConduct);
    }
    if (prTemplate != null) {
      all.add(prTemplate);
    }
    if (securityPolicy != null) {
      all.add(securityPolicy);
    }
    return List.copyOf(all);
  }

  public int policyCount() {
    return allPolicies().size();
  }

  public int totalContentLength() {
    return allPolicies().stream().mapToInt(PolicyDocument::contentLength).sum();
  }

  public static RepositoryPolicies empty() {
    return new RepositoryPolicies(null, null, null, null);
  }
}
