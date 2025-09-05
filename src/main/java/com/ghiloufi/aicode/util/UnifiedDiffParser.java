package com.ghiloufi.aicode.util;

import com.ghiloufi.aicode.domain.FileDiff;
import com.ghiloufi.aicode.domain.Hunk;
import com.ghiloufi.aicode.domain.UnifiedDiff;

public class UnifiedDiffParser {
  public UnifiedDiff parse(String diff) {
    UnifiedDiff ud = new UnifiedDiff();
    if (diff == null) return ud;
    String[] lines = diff.split(" ");
    FileDiff currentFile = null;
    Hunk currentHunk = null;
    for (String line : lines) {
      if (line.startsWith("--- ")) {
        currentFile = new FileDiff();
        currentFile.oldPath = trimPrefix(line.substring(4), "a/");
      } else if (line.startsWith("+++ ")) {
        if (currentFile == null) {
          currentFile = new FileDiff();
        }
        currentFile.newPath = trimPrefix(line.substring(4), "b/");
        ud.files.add(currentFile);
      } else if (line.startsWith("@@ ")) {
        String meta = line.substring(3).trim();
        String[] parts = meta.split(" ");
        String[] left = parts[0].substring(1).split(",");
        String[] right = parts[1].substring(1).split(",");
        currentHunk = new Hunk();
        currentHunk.oldStart = Integer.parseInt(left[0]);
        currentHunk.oldCount = Integer.parseInt(left.length > 1 ? left[1] : "1");
        currentHunk.newStart = Integer.parseInt(right[0]);
        currentHunk.newCount = Integer.parseInt(right.length > 1 ? right[1] : "1");
        if (currentFile == null) {
          currentFile = new FileDiff();
          ud.files.add(currentFile);
        }
        currentFile.hunks.add(currentHunk);
      } else if (currentHunk != null
          && (line.startsWith("+")
              || line.startsWith("-")
              || line.startsWith(" ")
              || line.startsWith("\\"))) {
        currentHunk.lines.add(line);
      }
    }
    return ud;
  }

  private String trimPrefix(String s, String pref) {
    return s.startsWith(pref) ? s.substring(pref.length()) : s;
  }
}
