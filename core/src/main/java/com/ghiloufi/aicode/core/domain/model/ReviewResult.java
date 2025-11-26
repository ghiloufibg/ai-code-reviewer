package com.ghiloufi.aicode.core.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = ReviewResult.Builder.class)
@Getter
public final class ReviewResult {

  private final String summary;
  private final List<Issue> issues;

  @JsonProperty("non_blocking_notes")
  private final List<Note> nonBlockingNotes;

  private final String llmProvider;
  private final String llmModel;
  private final String rawLlmResponse;

  private ReviewResult(final Builder builder) {
    this.summary = builder.summary;
    this.issues = builder.issues != null ? List.copyOf(builder.issues) : List.of();
    this.nonBlockingNotes =
        builder.nonBlockingNotes != null ? List.copyOf(builder.nonBlockingNotes) : List.of();
    this.llmProvider = builder.llmProvider;
    this.llmModel = builder.llmModel;
    this.rawLlmResponse = builder.rawLlmResponse;
  }

  public ReviewResult withLlmMetadata(final String provider, final String model) {
    return builder()
        .summary(this.summary)
        .issues(this.issues)
        .nonBlockingNotes(this.nonBlockingNotes)
        .llmProvider(provider)
        .llmModel(model)
        .rawLlmResponse(this.rawLlmResponse)
        .build();
  }

  public ReviewResult withRawLlmResponse(final String rawResponse) {
    return builder()
        .summary(this.summary)
        .issues(this.issues)
        .nonBlockingNotes(this.nonBlockingNotes)
        .llmProvider(this.llmProvider)
        .llmModel(this.llmModel)
        .rawLlmResponse(rawResponse)
        .build();
  }

  public ReviewResult withIssues(final List<Issue> newIssues) {
    return builder()
        .summary(this.summary)
        .issues(newIssues)
        .nonBlockingNotes(this.nonBlockingNotes)
        .llmProvider(this.llmProvider)
        .llmModel(this.llmModel)
        .rawLlmResponse(this.rawLlmResponse)
        .build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static ReviewResult empty() {
    return builder().build();
  }

  @JsonPOJOBuilder(withPrefix = "")
  public static final class Builder {
    private String summary;
    private List<Issue> issues = new ArrayList<>();

    @JsonProperty("non_blocking_notes")
    private List<Note> nonBlockingNotes = new ArrayList<>();

    private String llmProvider;
    private String llmModel;
    private String rawLlmResponse;

    private Builder() {}

    public Builder summary(final String summary) {
      this.summary = summary;
      return this;
    }

    public Builder issues(final List<Issue> issues) {
      this.issues = issues != null ? new ArrayList<>(issues) : new ArrayList<>();
      return this;
    }

    public Builder nonBlockingNotes(final List<Note> notes) {
      this.nonBlockingNotes = notes != null ? new ArrayList<>(notes) : new ArrayList<>();
      return this;
    }

    public Builder llmProvider(final String llmProvider) {
      this.llmProvider = llmProvider;
      return this;
    }

    public Builder llmModel(final String llmModel) {
      this.llmModel = llmModel;
      return this;
    }

    public Builder rawLlmResponse(final String rawLlmResponse) {
      this.rawLlmResponse = rawLlmResponse;
      return this;
    }

    public ReviewResult build() {
      return new ReviewResult(this);
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonDeserialize(builder = Issue.IssueBuilder.class)
  @Getter
  public static final class Issue {
    private final String file;

    @JsonProperty("start_line")
    private final int startLine;

    private final String severity;
    private final String title;
    private final String suggestion;
    private final Boolean inlineCommentPosted;
    private final String scmCommentId;
    private final String fallbackReason;
    private final String positionMetadata;
    private final Double confidenceScore;
    private final String confidenceExplanation;
    private final String suggestedFix;

    private Issue(final IssueBuilder builder) {
      this.file = builder.file;
      this.startLine = builder.startLine;
      this.severity = builder.severity;
      this.title = builder.title;
      this.suggestion = builder.suggestion;
      this.inlineCommentPosted = builder.inlineCommentPosted;
      this.scmCommentId = builder.scmCommentId;
      this.fallbackReason = builder.fallbackReason;
      this.positionMetadata = builder.positionMetadata;
      this.confidenceScore = builder.confidenceScore;
      this.confidenceExplanation = builder.confidenceExplanation;
      this.suggestedFix = builder.suggestedFix;
    }

    public boolean isHighConfidence() {
      return confidenceScore != null && confidenceScore >= 0.7;
    }

    public boolean hasFixSuggestion() {
      return suggestedFix != null && !suggestedFix.isBlank();
    }

    public Issue withConfidenceScore(final Double score) {
      return issueBuilder()
          .file(this.file)
          .startLine(this.startLine)
          .severity(this.severity)
          .title(this.title)
          .suggestion(this.suggestion)
          .inlineCommentPosted(this.inlineCommentPosted)
          .scmCommentId(this.scmCommentId)
          .fallbackReason(this.fallbackReason)
          .positionMetadata(this.positionMetadata)
          .confidenceScore(score)
          .confidenceExplanation(this.confidenceExplanation)
          .suggestedFix(this.suggestedFix)
          .build();
    }

    public Issue withConfidenceExplanation(final String explanation) {
      return issueBuilder()
          .file(this.file)
          .startLine(this.startLine)
          .severity(this.severity)
          .title(this.title)
          .suggestion(this.suggestion)
          .inlineCommentPosted(this.inlineCommentPosted)
          .scmCommentId(this.scmCommentId)
          .fallbackReason(this.fallbackReason)
          .positionMetadata(this.positionMetadata)
          .confidenceScore(this.confidenceScore)
          .confidenceExplanation(explanation)
          .suggestedFix(this.suggestedFix)
          .build();
    }

    public Issue withSuggestedFix(final String fix) {
      return issueBuilder()
          .file(this.file)
          .startLine(this.startLine)
          .severity(this.severity)
          .title(this.title)
          .suggestion(this.suggestion)
          .inlineCommentPosted(this.inlineCommentPosted)
          .scmCommentId(this.scmCommentId)
          .fallbackReason(this.fallbackReason)
          .positionMetadata(this.positionMetadata)
          .confidenceScore(this.confidenceScore)
          .confidenceExplanation(this.confidenceExplanation)
          .suggestedFix(fix)
          .build();
    }

    public Issue withInlineCommentPosted(
        final Boolean posted, final String commentId, final String reason, final String metadata) {
      return issueBuilder()
          .file(this.file)
          .startLine(this.startLine)
          .severity(this.severity)
          .title(this.title)
          .suggestion(this.suggestion)
          .inlineCommentPosted(posted)
          .scmCommentId(commentId)
          .fallbackReason(reason)
          .positionMetadata(metadata)
          .confidenceScore(this.confidenceScore)
          .confidenceExplanation(this.confidenceExplanation)
          .suggestedFix(this.suggestedFix)
          .build();
    }

    public static IssueBuilder issueBuilder() {
      return new IssueBuilder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class IssueBuilder {
      private String file;

      @JsonProperty("start_line")
      private int startLine;

      private String severity;
      private String title;
      private String suggestion;
      private Boolean inlineCommentPosted;
      private String scmCommentId;
      private String fallbackReason;
      private String positionMetadata;
      private Double confidenceScore;
      private String confidenceExplanation;
      private String suggestedFix;

      private IssueBuilder() {}

      public IssueBuilder file(final String file) {
        this.file = file;
        return this;
      }

      public IssueBuilder startLine(final int startLine) {
        this.startLine = startLine;
        return this;
      }

      public IssueBuilder severity(final String severity) {
        this.severity = severity;
        return this;
      }

      public IssueBuilder title(final String title) {
        this.title = title;
        return this;
      }

      public IssueBuilder suggestion(final String suggestion) {
        this.suggestion = suggestion;
        return this;
      }

      public IssueBuilder inlineCommentPosted(final Boolean inlineCommentPosted) {
        this.inlineCommentPosted = inlineCommentPosted;
        return this;
      }

      public IssueBuilder scmCommentId(final String scmCommentId) {
        this.scmCommentId = scmCommentId;
        return this;
      }

      public IssueBuilder fallbackReason(final String fallbackReason) {
        this.fallbackReason = fallbackReason;
        return this;
      }

      public IssueBuilder positionMetadata(final String positionMetadata) {
        this.positionMetadata = positionMetadata;
        return this;
      }

      public IssueBuilder confidenceScore(final Double confidenceScore) {
        this.confidenceScore = confidenceScore;
        return this;
      }

      public IssueBuilder confidenceExplanation(final String confidenceExplanation) {
        this.confidenceExplanation = confidenceExplanation;
        return this;
      }

      public IssueBuilder suggestedFix(final String suggestedFix) {
        this.suggestedFix = suggestedFix;
        return this;
      }

      public Issue build() {
        return new Issue(this);
      }
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonDeserialize(builder = Note.NoteBuilder.class)
  @Getter
  public static final class Note {
    private final String file;
    private final int line;
    private final String note;
    private final Boolean inlineCommentPosted;
    private final String scmCommentId;
    private final String fallbackReason;
    private final String positionMetadata;

    private Note(final NoteBuilder builder) {
      this.file = builder.file;
      this.line = builder.line;
      this.note = builder.note;
      this.inlineCommentPosted = builder.inlineCommentPosted;
      this.scmCommentId = builder.scmCommentId;
      this.fallbackReason = builder.fallbackReason;
      this.positionMetadata = builder.positionMetadata;
    }

    public Note withInlineCommentPosted(
        final Boolean posted, final String commentId, final String reason, final String metadata) {
      return noteBuilder()
          .file(this.file)
          .line(this.line)
          .note(this.note)
          .inlineCommentPosted(posted)
          .scmCommentId(commentId)
          .fallbackReason(reason)
          .positionMetadata(metadata)
          .build();
    }

    public static NoteBuilder noteBuilder() {
      return new NoteBuilder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class NoteBuilder {
      private String file;
      private int line;
      private String note;
      private Boolean inlineCommentPosted;
      private String scmCommentId;
      private String fallbackReason;
      private String positionMetadata;

      private NoteBuilder() {}

      public NoteBuilder file(final String file) {
        this.file = file;
        return this;
      }

      public NoteBuilder line(final int line) {
        this.line = line;
        return this;
      }

      public NoteBuilder note(final String note) {
        this.note = note;
        return this;
      }

      public NoteBuilder inlineCommentPosted(final Boolean inlineCommentPosted) {
        this.inlineCommentPosted = inlineCommentPosted;
        return this;
      }

      public NoteBuilder scmCommentId(final String scmCommentId) {
        this.scmCommentId = scmCommentId;
        return this;
      }

      public NoteBuilder fallbackReason(final String fallbackReason) {
        this.fallbackReason = fallbackReason;
        return this;
      }

      public NoteBuilder positionMetadata(final String positionMetadata) {
        this.positionMetadata = positionMetadata;
        return this;
      }

      public Note build() {
        return new Note(this);
      }
    }
  }
}
