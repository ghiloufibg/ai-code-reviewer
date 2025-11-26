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

    final ReviewResult.Issue issue =
        ReviewResult.Issue.issueBuilder()
            .file("src/Example.java")
            .startLine(53)
            .severity("major")
            .title("Test issue")
            .suggestion("Fix this")
            .build();

    final ReviewResult reviewResult =
        ReviewResult.builder().issues(List.of(issue)).nonBlockingNotes(List.of()).build();

    final DiffLineValidator.ValidationResult result = validator.validate(diff, reviewResult);

    assertThat(result.validIssues()).hasSize(1);
    assertThat(result.invalidIssues()).isEmpty();
    assertThat(result.validIssues().getFirst().getStartLine()).isEqualTo(53);
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

    final ReviewResult.Issue issue =
        ReviewResult.Issue.issueBuilder()
            .file("src/Example.java")
            .startLine(30)
            .severity("minor")
            .title("Issue before hunk")
            .suggestion("Fix")
            .build();

    final ReviewResult reviewResult =
        ReviewResult.builder().issues(List.of(issue)).nonBlockingNotes(List.of()).build();

    final DiffLineValidator.ValidationResult result = validator.validate(diff, reviewResult);

    assertThat(result.validIssues()).isEmpty();
    assertThat(result.invalidIssues()).hasSize(1);
    assertThat(result.invalidIssues().getFirst().getStartLine()).isEqualTo(30);
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

    final ReviewResult.Issue issue =
        ReviewResult.Issue.issueBuilder()
            .file("src/NewName.java")
            .startLine(11)
            .severity("major")
            .title("Issue in renamed file")
            .suggestion("Fix")
            .build();

    final ReviewResult reviewResult =
        ReviewResult.builder().issues(List.of(issue)).nonBlockingNotes(List.of()).build();

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

    final ReviewResult.Issue issue =
        ReviewResult.Issue.issueBuilder()
            .file("src/Deleted.java")
            .startLine(10)
            .severity("info")
            .title("Issue in deleted file")
            .suggestion("Cannot fix")
            .build();

    final ReviewResult reviewResult =
        ReviewResult.builder().issues(List.of(issue)).nonBlockingNotes(List.of()).build();

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

    final ReviewResult.Issue issue =
        ReviewResult.Issue.issueBuilder()
            .file("src/Example.java")
            .startLine(53)
            .severity("major")
            .title("Issue in second hunk")
            .suggestion("Fix")
            .build();

    final ReviewResult reviewResult =
        ReviewResult.builder().issues(List.of(issue)).nonBlockingNotes(List.of()).build();

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

    final ReviewResult.Issue issue =
        ReviewResult.Issue.issueBuilder()
            .file("src/DifferentFile.java")
            .startLine(20)
            .severity("critical")
            .title("Issue in non-existent file")
            .suggestion("Fix")
            .build();

    final ReviewResult reviewResult =
        ReviewResult.builder().issues(List.of(issue)).nonBlockingNotes(List.of()).build();

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

    final ReviewResult.Issue issue =
        ReviewResult.Issue.issueBuilder()
            .file("src/Example.java")
            .startLine(-5)
            .severity("major")
            .title("Issue with negative line")
            .suggestion("Fix")
            .build();

    final ReviewResult reviewResult =
        ReviewResult.builder().issues(List.of(issue)).nonBlockingNotes(List.of()).build();

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

    final ReviewResult.Issue validIssue =
        ReviewResult.Issue.issueBuilder()
            .file("src/Example.java")
            .startLine(12)
            .severity("major")
            .title("Valid issue")
            .suggestion("Fix")
            .build();

    final ReviewResult.Issue invalidIssue =
        ReviewResult.Issue.issueBuilder()
            .file("src/Example.java")
            .startLine(50)
            .severity("minor")
            .title("Invalid issue")
            .suggestion("Fix")
            .build();

    final ReviewResult.Note validNote =
        ReviewResult.Note.noteBuilder()
            .file("src/Example.java")
            .line(11)
            .note("Good practice")
            .build();

    final ReviewResult.Note invalidNote =
        ReviewResult.Note.noteBuilder()
            .file("src/Example.java")
            .line(100)
            .note("Invalid note")
            .build();

    final ReviewResult reviewResult =
        ReviewResult.builder()
            .issues(List.of(validIssue, invalidIssue))
            .nonBlockingNotes(List.of(validNote, invalidNote))
            .build();

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
