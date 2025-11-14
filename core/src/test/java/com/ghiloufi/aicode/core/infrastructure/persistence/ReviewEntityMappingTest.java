package com.ghiloufi.aicode.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.model.ReviewState;
import com.ghiloufi.aicode.core.infrastructure.persistence.entity.ReviewEntity;
import com.ghiloufi.aicode.core.infrastructure.persistence.entity.ReviewIssueEntity;
import com.ghiloufi.aicode.core.infrastructure.persistence.entity.ReviewNoteEntity;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Review Entity Mapping Tests - Unit Tests for Metadata Fields")
class ReviewEntityMappingTest {

  @Test
  @DisplayName("should map issue with inline comment metadata to entity")
  void should_map_issue_with_inline_metadata() {
    final ReviewResult.Issue issue = new ReviewResult.Issue();
    issue.file = "src/Main.java";
    issue.start_line = 42;
    issue.severity = "critical";
    issue.title = "Null pointer risk";
    issue.suggestion = "Add null check";
    issue.inlineCommentPosted = true;
    issue.scmCommentId = "discussion_123";
    issue.positionMetadata = "{\"base_sha\":\"abc\",\"head_sha\":\"def\"}";

    final ReviewIssueEntity entity =
        ReviewIssueEntity.builder()
            .filePath(issue.file)
            .startLine(issue.start_line)
            .severity(issue.severity)
            .title(issue.title)
            .suggestion(issue.suggestion)
            .inlineCommentPosted(issue.inlineCommentPosted != null && issue.inlineCommentPosted)
            .scmCommentId(issue.scmCommentId)
            .fallbackReason(issue.fallbackReason)
            .positionMetadata(issue.positionMetadata)
            .build();

    assertThat(entity.isInlineCommentPosted()).isTrue();
    assertThat(entity.getScmCommentId()).isEqualTo("discussion_123");
    assertThat(entity.getFallbackReason()).isNull();
    assertThat(entity.getPositionMetadata()).contains("base_sha");
  }

  @Test
  @DisplayName("should map issue with fallback metadata to entity")
  void should_map_issue_with_fallback_metadata() {
    final ReviewResult.Issue issue = new ReviewResult.Issue();
    issue.file = "src/Main.java";
    issue.start_line = 999;
    issue.severity = "major";
    issue.title = "Invalid line";
    issue.inlineCommentPosted = false;
    issue.scmCommentId = "note_456";
    issue.fallbackReason = "INVALID_LINE";

    final ReviewIssueEntity entity =
        ReviewIssueEntity.builder()
            .filePath(issue.file)
            .startLine(issue.start_line)
            .severity(issue.severity)
            .title(issue.title)
            .inlineCommentPosted(issue.inlineCommentPosted != null && issue.inlineCommentPosted)
            .scmCommentId(issue.scmCommentId)
            .fallbackReason(issue.fallbackReason)
            .positionMetadata(issue.positionMetadata)
            .build();

    assertThat(entity.isInlineCommentPosted()).isFalse();
    assertThat(entity.getScmCommentId()).isEqualTo("note_456");
    assertThat(entity.getFallbackReason()).isEqualTo("INVALID_LINE");
    assertThat(entity.getPositionMetadata()).isNull();
  }

  @Test
  @DisplayName("should map note with inline comment metadata to entity")
  void should_map_note_with_inline_metadata() {
    final ReviewResult.Note note = new ReviewResult.Note();
    note.file = "src/Utils.java";
    note.line = 15;
    note.note = "Consider using Optional";
    note.inlineCommentPosted = true;
    note.scmCommentId = "discussion_789";
    note.positionMetadata = "{\"new_line\":15}";

    final ReviewNoteEntity entity =
        ReviewNoteEntity.builder()
            .filePath(note.file)
            .lineNumber(note.line)
            .note(note.note)
            .inlineCommentPosted(note.inlineCommentPosted != null && note.inlineCommentPosted)
            .scmCommentId(note.scmCommentId)
            .fallbackReason(note.fallbackReason)
            .positionMetadata(note.positionMetadata)
            .build();

    assertThat(entity.isInlineCommentPosted()).isTrue();
    assertThat(entity.getScmCommentId()).isEqualTo("discussion_789");
    assertThat(entity.getPositionMetadata()).contains("new_line");
  }

  @Test
  @DisplayName("should handle null metadata fields gracefully")
  void should_handle_null_metadata() {
    final ReviewResult.Issue issue = new ReviewResult.Issue();
    issue.file = "src/Main.java";
    issue.start_line = 10;
    issue.severity = "minor";
    issue.title = "Legacy issue without metadata";

    final ReviewIssueEntity entity =
        ReviewIssueEntity.builder()
            .filePath(issue.file)
            .startLine(issue.start_line)
            .severity(issue.severity)
            .title(issue.title)
            .inlineCommentPosted(issue.inlineCommentPosted != null && issue.inlineCommentPosted)
            .scmCommentId(issue.scmCommentId)
            .fallbackReason(issue.fallbackReason)
            .positionMetadata(issue.positionMetadata)
            .build();

    assertThat(entity.isInlineCommentPosted()).isFalse();
    assertThat(entity.getScmCommentId()).isNull();
    assertThat(entity.getFallbackReason()).isNull();
    assertThat(entity.getPositionMetadata()).isNull();
  }

  @Test
  @DisplayName("should use builder default for inline comment posted")
  void should_use_builder_default() {
    final ReviewIssueEntity entity =
        ReviewIssueEntity.builder()
            .filePath("src/Test.java")
            .startLine(1)
            .severity("info")
            .title("Test")
            .build();

    assertThat(entity.isInlineCommentPosted()).isFalse();
  }

  @Test
  @DisplayName("should create review entity with all metadata fields")
  void should_create_complete_review_entity() {
    final ReviewEntity review =
        ReviewEntity.builder()
            .id(UUID.randomUUID())
            .repositoryId("test-repo")
            .changeRequestId("MR-123")
            .provider("GITLAB")
            .status(ReviewState.COMPLETED)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    final ReviewIssueEntity issue =
        ReviewIssueEntity.builder()
            .review(review)
            .filePath("src/Main.java")
            .startLine(42)
            .severity("critical")
            .title("Issue with metadata")
            .inlineCommentPosted(true)
            .scmCommentId("discussion_123")
            .positionMetadata("{\"sha\":\"abc\"}")
            .build();

    review.addIssue(issue);

    assertThat(review.getIssues()).hasSize(1);
    assertThat(review.getIssues().get(0).isInlineCommentPosted()).isTrue();
    assertThat(review.getIssues().get(0).getScmCommentId()).isEqualTo("discussion_123");
  }
}
