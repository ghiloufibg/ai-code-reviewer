package com.ghiloufi.aicode.infrastructure.adapter.output.external.llm;

import com.ghiloufi.aicode.application.port.output.AIAnalysisPort;
import com.ghiloufi.aicode.client.llm.LlmClient;
import com.ghiloufi.aicode.client.llm.PromptBuilder;
import com.ghiloufi.aicode.domain.entity.AnalysisResult;
import com.ghiloufi.aicode.domain.entity.DiffAnalysis;
import com.ghiloufi.aicode.domain.value.ReviewConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Infrastructure adapter that implements AIAnalysisPort using the existing LlmClient.
 *
 * <p>This adapter bridges the Clean Architecture ports with the existing LLM infrastructure,
 * providing AI-powered code analysis capabilities.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LlmAnalysisAdapter implements AIAnalysisPort {

  private final LlmClient llmClient;
  private final PromptBuilder promptBuilder;

  @Override
  public Mono<AnalysisResult> analyze(
      DiffAnalysis diffAnalysis, ReviewConfiguration configuration) {
    log.info("Starting AI analysis using LLM client with model: {}", configuration.model());

    return Mono.fromCallable(() -> buildAnalysisPrompt(diffAnalysis, configuration))
        .flatMap(
            prompt ->
                llmClient
                    .review(promptBuilder.getSystemPrompt(), prompt)
                    .map(response -> parseAnalysisResponse(response, diffAnalysis)))
        .doOnSuccess(
            result ->
                log.info(
                    "AI analysis completed with {} issues found",
                    result.getIssues() != null ? result.getIssues().size() : 0))
        .doOnError(error -> log.error("AI analysis failed for diff analysis", error));
  }

  @Override
  public boolean isAvailable() {
    try {
      // Simple availability check - could be enhanced with actual LLM ping
      return llmClient != null;
    } catch (Exception e) {
      log.warn("LLM client availability check failed", e);
      return false;
    }
  }

  @Override
  public String getModelInfo() {
    // Return model information from configuration
    return "LangChain4j-powered LLM";
  }

  /** Builds the analysis prompt for the LLM. */
  private String buildAnalysisPrompt(DiffAnalysis diffAnalysis, ReviewConfiguration configuration) {
    return promptBuilder.buildUserMessage(
        "unknown-repo", // repository name not available in DiffAnalysis
        "main", // default branch
        configuration.javaVersion(),
        configuration.buildSystem(),
        diffAnalysis.getRawDiff(),
        null, // static analysis reports
        null, // project configuration
        null // test status
        );
  }

  /** Parses the LLM response into an AnalysisResult. */
  private AnalysisResult parseAnalysisResponse(String llmResponse, DiffAnalysis diffAnalysis) {
    try {
      // For now, create a simplified AnalysisResult
      // This should be enhanced to properly parse the JSON response from LLM
      return new AnalysisResult(
          AnalysisResult.AnalysisType.LLM_ANALYSIS,
          "AI analysis completed",
          java.util.List.of(), // Would parse issues from JSON response
          java.util.List.of("Analysis performed using LangChain4j LLM"),
          getModelInfo());
    } catch (Exception e) {
      log.error("Failed to parse LLM response", e);
      throw new RuntimeException("Failed to parse AI analysis response", e);
    }
  }
}
