package com.ghiloufi.aicode.core.infrastructure.persistence.entity;

public enum MatchReasonEnum {
  FILE_REFERENCE,
  SIBLING_FILE,
  GIT_COCHANGE_HIGH,
  GIT_COCHANGE_MEDIUM,
  SAME_PACKAGE,
  RELATED_LAYER,
  TEST_COUNTERPART,
  PARENT_PACKAGE,
  DIRECT_IMPORT,
  TYPE_REFERENCE,
  METHOD_CALL,
  RAG_SEMANTIC
}
