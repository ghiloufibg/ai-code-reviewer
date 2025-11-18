package com.ghiloufi.security.adapter;

import com.ghiloufi.security.model.SecurityFinding;
import java.util.List;

public interface SecurityToolAdapter {

  String getToolName();

  String getToolVersion();

  List<SecurityFinding> analyze(String code, String filename);

  default int getTimeoutSeconds() {
    return 30;
  }

  default boolean isAvailable() {
    try {
      getToolVersion();
      return true;
    } catch (final Exception e) {
      return false;
    }
  }
}
