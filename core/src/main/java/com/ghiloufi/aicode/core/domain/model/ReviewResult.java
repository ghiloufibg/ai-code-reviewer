package com.ghiloufi.aicode.core.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewResult {

  public String summary;
  public List<Issue> issues = new ArrayList<>();
  public List<Note> non_blocking_notes = new ArrayList<>();
  public String llmProvider;
  public String llmModel;
  public String rawLlmResponse;

  public static class Issue {
    public String file;
    public int start_line;
    public String severity;
    public String title;
    public String suggestion;

    public Boolean inlineCommentPosted;
    public String scmCommentId;
    public String fallbackReason;
    public String positionMetadata;

    public Double confidenceScore;
    public String confidenceExplanation;
    public String suggestedFix;
    public String fixDiff;

    public boolean isHighConfidence() {
      return confidenceScore != null && confidenceScore >= 0.7;
    }

    public boolean hasFixSuggestion() {
      return suggestedFix != null && !suggestedFix.isBlank();
    }
  }

  public static class Note {
    public String file;
    public int line;
    public String note;

    public Boolean inlineCommentPosted;
    public String scmCommentId;
    public String fallbackReason;
    public String positionMetadata;
  }
}
