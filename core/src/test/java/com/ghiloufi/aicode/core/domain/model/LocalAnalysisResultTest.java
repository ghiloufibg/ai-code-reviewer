package com.ghiloufi.aicode.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class LocalAnalysisResultTest {

  private static final AnalysisMetadata SAMPLE_METADATA =
      AnalysisMetadata.started("image:latest", "container-123", "main", "abc123");

  @Test
  void should_create_empty_result() {
    final var result = LocalAnalysisResult.empty(SAMPLE_METADATA);

    assertThat(result.testResults()).isEmpty();
    assertThat(result.metadata()).isEqualTo(SAMPLE_METADATA);
  }

  @Test
  void should_create_result_with_test_results() {
    final var tests =
        List.of(TestResult.passed("c", "m1", null), TestResult.failed("c", "m2", "error", null));

    final var result = LocalAnalysisResult.withTestResults(tests, SAMPLE_METADATA);

    assertThat(result.testResults()).hasSize(2);
    assertThat(result.metadata()).isEqualTo(SAMPLE_METADATA);
  }

  @Test
  void should_throw_when_metadata_is_null() {
    assertThatThrownBy(() -> new LocalAnalysisResult(List.of(), null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("metadata");
  }

  @Test
  void should_handle_null_test_results_list() {
    final var result = new LocalAnalysisResult(null, SAMPLE_METADATA);

    assertThat(result.testResults()).isEmpty();
  }

  @Test
  void should_count_passed_tests() {
    final var tests =
        List.of(
            TestResult.passed("c", "m1", null),
            TestResult.passed("c", "m2", null),
            TestResult.failed("c", "m3", "msg", null));

    final var result = LocalAnalysisResult.withTestResults(tests, SAMPLE_METADATA);

    assertThat(result.passedTestCount()).isEqualTo(2);
  }

  @Test
  void should_count_failed_tests() {
    final var tests =
        List.of(
            TestResult.passed("c", "m1", null),
            TestResult.failed("c", "m2", "msg", null),
            TestResult.error("c", "m3", "msg", null));

    final var result = LocalAnalysisResult.withTestResults(tests, SAMPLE_METADATA);

    assertThat(result.failedTestCount()).isEqualTo(2);
  }

  @Test
  void should_count_skipped_tests() {
    final var tests =
        List.of(
            TestResult.passed("c", "m1", null),
            TestResult.skipped("c", "m2", null),
            TestResult.skipped("c", "m3", null));

    final var result = LocalAnalysisResult.withTestResults(tests, SAMPLE_METADATA);

    assertThat(result.skippedTestCount()).isEqualTo(2);
  }

  @Test
  void should_count_total_tests() {
    final var tests =
        List.of(
            TestResult.passed("c", "m1", null),
            TestResult.failed("c", "m2", "msg", null),
            TestResult.skipped("c", "m3", null));

    final var result = LocalAnalysisResult.withTestResults(tests, SAMPLE_METADATA);

    assertThat(result.totalTestCount()).isEqualTo(3);
  }

  @Test
  void should_detect_test_failures() {
    final var withFailures =
        LocalAnalysisResult.withTestResults(
            List.of(TestResult.failed("c", "m", "msg", null)), SAMPLE_METADATA);
    final var allPassed =
        LocalAnalysisResult.withTestResults(
            List.of(TestResult.passed("c", "m", null)), SAMPLE_METADATA);
    final var empty = LocalAnalysisResult.empty(SAMPLE_METADATA);

    assertThat(withFailures.hasTestFailures()).isTrue();
    assertThat(allPassed.hasTestFailures()).isFalse();
    assertThat(empty.hasTestFailures()).isFalse();
  }

  @Test
  void should_detect_all_tests_passed() {
    final var allPassed =
        LocalAnalysisResult.withTestResults(
            List.of(TestResult.passed("c", "m1", null), TestResult.passed("c", "m2", null)),
            SAMPLE_METADATA);
    final var withFailure =
        LocalAnalysisResult.withTestResults(
            List.of(TestResult.passed("c", "m1", null), TestResult.failed("c", "m2", "msg", null)),
            SAMPLE_METADATA);
    final var empty = LocalAnalysisResult.empty(SAMPLE_METADATA);

    assertThat(allPassed.allTestsPassed()).isTrue();
    assertThat(withFailure.allTestsPassed()).isFalse();
    assertThat(empty.allTestsPassed()).isFalse();
  }

  @Test
  void should_return_failed_tests() {
    final var tests =
        List.of(
            TestResult.passed("c", "m1", null),
            TestResult.failed("c", "m2", "msg1", null),
            TestResult.error("c", "m3", "msg2", null),
            TestResult.skipped("c", "m4", null));

    final var result = LocalAnalysisResult.withTestResults(tests, SAMPLE_METADATA);
    final var failedTests = result.failedTests();

    assertThat(failedTests).hasSize(2);
    assertThat(failedTests).extracting(TestResult::testMethod).containsExactly("m2", "m3");
  }

  @Test
  void should_merge_results() {
    final var result1 =
        LocalAnalysisResult.withTestResults(
            List.of(TestResult.passed("c", "m1", null)), SAMPLE_METADATA);
    final var result2 =
        LocalAnalysisResult.withTestResults(
            List.of(TestResult.passed("c", "m2", null), TestResult.failed("c", "m3", "msg", null)),
            SAMPLE_METADATA);

    final var merged = result1.merge(result2);

    assertThat(merged.testResults()).hasSize(3);
    assertThat(merged.metadata()).isEqualTo(SAMPLE_METADATA);
  }

  @Test
  void should_make_defensive_copies() {
    final var mutableTests = new ArrayList<>(List.of(TestResult.passed("c", "m", null)));

    final var result = new LocalAnalysisResult(mutableTests, SAMPLE_METADATA);

    mutableTests.clear();

    assertThat(result.testResults()).hasSize(1);
  }
}
