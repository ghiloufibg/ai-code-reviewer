package com.ghiloufi.security.model;

public record SecurityFinding(
    String type,
    String severity,
    int line,
    String message,
    String recommendation,
    String cweId,
    String owaspCategory,
    Double confidence) {

  public SecurityFinding(
      final String type,
      final String severity,
      final int line,
      final String message,
      final String recommendation,
      final String cweId,
      final String owaspCategory) {
    this(type, severity, line, message, recommendation, cweId, owaspCategory, null);
  }
}
