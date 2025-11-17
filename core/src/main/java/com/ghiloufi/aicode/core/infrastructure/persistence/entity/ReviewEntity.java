package com.ghiloufi.aicode.core.infrastructure.persistence.entity;

import com.ghiloufi.aicode.core.domain.model.ReviewState;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "repository_id", nullable = false)
  private String repositoryId;

  @Column(name = "change_request_id", nullable = false)
  private String changeRequestId;

  @Column(name = "provider", nullable = false)
  private String provider;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private ReviewState status;

  @Column(name = "llm_provider")
  private String llmProvider;

  @Column(name = "llm_model")
  private String llmModel;

  @Column(name = "summary", columnDefinition = "TEXT")
  private String summary;

  @Column(name = "raw_llm_response", columnDefinition = "TEXT")
  private String rawLlmResponse;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  @Builder.Default
  private List<ReviewIssueEntity> issues = new ArrayList<>();

  @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  @Builder.Default
  private List<ReviewNoteEntity> notes = new ArrayList<>();

  @PrePersist
  protected void onCreate() {
    final Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = Instant.now();
  }

  public void addIssue(final ReviewIssueEntity issue) {
    issues.add(issue);
    issue.setReview(this);
  }

  public void addNote(final ReviewNoteEntity note) {
    notes.add(note);
    note.setReview(this);
  }
}
