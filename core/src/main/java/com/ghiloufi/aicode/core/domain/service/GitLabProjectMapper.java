package com.ghiloufi.aicode.core.domain.service;

import com.ghiloufi.aicode.core.domain.model.RepositoryInfo;
import java.util.Optional;
import org.gitlab4j.api.models.Namespace;
import org.gitlab4j.api.models.Project;
import org.springframework.stereotype.Service;

@Service
public final class GitLabProjectMapper {

  public RepositoryInfo toRepositoryInfo(final Project project) {
    return new RepositoryInfo(
        project.getPathWithNamespace(),
        project.getName(),
        extractNamespacePath(project),
        extractDescription(project.getDescription()),
        "Java",
        extractIssuesEnabled(project),
        extractMergeRequestsEnabled(project),
        extractDefaultBranch(project),
        extractProjectId(project),
        isPrivateRepository(project),
        extractWebUrl(project));
  }

  private String extractNamespacePath(final Project project) {
    return Optional.ofNullable(project.getNamespace()).map(Namespace::getPath).orElse("");
  }

  private String extractDescription(final String description) {
    return Optional.ofNullable(description).orElse("");
  }

  private boolean extractIssuesEnabled(final Project project) {
    return Optional.ofNullable(project.getIssuesEnabled()).orElse(false);
  }

  private boolean extractMergeRequestsEnabled(final Project project) {
    return Optional.ofNullable(project.getMergeRequestsEnabled()).orElse(false);
  }

  private String extractDefaultBranch(final Project project) {
    return Optional.ofNullable(project.getDefaultBranch()).orElse("main");
  }

  private long extractProjectId(final Project project) {
    return Optional.ofNullable(project.getId()).orElse(0L);
  }

  private boolean isPrivateRepository(final Project project) {
    return project.getVisibility() == null
        || !project.getVisibility().toString().equalsIgnoreCase("public");
  }

  private String extractWebUrl(final Project project) {
    return Optional.ofNullable(project.getWebUrl()).orElse("");
  }
}
