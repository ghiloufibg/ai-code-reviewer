package com.ghiloufi.aicode.core.domain.model.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ReviewStatus Enum Tests")
final class ReviewStatusTest {

  @Nested
  @DisplayName("Enum Values")
  final class EnumValuesTests {

    @Test
    @DisplayName("should_have_all_expected_status_values")
    final void should_have_all_expected_status_values() {
      assertThat(ReviewStatus.values())
          .containsExactly(
              ReviewStatus.PENDING,
              ReviewStatus.PROCESSING,
              ReviewStatus.COMPLETED,
              ReviewStatus.FAILED);
    }

    @Test
    @DisplayName("should_have_exactly_four_values")
    final void should_have_exactly_four_values() {
      assertThat(ReviewStatus.values()).hasSize(4);
    }
  }

  @Nested
  @DisplayName("Value Of")
  final class ValueOfTests {

    @Test
    @DisplayName("should_parse_pending_from_string")
    final void should_parse_pending_from_string() {
      assertThat(ReviewStatus.valueOf("PENDING")).isEqualTo(ReviewStatus.PENDING);
    }

    @Test
    @DisplayName("should_parse_processing_from_string")
    final void should_parse_processing_from_string() {
      assertThat(ReviewStatus.valueOf("PROCESSING")).isEqualTo(ReviewStatus.PROCESSING);
    }

    @Test
    @DisplayName("should_parse_completed_from_string")
    final void should_parse_completed_from_string() {
      assertThat(ReviewStatus.valueOf("COMPLETED")).isEqualTo(ReviewStatus.COMPLETED);
    }

    @Test
    @DisplayName("should_parse_failed_from_string")
    final void should_parse_failed_from_string() {
      assertThat(ReviewStatus.valueOf("FAILED")).isEqualTo(ReviewStatus.FAILED);
    }

    @Test
    @DisplayName("should_throw_for_invalid_value")
    final void should_throw_for_invalid_value() {
      assertThatThrownBy(() -> ReviewStatus.valueOf("INVALID"))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should_be_case_sensitive")
    final void should_be_case_sensitive() {
      assertThatThrownBy(() -> ReviewStatus.valueOf("pending"))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("Name Conversion")
  final class NameConversionTests {

    @Test
    @DisplayName("should_convert_pending_to_string")
    final void should_convert_pending_to_string() {
      assertThat(ReviewStatus.PENDING.name()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("should_convert_processing_to_string")
    final void should_convert_processing_to_string() {
      assertThat(ReviewStatus.PROCESSING.name()).isEqualTo("PROCESSING");
    }

    @Test
    @DisplayName("should_convert_completed_to_string")
    final void should_convert_completed_to_string() {
      assertThat(ReviewStatus.COMPLETED.name()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("should_convert_failed_to_string")
    final void should_convert_failed_to_string() {
      assertThat(ReviewStatus.FAILED.name()).isEqualTo("FAILED");
    }
  }

  @Nested
  @DisplayName("Ordinal Values")
  final class OrdinalValuesTests {

    @Test
    @DisplayName("should_have_pending_as_first_ordinal")
    final void should_have_pending_as_first_ordinal() {
      assertThat(ReviewStatus.PENDING.ordinal()).isEqualTo(0);
    }

    @Test
    @DisplayName("should_have_processing_as_second_ordinal")
    final void should_have_processing_as_second_ordinal() {
      assertThat(ReviewStatus.PROCESSING.ordinal()).isEqualTo(1);
    }

    @Test
    @DisplayName("should_have_completed_as_third_ordinal")
    final void should_have_completed_as_third_ordinal() {
      assertThat(ReviewStatus.COMPLETED.ordinal()).isEqualTo(2);
    }

    @Test
    @DisplayName("should_have_failed_as_fourth_ordinal")
    final void should_have_failed_as_fourth_ordinal() {
      assertThat(ReviewStatus.FAILED.ordinal()).isEqualTo(3);
    }
  }
}
