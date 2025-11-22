package com.ghiloufi.aicode.core.domain.model;

import lombok.Getter;

@Getter
public enum MatchReason {
  FILE_REFERENCE(0.80, "Referenced in diff content"),
  SIBLING_FILE(0.75, "Same directory, related name"),
  GIT_COCHANGE_HIGH(0.85, "Frequently changed together (>70%)"),
  GIT_COCHANGE_MEDIUM(0.70, "Sometimes changed together (40-70%)"),
  SAME_PACKAGE(0.75, "Same package/directory"),
  RELATED_LAYER(0.70, "Related architectural layer"),
  TEST_COUNTERPART(0.85, "Test file for implementation"),
  PARENT_PACKAGE(0.60, "Parent package/directory"),
  DIRECT_IMPORT(0.95, "Direct import/dependency"),
  TYPE_REFERENCE(0.85, "Type reference in code"),
  METHOD_CALL(0.90, "Method/function call"),
  RAG_SEMANTIC(0.75, "Semantic similarity via embeddings");

  private final double baseConfidence;
  private final String description;

  MatchReason(final double baseConfidence, final String description) {
    this.baseConfidence = baseConfidence;
    this.description = description;
  }
}
