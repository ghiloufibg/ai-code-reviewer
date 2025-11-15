package com.ghiloufi.aicode.core.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.DiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.GitDiffDocument;
import com.ghiloufi.aicode.core.domain.service.DiffFormatter;
import com.ghiloufi.aicode.core.service.diff.UnifiedDiffParser;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

@DisplayName("Real-World Diff Parsing Integration Tests")
final class RealWorldDiffParsingIntegrationTest {

  private UnifiedDiffParser diffParser;
  private DiffFormatter diffFormatter;

  @BeforeEach
  final void setUp() {
    diffParser = new UnifiedDiffParser();
    diffFormatter = new DiffFormatter();
  }

  @Nested
  @DisplayName("Medium PR Diff Parsing")
  final class MediumPRDiffParsing {

    @Test
    @DisplayName("should_parse_medium_pr_diff_successfully")
    final void should_parse_medium_pr_diff_successfully() throws Exception {
      final String rawDiff = loadDiffFromClasspath("diff-samples/medium-pr.diff");

      final GitDiffDocument parsedDiff = diffParser.parse(rawDiff);

      assertThat(parsedDiff).isNotNull();
      assertThat(parsedDiff.files).isNotEmpty();
      assertThat(parsedDiff.files).hasSizeGreaterThanOrEqualTo(8);
    }

    @Test
    @DisplayName("should_parse_medium_pr_with_correct_file_paths")
    final void should_parse_medium_pr_with_correct_file_paths() throws Exception {
      final String rawDiff = loadDiffFromClasspath("diff-samples/medium-pr.diff");

      final GitDiffDocument parsedDiff = diffParser.parse(rawDiff);

      assertThat(parsedDiff.files)
          .extracting(file -> file.newPath)
          .contains(
              "src/main/java/com/example/service/UserService.java",
              "src/main/java/com/example/service/UserValidator.java",
              "src/main/java/com/example/service/EmailService.java",
              "src/main/java/com/example/exception/UserNotFoundException.java",
              "src/main/java/com/example/exception/InvalidUserDataException.java",
              "src/main/java/com/example/repository/UserRepository.java",
              "src/main/java/com/example/model/User.java",
              "src/test/java/com/example/service/UserServiceTest.java");
    }

    @Test
    @DisplayName("should_parse_medium_pr_with_hunks_and_lines")
    final void should_parse_medium_pr_with_hunks_and_lines() throws Exception {
      final String rawDiff = loadDiffFromClasspath("diff-samples/medium-pr.diff");

      final GitDiffDocument parsedDiff = diffParser.parse(rawDiff);

      assertThat(parsedDiff.files).allSatisfy(file -> assertThat(file.diffHunkBlocks).isNotEmpty());

      parsedDiff.files.stream()
          .flatMap(file -> file.diffHunkBlocks.stream())
          .forEach(
              hunk -> {
                assertThat(hunk.newStart).isGreaterThan(0);
                assertThat(hunk.newCount).isGreaterThanOrEqualTo(0);
                assertThat(hunk.lines).isNotEmpty();
              });
    }

    @Test
    @DisplayName("should_create_diff_analysis_bundle_for_medium_pr")
    final void should_create_diff_analysis_bundle_for_medium_pr() throws Exception {
      final String rawDiff = loadDiffFromClasspath("diff-samples/medium-pr.diff");

      final GitDiffDocument parsedDiff = diffParser.parse(rawDiff);
      final DiffAnalysisBundle bundle = new DiffAnalysisBundle(parsedDiff, rawDiff);

      assertThat(bundle).isNotNull();
      assertThat(bundle.structuredDiff()).isEqualTo(parsedDiff);
      assertThat(bundle.rawDiffText()).isEqualTo(rawDiff);
      assertThat(bundle.structuredDiff().files).hasSizeGreaterThanOrEqualTo(8);
    }

    @Test
    @DisplayName("should_format_medium_pr_diff_successfully")
    final void should_format_medium_pr_diff_successfully() throws Exception {
      final String rawDiff = loadDiffFromClasspath("diff-samples/medium-pr.diff");

      final GitDiffDocument parsedDiff = diffParser.parse(rawDiff);
      final String formattedDiff = diffFormatter.formatDiff(parsedDiff);

      assertThat(formattedDiff).isNotBlank();
      assertThat(formattedDiff).contains("FILE:");
      assertThat(formattedDiff).contains("UserService.java");
      assertThat(formattedDiff).contains("Hunk");
    }
  }

  @Nested
  @DisplayName("Large PR Diff Parsing")
  final class LargePRDiffParsing {

    @Test
    @DisplayName("should_parse_large_pr_diff_successfully")
    final void should_parse_large_pr_diff_successfully() throws Exception {
      final String rawDiff = loadDiffFromClasspath("diff-samples/large-pr.diff");

      final GitDiffDocument parsedDiff = diffParser.parse(rawDiff);

      assertThat(parsedDiff).isNotNull();
      assertThat(parsedDiff.files).isNotEmpty();
      assertThat(parsedDiff.files).hasSizeGreaterThanOrEqualTo(12);
    }

    @Test
    @DisplayName("should_parse_large_pr_with_correct_file_paths")
    final void should_parse_large_pr_with_correct_file_paths() throws Exception {
      final String rawDiff = loadDiffFromClasspath("diff-samples/large-pr.diff");

      final GitDiffDocument parsedDiff = diffParser.parse(rawDiff);

      assertThat(parsedDiff.files)
          .extracting(file -> file.newPath)
          .contains(
              "src/main/java/com/example/controller/UserController.java",
              "src/main/java/com/example/dto/UserCreateRequest.java",
              "src/main/java/com/example/dto/UserUpdateRequest.java",
              "src/main/java/com/example/dto/UserResponse.java",
              "src/main/java/com/example/mapper/UserMapper.java",
              "src/main/java/com/example/config/SecurityConfig.java",
              "src/main/java/com/example/config/DatabaseConfig.java",
              "src/main/java/com/example/exception/GlobalExceptionHandler.java",
              "src/main/java/com/example/exception/ErrorResponse.java");
    }

    @Test
    @DisplayName("should_parse_large_pr_with_hunks_and_lines")
    final void should_parse_large_pr_with_hunks_and_lines() throws Exception {
      final String rawDiff = loadDiffFromClasspath("diff-samples/large-pr.diff");

      final GitDiffDocument parsedDiff = diffParser.parse(rawDiff);

      assertThat(parsedDiff.files).allSatisfy(file -> assertThat(file.diffHunkBlocks).isNotEmpty());

      parsedDiff.files.stream()
          .flatMap(file -> file.diffHunkBlocks.stream())
          .forEach(
              hunk -> {
                assertThat(hunk.newStart).isGreaterThan(0);
                assertThat(hunk.newCount).isGreaterThanOrEqualTo(0);
                assertThat(hunk.lines).isNotEmpty();
              });
    }

    @Test
    @DisplayName("should_create_diff_analysis_bundle_for_large_pr")
    final void should_create_diff_analysis_bundle_for_large_pr() throws Exception {
      final String rawDiff = loadDiffFromClasspath("diff-samples/large-pr.diff");

      final GitDiffDocument parsedDiff = diffParser.parse(rawDiff);
      final DiffAnalysisBundle bundle = new DiffAnalysisBundle(parsedDiff, rawDiff);

      assertThat(bundle).isNotNull();
      assertThat(bundle.structuredDiff()).isEqualTo(parsedDiff);
      assertThat(bundle.rawDiffText()).isEqualTo(rawDiff);
      assertThat(bundle.structuredDiff().files).hasSizeGreaterThanOrEqualTo(12);
    }

    @Test
    @DisplayName("should_format_large_pr_diff_successfully")
    final void should_format_large_pr_diff_successfully() throws Exception {
      final String rawDiff = loadDiffFromClasspath("diff-samples/large-pr.diff");

      final GitDiffDocument parsedDiff = diffParser.parse(rawDiff);
      final String formattedDiff = diffFormatter.formatDiff(parsedDiff);

      assertThat(formattedDiff).isNotBlank();
      assertThat(formattedDiff).contains("FILE:");
      assertThat(formattedDiff).contains("UserController.java");
      assertThat(formattedDiff).contains("SecurityConfig.java");
      assertThat(formattedDiff).contains("Hunk");
    }

    @Test
    @DisplayName("should_handle_large_pr_with_performance_within_acceptable_limits")
    final void should_handle_large_pr_with_performance_within_acceptable_limits() throws Exception {
      final String rawDiff = loadDiffFromClasspath("diff-samples/large-pr.diff");

      final long startTime = System.currentTimeMillis();
      final GitDiffDocument parsedDiff = diffParser.parse(rawDiff);
      final long parseTime = System.currentTimeMillis() - startTime;

      assertThat(parsedDiff.files).hasSizeGreaterThanOrEqualTo(12);
      assertThat(parseTime).isLessThan(1000);
    }
  }

  @Nested
  @DisplayName("Edge Cases and Validation")
  final class EdgeCasesAndValidation {

    @Test
    @DisplayName("should_handle_empty_diff_gracefully")
    final void should_handle_empty_diff_gracefully() {
      final String emptyDiff = "";

      final GitDiffDocument parsedDiff = diffParser.parse(emptyDiff);

      assertThat(parsedDiff).isNotNull();
      assertThat(parsedDiff.files).isEmpty();
    }

    @Test
    @DisplayName("should_handle_diff_with_no_hunks")
    final void should_handle_diff_with_no_hunks() {
      final String diffWithNoHunks =
          """
          --- a/file.txt
          +++ b/file.txt
          """;

      final GitDiffDocument parsedDiff = diffParser.parse(diffWithNoHunks);

      assertThat(parsedDiff).isNotNull();
      assertThat(parsedDiff.files).hasSize(1);
      assertThat(parsedDiff.files.get(0).diffHunkBlocks).isEmpty();
    }

    @Test
    @DisplayName("should_parse_diff_with_new_file_creation")
    final void should_parse_diff_with_new_file_creation() {
      final String newFileDiff =
          """
          --- /dev/null
          +++ b/NewFile.java
          @@ -0,0 +1,5 @@
          +public class NewFile {
          +    public void method() {
          +        System.out.println("new");
          +    }
          +}
          """;

      final GitDiffDocument parsedDiff = diffParser.parse(newFileDiff);

      assertThat(parsedDiff).isNotNull();
      assertThat(parsedDiff.files).hasSize(1);
      assertThat(parsedDiff.files.get(0).oldPath).isEqualTo("/dev/null");
      assertThat(parsedDiff.files.get(0).newPath).isEqualTo("NewFile.java");
      assertThat(parsedDiff.files.get(0).diffHunkBlocks).hasSize(1);
    }

    @Test
    @DisplayName("should_parse_diff_with_file_deletion")
    final void should_parse_diff_with_file_deletion() {
      final String deletedFileDiff =
          """
          --- a/DeletedFile.java
          +++ /dev/null
          @@ -1,5 +0,0 @@
          -public class DeletedFile {
          -    public void method() {
          -        System.out.println("deleted");
          -    }
          -}
          """;

      final GitDiffDocument parsedDiff = diffParser.parse(deletedFileDiff);

      assertThat(parsedDiff).isNotNull();
      assertThat(parsedDiff.files).hasSize(1);
      assertThat(parsedDiff.files.get(0).oldPath).isEqualTo("DeletedFile.java");
      assertThat(parsedDiff.files.get(0).newPath).isEqualTo("/dev/null");
      assertThat(parsedDiff.files.get(0).diffHunkBlocks).hasSize(1);
    }
  }

  private String loadDiffFromClasspath(final String resourcePath) throws Exception {
    final ClassPathResource resource = new ClassPathResource(resourcePath);
    return resource.getContentAsString(StandardCharsets.UTF_8);
  }
}
