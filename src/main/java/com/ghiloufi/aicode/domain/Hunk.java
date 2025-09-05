package com.ghiloufi.aicode.domain;

import java.util.ArrayList;
import java.util.List;

public class Hunk {
  public int oldStart;
  public int oldCount;
  public int newStart;
  public int newCount;
  public List<String> lines = new ArrayList<>();
}
