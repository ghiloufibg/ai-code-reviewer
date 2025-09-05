package com.ghiloufi.aicode.core;

import com.ghiloufi.aicode.model.ReviewResult;
import java.util.*;

public class IssueAggregator {
  public ReviewResult merge(List<ReviewResult> parts) {
    ReviewResult all = new ReviewResult();
    StringBuilder sb = new StringBuilder();
    for (var r : parts) {
      if (r.summary != null && !r.summary.isBlank()) sb.append(r.summary).append(" ");
      all.issues.addAll(r.issues);
      all.non_blocking_notes.addAll(r.non_blocking_notes);
    }
    all.summary = sb.toString().trim();
    return all;
  }
}
