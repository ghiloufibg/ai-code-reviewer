package com.ghiloufi.aicode.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

final class AgentActionTest {

  @Test
  void should_create_clone_repository_action() {
    final var action =
        AgentAction.CloneRepository.started("https://github.com/org/repo", "main", "/workspace");

    assertThat(action.repositoryUrl()).isEqualTo("https://github.com/org/repo");
    assertThat(action.branch()).isEqualTo("main");
    assertThat(action.targetDirectory()).isEqualTo("/workspace");
    assertThat(action.startedAt()).isNotNull();
    assertThat(action.duration()).isNull();
    assertThat(action.success()).isFalse();
    assertThat(action.actionType()).isEqualTo("CLONE_REPOSITORY");
  }

  @Test
  void should_complete_clone_repository_action() {
    final var started = AgentAction.CloneRepository.started("url", "branch", "/dir");
    final var completed = started.completed(true);

    assertThat(completed.success()).isTrue();
    assertThat(completed.duration()).isNotNull();
    assertThat(completed.startedAt()).isEqualTo(started.startedAt());
  }

  @Test
  void should_throw_when_clone_repository_url_is_null() {
    assertThatThrownBy(() -> AgentAction.CloneRepository.started(null, "branch", "/dir"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("repositoryUrl");
  }

  @Test
  void should_create_run_tests_action() {
    final var action = AgentAction.RunTests.started("mvn test");

    assertThat(action.testCommand()).isEqualTo("mvn test");
    assertThat(action.passed()).isZero();
    assertThat(action.failed()).isZero();
    assertThat(action.skipped()).isZero();
    assertThat(action.actionType()).isEqualTo("RUN_TESTS");
  }

  @Test
  void should_complete_run_tests_action() {
    final var started = AgentAction.RunTests.started("mvn test");
    final var completed = started.completed(45, 3, 2, false);

    assertThat(completed.passed()).isEqualTo(45);
    assertThat(completed.failed()).isEqualTo(3);
    assertThat(completed.skipped()).isEqualTo(2);
    assertThat(completed.totalTests()).isEqualTo(50);
    assertThat(completed.success()).isFalse();
  }

  @Test
  void should_throw_when_test_command_is_null() {
    assertThatThrownBy(() -> AgentAction.RunTests.started(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("testCommand");
  }

  @Test
  void should_create_llm_review_action() {
    final var action = AgentAction.InvokeLlmReview.started("openai", "gpt-4o");

    assertThat(action.llmProvider()).isEqualTo("openai");
    assertThat(action.llmModel()).isEqualTo("gpt-4o");
    assertThat(action.issuesGenerated()).isZero();
    assertThat(action.actionType()).isEqualTo("INVOKE_LLM_REVIEW");
  }

  @Test
  void should_complete_llm_review_action() {
    final var started = AgentAction.InvokeLlmReview.started("anthropic", "claude-3");
    final var completed = started.completed(8, true);

    assertThat(completed.issuesGenerated()).isEqualTo(8);
    assertThat(completed.success()).isTrue();
    assertThat(completed.duration()).isNotNull();
  }

  @Test
  void should_create_publish_inline_comments_action() {
    final var action = AgentAction.PublishInlineComments.started();

    assertThat(action.commentsPosted()).isZero();
    assertThat(action.commentsFailed()).isZero();
    assertThat(action.actionType()).isEqualTo("PUBLISH_INLINE_COMMENTS");
  }

  @Test
  void should_complete_publish_inline_comments_action() {
    final var started = AgentAction.PublishInlineComments.started();
    final var completed = started.completed(10, 2, true);

    assertThat(completed.commentsPosted()).isEqualTo(10);
    assertThat(completed.commentsFailed()).isEqualTo(2);
    assertThat(completed.success()).isTrue();
  }

  @Test
  void should_create_publish_summary_action() {
    final var action = AgentAction.PublishSummary.started();

    assertThat(action.summaryCommentId()).isNull();
    assertThat(action.actionType()).isEqualTo("PUBLISH_SUMMARY");
  }

  @Test
  void should_complete_publish_summary_action() {
    final var started = AgentAction.PublishSummary.started();
    final var completed = started.completed("comment-123", true);

    assertThat(completed.summaryCommentId()).isEqualTo("comment-123");
    assertThat(completed.success()).isTrue();
  }

  @Test
  void should_create_success_terminate_action() {
    final var action = AgentAction.Terminate.success("Review completed successfully");

    assertThat(action.reason()).isEqualTo("Review completed successfully");
    assertThat(action.successful()).isTrue();
    assertThat(action.completedAt()).isNotNull();
    assertThat(action.actionType()).isEqualTo("TERMINATE");
  }

  @Test
  void should_create_failure_terminate_action() {
    final var action = AgentAction.Terminate.failure("Clone timeout");

    assertThat(action.reason()).isEqualTo("Clone timeout");
    assertThat(action.successful()).isFalse();
    assertThat(action.completedAt()).isNotNull();
  }

  @Test
  void should_throw_when_terminate_reason_is_null() {
    assertThatThrownBy(() -> AgentAction.Terminate.success(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("reason");
  }

  @Test
  void should_verify_sealed_hierarchy() {
    final var permitted = AgentAction.class.getPermittedSubclasses();

    assertThat(permitted).hasSize(6);
    assertThat(permitted)
        .containsExactlyInAnyOrder(
            AgentAction.CloneRepository.class,
            AgentAction.RunTests.class,
            AgentAction.InvokeLlmReview.class,
            AgentAction.PublishInlineComments.class,
            AgentAction.PublishSummary.class,
            AgentAction.Terminate.class);
  }
}
