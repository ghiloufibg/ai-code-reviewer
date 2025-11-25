package com.ghiloufi.aicode.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class RepositoryPoliciesTest {

  @Test
  void should_create_empty_policies() {
    final var policies = RepositoryPolicies.empty();

    assertThat(policies.hasPolicies()).isFalse();
    assertThat(policies.allPolicies()).isEmpty();
    assertThat(policies.policyCount()).isEqualTo(0);
  }

  @Test
  void should_detect_when_has_policies() {
    final var contributing =
        new PolicyDocument(
            "CONTRIBUTING.md", "CONTRIBUTING.md", "content", PolicyType.CONTRIBUTING, false);
    final var policies = new RepositoryPolicies(contributing, null, null, null);

    assertThat(policies.hasPolicies()).isTrue();
    assertThat(policies.policyCount()).isEqualTo(1);
  }

  @Test
  void should_return_all_non_null_policies() {
    final var contributing =
        new PolicyDocument("CONTRIBUTING.md", "path1", "content1", PolicyType.CONTRIBUTING, false);
    final var security =
        new PolicyDocument("SECURITY.md", "path2", "content2", PolicyType.SECURITY, false);
    final var policies = new RepositoryPolicies(contributing, null, null, security);

    assertThat(policies.allPolicies()).hasSize(2);
    assertThat(policies.policyCount()).isEqualTo(2);
  }

  @Test
  void should_calculate_total_content_length() {
    final var doc1 = new PolicyDocument("name1", "path1", "12345", PolicyType.CONTRIBUTING, false);
    final var doc2 = new PolicyDocument("name2", "path2", "1234567890", PolicyType.SECURITY, false);
    final var policies = new RepositoryPolicies(doc1, null, null, doc2);

    assertThat(policies.totalContentLength()).isEqualTo(15);
  }

  @Test
  void should_preserve_policy_order_in_all_policies() {
    final var contributing =
        new PolicyDocument("CONTRIBUTING.md", "path1", "c1", PolicyType.CONTRIBUTING, false);
    final var codeOfConduct =
        new PolicyDocument("CODE_OF_CONDUCT.md", "path2", "c2", PolicyType.CODE_OF_CONDUCT, false);
    final var prTemplate =
        new PolicyDocument(
            "PULL_REQUEST_TEMPLATE.md", "path3", "c3", PolicyType.PR_TEMPLATE, false);
    final var security =
        new PolicyDocument("SECURITY.md", "path4", "c4", PolicyType.SECURITY, false);
    final var policies = new RepositoryPolicies(contributing, codeOfConduct, prTemplate, security);

    final var allPolicies = policies.allPolicies();

    assertThat(allPolicies).hasSize(4);
    assertThat(allPolicies.get(0).type()).isEqualTo(PolicyType.CONTRIBUTING);
    assertThat(allPolicies.get(1).type()).isEqualTo(PolicyType.CODE_OF_CONDUCT);
    assertThat(allPolicies.get(2).type()).isEqualTo(PolicyType.PR_TEMPLATE);
    assertThat(allPolicies.get(3).type()).isEqualTo(PolicyType.SECURITY);
  }
}
