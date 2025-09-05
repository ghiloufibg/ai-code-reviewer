package com.ghiloufi.aicode.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewResult {
  public String summary;
  public List<Issue> issues = new ArrayList<>();
  public List<Note> non_blocking_notes = new ArrayList<>();

  public static ReviewResult fromJson(String json) {
    try {
      return new ObjectMapper().readValue(json, ReviewResult.class);
    } catch (Exception e) {
      int i = json.indexOf('{');
      int j = json.lastIndexOf('}');
      if (i >= 0 && j > i) {
        String sub = json.substring(i, j + 1);
        try {
          return new ObjectMapper().readValue(sub, ReviewResult.class);
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      }
      throw new RuntimeException(e);
    }
  }

  public String toJson() {
    try {
      return new ObjectMapper().writeValueAsString(this);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public String toPrettyJson() {
    try {
      return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static class Issue {
    public String file;
    public int start_line;
    public int end_line;
    public String severity;
    public String rule_id;
    public String title;
    public String rationale;
    public String suggestion;
    public java.util.List<String> references;
    public int hunk_index;
  }

  public static class Note {
    public String file;
    public int line;
    public String note;
  }
}
