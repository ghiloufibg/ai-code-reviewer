package com.ghiloufi.aicode.core.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.service.CommentPlacementRouter;
import com.ghiloufi.aicode.core.domain.service.DiffLineValidator;
import com.ghiloufi.aicode.core.domain.service.GitLabDiffBuilder;
import com.ghiloufi.aicode.core.domain.service.GitLabMergeRequestMapper;
import com.ghiloufi.aicode.core.domain.service.GitLabProjectMapper;
import com.ghiloufi.aicode.core.domain.service.SCMIdentifierValidator;
import com.ghiloufi.aicode.core.service.diff.UnifiedDiffParser;
import java.lang.reflect.Method;
import org.gitlab4j.api.GitLabApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("GitLab Inline Comment Formatting Tests")
final class GitLabInlineCommentFormattingTest {

  private Method formatInlineCommentMethod;
  private GitLabAdapter gitLabAdapter;

  @BeforeEach
  void setUp() throws Exception {
    formatInlineCommentMethod =
        GitLabAdapter.class.getDeclaredMethod("formatInlineComment", ReviewResult.Issue.class);
    formatInlineCommentMethod.setAccessible(true);

    final GitLabApi mockGitLabApi = Mockito.mock(GitLabApi.class);
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
  @DisplayName("should_include_suggested_fix_for_high_confidence_issues")
  void should_include_suggested_fix_for_high_confidence_issues() throws Exception {
    final ReviewResult.Issue issue =
        ReviewResult.Issue.issueBuilder()
            .file("Test.java")
            .startLine(10)
            .severity("major")
            .title("Potential null pointer dereference")
            .suggestion("Add null check before accessing the object")
            .confidenceScore(0.85)
            .suggestedFix(
                "```diff\n  String name = user.getName();\n+ if (user != null) {\n+   user.getName();\n+ }\n```")
            .build();

    final String result = invokeFormatInlineComment(issue);

    assertThat(result).contains("**Confidence: 85%**");
    assertThat(result).contains("```suggestion:");
    assertThat(result).contains("if (user != null)");
  }

  @Test
  @DisplayName("should_not_include_suggested_fix_for_low_confidence_issues")
  void should_not_include_suggested_fix_for_low_confidence_issues() throws Exception {
    final ReviewResult.Issue issue =
        ReviewResult.Issue.issueBuilder()
            .file("Test.java")
            .startLine(10)
            .severity("minor")
            .title("Consider using a more descriptive variable name")
            .suggestion("Rename 'x' to 'userCount' for better clarity")
            .confidenceScore(0.55)
            .suggestedFix(
                "```diff\n- int x = getUserCount();\n+ int userCount = getUserCount();\n```")
            .build();

    final String result = invokeFormatInlineComment(issue);

    assertThat(result).doesNotContain("```suggestion:");
    assertThat(result).contains("**Recommendation:**");
  }

  @Test
  @DisplayName("should_not_include_suggested_fix_when_fix_is_missing")
  void should_not_include_suggested_fix_when_fix_is_missing() throws Exception {
    final ReviewResult.Issue issue =
        ReviewResult.Issue.issueBuilder()
            .file("Test.java")
            .startLine(10)
            .severity("critical")
            .title("SQL injection vulnerability")
            .suggestion("Use parameterized queries instead of string concatenation")
            .confidenceScore(0.95)
            .suggestedFix(null)
            .build();

    final String result = invokeFormatInlineComment(issue);

    assertThat(result).doesNotContain("```suggestion:");
    assertThat(result).contains("**Recommendation:**");
  }

  @Test
  @DisplayName("should_format_complete_comment_with_all_sections")
  void should_format_complete_comment_with_all_sections() throws Exception {
    final ReviewResult.Issue issue =
        ReviewResult.Issue.issueBuilder()
            .file("Auth.java")
            .startLine(42)
            .severity("critical")
            .title("Missing authentication check")
            .suggestion("Add authentication validation before processing the request")
            .confidenceScore(0.92)
            .suggestedFix(
                "```diff\n  processRequest(request);\n+ if (!isAuthenticated(request)) {\n+   throw new UnauthorizedException();\n+ }\n```")
            .build();

    final String result = invokeFormatInlineComment(issue);

    assertThat(result).contains("issue (blocking), critical: Missing authentication check");
    assertThat(result).contains("**Recommendation:** Add authentication validation");
    assertThat(result).contains("**Confidence: 92%**");
    assertThat(result).contains("```suggestion:");
    assertThat(result).contains("if (!isAuthenticated(request))");
  }

  @Test
  @DisplayName("should_handle_suggested_fix_without_trailing_newline")
  void should_handle_suggested_fix_without_trailing_newline() throws Exception {
    final ReviewResult.Issue issue =
        ReviewResult.Issue.issueBuilder()
            .file("Test.java")
            .startLine(5)
            .severity("major")
            .title("Resource leak")
            .suggestion("Use try-with-resources")
            .confidenceScore(0.88)
            .suggestedFix(
                "```diff\n- InputStream is = new FileInputStream(file);\n+ try (InputStream is = new FileInputStream(file)) {\n+   // process\n+ }\n```")
            .build();

    final String result = invokeFormatInlineComment(issue);

    assertThat(result).contains("```suggestion:");
    assertThat(result).endsWith("\n");
  }

  @Test
  @DisplayName("should_include_confidence_percentage_when_available")
  void should_include_confidence_percentage_when_available() throws Exception {
    final ReviewResult.Issue issue =
        ReviewResult.Issue.issueBuilder()
            .file("Test.java")
            .startLine(10)
            .severity("major")
            .title("Test issue")
            .suggestion("Test suggestion")
            .confidenceScore(0.73)
            .suggestedFix("```diff\n- broken code\n+ fixed code\n```")
            .build();

    final String result = invokeFormatInlineComment(issue);

    assertThat(result).contains("**Confidence: 73%**");
  }

  @Test
  @DisplayName("should_work_with_exactly_70_percent_confidence_threshold")
  void should_work_with_exactly_70_percent_confidence_threshold() throws Exception {
    final ReviewResult.Issue issue =
        ReviewResult.Issue.issueBuilder()
            .file("Test.java")
            .startLine(10)
            .severity("major")
            .title("Test issue")
            .suggestion("Test suggestion")
            .confidenceScore(0.70)
            .suggestedFix("```diff\n- broken code\n+ fixed code\n```")
            .build();

    final String result = invokeFormatInlineComment(issue);

    assertThat(result).contains("```suggestion:");
    assertThat(result).contains("**Confidence: 70%**");
  }

  private String invokeFormatInlineComment(final ReviewResult.Issue issue) throws Exception {
    return (String) formatInlineCommentMethod.invoke(gitLabAdapter, issue);
  }
}
