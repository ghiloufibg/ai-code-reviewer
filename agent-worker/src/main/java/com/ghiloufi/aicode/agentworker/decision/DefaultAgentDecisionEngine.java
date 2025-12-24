package com.ghiloufi.aicode.agentworker.decision;

import com.ghiloufi.aicode.agentworker.config.AgentWorkerProperties;
import com.ghiloufi.aicode.core.domain.model.AgentAction;
import com.ghiloufi.aicode.core.domain.model.AgentConfiguration;
import com.ghiloufi.aicode.core.domain.model.AgentState;
import com.ghiloufi.aicode.core.domain.model.PrioritizedFindings;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.model.Suggestion;
import com.ghiloufi.aicode.core.domain.port.output.AgentDecisionEnginePort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultAgentDecisionEngine implements AgentDecisionEnginePort {

  private static final int MAX_REASONING_ITERATIONS = 3;
  private static final double MIN_CONFIDENCE_THRESHOLD = 0.7;

  private final AgentWorkerProperties properties;

  @Override
  public Mono<AgentAction> decideNextAction(final AgentState state, final AnalysisContext context) {
    return Mono.fromSupplier(() -> determineNextAction(state, context))
        .doOnNext(
            action ->
                log.debug(
                    "Decision engine selected action: {} for state: {}",
                    action.actionType(),
                    state.status()));
  }

  @Override
  public Mono<PrioritizedFindings> prioritizeFindings(
      final List<ReviewResult.Issue> findings, final AgentConfiguration config) {
    return Mono.fromSupplier(
        () -> {
          final var minConfidence = config.aggregation().minConfidence();
          final var maxIssuesPerFile = config.aggregation().maxIssuesPerFile();

          log.debug(
              "Prioritizing {} findings with minConfidence={}, maxIssuesPerFile={}",
              findings.size(),
              minConfidence,
              maxIssuesPerFile);

          return PrioritizedFindings.fromIssues(findings, minConfidence, maxIssuesPerFile);
        });
  }

  @Override
  public Mono<Boolean> shouldContinueAnalysis(
      final AgentState state, final AgentConfiguration config) {
    return Mono.fromSupplier(
        () -> {
          if (state.isTerminal()) {
            log.debug("State is terminal, stopping analysis");
            return false;
          }

          final var completedIterations = countReasoningIterations(state);
          final var maxIterations = resolveMaxIterations();

          if (completedIterations >= maxIterations) {
            log.debug(
                "Max reasoning iterations reached ({}/{}), stopping analysis",
                completedIterations,
                maxIterations);
            return false;
          }

          if (state.hasLlmResults() && state.hasAnalysisResults()) {
            final var confidence = calculateCurrentConfidence(state);
            final var minThreshold = resolveMinConfidenceThreshold();

            if (confidence >= minThreshold) {
              log.debug(
                  "Confidence threshold met ({:.2f} >= {:.2f}), stopping analysis",
                  confidence,
                  minThreshold);
              return false;
            }

            log.debug(
                "Confidence below threshold ({:.2f} < {:.2f}), continuing analysis",
                confidence,
                minThreshold);
            return true;
          }

          return !hasCompletedRequiredActions(state);
        });
  }

  @Override
  public Mono<List<Suggestion>> generateSuggestions(final List<ReviewResult.Issue> findings) {
    return Mono.fromSupplier(
        () -> {
          log.debug("Generating suggestions for {} findings", findings.size());

          return findings.stream()
              .filter(this::isActionableFinding)
              .map(this::toSuggestion)
              .toList();
        });
  }

  private AgentAction determineNextAction(final AgentState state, final AnalysisContext context) {
    final var status = state.status();

    return switch (status) {
      case PENDING ->
          AgentAction.CloneRepository.started(
              context.repositoryPath(), context.baseBranch(), context.repositoryPath());

      case CLONING -> {
        if (state.hasCompletedAction("CLONE_REPOSITORY")) {
          yield determinePostCloneAction(state, context);
        }
        yield AgentAction.Terminate.failure("Clone action not completed but status is CLONING");
      }

      case ANALYZING -> {
        if (context.testsEnabled() && !context.testsExecuted()) {
          yield AgentAction.RunTests.started("auto-detect");
        }
        yield AgentAction.InvokeLlmReview.started(resolveProvider(), resolveModel());
      }

      case REASONING -> {
        if (state.hasLlmResults()) {
          yield AgentAction.PublishInlineComments.started();
        }
        yield AgentAction.InvokeLlmReview.started(resolveProvider(), resolveModel());
      }

      case PUBLISHING -> {
        if (state.hasCompletedAction("PUBLISH_INLINE_COMMENTS")) {
          yield AgentAction.Terminate.success("Review published successfully");
        }
        yield AgentAction.PublishInlineComments.started();
      }

      case COMPLETED, FAILED -> AgentAction.Terminate.success("Already in terminal state");
    };
  }

  private AgentAction determinePostCloneAction(
      final AgentState state, final AnalysisContext context) {
    if (context.testsEnabled()) {
      return AgentAction.RunTests.started("auto-detect");
    }
    return AgentAction.InvokeLlmReview.started(resolveProvider(), resolveModel());
  }

  private int countReasoningIterations(final AgentState state) {
    return (int)
        state.completedActions().stream()
            .filter(a -> "INVOKE_LLM_REVIEW".equals(a.actionType()))
            .count();
  }

  private double calculateCurrentConfidence(final AgentState state) {
    if (state.llmReviewResult() == null) {
      return 0.0;
    }

    final var issues = state.llmReviewResult().getIssues();
    if (issues == null || issues.isEmpty()) {
      return 1.0;
    }

    return issues.stream()
        .filter(i -> i.getConfidenceScore() != null)
        .mapToDouble(ReviewResult.Issue::getConfidenceScore)
        .average()
        .orElse(0.8);
  }

  private boolean hasCompletedRequiredActions(final AgentState state) {
    return state.hasCompletedAction("CLONE_REPOSITORY")
        && state.hasCompletedAction("INVOKE_LLM_REVIEW")
        && state.hasCompletedAction("PUBLISH_INLINE_COMMENTS");
  }

  private boolean isActionableFinding(final ReviewResult.Issue issue) {
    if (issue.getSuggestion() == null || issue.getSuggestion().isBlank()) {
      return false;
    }

    final var confidence = issue.getConfidenceScore();
    return confidence == null || confidence >= MIN_CONFIDENCE_THRESHOLD;
  }

  private Suggestion toSuggestion(final ReviewResult.Issue issue) {
    return Suggestion.fromIssue(issue, issue.getSuggestion());
  }

  private int resolveMaxIterations() {
    final var configured = properties.getDecision().getMaxReasoningIterations();
    return configured > 0 ? configured : MAX_REASONING_ITERATIONS;
  }

  private double resolveMinConfidenceThreshold() {
    return properties.getAggregation().getFiltering().getMinConfidence();
  }

  private String resolveProvider() {
    return properties.getDecision().getLlmProvider();
  }

  private String resolveModel() {
    return properties.getDecision().getLlmModel();
  }
}
