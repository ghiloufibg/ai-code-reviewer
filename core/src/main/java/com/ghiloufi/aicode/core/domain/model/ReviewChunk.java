package com.ghiloufi.aicode.core.domain.model;

public record ReviewChunk(ChunkType type, String content, String metadata) {

  public enum ChunkType {
    ANALYSIS,
    SUGGESTION,
    SECURITY,
    PERFORMANCE,
    COMMENTARY,
    ERROR
  }

  public static ReviewChunk of(final ChunkType type, final String content) {
    return new ReviewChunk(type, content, null);
  }
}
