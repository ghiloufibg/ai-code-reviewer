package com.ghiloufi.security.mapper;

public final class SpotBugsSeverityMapper {

  private SpotBugsSeverityMapper() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static String mapPriority(final int spotBugsPriority) {
    return switch (spotBugsPriority) {
      case 1 -> "HIGH";
      case 2 -> "MEDIUM";
      default -> "LOW";
    };
  }
}
