package com.ghiloufi.aicode.core.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CommentPlacementRouter Tests")
class CommentPlacementRouterTest {

  private CommentPlacementRouter router;

  @BeforeEach
  void setUp() {
    router = new CommentPlacementRouter();
  }

  @Test
  @DisplayName("should_split_valid_and_invalid_issues_correctly")
  void should_split_valid_and_invalid_issues_correctly() {
    final ReviewResult.Issue validIssue1 = createIssue("File.java", 10, "major", "Issue 1");
    final ReviewResult.Issue validIssue2 = createIssue("File.java", 20, "minor", "Issue 2");
    final ReviewResult.Issue invalidIssue1 = createIssue("File.java", 99, "critical", "Issue 3");

    final DiffLineValidator.ValidationResult validationResult =
        new DiffLineValidator.ValidationResult(
            List.of(validIssue1, validIssue2), List.of(invalidIssue1), List.of(), List.of());

    final CommentPlacementRouter.SplitResult splitResult = router.split(validationResult);

    assertThat(splitResult.validForInline().getIssues()).hasSize(2);
    assertThat(splitResult.validForInline().getIssues()).containsExactly(validIssue1, validIssue2);

    assertThat(splitResult.invalidForFallback().getIssues()).hasSize(1);
    assertThat(splitResult.invalidForFallback().getIssues()).containsExactly(invalidIssue1);

    assertThat(splitResult.errors()).hasSize(1);
    assertThat(splitResult.errors().getFirst())
        .contains("Issue 3")
        .contains("File.java")
        .contains("99");
  }

  @Test
  @DisplayName("should_split_valid_and_invalid_notes_correctly")
  void should_split_valid_and_invalid_notes_correctly() {
    final ReviewResult.Note validNote = createNote("File.java", 15, "Good practice");
    final ReviewResult.Note invalidNote = createNote("File.java", 100, "Invalid note");

    final DiffLineValidator.ValidationResult validationResult =
        new DiffLineValidator.ValidationResult(
            List.of(), List.of(), List.of(validNote), List.of(invalidNote));

    final CommentPlacementRouter.SplitResult splitResult = router.split(validationResult);

    assertThat(splitResult.validForInline().getNonBlockingNotes()).hasSize(1);
    assertThat(splitResult.validForInline().getNonBlockingNotes()).containsExactly(validNote);

    assertThat(splitResult.invalidForFallback().getNonBlockingNotes()).hasSize(1);
    assertThat(splitResult.invalidForFallback().getNonBlockingNotes()).containsExactly(invalidNote);
  }

  @Test
  @DisplayName("should_handle_all_valid_results")
  void should_handle_all_valid_results() {
    final ReviewResult.Issue issue = createIssue("File.java", 10, "major", "Issue");
    final ReviewResult.Note note = createNote("File.java", 15, "Note");

    final DiffLineValidator.ValidationResult validationResult =
        new DiffLineValidator.ValidationResult(List.of(issue), List.of(), List.of(note), List.of());

    final CommentPlacementRouter.SplitResult splitResult = router.split(validationResult);

    assertThat(splitResult.validForInline().getIssues()).hasSize(1);
    assertThat(splitResult.validForInline().getNonBlockingNotes()).hasSize(1);
    assertThat(splitResult.invalidForFallback().getIssues()).isEmpty();
    assertThat(splitResult.invalidForFallback().getNonBlockingNotes()).isEmpty();
  }

  @Test
  @DisplayName("should_handle_all_invalid_results")
  void should_handle_all_invalid_results() {
    final ReviewResult.Issue issue = createIssue("File.java", 999, "critical", "Invalid issue");
    final ReviewResult.Note note = createNote("File.java", 888, "Invalid note");

    final DiffLineValidator.ValidationResult validationResult =
        new DiffLineValidator.ValidationResult(List.of(), List.of(issue), List.of(), List.of(note));

    final CommentPlacementRouter.SplitResult splitResult = router.split(validationResult);

    assertThat(splitResult.validForInline().getIssues()).isEmpty();
    assertThat(splitResult.validForInline().getNonBlockingNotes()).isEmpty();
    assertThat(splitResult.invalidForFallback().getIssues()).hasSize(1);
    assertThat(splitResult.invalidForFallback().getNonBlockingNotes()).hasSize(1);
  }

  @Test
  @DisplayName("should_preserve_issue_order")
  void should_preserve_issue_order() {
    final ReviewResult.Issue issue1 = createIssue("A.java", 10, "major", "First");
    final ReviewResult.Issue issue2 = createIssue("B.java", 20, "minor", "Second");
    final ReviewResult.Issue issue3 = createIssue("C.java", 30, "critical", "Third");

    final DiffLineValidator.ValidationResult validationResult =
        new DiffLineValidator.ValidationResult(
            List.of(issue1, issue2, issue3), List.of(), List.of(), List.of());

    final CommentPlacementRouter.SplitResult splitResult = router.split(validationResult);

    assertThat(splitResult.validForInline().getIssues()).containsExactly(issue1, issue2, issue3);
  }

  @Test
  @DisplayName("should_populate_error_list_for_invalid_items")
  void should_populate_error_list_for_invalid_items() {
    final ReviewResult.Issue invalidIssue = createIssue("File.java", 999, "major", "Invalid");
    final ReviewResult.Note invalidNote = createNote("File.java", 888, "Invalid note");

    final DiffLineValidator.ValidationResult validationResult =
        new DiffLineValidator.ValidationResult(
            List.of(), List.of(invalidIssue), List.of(), List.of(invalidNote));

    final CommentPlacementRouter.SplitResult splitResult = router.split(validationResult);

    assertThat(splitResult.errors()).hasSize(2);
    assertThat(splitResult.errors())
        .anyMatch(
            error ->
                error.contains("File.java") && error.contains("999") && error.contains("Issue"));
    assertThat(splitResult.errors())
        .anyMatch(
            error ->
                error.contains("File.java") && error.contains("888") && error.contains("Note"));
  }

  @Test
  @DisplayName("should_handle_empty_validation_result")
  void should_handle_empty_validation_result() {
    final DiffLineValidator.ValidationResult emptyValidation =
        new DiffLineValidator.ValidationResult(List.of(), List.of(), List.of(), List.of());

    final CommentPlacementRouter.SplitResult splitResult = router.split(emptyValidation);

    assertThat(splitResult.validForInline().getIssues()).isEmpty();
    assertThat(splitResult.validForInline().getNonBlockingNotes()).isEmpty();
    assertThat(splitResult.invalidForFallback().getIssues()).isEmpty();
    assertThat(splitResult.invalidForFallback().getNonBlockingNotes()).isEmpty();
    assertThat(splitResult.errors()).isEmpty();
  }

  private ReviewResult.Issue createIssue(
      final String file, final int line, final String severity, final String title) {
    return ReviewResult.Issue.issueBuilder()
        .file(file)
        .startLine(line)
        .severity(severity)
        .title(title)
        .suggestion("Fix it")
        .build();
  }

  private ReviewResult.Note createNote(final String file, final int line, final String noteText) {
    return ReviewResult.Note.noteBuilder().file(file).line(line).note(noteText).build();
  }
}
