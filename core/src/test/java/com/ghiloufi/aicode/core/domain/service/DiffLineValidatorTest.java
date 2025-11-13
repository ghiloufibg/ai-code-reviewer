package com.ghiloufi.aicode.core.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.DiffHunkBlock;
import com.ghiloufi.aicode.core.domain.model.GitDiffDocument;
import com.ghiloufi.aicode.core.domain.model.GitFileModification;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DiffLineValidator Tests")
class DiffLineValidatorTest {

  private DiffLineValidator validator;

  @BeforeEach
  void setUp() {
    validator = new DiffLineValidator();
  }

  @Test
  @DisplayName("should_validate_line_within_hunk_range_as_valid")
  void should_validate_line_within_hunk_range_as_valid() {
    final GitFileModification file =
        new GitFileModification("src/Example.java", "src/Example.java");
    final DiffHunkBlock hunk = new DiffHunkBlock(48, 5, 50, 7);
    hunk.lines = List.of(" context", "+added line 51", " context", "+added line 53", " context");
    file.diffHunkBlocks = List.of(hunk);

    final GitDiffDocument diff = new GitDiffDocument(List.of(file));

    final ReviewResult.Issue issue = new ReviewResult.Issue();
    issue.file = "src/Example.java";
    issue.start_line = 53;
    issue.severity = "major";
    issue.title = "Test issue";
    issue.suggestion = "Fix this";

    final ReviewResult reviewResult = new ReviewResult();
    reviewResult.issues = List.of(issue);

    final DiffLineValidator.ValidationResult result = validator.validate(diff, reviewResult);

    assertThat(result.validIssues()).hasSize(1);
    assertThat(result.invalidIssues()).isEmpty();
    assertThat(result.validIssues().getFirst().start_line).isEqualTo(53);
  }

  @Test
  @DisplayName("should_validate_line_before_hunk_as_invalid")
  void should_validate_line_before_hunk_as_invalid() {
    final GitFileModification file =
        new GitFileModification("src/Example.java", "src/Example.java");
    final DiffHunkBlock hunk = new DiffHunkBlock(48, 5, 50, 7);
    hunk.lines = List.of(" context", "+added", " context");
    file.diffHunkBlocks = List.of(hunk);

    final GitDiffDocument diff = new GitDiffDocument(List.of(file));

    final ReviewResult.Issue issue = new ReviewResult.Issue();
    issue.file = "src/Example.java";
    issue.start_line = 30;
    issue.severity = "minor";
    issue.title = "Issue before hunk";
    issue.suggestion = "Fix";

    final ReviewResult reviewResult = new ReviewResult();
    reviewResult.issues = List.of(issue);

    final DiffLineValidator.ValidationResult result = validator.validate(diff, reviewResult);

    assertThat(result.validIssues()).isEmpty();
    assertThat(result.invalidIssues()).hasSize(1);
    assertThat(result.invalidIssues().getFirst().start_line).isEqualTo(30);
  }

  @Test
  @DisplayName("should_validate_renamed_file_using_new_path")
  void should_validate_renamed_file_using_new_path() {
    final GitFileModification file =
        new GitFileModification("src/OldName.java", "src/NewName.java");
    final DiffHunkBlock hunk = new DiffHunkBlock(10, 3, 10, 3);
    hunk.lines = List.of(" context", " context", " context");
    file.diffHunkBlocks = List.of(hunk);

    final GitDiffDocument diff = new GitDiffDocument(List.of(file));

    final ReviewResult.Issue issue = new ReviewResult.Issue();
    issue.file = "src/NewName.java";
    issue.start_line = 11;
    issue.severity = "major";
    issue.title = "Issue in renamed file";
    issue.suggestion = "Fix";

    final ReviewResult reviewResult = new ReviewResult();
    reviewResult.issues = List.of(issue);

    final DiffLineValidator.ValidationResult result = validator.validate(diff, reviewResult);

    assertThat(result.validIssues()).hasSize(1);
    assertThat(result.invalidIssues()).isEmpty();
  }

  @Test
  @DisplayName("should_validate_deleted_file_as_invalid")
  void should_validate_deleted_file_as_invalid() {
    final GitFileModification file = new GitFileModification("src/Deleted.java", "/dev/null");
    final DiffHunkBlock hunk = new DiffHunkBlock(10, 3, 0, 0);
    hunk.lines = List.of("-deleted line", "-deleted line");
    file.diffHunkBlocks = List.of(hunk);

    final GitDiffDocument diff = new GitDiffDocument(List.of(file));

    final ReviewResult.Issue issue = new ReviewResult.Issue();
    issue.file = "src/Deleted.java";
    issue.start_line = 10;
    issue.severity = "info";
    issue.title = "Issue in deleted file";
    issue.suggestion = "Cannot fix";

    final ReviewResult reviewResult = new ReviewResult();
    reviewResult.issues = List.of(issue);

    final DiffLineValidator.ValidationResult result = validator.validate(diff, reviewResult);

    assertThat(result.validIssues()).isEmpty();
    assertThat(result.invalidIssues()).hasSize(1);
  }

  @Test
  @DisplayName("should_validate_line_across_multiple_hunks")
  void should_validate_line_across_multiple_hunks() {
    final GitFileModification file =
        new GitFileModification("src/Example.java", "src/Example.java");

    final DiffHunkBlock hunk1 = new DiffHunkBlock(10, 2, 10, 3);
    hunk1.lines = List.of(" context", "+added");

    final DiffHunkBlock hunk2 = new DiffHunkBlock(50, 3, 52, 4);
    hunk2.lines = List.of(" context", "+added", " context");

    final DiffHunkBlock hunk3 = new DiffHunkBlock(100, 2, 103, 2);
    hunk3.lines = List.of(" context", " context");

    file.diffHunkBlocks = List.of(hunk1, hunk2, hunk3);

    final GitDiffDocument diff = new GitDiffDocument(List.of(file));

    final ReviewResult.Issue issue = new ReviewResult.Issue();
    issue.file = "src/Example.java";
    issue.start_line = 53;
    issue.severity = "major";
    issue.title = "Issue in second hunk";
    issue.suggestion = "Fix";

    final ReviewResult reviewResult = new ReviewResult();
    reviewResult.issues = List.of(issue);

    final DiffLineValidator.ValidationResult result = validator.validate(diff, reviewResult);

    assertThat(result.validIssues()).hasSize(1);
    assertThat(result.invalidIssues()).isEmpty();
  }

  @Test
  @DisplayName("should_validate_file_not_in_diff_as_invalid")
  void should_validate_file_not_in_diff_as_invalid() {
    final GitFileModification file =
        new GitFileModification("src/Example.java", "src/Example.java");
    final DiffHunkBlock hunk = new DiffHunkBlock(10, 2, 10, 2);
    hunk.lines = List.of(" context", " context");
    file.diffHunkBlocks = List.of(hunk);

    final GitDiffDocument diff = new GitDiffDocument(List.of(file));

    final ReviewResult.Issue issue = new ReviewResult.Issue();
    issue.file = "src/DifferentFile.java";
    issue.start_line = 20;
    issue.severity = "critical";
    issue.title = "Issue in non-existent file";
    issue.suggestion = "Fix";

    final ReviewResult reviewResult = new ReviewResult();
    reviewResult.issues = List.of(issue);

    final DiffLineValidator.ValidationResult result = validator.validate(diff, reviewResult);

    assertThat(result.validIssues()).isEmpty();
    assertThat(result.invalidIssues()).hasSize(1);
  }

  @Test
  @DisplayName("should_validate_negative_line_number_as_invalid")
  void should_validate_negative_line_number_as_invalid() {
    final GitFileModification file =
        new GitFileModification("src/Example.java", "src/Example.java");
    final DiffHunkBlock hunk = new DiffHunkBlock(10, 2, 10, 2);
    hunk.lines = List.of(" context", " context");
    file.diffHunkBlocks = List.of(hunk);

    final GitDiffDocument diff = new GitDiffDocument(List.of(file));

    final ReviewResult.Issue issue = new ReviewResult.Issue();
    issue.file = "src/Example.java";
    issue.start_line = -5;
    issue.severity = "major";
    issue.title = "Issue with negative line";
    issue.suggestion = "Fix";

    final ReviewResult reviewResult = new ReviewResult();
    reviewResult.issues = List.of(issue);

    final DiffLineValidator.ValidationResult result = validator.validate(diff, reviewResult);

    assertThat(result.validIssues()).isEmpty();
    assertThat(result.invalidIssues()).hasSize(1);
  }

  @Test
  @DisplayName("should_validate_both_issues_and_notes")
  void should_validate_both_issues_and_notes() {
    final GitFileModification file =
        new GitFileModification("src/Example.java", "src/Example.java");
    final DiffHunkBlock hunk = new DiffHunkBlock(10, 5, 10, 5);
    hunk.lines = List.of(" line 10", " line 11", "+added 12", " line 13", " line 14");
    file.diffHunkBlocks = List.of(hunk);

    final GitDiffDocument diff = new GitDiffDocument(List.of(file));

    final ReviewResult.Issue validIssue = new ReviewResult.Issue();
    validIssue.file = "src/Example.java";
    validIssue.start_line = 12;
    validIssue.severity = "major";
    validIssue.title = "Valid issue";
    validIssue.suggestion = "Fix";

    final ReviewResult.Issue invalidIssue = new ReviewResult.Issue();
    invalidIssue.file = "src/Example.java";
    invalidIssue.start_line = 50;
    invalidIssue.severity = "minor";
    invalidIssue.title = "Invalid issue";
    invalidIssue.suggestion = "Fix";

    final ReviewResult.Note validNote = new ReviewResult.Note();
    validNote.file = "src/Example.java";
    validNote.line = 11;
    validNote.note = "Good practice";

    final ReviewResult.Note invalidNote = new ReviewResult.Note();
    invalidNote.file = "src/Example.java";
    invalidNote.line = 100;
    invalidNote.note = "Invalid note";

    final ReviewResult reviewResult = new ReviewResult();
    reviewResult.issues = List.of(validIssue, invalidIssue);
    reviewResult.non_blocking_notes = List.of(validNote, invalidNote);

    final DiffLineValidator.ValidationResult result = validator.validate(diff, reviewResult);

    assertThat(result.validIssues()).hasSize(1);
    assertThat(result.invalidIssues()).hasSize(1);
    assertThat(result.validNotes()).hasSize(1);
    assertThat(result.invalidNotes()).hasSize(1);
  }

  @Test
  @DisplayName("should_use_isLineInDiff_to_check_single_line")
  void should_use_isLineInDiff_to_check_single_line() {
    final GitFileModification file = new GitFileModification("src/Test.java", "src/Test.java");
    final DiffHunkBlock hunk = new DiffHunkBlock(20, 3, 20, 3);
    hunk.lines = List.of(" line 20", " line 21", " line 22");
    file.diffHunkBlocks = List.of(hunk);

    final GitDiffDocument diff = new GitDiffDocument(List.of(file));

    final boolean validLine = validator.isLineInDiff(diff, "src/Test.java", 21);
    final boolean invalidLine = validator.isLineInDiff(diff, "src/Test.java", 50);

    assertThat(validLine).isTrue();
    assertThat(invalidLine).isFalse();
  }
}
