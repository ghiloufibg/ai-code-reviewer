package com.ghiloufi.aicode.domain;

import java.util.ArrayList;
import java.util.List;

public class FileDiff {
  public String oldPath;
  public String newPath;
  public List<Hunk> hunks = new ArrayList<>();

  public FileDiff shallowCopyWithSingleHunk(Hunk h) {
    FileDiff fd = new FileDiff();
    fd.oldPath = oldPath;
    fd.newPath = newPath;
    fd.hunks = new ArrayList<>();
    fd.hunks.add(h);
    return fd;
  }
}
