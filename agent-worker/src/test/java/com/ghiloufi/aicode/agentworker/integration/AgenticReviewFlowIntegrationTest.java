package com.ghiloufi.aicode.agentworker.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ghiloufi.aicode.agentworker.analysis.TestExecutionResult;
import com.ghiloufi.aicode.core.domain.model.AgentAction;
import com.ghiloufi.aicode.core.domain.model.AgentConfiguration;
import com.ghiloufi.aicode.core.domain.model.AgentStatus;
import com.ghiloufi.aicode.core.domain.model.AgentTask;
import com.ghiloufi.aicode.core.domain.model.GitHubRepositoryId;
import com.ghiloufi.aicode.core.domain.model.PrioritizedFindings;
import com.ghiloufi.aicode.core.domain.model.PullRequestId;
import com.ghiloufi.aicode.core.domain.model.ReviewMode;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.model.TestResult;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Agentic Review Flow Integration Tests")
final class AgenticReviewFlowIntegrationTest {

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @Nested
  @DisplayName("AgentTask Serialization")
  final class TaskSerialization {

    @Test
    @DisplayName("should_serialize_agent_task_to_json")
    void should_serialize_agent_task_to_json() throws Exception {
      final var task = createSampleTask();

      final var json = objectMapper.writeValueAsString(task);

      assertThat(json).contains("\"taskId\"");
      assertThat(json).contains("\"repository\"");
      assertThat(json).contains("\"changeRequest\"");
      assertThat(json).contains("GITHUB");
    }

    @Test
    @DisplayName("should_preserve_agent_state_in_serialization")
    void should_preserve_agent_state_in_serialization() throws Exception {
      final var task = createSampleTask().updateStatus(AgentStatus.ANALYZING);

      final var json = objectMapper.writeValueAsString(task);

      assertThat(json).contains("\"status\"");
      assertThat(json).contains("ANALYZING");
    }
  }

  @Nested
  @DisplayName("Agent State Transitions")
  final class StateTransitions {

    @Test
    @DisplayName("should_transition_from_pending_to_cloning")
    void should_transition_from_pending_to_cloning() {
      final var task = createSampleTask();
      assertThat(task.state().status()).isEqualTo(AgentStatus.PENDING);

      final var cloningTask = task.updateStatus(AgentStatus.CLONING);

      assertThat(cloningTask.state().status()).isEqualTo(AgentStatus.CLONING);
    }

    @Test
    @DisplayName("should_transition_through_full_workflow")
    void should_transition_through_full_workflow() {
      var task = createSampleTask();

      task = task.updateStatus(AgentStatus.CLONING);
      assertThat(task.state().status()).isEqualTo(AgentStatus.CLONING);

      task = task.updateStatus(AgentStatus.ANALYZING);
      assertThat(task.state().status()).isEqualTo(AgentStatus.ANALYZING);

      task = task.updateStatus(AgentStatus.REASONING);
      assertThat(task.state().status()).isEqualTo(AgentStatus.REASONING);

      task = task.updateStatus(AgentStatus.PUBLISHING);
      assertThat(task.state().status()).isEqualTo(AgentStatus.PUBLISHING);

      task = task.updateStatus(AgentStatus.COMPLETED);
      assertThat(task.state().status()).isEqualTo(AgentStatus.COMPLETED);
      assertThat(task.isTerminal()).isTrue();
    }

    @Test
    @DisplayName("should_handle_failure_state")
    void should_handle_failure_state() {
      final var task = createSampleTask().updateStatus(AgentStatus.ANALYZING);

      final var failedTask = task.fail("Clone operation timed out");

      assertThat(failedTask.state().status()).isEqualTo(AgentStatus.FAILED);
      assertThat(failedTask.isTerminal()).isTrue();
    }
  }

  @Nested
  @DisplayName("Agent Action Tracking")
  final class ActionTracking {

    @Test
    @DisplayName("should_track_clone_action")
    void should_track_clone_action() {
      final var task = createSampleTask();
      final var cloneAction =
          AgentAction.CloneRepository.started("https://github.com/org/repo.git", "main", "/tmp/ws");

      final var taskWithAction = task.startAction(cloneAction);

      assertThat(taskWithAction.state().currentAction()).isEqualTo(cloneAction);
    }

    @Test
    @DisplayName("should_complete_action_and_add_to_history")
    void should_complete_action_and_add_to_history() {
      final var task = createSampleTask();
      final var cloneAction =
          AgentAction.CloneRepository.started("https://github.com/org/repo.git", "main", "/tmp/ws");

      final var taskWithAction = task.startAction(cloneAction);
      final var completedTask = taskWithAction.completeCurrentAction();

      assertThat(completedTask.state().currentAction()).isNull();
      assertThat(completedTask.state().completedActions()).contains(cloneAction);
    }

    @Test
    @DisplayName("should_track_multiple_actions_in_sequence")
    void should_track_multiple_actions_in_sequence() {
      var task = createSampleTask();

      final var cloneAction =
          AgentAction.CloneRepository.started("https://github.com/org/repo.git", "main", "/tmp");
      task = task.startAction(cloneAction).completeCurrentAction();

      final var testsAction = AgentAction.RunTests.started("maven");
      task = task.startAction(testsAction).completeCurrentAction();

      final var llmAction = AgentAction.InvokeLlmReview.started("openai", "gpt-4o");
      task = task.startAction(llmAction).completeCurrentAction();

      assertThat(task.state().completedActions()).hasSize(3);
    }
  }

  @Nested
  @DisplayName("Test Execution Result Flow")
  final class TestExecutionFlow {

    @Test
    @DisplayName("should_create_success_test_result")
    void should_create_success_test_result() {
      final var testResult =
          TestExecutionResult.success(
              "maven",
              List.of(
                  TestResult.passed("AuthTest", "should_authenticate_user", Duration.ofMillis(50)),
                  TestResult.passed(
                      "AuthTest", "should_reject_invalid_token", Duration.ofMillis(30))),
              2,
              2,
              0,
              0,
              Duration.ofSeconds(5),
              "Build successful");

      assertThat(testResult.success()).isTrue();
      assertThat(testResult.executed()).isTrue();
      assertThat(testResult.totalTests()).isEqualTo(2);
      assertThat(testResult.passedTests()).isEqualTo(2);
      assertThat(testResult.failedTests()).isZero();
    }

    @Test
    @DisplayName("should_create_failure_test_result")
    void should_create_failure_test_result() {
      final var testResult =
          TestExecutionResult.failure(
              "gradle",
              List.of(
                  TestResult.passed("AuthTest", "should_authenticate_user", Duration.ofMillis(50)),
                  TestResult.failed(
                      "AuthTest",
                      "should_reject_expired_token",
                      "AssertionError: expected 401",
                      null)),
              Duration.ofSeconds(10),
              "Test output",
              "1 test failed");

      assertThat(testResult.success()).isFalse();
      assertThat(testResult.executed()).isTrue();
      assertThat(testResult.failedTests()).isEqualTo(1);
    }

    @Test
    @DisplayName("should_create_not_executed_result")
    void should_create_not_executed_result() {
      final var testResult = TestExecutionResult.notExecuted("Tests disabled by configuration");

      assertThat(testResult.executed()).isFalse();
      assertThat(testResult.success()).isTrue();
      assertThat(testResult.errorMessage()).isEqualTo("Tests disabled by configuration");
    }
  }

  @Nested
  @DisplayName("Prioritized Findings Flow")
  final class PrioritizedFindingsFlow {

    @Test
    @DisplayName("should_prioritize_issues_by_severity")
    void should_prioritize_issues_by_severity() {
      final var issues =
          List.of(
              createIssue("critical", 0.95),
              createIssue("error", 0.9),
              createIssue("warning", 0.85),
              createIssue("info", 0.8));

      final var prioritized = PrioritizedFindings.fromIssues(issues, 0.7, 10);

      assertThat(prioritized.criticalIssues()).hasSize(1);
      assertThat(prioritized.highPriorityIssues()).hasSize(1);
      assertThat(prioritized.mediumPriorityIssues()).hasSize(1);
      assertThat(prioritized.lowPriorityIssues()).hasSize(1);
      assertThat(prioritized.totalIncludedCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("should_filter_low_confidence_issues")
    void should_filter_low_confidence_issues() {
      final var issues =
          List.of(
              createIssue("warning", 0.9),
              createIssue("warning", 0.5),
              createIssue("warning", 0.3));

      final var prioritized = PrioritizedFindings.fromIssues(issues, 0.7, 10);

      assertThat(prioritized.totalIncludedCount()).isEqualTo(1);
      assertThat(prioritized.totalFilteredCount()).isEqualTo(2);
    }
  }

  @Nested
  @DisplayName("Redis Message Format")
  final class RedisMessageFormat {

    @Test
    @DisplayName("should_format_agent_request_for_redis_stream")
    void should_format_agent_request_for_redis_stream() throws Exception {
      final var task = createSampleTask();
      final var payload = objectMapper.writeValueAsString(task);

      final Map<String, String> redisMessage =
          Map.of(
              "taskId", task.taskId(),
              "reviewMode", ReviewMode.AGENTIC.name(),
              "payload", payload);

      assertThat(redisMessage).containsKey("taskId");
      assertThat(redisMessage).containsEntry("reviewMode", "AGENTIC");
      assertThat(redisMessage.get("payload")).contains("repository");
    }

    @Test
    @DisplayName("should_distinguish_agentic_from_diff_mode")
    void should_distinguish_agentic_from_diff_mode() {
      final Map<String, String> agenticMessage =
          Map.of("reviewMode", ReviewMode.AGENTIC.name(), "streamKey", "review:agent-requests");

      final Map<String, String> diffMessage =
          Map.of("reviewMode", ReviewMode.DIFF.name(), "streamKey", "review:requests");

      assertThat(agenticMessage.get("streamKey")).isEqualTo("review:agent-requests");
      assertThat(diffMessage.get("streamKey")).isEqualTo("review:requests");
    }
  }

  @Nested
  @DisplayName("Agent Configuration")
  final class ConfigurationTests {

    @Test
    @DisplayName("should_create_default_configuration")
    void should_create_default_configuration() {
      final var config = AgentConfiguration.defaults();

      assertThat(config).isNotNull();
      assertThat(config.aggregation().minConfidence()).isEqualTo(0.7);
      assertThat(config.aggregation().maxIssuesPerFile()).isEqualTo(10);
    }

    @Test
    @DisplayName("should_create_configuration_with_custom_docker_settings")
    void should_create_configuration_with_custom_docker_settings() {
      final var config = AgentConfiguration.defaults();

      assertThat(config.docker()).isNotNull();
      assertThat(config.docker().autoRemove()).isTrue();
    }
  }

  @Nested
  @DisplayName("Complete Workflow Simulation")
  final class CompleteWorkflowSimulation {

    @Test
    @DisplayName("should_simulate_successful_agentic_review")
    void should_simulate_successful_agentic_review() {
      var task = createSampleTask();

      task = task.updateStatus(AgentStatus.CLONING);
      final var cloneAction =
          AgentAction.CloneRepository.started("https://github.com/org/repo.git", "main", "/tmp/ws");
      task = task.startAction(cloneAction).completeCurrentAction();
      task =
          task.withState(
              task.state()
                  .withContextValue("clonePath", "/tmp/ws")
                  .withContextValue("commitHash", "abc123"));

      task = task.updateStatus(AgentStatus.ANALYZING);
      final var testAction = AgentAction.RunTests.started("maven");
      task = task.startAction(testAction).completeCurrentAction();

      task = task.updateStatus(AgentStatus.REASONING);
      final var llmAction = AgentAction.InvokeLlmReview.started("openai", "gpt-4o");
      task = task.startAction(llmAction).completeCurrentAction();

      final var prioritized =
          PrioritizedFindings.fromIssues(
              List.of(createIssue("warning", 0.85), createIssue("error", 0.9)), 0.7, 10);
      task = task.withState(task.state().withContextValue("prioritizedFindings", prioritized));

      task = task.updateStatus(AgentStatus.PUBLISHING);
      final var publishAction = AgentAction.PublishInlineComments.started();
      task = task.startAction(publishAction).completeCurrentAction();

      task = task.complete();

      assertThat(task.state().status()).isEqualTo(AgentStatus.COMPLETED);
      assertThat(task.state().completedActions()).hasSize(4);
      assertThat(task.isTerminal()).isTrue();
    }

    @Test
    @DisplayName("should_handle_clone_failure_gracefully")
    void should_handle_clone_failure_gracefully() {
      var task = createSampleTask();

      task = task.updateStatus(AgentStatus.CLONING);
      final var cloneAction =
          AgentAction.CloneRepository.started("https://github.com/org/repo.git", "main", "/tmp/ws");
      task = task.startAction(cloneAction);

      task = task.fail("Repository not found or access denied");

      assertThat(task.state().status()).isEqualTo(AgentStatus.FAILED);
      assertThat(task.isTerminal()).isTrue();
    }

    @Test
    @DisplayName("should_continue_after_test_failure")
    void should_continue_after_test_failure() {
      var task = createSampleTask();

      task = task.updateStatus(AgentStatus.CLONING);
      task =
          task.startAction(
                  AgentAction.CloneRepository.started(
                      "https://github.com/org/repo.git", "main", "/tmp"))
              .completeCurrentAction();

      task = task.updateStatus(AgentStatus.ANALYZING);
      final var testResult =
          TestExecutionResult.failure(
              "maven",
              List.of(TestResult.failed("Test", "failing", "assertion failed", null)),
              Duration.ofSeconds(5),
              "output",
              "1 test failed");

      assertThat(testResult.success()).isFalse();
      assertThat(testResult.executed()).isTrue();

      task = task.updateStatus(AgentStatus.REASONING);
      assertThat(task.state().status()).isEqualTo(AgentStatus.REASONING);
    }
  }

  private AgentTask createSampleTask() {
    final var repository = new GitHubRepositoryId("org", "repo");
    final var changeRequest = new PullRequestId(123);
    final var configuration = AgentConfiguration.defaults();

    return AgentTask.create(repository, changeRequest, configuration, "correlation-123");
  }

  private ReviewResult.Issue createIssue(String severity, double confidence) {
    return ReviewResult.Issue.issueBuilder()
        .file("Test.java")
        .startLine(1)
        .severity(severity)
        .title("Test issue")
        .suggestion("Fix this")
        .confidenceScore(confidence)
        .build();
  }
}
