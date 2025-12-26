package com.ghiloufi.aicode.agentworker.decision;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.agentworker.config.AgentWorkerProperties;
import com.ghiloufi.aicode.agentworker.config.AgentWorkerProperties.AggregationProperties;
import com.ghiloufi.aicode.agentworker.config.AgentWorkerProperties.AggregationProperties.DeduplicationProperties;
import com.ghiloufi.aicode.agentworker.config.AgentWorkerProperties.AggregationProperties.FilteringProperties;
import com.ghiloufi.aicode.agentworker.config.AgentWorkerProperties.CloneProperties;
import com.ghiloufi.aicode.agentworker.config.AgentWorkerProperties.ConsumerProperties;
import com.ghiloufi.aicode.agentworker.config.AgentWorkerProperties.DecisionProperties;
import com.ghiloufi.aicode.agentworker.config.AgentWorkerProperties.DockerProperties;
import com.ghiloufi.aicode.agentworker.config.AgentWorkerProperties.DockerProperties.ResourceLimitsProperties;
import com.ghiloufi.aicode.core.domain.model.AgentAction;
import com.ghiloufi.aicode.core.domain.model.AgentConfiguration;
import com.ghiloufi.aicode.core.domain.model.AgentState;
import com.ghiloufi.aicode.core.domain.model.AgentStatus;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.port.output.AgentDecisionEnginePort.AnalysisContext;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class DefaultAgentDecisionEngineTest {

  private DefaultAgentDecisionEngine decisionEngine;
  private AgentWorkerProperties properties;

  @BeforeEach
  void setUp() {
    properties = createDefaultProperties();
    decisionEngine = new DefaultAgentDecisionEngine(properties);
  }

  @Nested
  final class DecideNextAction {

    @Test
    void should_return_clone_action_for_pending_state() {
      final var state = AgentState.initial();
      final var context = AnalysisContext.initial("/repo", "abc123", "main");

      final var action = decisionEngine.decideNextAction(state, context).block();

      assertThat(action).isInstanceOf(AgentAction.CloneRepository.class);
    }

    @Test
    void should_return_run_tests_for_analyzing_state_with_tests_enabled() {
      final var state = AgentState.initial().withStatus(AgentStatus.ANALYZING);
      final var context =
          AnalysisContext.initial("/repo", "abc123", "main")
              .withTestsEnabled(true)
              .withTestsExecuted(false);

      final var action = decisionEngine.decideNextAction(state, context).block();

      assertThat(action).isInstanceOf(AgentAction.RunTests.class);
    }

    @Test
    void should_return_llm_review_for_analyzing_state_without_tests() {
      final var state = AgentState.initial().withStatus(AgentStatus.ANALYZING);
      final var context =
          AnalysisContext.initial("/repo", "abc123", "main")
              .withTestsEnabled(false)
              .withTestsExecuted(false);

      final var action = decisionEngine.decideNextAction(state, context).block();

      assertThat(action).isInstanceOf(AgentAction.InvokeLlmReview.class);
    }

    @Test
    void should_return_publish_for_reasoning_state_with_llm_results() {
      final var llmResult =
          ReviewResult.builder().summary("Test summary").issues(List.of()).build();
      final var state =
          AgentState.initial().withStatus(AgentStatus.REASONING).withLlmReviewResult(llmResult);
      final var context = AnalysisContext.initial("/repo", "abc123", "main");

      final var action = decisionEngine.decideNextAction(state, context).block();

      assertThat(action).isInstanceOf(AgentAction.PublishInlineComments.class);
    }

    @Test
    void should_return_terminate_for_completed_state() {
      final var state = AgentState.initial().withStatus(AgentStatus.COMPLETED);
      final var context = AnalysisContext.initial("/repo", "abc123", "main");

      final var action = decisionEngine.decideNextAction(state, context).block();

      assertThat(action).isInstanceOf(AgentAction.Terminate.class);
    }
  }

  @Nested
  final class PrioritizeFindings {

    @Test
    void should_prioritize_issues_by_severity() {
      final var critical = createIssue("critical", 0.95);
      final var high = createIssue("error", 0.9);
      final var medium = createIssue("warning", 0.85);
      final var low = createIssue("info", 0.8);
      final var config = AgentConfiguration.defaults();

      final var prioritized =
          decisionEngine.prioritizeFindings(List.of(critical, high, medium, low), config).block();

      assertThat(prioritized).isNotNull();
      assertThat(prioritized.criticalIssues()).hasSize(1);
      assertThat(prioritized.highPriorityIssues()).hasSize(1);
      assertThat(prioritized.mediumPriorityIssues()).hasSize(1);
      assertThat(prioritized.lowPriorityIssues()).hasSize(1);
    }

    @Test
    void should_filter_low_confidence_issues() {
      final var highConfidence = createIssue("warning", 0.9);
      final var lowConfidence = createIssue("warning", 0.5);
      final var config = AgentConfiguration.defaults();

      final var prioritized =
          decisionEngine.prioritizeFindings(List.of(highConfidence, lowConfidence), config).block();

      assertThat(prioritized).isNotNull();
      assertThat(prioritized.totalIncludedCount()).isEqualTo(1);
      assertThat(prioritized.totalFilteredCount()).isEqualTo(1);
    }

    @Test
    void should_return_empty_for_empty_findings() {
      final var config = AgentConfiguration.defaults();

      final var prioritized = decisionEngine.prioritizeFindings(List.of(), config).block();

      assertThat(prioritized).isNotNull();
      assertThat(prioritized.isEmpty()).isTrue();
    }
  }

  @Nested
  final class ShouldContinueAnalysis {

    @Test
    void should_stop_for_terminal_state() {
      final var state = AgentState.initial().withStatus(AgentStatus.COMPLETED);
      final var config = AgentConfiguration.defaults();

      final var shouldContinue = decisionEngine.shouldContinueAnalysis(state, config).block();

      assertThat(shouldContinue).isFalse();
    }

    @Test
    void should_continue_when_required_actions_not_completed() {
      final var state = AgentState.initial().withStatus(AgentStatus.ANALYZING);
      final var config = AgentConfiguration.defaults();

      final var shouldContinue = decisionEngine.shouldContinueAnalysis(state, config).block();

      assertThat(shouldContinue).isTrue();
    }

    @Test
    void should_stop_when_all_required_actions_completed() {
      var state = AgentState.initial();

      state = state.withCurrentAction(AgentAction.CloneRepository.started("url", "main", "/path"));
      state = state.completeCurrentAction();

      state = state.withCurrentAction(AgentAction.InvokeLlmReview.started("openai", "gpt-4o"));
      state = state.completeCurrentAction();

      state = state.withCurrentAction(AgentAction.PublishInlineComments.started());
      state = state.completeCurrentAction();

      final var config = AgentConfiguration.defaults();
      final var shouldContinue = decisionEngine.shouldContinueAnalysis(state, config).block();

      assertThat(shouldContinue).isFalse();
    }
  }

  @Nested
  final class GenerateSuggestions {

    @Test
    void should_generate_suggestions_from_actionable_findings() {
      final var actionable = createIssue("warning", 0.85);
      final var nonActionable = createIssueWithoutSuggestion("warning", 0.85);

      final var suggestions =
          decisionEngine.generateSuggestions(List.of(actionable, nonActionable)).block();

      assertThat(suggestions).isNotNull().hasSize(1);
    }

    @Test
    void should_filter_low_confidence_from_suggestions() {
      final var highConfidence = createIssue("warning", 0.85);
      final var lowConfidence = createIssue("warning", 0.5);

      final var suggestions =
          decisionEngine.generateSuggestions(List.of(highConfidence, lowConfidence)).block();

      assertThat(suggestions).isNotNull().hasSize(1);
    }

    @Test
    void should_return_empty_for_no_actionable_findings() {
      final var nonActionable = createIssueWithoutSuggestion("warning", 0.85);

      final var suggestions = decisionEngine.generateSuggestions(List.of(nonActionable)).block();

      assertThat(suggestions).isNotNull().isEmpty();
    }
  }

  private ReviewResult.Issue createIssue(String severity, double confidence) {
    return ReviewResult.Issue.issueBuilder()
        .file("Test.java")
        .startLine(1)
        .severity(severity)
        .title("Test issue")
        .suggestion("Fix this by doing X")
        .confidenceScore(confidence)
        .build();
  }

  private ReviewResult.Issue createIssueWithoutSuggestion(String severity, double confidence) {
    return ReviewResult.Issue.issueBuilder()
        .file("Test.java")
        .startLine(1)
        .severity(severity)
        .title("Test issue")
        .confidenceScore(confidence)
        .build();
  }

  private AgentWorkerProperties createDefaultProperties() {
    final var consumer =
        new ConsumerProperties(
            "review:agent-requests", "agent-workers", "agent-worker-1", 1, Duration.ofSeconds(5));
    final var clone = new CloneProperties(1, Duration.ofSeconds(120), "token");
    final var resourceLimits = new ResourceLimitsProperties(2147483648L, 2000000000L);
    final var docker =
        new DockerProperties(
            "unix:///var/run/docker.sock",
            "ai-code-reviewer-analysis:latest",
            resourceLimits,
            Duration.ofSeconds(600),
            true);
    final var deduplication = new DeduplicationProperties(true, 0.85);
    final var filtering = new FilteringProperties(0.7, 10);
    final var aggregation = new AggregationProperties(deduplication, filtering);
    final var decision = new DecisionProperties("openai", "gpt-4o", 3);

    return new AgentWorkerProperties(consumer, clone, docker, aggregation, decision);
  }
}
