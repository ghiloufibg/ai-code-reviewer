package com.ghiloufi.aicode.core.application.service.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.ContextMatch;
import com.ghiloufi.aicode.core.domain.model.GitFileModification;
import com.ghiloufi.aicode.core.domain.model.MatchReason;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DirectorySiblingAnalyzer Tests")
final class DirectorySiblingAnalyzerTest {

  private DirectorySiblingAnalyzer analyzer;

  @BeforeEach
  final void setUp() {
    analyzer = new DirectorySiblingAnalyzer();
  }

  @Test
  @DisplayName("should_find_sibling_files_in_same_directory")
  final void should_find_sibling_files_in_same_directory() {
    final List<GitFileModification> modifiedFiles =
        List.of(
            new GitFileModification(
                "src/service/UserService.java", "src/service/UserService.java"));

    final List<String> repositoryFiles =
        List.of(
            "src/service/UserService.java",
            "src/service/UserRepository.java",
            "src/service/UserController.java",
            "src/model/User.java");

    final List<ContextMatch> matches = analyzer.analyzeSiblings(modifiedFiles, repositoryFiles);

    assertThat(matches).hasSize(2);
    assertThat(matches).allMatch(match -> match.reason() == MatchReason.SIBLING_FILE);
    assertThat(matches)
        .anyMatch(match -> match.filePath().equals("src/service/UserRepository.java"));
    assertThat(matches)
        .anyMatch(match -> match.filePath().equals("src/service/UserController.java"));
  }

  @Test
  @DisplayName("should_boost_confidence_for_related_names")
  final void should_boost_confidence_for_related_names() {
    final List<GitFileModification> modifiedFiles =
        List.of(
            new GitFileModification(
                "src/service/UserService.java", "src/service/UserService.java"));

    final List<String> repositoryFiles =
        List.of(
            "src/service/UserService.java",
            "src/service/UserServiceImpl.java",
            "src/service/ProductService.java");

    final List<ContextMatch> matches = analyzer.analyzeSiblings(modifiedFiles, repositoryFiles);

    final ContextMatch userServiceImpl =
        matches.stream()
            .filter(match -> match.filePath().equals("src/service/UserServiceImpl.java"))
            .findFirst()
            .orElseThrow();

    final ContextMatch productService =
        matches.stream()
            .filter(match -> match.filePath().equals("src/service/ProductService.java"))
            .findFirst()
            .orElseThrow();

    assertThat(userServiceImpl.confidence()).isGreaterThan(productService.confidence());
  }

  @Test
  @DisplayName("should_exclude_modified_file_itself")
  final void should_exclude_modified_file_itself() {
    final List<GitFileModification> modifiedFiles =
        List.of(
            new GitFileModification(
                "src/service/UserService.java", "src/service/UserService.java"));

    final List<String> repositoryFiles =
        List.of("src/service/UserService.java", "src/service/UserRepository.java");

    final List<ContextMatch> matches = analyzer.analyzeSiblings(modifiedFiles, repositoryFiles);

    assertThat(matches).noneMatch(match -> match.filePath().equals("src/service/UserService.java"));
  }

  @Test
  @DisplayName("should_return_empty_list_when_no_siblings_found")
  final void should_return_empty_list_when_no_siblings_found() {
    final List<GitFileModification> modifiedFiles =
        List.of(
            new GitFileModification(
                "src/service/UserService.java", "src/service/UserService.java"));

    final List<String> repositoryFiles =
        List.of(
            "src/service/UserService.java",
            "src/controller/ProductController.java",
            "src/model/Order.java");

    final List<ContextMatch> matches = analyzer.analyzeSiblings(modifiedFiles, repositoryFiles);

    assertThat(matches).isEmpty();
  }

  @Test
  @DisplayName("should_handle_multiple_modified_files")
  final void should_handle_multiple_modified_files() {
    final List<GitFileModification> modifiedFiles =
        List.of(
            new GitFileModification("src/service/UserService.java", "src/service/UserService.java"),
            new GitFileModification(
                "src/controller/UserController.java", "src/controller/UserController.java"));

    final List<String> repositoryFiles =
        List.of(
            "src/service/UserService.java",
            "src/service/UserRepository.java",
            "src/controller/UserController.java",
            "src/controller/ProductController.java");

    final List<ContextMatch> matches = analyzer.analyzeSiblings(modifiedFiles, repositoryFiles);

    assertThat(matches).hasSize(2);
    assertThat(matches)
        .anyMatch(match -> match.filePath().equals("src/service/UserRepository.java"));
    assertThat(matches)
        .anyMatch(match -> match.filePath().equals("src/controller/ProductController.java"));
  }

  @Test
  @DisplayName("should_use_base_confidence_from_match_reason")
  final void should_use_base_confidence_from_match_reason() {
    final List<GitFileModification> modifiedFiles =
        List.of(
            new GitFileModification(
                "src/service/UserService.java", "src/service/UserService.java"));

    final List<String> repositoryFiles =
        List.of("src/service/UserService.java", "src/service/OrderService.java");

    final List<ContextMatch> matches = analyzer.analyzeSiblings(modifiedFiles, repositoryFiles);

    assertThat(matches).hasSize(1);
    assertThat(matches.get(0).confidence())
        .isGreaterThanOrEqualTo(MatchReason.SIBLING_FILE.getBaseConfidence());
  }

  @Test
  @DisplayName("should_handle_file_rename_scenario")
  final void should_handle_file_rename_scenario() {
    final List<GitFileModification> modifiedFiles =
        List.of(
            new GitFileModification(
                "src/service/OldUserService.java", "src/service/NewUserService.java"));

    final List<String> repositoryFiles =
        List.of(
            "src/service/NewUserService.java",
            "src/service/UserRepository.java",
            "src/service/UserValidator.java");

    final List<ContextMatch> matches = analyzer.analyzeSiblings(modifiedFiles, repositoryFiles);

    assertThat(matches).hasSize(2);
    assertThat(matches)
        .anyMatch(match -> match.filePath().equals("src/service/UserRepository.java"));
    assertThat(matches)
        .anyMatch(match -> match.filePath().equals("src/service/UserValidator.java"));
  }
}
