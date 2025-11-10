package com.ghiloufi.aicode.core.domain.service;

import java.util.List;
import org.gitlab4j.api.models.Diff;
import org.springframework.stereotype.Service;

@Service
public final class GitLabDiffBuilder {

  public final String buildRawDiff(final List<Diff> diffs) {
    final StringBuilder rawDiff = new StringBuilder();

    for (final Diff diff : diffs) {
      appendDiffHeader(rawDiff, diff);
      appendDiffContent(rawDiff, diff);
    }

    return rawDiff.toString();
  }

  private void appendDiffHeader(final StringBuilder builder, final Diff diff) {
    builder
        .append("diff --git a/")
        .append(diff.getOldPath())
        .append(" b/")
        .append(diff.getNewPath())
        .append("\n");
    builder.append("--- a/").append(diff.getOldPath()).append("\n");
    builder.append("+++ b/").append(diff.getNewPath()).append("\n");
  }

  private void appendDiffContent(final StringBuilder builder, final Diff diff) {
    if (diff.getDiff() != null) {
      builder.append(diff.getDiff()).append("\n");
    }
  }
}
