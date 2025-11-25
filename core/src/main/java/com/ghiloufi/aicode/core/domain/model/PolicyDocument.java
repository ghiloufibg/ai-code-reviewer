package com.ghiloufi.aicode.core.domain.model;

import java.util.Objects;

public record PolicyDocument(
    String name, String path, String content, PolicyType type, boolean truncated) {

  public PolicyDocument {
    Objects.requireNonNull(name, "Name cannot be null");
    Objects.requireNonNull(path, "Path cannot be null");
    Objects.requireNonNull(type, "Type cannot be null");

    if (name.isBlank()) {
      throw new IllegalArgumentException("Name cannot be blank");
    }

    if (path.isBlank()) {
      throw new IllegalArgumentException("Path cannot be blank");
    }
  }

  public boolean hasContent() {
    return content != null && !content.isBlank();
  }

  public int contentLength() {
    return content != null ? content.length() : 0;
  }
}
