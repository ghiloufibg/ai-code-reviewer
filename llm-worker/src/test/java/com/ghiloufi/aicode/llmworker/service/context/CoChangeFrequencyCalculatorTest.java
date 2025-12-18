package com.ghiloufi.aicode.llmworker.service.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.CoChangeMetrics;
import com.ghiloufi.aicode.core.domain.model.CommitInfo;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CoChangeFrequencyCalculator Tests")
final class CoChangeFrequencyCalculatorTest {

  private CoChangeFrequencyCalculator calculator;

  @BeforeEach
  final void setUp() {
    calculator = new CoChangeFrequencyCalculator();
  }

  @Nested
  @DisplayName("Frequency Calculation")
  final class FrequencyCalculation {

    @Test
    @DisplayName("should_calculate_cochange_frequency_from_commits")
    final void should_calculate_cochange_frequency_from_commits() {
      final String targetFile = "src/UserService.java";

      final List<CommitInfo> commits =
          List.of(
              new CommitInfo(
                  "commit1",
                  "msg",
                  "author",
                  Instant.now(),
                  List.of("src/UserService.java", "src/UserRepository.java")),
              new CommitInfo(
                  "commit2",
                  "msg",
                  "author",
                  Instant.now(),
                  List.of(
                      "src/UserService.java",
                      "src/UserRepository.java",
                      "src/UserController.java")),
              new CommitInfo(
                  "commit3",
                  "msg",
                  "author",
                  Instant.now(),
                  List.of("src/UserService.java", "src/UserMapper.java")));

      final Map<String, Integer> frequency = calculator.calculateFrequency(targetFile, commits);

      assertThat(frequency).containsEntry("src/UserRepository.java", 2);
      assertThat(frequency).containsEntry("src/UserController.java", 1);
      assertThat(frequency).containsEntry("src/UserMapper.java", 1);
      assertThat(frequency).doesNotContainKey("src/UserService.java");
    }

    @Test
    @DisplayName("should_return_empty_map_when_no_commits")
    final void should_return_empty_map_when_no_commits() {
      final String targetFile = "src/UserService.java";

      final Map<String, Integer> frequency = calculator.calculateFrequency(targetFile, List.of());

      assertThat(frequency).isEmpty();
    }

    @Test
    @DisplayName("should_exclude_target_file_from_results")
    final void should_exclude_target_file_from_results() {
      final String targetFile = "src/UserService.java";

      final List<CommitInfo> commits =
          List.of(
              new CommitInfo(
                  "commit1",
                  "msg",
                  "author",
                  Instant.now(),
                  List.of("src/UserService.java", "src/UserRepository.java")));

      final Map<String, Integer> frequency = calculator.calculateFrequency(targetFile, commits);

      assertThat(frequency).doesNotContainKey("src/UserService.java");
    }

    @Test
    @DisplayName("should_handle_commits_not_touching_target_file")
    final void should_handle_commits_not_touching_target_file() {
      final String targetFile = "src/UserService.java";

      final List<CommitInfo> commits =
          List.of(
              new CommitInfo(
                  "commit1",
                  "msg",
                  "author",
                  Instant.now(),
                  List.of("src/UserService.java", "src/UserRepository.java")),
              new CommitInfo(
                  "commit2", "msg", "author", Instant.now(), List.of("src/OtherFile.java")),
              new CommitInfo(
                  "commit3",
                  "msg",
                  "author",
                  Instant.now(),
                  List.of("src/UserService.java", "src/UserController.java")));

      final Map<String, Integer> frequency = calculator.calculateFrequency(targetFile, commits);

      assertThat(frequency).hasSize(2);
      assertThat(frequency).containsEntry("src/UserRepository.java", 1);
      assertThat(frequency).containsEntry("src/UserController.java", 1);
      assertThat(frequency).doesNotContainKey("src/OtherFile.java");
    }
  }

  @Nested
  @DisplayName("Normalization")
  final class Normalization {

    @Test
    @DisplayName("should_normalize_frequency_to_range_0_to_1")
    final void should_normalize_frequency_to_range_0_to_1() {
      final Map<String, Integer> frequency =
          Map.of(
              "fileA.java", 20,
              "fileB.java", 10,
              "fileC.java", 5);

      final List<CoChangeMetrics> metrics = calculator.normalizeFrequency(frequency);

      assertThat(metrics).hasSize(3);

      final CoChangeMetrics fileA =
          metrics.stream().filter(m -> m.filePath().equals("fileA.java")).findFirst().orElseThrow();
      final CoChangeMetrics fileB =
          metrics.stream().filter(m -> m.filePath().equals("fileB.java")).findFirst().orElseThrow();
      final CoChangeMetrics fileC =
          metrics.stream().filter(m -> m.filePath().equals("fileC.java")).findFirst().orElseThrow();

      assertThat(fileA.normalizedFrequency()).isEqualTo(1.0);
      assertThat(fileB.normalizedFrequency()).isEqualTo(0.5);
      assertThat(fileC.normalizedFrequency()).isEqualTo(0.25);
    }

    @Test
    @DisplayName("should_handle_single_entry_normalization")
    final void should_handle_single_entry_normalization() {
      final Map<String, Integer> frequency = Map.of("fileA.java", 5);

      final List<CoChangeMetrics> metrics = calculator.normalizeFrequency(frequency);

      assertThat(metrics).hasSize(1);
      assertThat(metrics.get(0).normalizedFrequency()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should_return_empty_list_for_empty_frequency")
    final void should_return_empty_list_for_empty_frequency() {
      final List<CoChangeMetrics> metrics = calculator.normalizeFrequency(Map.of());

      assertThat(metrics).isEmpty();
    }
  }

  @Nested
  @DisplayName("Integration")
  final class Integration {

    @Test
    @DisplayName("should_calculate_and_normalize_in_one_step")
    final void should_calculate_and_normalize_in_one_step() {
      final String targetFile = "src/UserService.java";

      final List<CommitInfo> commits =
          List.of(
              new CommitInfo(
                  "c1",
                  "msg",
                  "author",
                  Instant.now(),
                  List.of("src/UserService.java", "src/UserRepository.java")),
              new CommitInfo(
                  "c2",
                  "msg",
                  "author",
                  Instant.now(),
                  List.of("src/UserService.java", "src/UserRepository.java")),
              new CommitInfo(
                  "c3",
                  "msg",
                  "author",
                  Instant.now(),
                  List.of("src/UserService.java", "src/UserController.java")));

      final List<CoChangeMetrics> metrics = calculator.calculateAndNormalize(targetFile, commits);

      assertThat(metrics).hasSize(2);

      final CoChangeMetrics repo =
          metrics.stream()
              .filter(m -> m.filePath().equals("src/UserRepository.java"))
              .findFirst()
              .orElseThrow();

      final CoChangeMetrics controller =
          metrics.stream()
              .filter(m -> m.filePath().equals("src/UserController.java"))
              .findFirst()
              .orElseThrow();

      assertThat(repo.coChangeCount()).isEqualTo(2);
      assertThat(repo.normalizedFrequency()).isEqualTo(1.0);

      assertThat(controller.coChangeCount()).isEqualTo(1);
      assertThat(controller.normalizedFrequency()).isEqualTo(0.5);
    }
  }
}
