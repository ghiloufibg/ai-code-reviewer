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

    final ReviewResult validReview =
        ReviewResult.builder()
            .issues(new ArrayList<>(validationResult.validIssues()))
            .nonBlockingNotes(new ArrayList<>(validationResult.validNotes()))
            .build();

    final ReviewResult invalidReview =
        ReviewResult.builder()
            .issues(new ArrayList<>(validationResult.invalidIssues()))
            .nonBlockingNotes(new ArrayList<>(validationResult.invalidNotes()))
            .build();

    final List<String> errors = new ArrayList<>();

    for (final ReviewResult.Issue issue : validationResult.invalidIssues()) {
      errors.add(
          String.format(
              "Issue '%s' at %s:%d is outside diff range",
              issue.getTitle(), issue.getFile(), issue.getStartLine()));
    }

    for (final ReviewResult.Note note : validationResult.invalidNotes()) {
      errors.add(
          String.format("Note at %s:%d is outside diff range", note.getFile(), note.getLine()));
    }

    return new SplitResult(validReview, invalidReview, errors);
  }
}
