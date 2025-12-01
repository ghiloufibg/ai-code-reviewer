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
    final ReviewResult.Issue issue =
        ReviewResult.Issue.issueBuilder()
            .file("src/Main.java")
            .startLine(42)
            .severity("critical")
            .title("Null pointer risk")
            .suggestion("Add null check")
            .inlineCommentPosted(true)
            .scmCommentId("discussion_123")
            .positionMetadata("{\"base_sha\":\"abc\",\"head_sha\":\"def\"}")
            .build();

    final ReviewIssueEntity entity =
        ReviewIssueEntity.builder()
            .filePath(issue.getFile())
            .startLine(issue.getStartLine())
            .severity(issue.getSeverity())
            .title(issue.getTitle())
            .suggestion(issue.getSuggestion())
            .inlineCommentPosted(
                issue.getInlineCommentPosted() != null && issue.getInlineCommentPosted())
            .scmCommentId(issue.getScmCommentId())
            .fallbackReason(issue.getFallbackReason())
            .positionMetadata(issue.getPositionMetadata())
            .build();

    assertThat(entity.isInlineCommentPosted()).isTrue();
    assertThat(entity.getScmCommentId()).isEqualTo("discussion_123");
    assertThat(entity.getFallbackReason()).isNull();
    assertThat(entity.getPositionMetadata()).contains("base_sha");
  }

  @Test
  @DisplayName("should map issue with fallback metadata to entity")
  void should_map_issue_with_fallback_metadata() {
    final ReviewResult.Issue issue =
        ReviewResult.Issue.issueBuilder()
            .file("src/Main.java")
            .startLine(999)
            .severity("major")
            .title("Invalid line")
            .inlineCommentPosted(false)
            .scmCommentId("note_456")
            .fallbackReason("INVALID_LINE")
            .build();

    final ReviewIssueEntity entity =
        ReviewIssueEntity.builder()
            .filePath(issue.getFile())
            .startLine(issue.getStartLine())
            .severity(issue.getSeverity())
            .title(issue.getTitle())
            .inlineCommentPosted(
                issue.getInlineCommentPosted() != null && issue.getInlineCommentPosted())
            .scmCommentId(issue.getScmCommentId())
            .fallbackReason(issue.getFallbackReason())
            .positionMetadata(issue.getPositionMetadata())
            .build();

    assertThat(entity.isInlineCommentPosted()).isFalse();
    assertThat(entity.getScmCommentId()).isEqualTo("note_456");
    assertThat(entity.getFallbackReason()).isEqualTo("INVALID_LINE");
    assertThat(entity.getPositionMetadata()).isNull();
  }

  @Test
  @DisplayName("should map note with inline comment metadata to entity")
  void should_map_note_with_inline_metadata() {
    final ReviewResult.Note note =
        ReviewResult.Note.noteBuilder()
            .file("src/Utils.java")
            .line(15)
            .note("Consider using Optional")
            .inlineCommentPosted(true)
            .scmCommentId("discussion_789")
            .positionMetadata("{\"new_line\":15}")
            .build();

    final ReviewNoteEntity entity =
        ReviewNoteEntity.builder()
            .filePath(note.getFile())
            .lineNumber(note.getLine())
            .note(note.getNote())
            .inlineCommentPosted(
                note.getInlineCommentPosted() != null && note.getInlineCommentPosted())
            .scmCommentId(note.getScmCommentId())
            .fallbackReason(note.getFallbackReason())
            .positionMetadata(note.getPositionMetadata())
            .build();

    assertThat(entity.isInlineCommentPosted()).isTrue();
    assertThat(entity.getScmCommentId()).isEqualTo("discussion_789");
    assertThat(entity.getPositionMetadata()).contains("new_line");
  }

  @Test
  @DisplayName("should handle null metadata fields gracefully")
  void should_handle_null_metadata() {
    final ReviewResult.Issue issue =
        ReviewResult.Issue.issueBuilder()
            .file("src/Main.java")
            .startLine(10)
            .severity("minor")
            .title("Legacy issue without metadata")
            .build();

    final ReviewIssueEntity entity =
        ReviewIssueEntity.builder()
            .filePath(issue.getFile())
            .startLine(issue.getStartLine())
            .severity(issue.getSeverity())
            .title(issue.getTitle())
            .inlineCommentPosted(
                issue.getInlineCommentPosted() != null && issue.getInlineCommentPosted())
            .scmCommentId(issue.getScmCommentId())
            .fallbackReason(issue.getFallbackReason())
            .positionMetadata(issue.getPositionMetadata())
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
