package com.ghiloufi.aicode.core.service.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.service.prompt.TokenCounter.TokenComparison;
import com.ghiloufi.aicode.core.service.prompt.TokenCounter.TokenCount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TokenCounter Tests")
final class TokenCounterTest {

  private TokenCounter tokenCounter;

  @BeforeEach
  void setUp() {
    tokenCounter = new TokenCounter();
  }

  @Test
  @DisplayName("should_count_tokens_for_simple_text")
  void should_count_tokens_for_simple_text() {
    final String text = "Hello World";
    final TokenCount count = tokenCounter.countTokens(text);

    assertThat(count.characters()).isEqualTo(11);
    assertThat(count.tokens()).isEqualTo(3);
  }

  @Test
  @DisplayName("should_return_zero_for_empty_text")
  void should_return_zero_for_empty_text() {
    final TokenCount count = tokenCounter.countTokens("");

    assertThat(count.characters()).isZero();
    assertThat(count.tokens()).isZero();
  }

  @Test
  @DisplayName("should_return_zero_for_null_text")
  void should_return_zero_for_null_text() {
    final TokenCount count = tokenCounter.countTokens(null);

    assertThat(count.characters()).isZero();
    assertThat(count.tokens()).isZero();
  }

  @Test
  @DisplayName("should_estimate_tokens_based_on_character_count")
  void should_estimate_tokens_based_on_character_count() {
    final String text = "a".repeat(100);
    final TokenCount count = tokenCounter.countTokens(text);

    assertThat(count.characters()).isEqualTo(100);
    assertThat(count.tokens()).isEqualTo(25);
  }

  @Test
  @DisplayName("should_format_token_count_summary")
  void should_format_token_count_summary() {
    final TokenCount count = new TokenCount(1000, 250);
    final String summary = count.formatSummary();

    assertThat(summary).containsPattern("1.000");
    assertThat(summary).contains("250");
    assertThat(summary).contains("Characters:");
    assertThat(summary).contains("tokens:");
  }

  @Test
  @DisplayName("should_compare_current_and_optimized_prompts")
  void should_compare_current_and_optimized_prompts() {
    final String currentPrompt = "a".repeat(1000);
    final String optimizedPrompt = "a".repeat(500);

    final TokenComparison comparison = tokenCounter.comparePrompts(currentPrompt, optimizedPrompt);

    assertThat(comparison.current().characters()).isEqualTo(1000);
    assertThat(comparison.optimized().characters()).isEqualTo(500);
    assertThat(comparison.tokenReduction()).isEqualTo(125);
    assertThat(comparison.reductionPercentage()).isEqualTo(50.0);
  }

  @Test
  @DisplayName("should_calculate_reduction_percentage_correctly")
  void should_calculate_reduction_percentage_correctly() {
    final String currentPrompt = "a".repeat(1380);
    final String optimizedPrompt = "a".repeat(828);

    final TokenComparison comparison = tokenCounter.comparePrompts(currentPrompt, optimizedPrompt);

    assertThat(comparison.reductionPercentage())
        .isCloseTo(40.0, org.assertj.core.data.Offset.offset(0.1));
  }

  @Test
  @DisplayName("should_detect_when_reduction_meets_target")
  void should_detect_when_reduction_meets_target() {
    final String currentPrompt = "a".repeat(1000);
    final String optimizedPrompt = "a".repeat(500);

    final TokenComparison comparison = tokenCounter.comparePrompts(currentPrompt, optimizedPrompt);

    assertThat(comparison.meetsTarget(40.0)).isTrue();
    assertThat(comparison.meetsTarget(50.0)).isTrue();
    assertThat(comparison.meetsTarget(60.0)).isFalse();
  }

  @Test
  @DisplayName("should_format_comparison_summary")
  void should_format_comparison_summary() {
    final String currentPrompt = "a".repeat(1000);
    final String optimizedPrompt = "a".repeat(600);

    final TokenComparison comparison = tokenCounter.comparePrompts(currentPrompt, optimizedPrompt);
    final String summary = comparison.formatSummary();

    assertThat(summary).contains("Current:");
    assertThat(summary).contains("Optimized:");
    assertThat(summary).contains("Reduction:");
    assertThat(summary).containsPattern("1.000");
    assertThat(summary).contains("600");
    assertThat(summary).containsPattern("40.0%");
  }

  @Test
  @DisplayName("should_handle_zero_current_tokens_without_division_by_zero")
  void should_handle_zero_current_tokens_without_division_by_zero() {
    final TokenComparison comparison = tokenCounter.comparePrompts("", "test");

    assertThat(comparison.reductionPercentage()).isZero();
  }

  @Test
  @DisplayName("should_calculate_negative_reduction_when_optimized_is_larger")
  void should_calculate_negative_reduction_when_optimized_is_larger() {
    final String currentPrompt = "a".repeat(100);
    final String optimizedPrompt = "a".repeat(200);

    final TokenComparison comparison = tokenCounter.comparePrompts(currentPrompt, optimizedPrompt);

    assertThat(comparison.tokenReduction()).isNegative();
    assertThat(comparison.reductionPercentage()).isNegative();
    assertThat(comparison.meetsTarget(40.0)).isFalse();
  }
}
