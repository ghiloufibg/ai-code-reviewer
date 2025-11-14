package com.ghiloufi.aicode.core.infrastructure.persistence;

import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.model.ReviewState;
import com.ghiloufi.aicode.core.domain.model.ReviewState.StateTransition;
import com.ghiloufi.aicode.core.infrastructure.persistence.entity.ReviewEntity;
import com.ghiloufi.aicode.core.infrastructure.persistence.entity.ReviewIssueEntity;
import com.ghiloufi.aicode.core.infrastructure.persistence.entity.ReviewNoteEntity;
import com.ghiloufi.aicode.core.infrastructure.persistence.repository.ReviewJpaRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Repository
@Slf4j
public class PostgresReviewRepository {

  private static final Duration DEFAULT_RETENTION = Duration.ofHours(24);

  private final ReviewJpaRepository jpaRepository;
  private final Duration retentionDuration;

  @Autowired
  public PostgresReviewRepository(final ReviewJpaRepository jpaRepository) {
    this(jpaRepository, DEFAULT_RETENTION);
  }

  public PostgresReviewRepository(
      final ReviewJpaRepository jpaRepository, final Duration retentionDuration) {
    this.jpaRepository = jpaRepository;
    this.retentionDuration = retentionDuration;
    log.info("PostgresReviewRepository initialized with retention: {}", retentionDuration);
  }

  @Transactional
  public Mono<Void> save(final String reviewId, final ReviewResult result) {
    return Mono.fromRunnable(
        () -> {
          final String repositoryId = extractRepositoryId(reviewId);
          final String changeRequestId = extractChangeRequestId(reviewId);
          final String provider = extractProvider(reviewId);

          final Optional<ReviewEntity> existing =
              jpaRepository.findByRepositoryIdAndChangeRequestIdAndProvider(
                  repositoryId, changeRequestId, provider);

          final ReviewEntity entity;
          if (existing.isPresent()) {
            entity = existing.get();
            entity.setSummary(result.summary);
            entity.setLlmProvider(result.llmProvider);
            entity.setLlmModel(result.llmModel);
            updateIssuesAndNotes(entity, result);
          } else {
            entity = convertToEntity(reviewId, result);
          }

          jpaRepository.save(entity);
          log.debug("Saved review: {} with state: PENDING", reviewId);
        });
  }

  private void updateIssuesAndNotes(final ReviewEntity entity, final ReviewResult result) {
    entity.getIssues().clear();
    if (result.issues != null) {
      result.issues.forEach(
          issue -> {
            final ReviewIssueEntity issueEntity =
                ReviewIssueEntity.builder()
                    .filePath(issue.file)
                    .startLine(issue.start_line)
                    .severity(issue.severity)
                    .title(issue.title)
                    .suggestion(issue.suggestion)
                    .inlineCommentPosted(
                        issue.inlineCommentPosted != null && issue.inlineCommentPosted)
                    .scmCommentId(issue.scmCommentId)
                    .fallbackReason(issue.fallbackReason)
                    .positionMetadata(issue.positionMetadata)
                    .build();
            entity.addIssue(issueEntity);
          });
    }

    entity.getNotes().clear();
    if (result.non_blocking_notes != null) {
      result.non_blocking_notes.forEach(
          note -> {
            final ReviewNoteEntity noteEntity =
                ReviewNoteEntity.builder()
                    .filePath(note.file)
                    .lineNumber(note.line)
                    .note(note.note)
                    .inlineCommentPosted(
                        note.inlineCommentPosted != null && note.inlineCommentPosted)
                    .scmCommentId(note.scmCommentId)
                    .fallbackReason(note.fallbackReason)
                    .positionMetadata(note.positionMetadata)
                    .build();
            entity.addNote(noteEntity);
          });
    }
  }

  @Transactional(readOnly = true)
  public Mono<Optional<ReviewResult>> findById(final String reviewId) {
    return Mono.fromSupplier(
        () -> {
          final String repositoryId = extractRepositoryId(reviewId);
          final String changeRequestId = extractChangeRequestId(reviewId);
          final String provider = extractProvider(reviewId);

          return jpaRepository
              .findByRepositoryIdAndChangeRequestIdAndProvider(
                  repositoryId, changeRequestId, provider)
              .map(this::convertToDomain);
        });
  }

  @Transactional
  public Mono<Void> updateState(final String reviewId, final ReviewState newState) {
    return Mono.fromRunnable(
        () -> {
          final String repositoryId = extractRepositoryId(reviewId);
          final String changeRequestId = extractChangeRequestId(reviewId);
          final String provider = extractProvider(reviewId);

          final ReviewEntity entity =
              jpaRepository
                  .findByRepositoryIdAndChangeRequestIdAndProvider(
                      repositoryId, changeRequestId, provider)
                  .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));

          final ReviewState oldState = entity.getStatus();
          entity.setStatus(newState);

          if (newState.isTerminal()) {
            entity.setCompletedAt(Instant.now());
          }

          jpaRepository.save(entity);
          log.debug("Updated review {} state: {} -> {}", reviewId, oldState, newState);
        });
  }

  @Transactional
  public Mono<Void> updateResultAndState(
      final String reviewId, final ReviewResult result, final ReviewState newState) {
    return Mono.fromRunnable(
        () -> {
          final String repositoryId = extractRepositoryId(reviewId);
          final String changeRequestId = extractChangeRequestId(reviewId);
          final String provider = extractProvider(reviewId);

          final ReviewEntity entity =
              jpaRepository
                  .findByRepositoryIdAndChangeRequestIdAndProvider(
                      repositoryId, changeRequestId, provider)
                  .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));

          entity.setSummary(result.summary);
          entity.setLlmProvider(result.llmProvider);
          entity.setLlmModel(result.llmModel);
          entity.setStatus(newState);

          if (newState.isTerminal()) {
            entity.setCompletedAt(Instant.now());
          }

          entity.getIssues().clear();
          if (result.issues != null) {
            result.issues.forEach(
                issue -> {
                  final ReviewIssueEntity issueEntity =
                      ReviewIssueEntity.builder()
                          .filePath(issue.file)
                          .startLine(issue.start_line)
                          .severity(issue.severity)
                          .title(issue.title)
                          .suggestion(issue.suggestion)
                          .build();
                  entity.addIssue(issueEntity);
                });
          }

          entity.getNotes().clear();
          if (result.non_blocking_notes != null) {
            result.non_blocking_notes.forEach(
                note -> {
                  final ReviewNoteEntity noteEntity =
                      ReviewNoteEntity.builder()
                          .filePath(note.file)
                          .lineNumber(note.line)
                          .note(note.note)
                          .build();
                  entity.addNote(noteEntity);
                });
          }

          jpaRepository.save(entity);
          log.debug("Updated review {} with new result and state: {}", reviewId, newState);
        });
  }

  @Transactional(readOnly = true)
  public Mono<Optional<StateTransition>> getState(final String reviewId) {
    return Mono.fromSupplier(
        () -> {
          final String repositoryId = extractRepositoryId(reviewId);
          final String changeRequestId = extractChangeRequestId(reviewId);
          final String provider = extractProvider(reviewId);

          return jpaRepository
              .findByRepositoryIdAndChangeRequestIdAndProvider(
                  repositoryId, changeRequestId, provider)
              .map(entity -> StateTransition.now(entity.getStatus()));
        });
  }

  @Scheduled(fixedRate = 3600000)
  @Transactional
  public void cleanupOldReviews() {
    final Instant cutoffTime = Instant.now().minus(retentionDuration);
    final List<ReviewEntity> expiredReviews = jpaRepository.findOldReviews(cutoffTime);

    if (!expiredReviews.isEmpty()) {
      jpaRepository.deleteAll(expiredReviews);
      log.info(
          "Cleaned up {} expired reviews (retention: {})",
          expiredReviews.size(),
          retentionDuration);
    }
  }

  private ReviewEntity convertToEntity(final String reviewId, final ReviewResult result) {
    final ReviewEntity entity =
        ReviewEntity.builder()
            .repositoryId(extractRepositoryId(reviewId))
            .changeRequestId(extractChangeRequestId(reviewId))
            .provider(extractProvider(reviewId))
            .status(ReviewState.PENDING)
            .summary(result.summary)
            .llmProvider(result.llmProvider)
            .llmModel(result.llmModel)
            .build();

    if (result.issues != null) {
      result.issues.forEach(
          issue -> {
            final ReviewIssueEntity issueEntity =
                ReviewIssueEntity.builder()
                    .filePath(issue.file)
                    .startLine(issue.start_line)
                    .severity(issue.severity)
                    .title(issue.title)
                    .suggestion(issue.suggestion)
                    .build();
            entity.addIssue(issueEntity);
          });
    }

    if (result.non_blocking_notes != null) {
      result.non_blocking_notes.forEach(
          note -> {
            final ReviewNoteEntity noteEntity =
                ReviewNoteEntity.builder()
                    .filePath(note.file)
                    .lineNumber(note.line)
                    .note(note.note)
                    .build();
            entity.addNote(noteEntity);
          });
    }

    return entity;
  }

  private ReviewResult convertToDomain(final ReviewEntity entity) {
    final ReviewResult result = new ReviewResult();
    result.summary = entity.getSummary();

    if (entity.getIssues() != null) {
      entity
          .getIssues()
          .forEach(
              issueEntity -> {
                final ReviewResult.Issue issue = new ReviewResult.Issue();
                issue.file = issueEntity.getFilePath();
                issue.start_line = issueEntity.getStartLine();
                issue.severity = issueEntity.getSeverity();
                issue.title = issueEntity.getTitle();
                issue.suggestion = issueEntity.getSuggestion();
                result.issues.add(issue);
              });
    }

    if (entity.getNotes() != null) {
      entity
          .getNotes()
          .forEach(
              noteEntity -> {
                final ReviewResult.Note note = new ReviewResult.Note();
                note.file = noteEntity.getFilePath();
                note.line = noteEntity.getLineNumber();
                note.note = noteEntity.getNote();
                result.non_blocking_notes.add(note);
              });
    }

    return result;
  }

  private UUID parseReviewId(final String reviewId) {
    try {
      return UUID.fromString(reviewId);
    } catch (IllegalArgumentException e) {
      return UUID.nameUUIDFromBytes(reviewId.getBytes());
    }
  }

  private String extractRepositoryId(final String reviewId) {
    return reviewId.split("_")[0];
  }

  private String extractChangeRequestId(final String reviewId) {
    final String[] parts = reviewId.split("_");
    return parts.length > 1 ? parts[1] : "unknown";
  }

  private String extractProvider(final String reviewId) {
    final String[] parts = reviewId.split("_");
    return parts.length > 2 ? parts[2] : "unknown";
  }
}
