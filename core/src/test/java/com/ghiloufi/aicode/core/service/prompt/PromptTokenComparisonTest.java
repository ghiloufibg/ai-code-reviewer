package com.ghiloufi.aicode.core.service.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.config.FeaturesConfiguration;
import com.ghiloufi.aicode.core.config.OptimizedPromptProperties;
import com.ghiloufi.aicode.core.config.PromptProperties;
import com.ghiloufi.aicode.core.config.PromptPropertiesFactory;
import com.ghiloufi.aicode.core.service.prompt.TokenCounter.TokenComparison;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@DisplayName("Prompt Token Comparison Tests")
@SpringBootTest(
    classes = {
      FeaturesConfiguration.class,
      PromptTemplateService.class,
      PromptPropertiesFactory.class,
      TokenCounter.class
    })
final class PromptTokenComparisonTest {

  @Autowired private PromptProperties currentPromptProperties;
  @Autowired private OptimizedPromptProperties optimizedPromptProperties;
  @Autowired private TokenCounter tokenCounter;

  private String currentSystemPrompt;
  private String optimizedSystemPrompt;

  @BeforeEach
  void setUp() {
    currentSystemPrompt = currentPromptProperties.getSystem();
    optimizedSystemPrompt = optimizedPromptProperties.getSystem();
  }

  @Test
  @DisplayName("should_have_comparable_system_prompt_tokens")
  void should_have_comparable_system_prompt_tokens() {
    final TokenComparison comparison =
        tokenCounter.comparePrompts(currentSystemPrompt, optimizedSystemPrompt);

    assertThat(comparison.optimized().tokens())
        .as("Optimized system prompt should be reasonably sized (under 500 tokens)")
        .isLessThan(500);
  }

  @Test
  @DisplayName("should_achieve_token_reduction_in_confidence_instructions")
  void should_achieve_token_reduction_in_confidence_instructions() {
    final String currentConfidence = currentPromptProperties.getConfidence();
    final String optimizedConfidence = optimizedPromptProperties.getConfidence();

    final TokenComparison comparison =
        tokenCounter.comparePrompts(currentConfidence, optimizedConfidence);

    assertThat(comparison.reductionPercentage())
        .as("Confidence instructions should achieve significant token reduction")
        .isGreaterThanOrEqualTo(20.0);
  }

  @Test
  @DisplayName("should_achieve_overall_token_reduction_across_all_prompts")
  void should_achieve_overall_token_reduction_across_all_prompts() {
    final String currentComplete = buildCompletePrompt(currentPromptProperties);
    final String optimizedComplete = buildCompletePrompt(optimizedPromptProperties);

    final TokenComparison comparison =
        tokenCounter.comparePrompts(currentComplete, optimizedComplete);

    assertThat(comparison.reductionPercentage())
        .as("Complete prompt should achieve at least 30%% overall token reduction")
        .isGreaterThanOrEqualTo(30.0);

    assertThat(comparison.tokenReduction())
        .as("Should reduce by at least 300 tokens")
        .isGreaterThanOrEqualTo(300);
  }

  @Test
  @DisplayName("should_report_token_counts_for_current_prompts")
  void should_report_token_counts_for_current_prompts() {
    final String currentComplete = buildCompletePrompt(currentPromptProperties);
    final TokenCounter.TokenCount count = tokenCounter.countTokens(currentComplete);

    assertThat(count.tokens())
        .as("Current prompt token count should be recorded for comparison")
        .isGreaterThan(1000);
  }

  @Test
  @DisplayName("should_report_token_counts_for_optimized_prompts")
  void should_report_token_counts_for_optimized_prompts() {
    final String optimizedComplete = buildCompletePrompt(optimizedPromptProperties);
    final TokenCounter.TokenCount count = tokenCounter.countTokens(optimizedComplete);

    assertThat(count.tokens()).as("Optimized prompt should be reasonably sized").isLessThan(1500);
  }

  @Test
  @DisplayName("should_display_detailed_comparison_summary")
  void should_display_detailed_comparison_summary() {
    final String currentComplete = buildCompletePrompt(currentPromptProperties);
    final String optimizedComplete = buildCompletePrompt(optimizedPromptProperties);

    final TokenComparison comparison =
        tokenCounter.comparePrompts(currentComplete, optimizedComplete);

    final String summary = comparison.formatSummary();

    assertThat(summary).contains("Current:", "Optimized:", "Reduction:");
    assertThat(summary).containsPattern("\\d+%");
  }

  @Test
  @DisplayName("should_validate_optimized_prompt_is_not_empty")
  void should_validate_optimized_prompt_is_not_empty() {
    assertThat(optimizedSystemPrompt).isNotEmpty();
    assertThat(optimizedPromptProperties.getConfidence()).isNotEmpty();
    assertThat(optimizedPromptProperties.getSchema()).isNotEmpty();
    assertThat(optimizedPromptProperties.getOutputRequirements()).isNotEmpty();
  }

  @Test
  @DisplayName("should_validate_schema_is_simplified_but_compatible")
  void should_validate_schema_is_simplified_but_compatible() {
    final String currentSchema = currentPromptProperties.getSchema();
    final String optimizedSchema = optimizedPromptProperties.getSchema();

    assertThat(optimizedSchema)
        .as("Optimized schema should be simpler than current")
        .hasSizeLessThan(currentSchema.length());

    assertThat(optimizedSchema)
        .as("Optimized schema should contain required fields")
        .contains("summary", "issues", "non_blocking_notes");
  }

  private String buildCompletePrompt(final PromptProperties properties) {
    return String.join(
        "\n\n",
        properties.getSystem(),
        properties.getConfidence(),
        properties.getSchema(),
        properties.getOutputRequirements());
  }

  private String buildCompletePrompt(final OptimizedPromptProperties properties) {
    return String.join(
        "\n\n",
        properties.getSystem(),
        properties.getConfidence(),
        properties.getSchema(),
        properties.getOutputRequirements());
  }
}
