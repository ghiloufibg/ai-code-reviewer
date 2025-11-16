package com.ghiloufi.aicode.core.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.ReviewResult;
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
            Mockito.mock(com.ghiloufi.aicode.core.service.diff.UnifiedDiffParser.class),
            Mockito.mock(com.ghiloufi.aicode.core.domain.service.SCMIdentifierValidator.class),
            Mockito.mock(com.ghiloufi.aicode.core.domain.service.GitLabDiffBuilder.class),
            Mockito.mock(com.ghiloufi.aicode.core.domain.service.GitLabMergeRequestMapper.class),
            Mockito.mock(com.ghiloufi.aicode.core.domain.service.GitLabProjectMapper.class),
            Mockito.mock(com.ghiloufi.aicode.core.domain.service.DiffLineValidator.class),
            Mockito.mock(com.ghiloufi.aicode.core.domain.service.CommentPlacementRouter.class));
  }

  @Test
  @DisplayName("should_include_suggested_fix_for_high_confidence_issues")
  void should_include_suggested_fix_for_high_confidence_issues() throws Exception {
    final ReviewResult.Issue issue = new ReviewResult.Issue();
    issue.file = "Test.java";
    issue.start_line = 10;
    issue.severity = "major";
    issue.title = "Potential null pointer dereference";
    issue.suggestion = "Add null check before accessing the object";
    issue.confidenceScore = 0.85;
    issue.suggestedFix =
        "```diff\n  String name = user.getName();\n+ if (user != null) {\n+   user.getName();\n+ }\n```";

    final String result = invokeFormatInlineComment(issue);

    assertThat(result).contains("**Confidence: 85%**");
    assertThat(result).contains("```suggestion:");
    assertThat(result).contains("if (user != null)");
  }

  @Test
  @DisplayName("should_not_include_suggested_fix_for_low_confidence_issues")
  void should_not_include_suggested_fix_for_low_confidence_issues() throws Exception {
    final ReviewResult.Issue issue = new ReviewResult.Issue();
    issue.file = "Test.java";
    issue.start_line = 10;
    issue.severity = "minor";
    issue.title = "Consider using a more descriptive variable name";
    issue.suggestion = "Rename 'x' to 'userCount' for better clarity";
    issue.confidenceScore = 0.55;
    issue.suggestedFix =
        "```diff\n- int x = getUserCount();\n+ int userCount = getUserCount();\n```";

    final String result = invokeFormatInlineComment(issue);

    assertThat(result).doesNotContain("```suggestion:");
    assertThat(result).contains("**Recommendation:**");
  }

  @Test
  @DisplayName("should_not_include_suggested_fix_when_fix_is_missing")
  void should_not_include_suggested_fix_when_fix_is_missing() throws Exception {
    final ReviewResult.Issue issue = new ReviewResult.Issue();
    issue.file = "Test.java";
    issue.start_line = 10;
    issue.severity = "critical";
    issue.title = "SQL injection vulnerability";
    issue.suggestion = "Use parameterized queries instead of string concatenation";
    issue.confidenceScore = 0.95;
    issue.suggestedFix = null;

    final String result = invokeFormatInlineComment(issue);

    assertThat(result).doesNotContain("```suggestion:");
    assertThat(result).contains("**Recommendation:**");
  }

  @Test
  @DisplayName("should_format_complete_comment_with_all_sections")
  void should_format_complete_comment_with_all_sections() throws Exception {
    final ReviewResult.Issue issue = new ReviewResult.Issue();
    issue.file = "Auth.java";
    issue.start_line = 42;
    issue.severity = "critical";
    issue.title = "Missing authentication check";
    issue.suggestion = "Add authentication validation before processing the request";
    issue.confidenceScore = 0.92;
    issue.suggestedFix =
        "```diff\n  processRequest(request);\n+ if (!isAuthenticated(request)) {\n+   throw new UnauthorizedException();\n+ }\n```";

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
    final ReviewResult.Issue issue = new ReviewResult.Issue();
    issue.file = "Test.java";
    issue.start_line = 5;
    issue.severity = "major";
    issue.title = "Resource leak";
    issue.suggestion = "Use try-with-resources";
    issue.confidenceScore = 0.88;
    issue.suggestedFix =
        "```diff\n- InputStream is = new FileInputStream(file);\n+ try (InputStream is = new FileInputStream(file)) {\n+   // process\n+ }\n```";

    final String result = invokeFormatInlineComment(issue);

    assertThat(result).contains("```suggestion:");
    assertThat(result).endsWith("\n");
  }

  @Test
  @DisplayName("should_include_confidence_percentage_when_available")
  void should_include_confidence_percentage_when_available() throws Exception {
    final ReviewResult.Issue issue = new ReviewResult.Issue();
    issue.file = "Test.java";
    issue.start_line = 10;
    issue.severity = "major";
    issue.title = "Test issue";
    issue.suggestion = "Test suggestion";
    issue.confidenceScore = 0.73;
    issue.suggestedFix = "```diff\n- broken code\n+ fixed code\n```";

    final String result = invokeFormatInlineComment(issue);

    assertThat(result).contains("**Confidence: 73%**");
  }

  @Test
  @DisplayName("should_work_with_exactly_70_percent_confidence_threshold")
  void should_work_with_exactly_70_percent_confidence_threshold() throws Exception {
    final ReviewResult.Issue issue = new ReviewResult.Issue();
    issue.file = "Test.java";
    issue.start_line = 10;
    issue.severity = "major";
    issue.title = "Test issue";
    issue.suggestion = "Test suggestion";
    issue.confidenceScore = 0.70;
    issue.suggestedFix = "```diff\n- broken code\n+ fixed code\n```";

    final String result = invokeFormatInlineComment(issue);

    assertThat(result).contains("```suggestion:");
    assertThat(result).contains("**Confidence: 70%**");
  }

  private String invokeFormatInlineComment(final ReviewResult.Issue issue) throws Exception {
    return (String) formatInlineCommentMethod.invoke(gitLabAdapter, issue);
  }
}
