package com.ghiloufi.aicode.agentworker.agent;

import com.ghiloufi.aicode.agentworker.aggregation.ResultAggregator;
import com.ghiloufi.aicode.agentworker.analysis.TestExecutionResult;
import com.ghiloufi.aicode.agentworker.analysis.TestRunner;
import com.ghiloufi.aicode.agentworker.config.AgentWorkerProperties;
import com.ghiloufi.aicode.agentworker.config.ScmProperties;
import com.ghiloufi.aicode.agentworker.publisher.AgentResultPublisher;
import com.ghiloufi.aicode.agentworker.repository.CloneRequest;
import com.ghiloufi.aicode.agentworker.repository.RepositoryCloner;
import com.ghiloufi.aicode.core.domain.model.AgentAction;
import com.ghiloufi.aicode.core.domain.model.AgentStatus;
import com.ghiloufi.aicode.core.domain.model.AgentTask;
import com.ghiloufi.aicode.core.domain.model.AggregatedFindings;
import com.ghiloufi.aicode.core.domain.model.AnalysisMetadata;
import com.ghiloufi.aicode.core.domain.model.LocalAnalysisResult;
import com.ghiloufi.aicode.core.domain.model.PrioritizedFindings;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.model.TestResult;
import com.ghiloufi.aicode.core.domain.port.output.AgentDecisionEnginePort;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CodeReviewAgent {

  private static final String CONTEXT_CLONE_PATH = "clonePath";
  private static final String CONTEXT_COMMIT_HASH = "commitHash";
  private static final String CONTEXT_PRIORITIZED_FINDINGS = "prioritizedFindings";

  private final RepositoryCloner repositoryCloner;
  private final TestRunner testRunner;
  private final ResultAggregator resultAggregator;
  private final AgentResultPublisher resultPublisher;
  private final AgentDecisionEnginePort decisionEngine;
  private final AgentWorkerProperties properties;
  private final ScmProperties scmProperties;

  public AgentTask execute(AgentTask task) {
    log.info(
        "Starting agent execution for task {} (repo: {}, PR: {})",
        task.taskId(),
        task.repository().getDisplayName(),
        task.changeRequest().getNumber());

    var currentTask = task.updateStatus(AgentStatus.CLONING);
    Path workspacePath = null;

    try {
      currentTask = executeCloning(currentTask);
      if (currentTask.isTerminal()) {
        return currentTask;
      }

      workspacePath = currentTask.state().getContextValue(CONTEXT_CLONE_PATH, Path.class);

      currentTask = executeAnalysis(currentTask, workspacePath);
      if (currentTask.isTerminal()) {
        return currentTask;
      }

      currentTask = executeReasoning(currentTask);
      if (currentTask.isTerminal()) {
        return currentTask;
      }

      currentTask = executePublishing(currentTask);

      log.info(
          "Agent execution completed for task {} in {}ms",
          task.taskId(),
          currentTask.state().lastUpdated().toEpochMilli() - task.createdAt().toEpochMilli());

      return currentTask.complete();

    } catch (Exception e) {
      log.error("Agent execution failed for task {}", task.taskId(), e);
      return currentTask.fail("Agent execution failed: " + e.getMessage());
    } finally {
      if (workspacePath != null) {
        cleanupWorkspace(workspacePath);
      }
    }
  }

  private AgentTask executeCloning(AgentTask task) {
    log.debug("Executing CLONE_REPOSITORY action for task {}", task.taskId());

    try {
      final var workspacePath = createWorkspaceDirectory(task.taskId());
      final var repositoryUrl = buildRepositoryUrl(task);

      final var action =
          AgentAction.CloneRepository.started(repositoryUrl, "main", workspacePath.toString());
      var currentTask = task.startAction(action);

      final var cloneRequest =
          CloneRequest.builder()
              .repositoryUrl(repositoryUrl)
              .branch("main")
              .targetDirectory(workspacePath.toString())
              .depth(properties.getClone().getDepth())
              .authToken(getScmToken(task))
              .build();

      final var cloneResult = repositoryCloner.clone(cloneRequest);

      if (!cloneResult.success()) {
        log.error("Clone failed: {}", cloneResult.errorMessage());
        return currentTask.fail("Clone failed: " + cloneResult.errorMessage());
      }

      currentTask =
          currentTask
              .withState(
                  currentTask
                      .state()
                      .withContextValue(CONTEXT_CLONE_PATH, cloneResult.clonedPath())
                      .withContextValue(CONTEXT_COMMIT_HASH, cloneResult.commitHash()))
              .completeCurrentAction()
              .updateStatus(AgentStatus.ANALYZING);

      log.info("Clone completed: commit {}", cloneResult.commitHash());
      return currentTask;

    } catch (Exception e) {
      log.error("Clone action failed", e);
      return task.fail("Clone action failed: " + e.getMessage());
    }
  }

  private AgentTask executeAnalysis(AgentTask task, Path workspacePath) {
    log.debug("Executing analysis actions for task {}", task.taskId());

    final var runTestsAction = AgentAction.RunTests.started("auto-detect");
    var currentTask = task.startAction(runTestsAction);

    try {
      final var repoPath = workspacePath.resolve("repo");
      final var testResult = testRunner.runTests(repoPath);

      final var localAnalysisResult = buildLocalAnalysisResult(testResult);
      currentTask =
          currentTask.withLocalAnalysisResult(localAnalysisResult).completeCurrentAction();

      log.info(
          "Analysis completed: {} tests executed, {} passed, {} failed",
          testResult.totalTests(),
          testResult.passedTests(),
          testResult.failedTests());

      return currentTask.updateStatus(AgentStatus.REASONING);

    } catch (Exception e) {
      log.error("Analysis action failed", e);
      return currentTask.fail("Analysis failed: " + e.getMessage());
    }
  }

  private AgentTask executeReasoning(AgentTask task) {
    log.debug("Executing INVOKE_LLM_REVIEW action for task {}", task.taskId());

    final var action =
        AgentAction.InvokeLlmReview.started(
            properties.getDecision().getLlmProvider(), properties.getDecision().getLlmModel());
    var currentTask = task.startAction(action);

    try {
      final var testResult = extractTestResult(currentTask);
      final var aggregatedFindings =
          resultAggregator.aggregate(currentTask.state().llmReviewResult(), testResult);

      final var prioritizedFindings =
          decisionEngine
              .prioritizeFindings(aggregatedFindings.issues(), task.configuration())
              .block();

      logPrioritizationResults(prioritizedFindings);

      currentTask =
          currentTask
              .withState(
                  currentTask
                      .state()
                      .withContextValue("aggregatedFindings", aggregatedFindings)
                      .withContextValue(CONTEXT_PRIORITIZED_FINDINGS, prioritizedFindings))
              .completeCurrentAction()
              .updateStatus(AgentStatus.PUBLISHING);

      log.info(
          "Reasoning completed: {} issues prioritized (critical={}, high={}, filtered={})",
          prioritizedFindings != null ? prioritizedFindings.totalIncludedCount() : 0,
          prioritizedFindings != null ? prioritizedFindings.criticalIssues().size() : 0,
          prioritizedFindings != null ? prioritizedFindings.highPriorityIssues().size() : 0,
          prioritizedFindings != null ? prioritizedFindings.totalFilteredCount() : 0);

      return currentTask;

    } catch (Exception e) {
      log.error("Reasoning action failed", e);
      return currentTask.fail("Reasoning failed: " + e.getMessage());
    }
  }

  private void logPrioritizationResults(PrioritizedFindings findings) {
    if (findings == null) {
      log.warn("Prioritization returned null");
      return;
    }

    final var metrics = findings.metrics();
    log.debug(
        "Prioritization metrics: input={}, output={}, filtered={}, avgConfidence={:.2f}",
        metrics.totalInputIssues(),
        metrics.totalOutputIssues(),
        metrics.filteredByConfidence(),
        metrics.averageConfidence());

    if (findings.hasCriticalIssues()) {
      log.info(
          "CRITICAL issues detected: {} issues require immediate attention",
          findings.criticalIssues().size());
    }
  }

  private AgentTask executePublishing(AgentTask task) {
    log.debug("Executing PUBLISH_INLINE_COMMENTS action for task {}", task.taskId());

    final var action = AgentAction.PublishInlineComments.started();
    var currentTask = task.startAction(action);

    try {
      final var prioritizedFindings =
          currentTask
              .state()
              .getContextValue(CONTEXT_PRIORITIZED_FINDINGS, PrioritizedFindings.class);
      final var aggregatedFindings =
          currentTask.state().getContextValue("aggregatedFindings", AggregatedFindings.class);

      if (prioritizedFindings == null && aggregatedFindings == null) {
        log.warn("No findings available for publishing");
        return currentTask.completeCurrentAction();
      }

      final var reviewResult =
          prioritizedFindings != null
              ? buildReviewResultFromPrioritized(prioritizedFindings, aggregatedFindings)
              : buildReviewResult(aggregatedFindings);

      resultPublisher.publish(task, reviewResult);

      currentTask = currentTask.completeCurrentAction();

      final var issueCount =
          prioritizedFindings != null
              ? prioritizedFindings.totalIncludedCount()
              : (aggregatedFindings != null ? aggregatedFindings.issues().size() : 0);

      log.info(
          "Published {} prioritized issues to SCM (filtered out: {})",
          issueCount,
          prioritizedFindings != null ? prioritizedFindings.totalFilteredCount() : 0);

      return currentTask;

    } catch (Exception e) {
      log.error("Publishing action failed", e);
      return currentTask.fail("Publishing failed: " + e.getMessage());
    }
  }

  private ReviewResult buildReviewResultFromPrioritized(
      PrioritizedFindings prioritized, AggregatedFindings aggregated) {
    final var allPrioritizedIssues = prioritized.allPrioritizedIssues();
    final var notes = aggregated != null ? aggregated.notes() : List.<ReviewResult.Note>of();

    final var summary =
        buildPrioritizedSummary(prioritized, aggregated != null ? aggregated.summary() : "");

    return ReviewResult.builder()
        .summary(summary)
        .issues(allPrioritizedIssues)
        .nonBlockingNotes(notes)
        .build();
  }

  private String buildPrioritizedSummary(PrioritizedFindings prioritized, String baseSummary) {
    final var sb = new StringBuilder();

    if (baseSummary != null && !baseSummary.isBlank()) {
      sb.append(baseSummary).append("\n\n");
    }

    sb.append("**Priority Summary:**\n");
    if (!prioritized.criticalIssues().isEmpty()) {
      sb.append("- 🔴 Critical: ").append(prioritized.criticalIssues().size()).append("\n");
    }
    if (!prioritized.highPriorityIssues().isEmpty()) {
      sb.append("- 🟠 High: ").append(prioritized.highPriorityIssues().size()).append("\n");
    }
    if (!prioritized.mediumPriorityIssues().isEmpty()) {
      sb.append("- 🟡 Medium: ").append(prioritized.mediumPriorityIssues().size()).append("\n");
    }
    if (!prioritized.lowPriorityIssues().isEmpty()) {
      sb.append("- 🟢 Low: ").append(prioritized.lowPriorityIssues().size()).append("\n");
    }

    if (prioritized.totalFilteredCount() > 0) {
      sb.append("\n_")
          .append(prioritized.totalFilteredCount())
          .append(" lower-priority issues were filtered._");
    }

    return sb.toString();
  }

  private Path createWorkspaceDirectory(String taskId) throws Exception {
    final var workspacePath = Path.of(System.getProperty("java.io.tmpdir"), "agent-" + taskId);
    Files.createDirectories(workspacePath);
    return workspacePath;
  }

  private String buildRepositoryUrl(AgentTask task) {
    final var provider = task.repository().getProvider();
    final var displayName = task.repository().getDisplayName();

    return switch (provider) {
      case GITHUB -> "https://github.com/" + displayName + ".git";
      case GITLAB -> "https://gitlab.com/" + displayName + ".git";
    };
  }

  private LocalAnalysisResult buildLocalAnalysisResult(TestExecutionResult testResult) {
    final var frameworkName =
        testResult.framework() != null ? testResult.framework().name() : "none";
    final var metadata =
        AnalysisMetadata.started("analysis-container", "local", "main", "HEAD")
            .withToolCompleted("tests-" + frameworkName, testResult.duration())
            .completed(0, 0);

    return new LocalAnalysisResult(
        testResult.executed() ? testResult.testResults() : List.of(), metadata);
  }

  private TestExecutionResult extractTestResult(AgentTask task) {
    final var localResult = task.state().localAnalysisResult();
    if (localResult == null || localResult.testResults().isEmpty()) {
      return TestExecutionResult.notExecuted("No tests executed");
    }

    final var passed =
        (int) localResult.testResults().stream().filter(TestResult::isSuccess).count();
    final var failed = localResult.testResults().size() - passed;
    final var duration =
        localResult.metadata().totalDuration() != null
            ? localResult.metadata().totalDuration()
            : Duration.ZERO;

    return TestExecutionResult.success(
        null,
        localResult.testResults(),
        localResult.testResults().size(),
        passed,
        failed,
        0,
        duration,
        null);
  }

  private ReviewResult buildReviewResult(AggregatedFindings findings) {
    return ReviewResult.builder()
        .summary(findings.summary())
        .issues(findings.issues())
        .nonBlockingNotes(findings.notes())
        .build();
  }

  private void cleanupWorkspace(Path workspacePath) {
    try {
      if (Files.exists(workspacePath)) {
        Files.walk(workspacePath)
            .sorted(Comparator.reverseOrder())
            .forEach(
                path -> {
                  try {
                    Files.delete(path);
                  } catch (Exception e) {
                    log.trace("Failed to delete {}: {}", path, e.getMessage());
                  }
                });
        log.debug("Cleaned up workspace: {}", workspacePath);
      }
    } catch (Exception e) {
      log.warn("Failed to cleanup workspace {}: {}", workspacePath, e.getMessage());
    }
  }

  private String getScmToken(AgentTask task) {
    final var provider = task.repository().getProvider();
    return switch (provider) {
      case GITHUB -> scmProperties.getGithub().getToken();
      case GITLAB -> scmProperties.getGitlab().getToken();
    };
  }
}
