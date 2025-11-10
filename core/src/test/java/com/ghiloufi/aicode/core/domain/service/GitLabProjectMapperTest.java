package com.ghiloufi.aicode.core.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.RepositoryInfo;
import org.gitlab4j.api.models.Namespace;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.Visibility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GitLabProjectMapper")
final class GitLabProjectMapperTest {

  private GitLabProjectMapper mapper;

  @BeforeEach
  final void setUp() {
    mapper = new GitLabProjectMapper();
  }

  @Nested
  @DisplayName("when mapping project to repository info")
  final class ToRepositoryInfo {

    @Test
    @DisplayName("should_map_all_fields_correctly")
    final void should_map_all_fields_correctly() {
      final Project project = createFullProject();

      final RepositoryInfo result = mapper.toRepositoryInfo(project);

      assertThat(result.fullName()).isEqualTo("test-org/test-project");
      assertThat(result.name()).isEqualTo("test-project");
      assertThat(result.owner()).isEqualTo("test-org");
      assertThat(result.description()).isEqualTo("Test description");
      assertThat(result.language()).isEqualTo("Java");
      assertThat(result.hasIssues()).isTrue();
      assertThat(result.hasPullRequests()).isTrue();
      assertThat(result.defaultBranch()).isEqualTo("main");
      assertThat(result.id()).isEqualTo(12345L);
      assertThat(result.isPrivate()).isFalse();
      assertThat(result.htmlUrl()).isEqualTo("https://gitlab.com/test-org/test-project");
    }

    @Test
    @DisplayName("should_use_empty_namespace_when_null")
    final void should_use_empty_namespace_when_null() {
      final Project project = createFullProject();
      project.setNamespace(null);

      final RepositoryInfo result = mapper.toRepositoryInfo(project);

      assertThat(result.owner()).isEmpty();
    }

    @Test
    @DisplayName("should_use_empty_description_when_null")
    final void should_use_empty_description_when_null() {
      final Project project = createFullProject();
      project.setDescription(null);

      final RepositoryInfo result = mapper.toRepositoryInfo(project);

      assertThat(result.description()).isEmpty();
    }

    @Test
    @DisplayName("should_use_false_for_issues_enabled_when_null")
    final void should_use_false_for_issues_enabled_when_null() {
      final Project project = createFullProject();
      project.setIssuesEnabled(null);

      final RepositoryInfo result = mapper.toRepositoryInfo(project);

      assertThat(result.hasIssues()).isFalse();
    }

    @Test
    @DisplayName("should_use_false_for_merge_requests_enabled_when_null")
    final void should_use_false_for_merge_requests_enabled_when_null() {
      final Project project = createFullProject();
      project.setMergeRequestsEnabled(null);

      final RepositoryInfo result = mapper.toRepositoryInfo(project);

      assertThat(result.hasPullRequests()).isFalse();
    }

    @Test
    @DisplayName("should_use_main_as_default_branch_when_null")
    final void should_use_main_as_default_branch_when_null() {
      final Project project = createFullProject();
      project.setDefaultBranch(null);

      final RepositoryInfo result = mapper.toRepositoryInfo(project);

      assertThat(result.defaultBranch()).isEqualTo("main");
    }

    @Test
    @DisplayName("should_use_zero_for_project_id_when_null")
    final void should_use_zero_for_project_id_when_null() {
      final Project project = createFullProject();
      project.setId(null);

      final RepositoryInfo result = mapper.toRepositoryInfo(project);

      assertThat(result.id()).isZero();
    }

    @Test
    @DisplayName("should_mark_as_private_when_visibility_is_null")
    final void should_mark_as_private_when_visibility_is_null() {
      final Project project = createFullProject();
      project.setVisibility(null);

      final RepositoryInfo result = mapper.toRepositoryInfo(project);

      assertThat(result.isPrivate()).isTrue();
    }

    @Test
    @DisplayName("should_mark_as_private_when_visibility_is_private")
    final void should_mark_as_private_when_visibility_is_private() {
      final Project project = createFullProject();
      project.setVisibility(Visibility.PRIVATE);

      final RepositoryInfo result = mapper.toRepositoryInfo(project);

      assertThat(result.isPrivate()).isTrue();
    }

    @Test
    @DisplayName("should_mark_as_private_when_visibility_is_internal")
    final void should_mark_as_private_when_visibility_is_internal() {
      final Project project = createFullProject();
      project.setVisibility(Visibility.INTERNAL);

      final RepositoryInfo result = mapper.toRepositoryInfo(project);

      assertThat(result.isPrivate()).isTrue();
    }

    @Test
    @DisplayName("should_mark_as_public_when_visibility_is_public")
    final void should_mark_as_public_when_visibility_is_public() {
      final Project project = createFullProject();
      project.setVisibility(Visibility.PUBLIC);

      final RepositoryInfo result = mapper.toRepositoryInfo(project);

      assertThat(result.isPrivate()).isFalse();
    }

    @Test
    @DisplayName("should_use_empty_web_url_when_null")
    final void should_use_empty_web_url_when_null() {
      final Project project = createFullProject();
      project.setWebUrl(null);

      final RepositoryInfo result = mapper.toRepositoryInfo(project);

      assertThat(result.htmlUrl()).isEmpty();
    }

    private Project createFullProject() {
      final Project project = new Project();
      project.setPathWithNamespace("test-org/test-project");
      project.setName("test-project");

      final Namespace namespace = new Namespace();
      namespace.setPath("test-org");
      project.setNamespace(namespace);

      project.setDescription("Test description");
      project.setIssuesEnabled(true);
      project.setMergeRequestsEnabled(true);
      project.setDefaultBranch("main");
      project.setId(12345L);
      project.setVisibility(Visibility.PUBLIC);
      project.setWebUrl("https://gitlab.com/test-org/test-project");

      return project;
    }
  }
}
