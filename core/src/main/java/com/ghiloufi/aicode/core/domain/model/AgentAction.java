package com.ghiloufi.aicode.core.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public sealed interface AgentAction
    permits AgentAction.CloneRepository,
        AgentAction.RunTests,
        AgentAction.InvokeLlmReview,
        AgentAction.PublishInlineComments,
        AgentAction.PublishSummary,
        AgentAction.Terminate {

  String actionType();

  record CloneRepository(
      String repositoryUrl,
      String branch,
      String targetDirectory,
      Instant startedAt,
      Duration duration,
      boolean success)
      implements AgentAction {

    public CloneRepository {
      Objects.requireNonNull(repositoryUrl, "repositoryUrl must not be null");
      Objects.requireNonNull(branch, "branch must not be null");
      Objects.requireNonNull(targetDirectory, "targetDirectory must not be null");
      Objects.requireNonNull(startedAt, "startedAt must not be null");
    }

    public static CloneRepository started(
        final String repositoryUrl, final String branch, final String targetDirectory) {
      return new CloneRepository(
          repositoryUrl, branch, targetDirectory, Instant.now(), null, false);
    }

    public CloneRepository completed(final boolean success) {
      return new CloneRepository(
          repositoryUrl,
          branch,
          targetDirectory,
          startedAt,
          Duration.between(startedAt, Instant.now()),
          success);
    }

    @Override
    public String actionType() {
      return "CLONE_REPOSITORY";
    }
  }

  record RunTests(
      String testCommand,
      Instant startedAt,
      Duration duration,
      int passed,
      int failed,
      int skipped,
      boolean success)
      implements AgentAction {

    public RunTests {
      Objects.requireNonNull(testCommand, "testCommand must not be null");
      Objects.requireNonNull(startedAt, "startedAt must not be null");
    }

    public static RunTests started(final String testCommand) {
      return new RunTests(testCommand, Instant.now(), null, 0, 0, 0, false);
    }

    public RunTests completed(
        final int passed, final int failed, final int skipped, final boolean success) {
      return new RunTests(
          testCommand,
          startedAt,
          Duration.between(startedAt, Instant.now()),
          passed,
          failed,
          skipped,
          success);
    }

    public int totalTests() {
      return passed + failed + skipped;
    }

    @Override
    public String actionType() {
      return "RUN_TESTS";
    }
  }

  record InvokeLlmReview(
      String llmProvider,
      String llmModel,
      Instant startedAt,
      Duration duration,
      int issuesGenerated,
      boolean success)
      implements AgentAction {

    public InvokeLlmReview {
      Objects.requireNonNull(llmProvider, "llmProvider must not be null");
      Objects.requireNonNull(llmModel, "llmModel must not be null");
      Objects.requireNonNull(startedAt, "startedAt must not be null");
    }

    public static InvokeLlmReview started(final String llmProvider, final String llmModel) {
      return new InvokeLlmReview(llmProvider, llmModel, Instant.now(), null, 0, false);
    }

    public InvokeLlmReview completed(final int issues, final boolean success) {
      return new InvokeLlmReview(
          llmProvider,
          llmModel,
          startedAt,
          Duration.between(startedAt, Instant.now()),
          issues,
          success);
    }

    @Override
    public String actionType() {
      return "INVOKE_LLM_REVIEW";
    }
  }

  record PublishInlineComments(
      Instant startedAt, Duration duration, int commentsPosted, int commentsFailed, boolean success)
      implements AgentAction {

    public PublishInlineComments {
      Objects.requireNonNull(startedAt, "startedAt must not be null");
    }

    public static PublishInlineComments started() {
      return new PublishInlineComments(Instant.now(), null, 0, 0, false);
    }

    public PublishInlineComments completed(
        final int posted, final int failed, final boolean success) {
      return new PublishInlineComments(
          startedAt, Duration.between(startedAt, Instant.now()), posted, failed, success);
    }

    @Override
    public String actionType() {
      return "PUBLISH_INLINE_COMMENTS";
    }
  }

  record PublishSummary(
      Instant startedAt, Duration duration, String summaryCommentId, boolean success)
      implements AgentAction {

    public PublishSummary {
      Objects.requireNonNull(startedAt, "startedAt must not be null");
    }

    public static PublishSummary started() {
      return new PublishSummary(Instant.now(), null, null, false);
    }

    public PublishSummary completed(final String commentId, final boolean success) {
      return new PublishSummary(
          startedAt, Duration.between(startedAt, Instant.now()), commentId, success);
    }

    @Override
    public String actionType() {
      return "PUBLISH_SUMMARY";
    }
  }

  record Terminate(Instant completedAt, String reason, boolean successful) implements AgentAction {

    public Terminate {
      Objects.requireNonNull(completedAt, "completedAt must not be null");
      Objects.requireNonNull(reason, "reason must not be null");
    }

    public static Terminate success(final String reason) {
      return new Terminate(Instant.now(), reason, true);
    }

    public static Terminate failure(final String reason) {
      return new Terminate(Instant.now(), reason, false);
    }

    @Override
    public String actionType() {
      return "TERMINATE";
    }
  }
}
