package com.ghiloufi.aicode.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record DiffBundle(UnifiedDiff diff, String unifiedDiff) {

  public Map<String, Object> projectConfig() {
    return Map.of();
  }

  public Map<String, Object> testStatus() {
    return Map.of();
  }

  public List<UnifiedDiff> splitByMaxLines(int maxLines) {
    List<UnifiedDiff> chunks = new ArrayList<>();
    UnifiedDiff current = new UnifiedDiff();
    int count = 0;
    for (var fd : diff.files) {
      for (var h : fd.hunks) {
        int lines = h.lines.size();
        if (count + lines > maxLines && count > 0) {
          chunks.add(current);
          current = new UnifiedDiff();
          count = 0;
        }
        var fd2 = fd.shallowCopyWithSingleHunk(h);
        current.files.add(fd2);
        count += lines;
      }
    }
    if (!current.files.isEmpty()) chunks.add(current);
    return chunks;
  }
}
