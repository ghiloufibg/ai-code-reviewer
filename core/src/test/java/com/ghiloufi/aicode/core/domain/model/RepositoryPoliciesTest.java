package com.ghiloufi.aicode.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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
        new PolicyDocument("CONTRIBUTING.md", "CONTRIBUTING.md", "content", false);
    final var policies = new RepositoryPolicies(List.of(contributing));

    assertThat(policies.hasPolicies()).isTrue();
    assertThat(policies.policyCount()).isEqualTo(1);
  }

  @Test
  void should_return_all_policies() {
    final var contributing = new PolicyDocument("CONTRIBUTING.md", "path1", "content1", false);
    final var security = new PolicyDocument("SECURITY.md", "path2", "content2", false);
    final var policies = new RepositoryPolicies(List.of(contributing, security));

    assertThat(policies.allPolicies()).hasSize(2);
    assertThat(policies.policyCount()).isEqualTo(2);
  }

  @Test
  void should_calculate_total_content_length() {
    final var doc1 = new PolicyDocument("name1", "path1", "12345", false);
    final var doc2 = new PolicyDocument("name2", "path2", "1234567890", false);
    final var policies = new RepositoryPolicies(List.of(doc1, doc2));

    assertThat(policies.totalContentLength()).isEqualTo(15);
  }

  @Test
  void should_preserve_policy_order() {
    final var contributing = new PolicyDocument("CONTRIBUTING.md", "path1", "c1", false);
    final var security = new PolicyDocument("SECURITY.md", "path2", "c2", false);
    final var prTemplate = new PolicyDocument("PULL_REQUEST_TEMPLATE.md", "path3", "c3", false);
    final var policies = new RepositoryPolicies(List.of(contributing, security, prTemplate));

    final var allPolicies = policies.allPolicies();

    assertThat(allPolicies).hasSize(3);
    assertThat(allPolicies.get(0).name()).isEqualTo("CONTRIBUTING.md");
    assertThat(allPolicies.get(1).name()).isEqualTo("SECURITY.md");
    assertThat(allPolicies.get(2).name()).isEqualTo("PULL_REQUEST_TEMPLATE.md");
  }

  @Test
  void should_handle_null_documents_list() {
    final var policies = new RepositoryPolicies(null);

    assertThat(policies.hasPolicies()).isFalse();
    assertThat(policies.allPolicies()).isEmpty();
  }
}
