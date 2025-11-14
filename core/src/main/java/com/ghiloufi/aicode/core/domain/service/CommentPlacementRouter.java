package com.ghiloufi.aicode.core.domain.service;

import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class CommentPlacementRouter {

  public record SplitResult(
      ReviewResult validForInline, ReviewResult invalidForFallback, List<String> errors) {}

  public SplitResult split(final DiffLineValidator.ValidationResult validationResult) {
    Objects.requireNonNull(validationResult, "ValidationResult cannot be null");

    final ReviewResult validReview = new ReviewResult();
    validReview.issues = new ArrayList<>(validationResult.validIssues());
    validReview.non_blocking_notes = new ArrayList<>(validationResult.validNotes());

    final ReviewResult invalidReview = new ReviewResult();
    invalidReview.issues = new ArrayList<>(validationResult.invalidIssues());
    invalidReview.non_blocking_notes = new ArrayList<>(validationResult.invalidNotes());

    final List<String> errors = new ArrayList<>();

    for (final ReviewResult.Issue issue : validationResult.invalidIssues()) {
      errors.add(
          String.format(
              "Issue '%s' at %s:%d is outside diff range",
              issue.title, issue.file, issue.start_line));
    }

    for (final ReviewResult.Note note : validationResult.invalidNotes()) {
      errors.add(String.format("Note at %s:%d is outside diff range", note.file, note.line));
    }

    return new SplitResult(validReview, invalidReview, errors);
  }
}
