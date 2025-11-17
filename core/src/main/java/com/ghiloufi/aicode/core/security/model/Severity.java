package com.ghiloufi.aicode.core.security.model;

public enum Severity {
  CRITICAL(10.0),
  HIGH(7.0),
  MEDIUM(4.0),
  LOW(1.0),
  INFO(0.1);

  private final double weight;

  Severity(final double weight) {
    this.weight = weight;
  }

  public double getWeight() {
    return weight;
  }
}
