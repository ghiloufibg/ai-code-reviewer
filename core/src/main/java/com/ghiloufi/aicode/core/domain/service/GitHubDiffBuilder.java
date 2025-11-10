package com.ghiloufi.aicode.core.domain.service;

import java.util.List;
import java.util.Optional;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.springframework.stereotype.Service;

@Service
public final class GitHubDiffBuilder {

  public final String buildRawDiff(final List<GHPullRequestFileDetail> files) {
    final StringBuilder rawDiff = new StringBuilder();

    for (final GHPullRequestFileDetail file : files) {
      appendDiffHeader(rawDiff, file);
      appendDiffContent(rawDiff, file);
    }

    return rawDiff.toString();
  }

  private void appendDiffHeader(final StringBuilder builder, final GHPullRequestFileDetail file) {
    builder
        .append("diff --git a/")
        .append(file.getFilename())
        .append(" b/")
        .append(file.getFilename())
        .append("\n");
    builder.append("--- a/").append(extractPreviousFilename(file)).append("\n");
    builder.append("+++ b/").append(file.getFilename()).append("\n");
  }

  private void appendDiffContent(final StringBuilder builder, final GHPullRequestFileDetail file) {
    if (file.getPatch() != null) {
      builder.append(file.getPatch()).append("\n");
    }
  }

  private String extractPreviousFilename(final GHPullRequestFileDetail file) {
    return Optional.ofNullable(file.getPreviousFilename()).orElse(file.getFilename());
  }
}
