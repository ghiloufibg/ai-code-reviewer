package com.ghiloufi.aicode.domain;

import java.util.*;

public class UnifiedDiff {
  public List<FileDiff> files = new ArrayList<>();

  public String toUnifiedString() {
    StringBuilder sb = new StringBuilder();
    for (var f : files) {
      sb.append("--- a/").append(f.oldPath).append(" ");
      sb.append("+++ b/").append(f.newPath).append(" ");
      for (var h : f.hunks) {
        sb.append("@@ -")
            .append(h.oldStart)
            .append(",")
            .append(h.oldCount)
            .append(" +")
            .append(h.newStart)
            .append(",")
            .append(h.newCount)
            .append(" @@ ");
        for (var line : h.lines) sb.append(line).append(' ');
      }
    }
    return sb.toString();
  }
}
