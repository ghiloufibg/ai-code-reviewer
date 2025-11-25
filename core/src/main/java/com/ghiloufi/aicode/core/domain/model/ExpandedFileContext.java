package com.ghiloufi.aicode.core.domain.model;

import java.util.Objects;

public record ExpandedFileContext(
    String filePath, String content, int lineCount, boolean truncated) {

  public ExpandedFileContext {
    Objects.requireNonNull(filePath, "File path cannot be null");

    if (filePath.isBlank()) {
      throw new IllegalArgumentException("File path cannot be blank");
    }

    if (lineCount < 0) {
      throw new IllegalArgumentException("Line count cannot be negative");
    }
  }

  public boolean hasContent() {
    return content != null && !content.isBlank();
  }

  public int contentLength() {
    return content != null ? content.length() : 0;
  }

  public static ExpandedFileContext empty(final String filePath) {
    return new ExpandedFileContext(filePath, "", 0, false);
  }

  public static ExpandedFileContext truncated(
      final String filePath, final String content, final int originalLineCount) {
    return new ExpandedFileContext(filePath, content, originalLineCount, true);
  }

  public static ExpandedFileContext of(final String filePath, final String content) {
    final int lineCount = content != null ? content.split("\n").length : 0;
    return new ExpandedFileContext(filePath, content, lineCount, false);
  }
}
