package com.ghiloufi.aicode.core.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.ChangeRequestIdentifier;
import com.ghiloufi.aicode.core.domain.model.GitLabRepositoryId;
import com.ghiloufi.aicode.core.domain.model.MergeRequestId;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.service.CommentPlacementRouter;
import com.ghiloufi.aicode.core.domain.service.DiffLineValidator;
import com.ghiloufi.aicode.core.domain.service.GitLabDiffBuilder;
import com.ghiloufi.aicode.core.domain.service.GitLabMergeRequestMapper;
import com.ghiloufi.aicode.core.domain.service.GitLabProjectMapper;
import com.ghiloufi.aicode.core.domain.service.SCMIdentifierValidator;
import com.ghiloufi.aicode.core.service.diff.UnifiedDiffParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

final class GitLabAdapterSummaryCommentTest {

  private GitLabAdapter gitLabAdapter;

  @BeforeEach
  void setUp() {
    gitLabAdapter =
        new GitLabAdapter(
            "https://gitlab.com",
            "test-token",
            Mockito.mock(UnifiedDiffParser.class),
            Mockito.mock(SCMIdentifierValidator.class),
            Mockito.mock(GitLabDiffBuilder.class),
            Mockito.mock(GitLabMergeRequestMapper.class),
            Mockito.mock(GitLabProjectMapper.class),
            Mockito.mock(DiffLineValidator.class),
            Mockito.mock(CommentPlacementRouter.class));
  }

  @Test
  void should_return_mono_void_from_publish_summary_comment() {
    final RepositoryIdentifier repo = new GitLabRepositoryId("test/repo");
    final ChangeRequestIdentifier changeRequest = new MergeRequestId(1);
    final String summaryComment = "## Test Summary\n\nTest content";

    final Mono<Void> result =
        gitLabAdapter.publishSummaryComment(repo, changeRequest, summaryComment);

    assertThat(result).isNotNull();
  }

  @Test
  void should_handle_null_summary_comment_gracefully() {
    final RepositoryIdentifier repo = new GitLabRepositoryId("test/repo");
    final ChangeRequestIdentifier changeRequest = new MergeRequestId(1);

    final Mono<Void> result = gitLabAdapter.publishSummaryComment(repo, changeRequest, null);

    assertThat(result).isNotNull();
  }

  @Test
  void should_handle_empty_summary_comment_gracefully() {
    final RepositoryIdentifier repo = new GitLabRepositoryId("test/repo");
    final ChangeRequestIdentifier changeRequest = new MergeRequestId(1);

    final Mono<Void> result = gitLabAdapter.publishSummaryComment(repo, changeRequest, "");

    assertThat(result).isNotNull();
  }
}
