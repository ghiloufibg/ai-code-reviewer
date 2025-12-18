package com.ghiloufi.aicode.llmworker.service.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.ContextMatch;
import com.ghiloufi.aicode.core.domain.model.GitFileModification;
import com.ghiloufi.aicode.core.domain.model.MatchReason;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("EnhancedPathAnalyzer Tests")
final class EnhancedPathAnalyzerTest {

  private EnhancedPathAnalyzer analyzer;

  @BeforeEach
  final void setUp() {
    analyzer = new EnhancedPathAnalyzer();
  }

  @Nested
  @DisplayName("Same Package Detection")
  final class SamePackageDetection {

    @Test
    @DisplayName("should_find_files_in_same_package")
    final void should_find_files_in_same_package() {
      final List<GitFileModification> modifiedFiles =
          List.of(
              new GitFileModification(
                  "src/main/java/com/example/service/UserService.java",
                  "src/main/java/com/example/service/UserService.java"));

      final List<String> repositoryFiles =
          List.of(
              "src/main/java/com/example/service/UserService.java",
              "src/main/java/com/example/service/OrderService.java",
              "src/main/java/com/example/service/ProductService.java",
              "src/main/java/com/example/controller/UserController.java");

      final List<ContextMatch> matches =
          analyzer.analyzePathPatterns(modifiedFiles, repositoryFiles);

      assertThat(matches)
          .anyMatch(
              match ->
                  match.filePath().equals("src/main/java/com/example/service/OrderService.java")
                      && match.reason() == MatchReason.SAME_PACKAGE);
      assertThat(matches)
          .anyMatch(
              match ->
                  match.filePath().equals("src/main/java/com/example/service/ProductService.java")
                      && match.reason() == MatchReason.SAME_PACKAGE);
    }

    @Test
    @DisplayName("should_exclude_modified_file_from_same_package_matches")
    final void should_exclude_modified_file_from_same_package_matches() {
      final List<GitFileModification> modifiedFiles =
          List.of(
              new GitFileModification(
                  "src/main/java/com/example/service/UserService.java",
                  "src/main/java/com/example/service/UserService.java"));

      final List<String> repositoryFiles =
          List.of(
              "src/main/java/com/example/service/UserService.java",
              "src/main/java/com/example/service/OrderService.java");

      final List<ContextMatch> matches =
          analyzer.analyzePathPatterns(modifiedFiles, repositoryFiles);

      assertThat(matches)
          .noneMatch(
              match ->
                  match.filePath().equals("src/main/java/com/example/service/UserService.java"));
    }
  }

  @Nested
  @DisplayName("Related Layer Detection")
  final class RelatedLayerDetection {

    @Test
    @DisplayName("should_find_repository_layer_for_service_modification")
    final void should_find_repository_layer_for_service_modification() {
      final List<GitFileModification> modifiedFiles =
          List.of(
              new GitFileModification(
                  "src/main/java/com/example/service/UserService.java",
                  "src/main/java/com/example/service/UserService.java"));

      final List<String> repositoryFiles =
          List.of(
              "src/main/java/com/example/service/UserService.java",
              "src/main/java/com/example/repository/UserRepository.java",
              "src/main/java/com/example/model/User.java");

      final List<ContextMatch> matches =
          analyzer.analyzePathPatterns(modifiedFiles, repositoryFiles);

      assertThat(matches)
          .anyMatch(
              match ->
                  match
                          .filePath()
                          .equals("src/main/java/com/example/repository/UserRepository.java")
                      && match.reason() == MatchReason.RELATED_LAYER);
    }

    @Test
    @DisplayName("should_find_controller_layer_for_service_modification")
    final void should_find_controller_layer_for_service_modification() {
      final List<GitFileModification> modifiedFiles =
          List.of(
              new GitFileModification(
                  "src/main/java/com/example/service/OrderService.java",
                  "src/main/java/com/example/service/OrderService.java"));

      final List<String> repositoryFiles =
          List.of(
              "src/main/java/com/example/service/OrderService.java",
              "src/main/java/com/example/controller/OrderController.java",
              "src/main/java/com/example/dto/OrderDto.java");

      final List<ContextMatch> matches =
          analyzer.analyzePathPatterns(modifiedFiles, repositoryFiles);

      assertThat(matches)
          .anyMatch(
              match ->
                  match
                          .filePath()
                          .equals("src/main/java/com/example/controller/OrderController.java")
                      && match.reason() == MatchReason.RELATED_LAYER);
    }

    @Test
    @DisplayName("should_find_model_layer_for_entity_modification")
    final void should_find_model_layer_for_entity_modification() {
      final List<GitFileModification> modifiedFiles =
          List.of(
              new GitFileModification(
                  "src/main/java/com/example/entity/Product.java",
                  "src/main/java/com/example/entity/Product.java"));

      final List<String> repositoryFiles =
          List.of(
              "src/main/java/com/example/entity/Product.java",
              "src/main/java/com/example/model/ProductModel.java",
              "src/main/java/com/example/dto/ProductDto.java");

      final List<ContextMatch> matches =
          analyzer.analyzePathPatterns(modifiedFiles, repositoryFiles);

      assertThat(matches)
          .anyMatch(
              match ->
                  match.filePath().equals("src/main/java/com/example/model/ProductModel.java")
                      && match.reason() == MatchReason.RELATED_LAYER);
    }
  }

  @Nested
  @DisplayName("Test Counterpart Detection")
  final class TestCounterpartDetection {

    @Test
    @DisplayName("should_find_test_file_for_production_code")
    final void should_find_test_file_for_production_code() {
      final List<GitFileModification> modifiedFiles =
          List.of(
              new GitFileModification(
                  "src/main/java/com/example/service/UserService.java",
                  "src/main/java/com/example/service/UserService.java"));

      final List<String> repositoryFiles =
          List.of(
              "src/main/java/com/example/service/UserService.java",
              "src/test/java/com/example/service/UserServiceTest.java");

      final List<ContextMatch> matches =
          analyzer.analyzePathPatterns(modifiedFiles, repositoryFiles);

      assertThat(matches)
          .anyMatch(
              match ->
                  match.filePath().equals("src/test/java/com/example/service/UserServiceTest.java")
                      && match.reason() == MatchReason.TEST_COUNTERPART);
    }

    @Test
    @DisplayName("should_find_production_file_for_test_code")
    final void should_find_production_file_for_test_code() {
      final List<GitFileModification> modifiedFiles =
          List.of(
              new GitFileModification(
                  "src/test/java/com/example/controller/OrderControllerTest.java",
                  "src/test/java/com/example/controller/OrderControllerTest.java"));

      final List<String> repositoryFiles =
          List.of(
              "src/test/java/com/example/controller/OrderControllerTest.java",
              "src/main/java/com/example/controller/OrderController.java");

      final List<ContextMatch> matches =
          analyzer.analyzePathPatterns(modifiedFiles, repositoryFiles);

      assertThat(matches)
          .anyMatch(
              match ->
                  match
                          .filePath()
                          .equals("src/main/java/com/example/controller/OrderController.java")
                      && match.reason() == MatchReason.TEST_COUNTERPART);
    }
  }

  @Nested
  @DisplayName("Parent Package Detection")
  final class ParentPackageDetection {

    @Test
    @DisplayName("should_find_parent_package_files")
    final void should_find_parent_package_files() {
      final List<GitFileModification> modifiedFiles =
          List.of(
              new GitFileModification(
                  "src/main/java/com/example/service/impl/UserServiceImpl.java",
                  "src/main/java/com/example/service/impl/UserServiceImpl.java"));

      final List<String> repositoryFiles =
          List.of(
              "src/main/java/com/example/service/impl/UserServiceImpl.java",
              "src/main/java/com/example/service/UserService.java",
              "src/main/java/com/example/service/ServiceConfig.java");

      final List<ContextMatch> matches =
          analyzer.analyzePathPatterns(modifiedFiles, repositoryFiles);

      assertThat(matches)
          .anyMatch(
              match ->
                  match.filePath().equals("src/main/java/com/example/service/UserService.java")
                      && match.reason() == MatchReason.PARENT_PACKAGE);
      assertThat(matches)
          .anyMatch(
              match ->
                  match.filePath().equals("src/main/java/com/example/service/ServiceConfig.java")
                      && match.reason() == MatchReason.PARENT_PACKAGE);
    }
  }

  @Nested
  @DisplayName("Multiple Patterns")
  final class MultiplePatterns {

    @Test
    @DisplayName("should_combine_multiple_pattern_matches")
    final void should_combine_multiple_pattern_matches() {
      final List<GitFileModification> modifiedFiles =
          List.of(
              new GitFileModification(
                  "src/main/java/com/example/service/UserService.java",
                  "src/main/java/com/example/service/UserService.java"));

      final List<String> repositoryFiles =
          List.of(
              "src/main/java/com/example/service/UserService.java",
              "src/main/java/com/example/service/OrderService.java",
              "src/main/java/com/example/repository/UserRepository.java",
              "src/test/java/com/example/service/UserServiceTest.java");

      final List<ContextMatch> matches =
          analyzer.analyzePathPatterns(modifiedFiles, repositoryFiles);

      assertThat(matches).hasSizeGreaterThanOrEqualTo(3);
      assertThat(matches).anyMatch(match -> match.reason() == MatchReason.SAME_PACKAGE);
      assertThat(matches).anyMatch(match -> match.reason() == MatchReason.RELATED_LAYER);
      assertThat(matches).anyMatch(match -> match.reason() == MatchReason.TEST_COUNTERPART);
    }

    @Test
    @DisplayName("should_return_empty_list_when_no_patterns_match")
    final void should_return_empty_list_when_no_patterns_match() {
      final List<GitFileModification> modifiedFiles =
          List.of(
              new GitFileModification(
                  "src/main/java/com/example/service/UserService.java",
                  "src/main/java/com/example/service/UserService.java"));

      final List<String> repositoryFiles =
          List.of("src/main/java/com/example/service/UserService.java", "README.md", "pom.xml");

      final List<ContextMatch> matches =
          analyzer.analyzePathPatterns(modifiedFiles, repositoryFiles);

      assertThat(matches).isEmpty();
    }
  }

  @Nested
  @DisplayName("Confidence Scoring")
  final class ConfidenceScoring {

    @Test
    @DisplayName("should_use_correct_base_confidence_for_each_reason")
    final void should_use_correct_base_confidence_for_each_reason() {
      final List<GitFileModification> modifiedFiles =
          List.of(
              new GitFileModification(
                  "src/main/java/com/example/service/UserService.java",
                  "src/main/java/com/example/service/UserService.java"));

      final List<String> repositoryFiles =
          List.of(
              "src/main/java/com/example/service/UserService.java",
              "src/main/java/com/example/service/OrderService.java",
              "src/main/java/com/example/repository/UserRepository.java",
              "src/test/java/com/example/service/UserServiceTest.java");

      final List<ContextMatch> matches =
          analyzer.analyzePathPatterns(modifiedFiles, repositoryFiles);

      final ContextMatch samePackageMatch =
          matches.stream()
              .filter(match -> match.reason() == MatchReason.SAME_PACKAGE)
              .findFirst()
              .orElseThrow();

      final ContextMatch relatedLayerMatch =
          matches.stream()
              .filter(match -> match.reason() == MatchReason.RELATED_LAYER)
              .findFirst()
              .orElseThrow();

      final ContextMatch testCounterpartMatch =
          matches.stream()
              .filter(match -> match.reason() == MatchReason.TEST_COUNTERPART)
              .findFirst()
              .orElseThrow();

      assertThat(samePackageMatch.confidence())
          .isEqualTo(MatchReason.SAME_PACKAGE.getBaseConfidence());
      assertThat(relatedLayerMatch.confidence())
          .isEqualTo(MatchReason.RELATED_LAYER.getBaseConfidence());
      assertThat(testCounterpartMatch.confidence())
          .isEqualTo(MatchReason.TEST_COUNTERPART.getBaseConfidence());
    }
  }
}
