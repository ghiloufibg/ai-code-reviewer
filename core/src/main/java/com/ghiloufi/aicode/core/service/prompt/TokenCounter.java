package com.ghiloufi.aicode.core.service.prompt;

import org.springframework.stereotype.Component;

@Component
public class TokenCounter {

  private static final double AVERAGE_CHARS_PER_TOKEN = 4.0;

  public TokenCount countTokens(final String text) {
    if (text == null || text.isEmpty()) {
      return new TokenCount(0, 0);
    }

    final int characterCount = text.length();
    final int estimatedTokens = (int) Math.ceil(characterCount / AVERAGE_CHARS_PER_TOKEN);

    return new TokenCount(characterCount, estimatedTokens);
  }

  public TokenComparison comparePrompts(final String currentPrompt, final String optimizedPrompt) {
    final TokenCount currentCount = countTokens(currentPrompt);
    final TokenCount optimizedCount = countTokens(optimizedPrompt);

    final int tokenReduction = currentCount.tokens() - optimizedCount.tokens();
    final double reductionPercentage =
        currentCount.tokens() > 0 ? ((double) tokenReduction / currentCount.tokens()) * 100 : 0.0;

    return new TokenComparison(currentCount, optimizedCount, tokenReduction, reductionPercentage);
  }

  public record TokenCount(int characters, int tokens) {
    public String formatSummary() {
      return String.format("Characters: %,d | Estimated tokens: %,d", characters, tokens);
    }
  }

  public record TokenComparison(
      TokenCount current, TokenCount optimized, int tokenReduction, double reductionPercentage) {
    public String formatSummary() {
      return String.format(
          """
          Current:   %s
          Optimized: %s
          Reduction: %,d tokens (%.1f%%)
          """,
          current.formatSummary(), optimized.formatSummary(), tokenReduction, reductionPercentage);
    }

    public boolean meetsTarget(final double targetPercentage) {
      return reductionPercentage >= targetPercentage;
    }
  }
}
