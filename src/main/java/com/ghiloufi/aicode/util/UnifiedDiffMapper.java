package com.ghiloufi.aicode.util;

import com.ghiloufi.aicode.domain.UnifiedDiff;

public class UnifiedDiffMapper {
  private final UnifiedDiff ud;

  public UnifiedDiffMapper(UnifiedDiff ud) {
    this.ud = ud;
  }

  public int positionFor(String path, int newLine) {
    int pos = 0;
    for (var f : ud.files) {
      String p = f.newPath != null ? f.newPath : f.oldPath;
      if (!p.equals(path)) {
        for (var h : f.hunks) {
          pos += 1 + h.lines.size();
        }
        continue;
      }
      for (var h : f.hunks) {
        pos++;
        int curNew = h.newStart - 1;
        for (String l : h.lines) {
          pos++;
          if (l.startsWith("+") || l.startsWith(" ")) curNew++;
          if (curNew == newLine) return pos;
        }
      }
    }
    return -1;
  }
}
