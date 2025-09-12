package com.ghiloufi.aicode.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ghiloufi.aicode.domain.*;
import com.ghiloufi.aicode.domain.DiffHunkBlock;
import com.ghiloufi.aicode.github.GithubClient;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests unitaires pour la classe GitHubReviewPublisher.
 *
 * <p>Ces tests couvrent tous les scénarios principaux : - Publication réussie avec commentaires
 * inline - Gestion des cas d'erreur et des données manquantes - Validation des entrées -
 * Construction correcte des contenus de commentaires
 */
@ExtendWith(MockitoExtension.class)
class GitHubReviewPublisherTest {

  private static final int TEST_PR_NUMBER = 123;
  @Mock private GithubClient mockGithubClient;
  @Mock private GitHubDiffPositionMapper mockPositionMapper;
  @Captor private ArgumentCaptor<String> summaryCaptor;
  @Captor private ArgumentCaptor<List<GithubClient.ReviewComment>> reviewCommentsCaptor;
  private GitHubReviewPublisher publisher;

  private static StringAssertion assertThat(String actual) {
    return new StringAssertion(actual);
  }

  @BeforeEach
  void setUp() {
    publisher = new GitHubReviewPublisher(mockGithubClient);
  }

  private ReviewResult createValidReviewResult() {
    ReviewResult result = new ReviewResult();
    result.summary = "Code analysis completed successfully";
    result.issues =
        Arrays.asList(
            createValidIssue("src/main/java/Test.java", 10, "ERROR", "Critical issue found"),
            createValidIssue("src/main/java/Test.java", 20, "WARNING", "Minor issue detected"));
    return result;
  }

  private ReviewResult createReviewResultWithoutIssues() {
    ReviewResult result = new ReviewResult();
    result.summary = "Code analysis completed successfully";
    result.issues = Collections.emptyList();
    return result;
  }

  private ReviewResult.Issue createValidIssue(
      String file, int line, String severity, String title) {
    ReviewResult.Issue issue = new ReviewResult.Issue();
    issue.file = file;
    issue.start_line = line;
    issue.severity = severity;
    issue.title = title;
    issue.rationale = "Test rationale for " + title;
    issue.suggestion = "Test suggestion for " + title;
    return issue;
  }

  private DiffAnalysisBundle createValidDiffBundle() {
    GitDiffDocument gitDiffDocument = new GitDiffDocument();
    gitDiffDocument.files = Collections.singletonList(createValidFileDiff());
    return new DiffAnalysisBundle(gitDiffDocument, "mock diff content");
  }

  // Helper methods pour créer des objets de test

  private GitFileModification createValidFileDiff() {
    GitFileModification gitFileModification = new GitFileModification();
    gitFileModification.newPath = "src/main/java/Test.java";
    gitFileModification.oldPath = "src/main/java/Test.java";

    DiffHunkBlock diffHunkBlock = new DiffHunkBlock();
    diffHunkBlock.newStart = 1;
    diffHunkBlock.lines =
        Arrays.asList(" public class Test {", "+ // New line added", " // Existing line");

    gitFileModification.diffHunkBlocks = Collections.singletonList(diffHunkBlock);
    return gitFileModification;
  }

  private GitHubReviewPublisher createPublisherWithMockedMapper() {
    // Cette méthode simule l'injection d'un mapper mocké
    // En pratique, vous pourriez utiliser des frameworks comme Spring pour l'injection
    return new GitHubReviewPublisher(mockGithubClient) {
      @Override
      protected GitHubDiffPositionMapper createPositionMapper(GitDiffDocument diff) {
        return mockPositionMapper;
      }
    };
  }

  // Custom assertion helper
  private static class StringAssertion {
    private final String actual;

    private StringAssertion(String actual) {
      this.actual = actual;
    }

    public StringAssertion contains(String expected) {
      assertTrue(
          actual.contains(expected),
          String.format("Expected string to contain '%s' but was '%s'", expected, actual));
      return this;
    }

    public StringAssertion doesNotContain(String unexpected) {
      assertFalse(
          actual.contains(unexpected),
          String.format("Expected string to not contain '%s' but was '%s'", unexpected, actual));
      return this;
    }
  }

  @Nested
  @DisplayName("Construction et validation")
  class ConstructorAndValidationTests {

    @Test
    @DisplayName("Doit lever une exception si le client GitHub est null")
    void shouldThrowExceptionWhenGithubClientIsNull() {
      IllegalArgumentException exception =
          assertThrows(IllegalArgumentException.class, () -> new GitHubReviewPublisher(null));
      assertEquals("GithubClient ne peut pas être null", exception.getMessage());
    }

    @Test
    @DisplayName("Doit créer l'instance avec succès quand le client est valide")
    void shouldCreateInstanceSuccessfully() {
      assertDoesNotThrow(() -> new GitHubReviewPublisher(mockGithubClient));
    }
  }

  @Nested
  @DisplayName("Publication - Cas de succès")
  class SuccessfulPublicationTests {

    @Test
    @DisplayName("Doit publier un commentaire de synthèse et des commentaires inline avec succès")
    void shouldPublishSummaryAndInlineCommentsSuccessfully() {
      // Given
      ReviewResult reviewResult = createValidReviewResult();
      DiffAnalysisBundle diffAnalysisBundle = createValidDiffBundle();

      // Mock the position mapper to return valid positions
      GitHubReviewPublisher publisherWithMockedMapper = createPublisherWithMockedMapper();
      when(mockPositionMapper.positionFor("src/main/java/Test.java", 10)).thenReturn(5);
      when(mockPositionMapper.positionFor("src/main/java/Test.java", 20)).thenReturn(15);

      // When
      publisherWithMockedMapper.publish(TEST_PR_NUMBER, reviewResult, diffAnalysisBundle);

      // Then
      verify(mockGithubClient).postIssueComment(eq(TEST_PR_NUMBER), summaryCaptor.capture());
      verify(mockGithubClient).createReview(eq(TEST_PR_NUMBER), reviewCommentsCaptor.capture());

      // Verify summary content
      String summaryComment = summaryCaptor.getValue();
      assertThat(summaryComment)
          .contains("🤖 AI Review Summary")
          .contains("Code analysis completed successfully")
          .contains("**Findings:** 2 issue(s)");

      // Verify inline comments
      List<GithubClient.ReviewComment> reviewComments = reviewCommentsCaptor.getValue();
      assertEquals(2, reviewComments.size());
    }

    @Test
    @DisplayName("Doit publier seulement le résumé quand aucune issue n'est présente")
    void shouldPublishOnlySummaryWhenNoIssues() {
      // Given
      ReviewResult reviewResult = createReviewResultWithoutIssues();
      DiffAnalysisBundle diffAnalysisBundle = createValidDiffBundle();

      // When
      publisher.publish(TEST_PR_NUMBER, reviewResult, diffAnalysisBundle);

      // Then
      verify(mockGithubClient).postIssueComment(eq(TEST_PR_NUMBER), summaryCaptor.capture());
      verify(mockGithubClient, never()).createReview(anyInt(), any());

      String summaryComment = summaryCaptor.getValue();
      assertThat(summaryComment).contains("**Findings:** 0 issue(s)");
    }

    @Test
    @DisplayName("Doit gérer correctement un résumé avec du contenu riche")
    void shouldHandleRichSummaryContent() {
      // Given
      ReviewResult reviewResult = new ReviewResult();
      reviewResult.summary =
          "  Detailed analysis with **markdown** and multiple lines.\n\nSecond paragraph.  ";
      reviewResult.issues = Collections.emptyList();

      // When
      publisher.publish(TEST_PR_NUMBER, reviewResult, null);

      // Then
      verify(mockGithubClient).postIssueComment(eq(TEST_PR_NUMBER), summaryCaptor.capture());
      String summaryComment = summaryCaptor.getValue();
      assertThat(summaryComment)
          .contains("Detailed analysis with **markdown** and multiple lines.\n\nSecond paragraph.");
    }
  }

  @Nested
  @DisplayName("Publication - Cas d'erreur")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Doit gérer gracieusement un ReviewResult null")
    void shouldHandleNullReviewResultGracefully() {
      // When & Then
      assertDoesNotThrow(() -> publisher.publish(TEST_PR_NUMBER, null, null));
      verify(mockGithubClient, never()).postIssueComment(anyInt(), anyString());
      verify(mockGithubClient, never()).createReview(anyInt(), any());
    }

    @Test
    @DisplayName("Doit publier seulement le résumé quand le DiffBundle est null")
    void shouldPublishOnlySummaryWhenDiffBundleIsNull() {
      // Given
      ReviewResult reviewResult = createValidReviewResult();

      // When
      publisher.publish(TEST_PR_NUMBER, reviewResult, null);

      // Then
      verify(mockGithubClient).postIssueComment(eq(TEST_PR_NUMBER), any());
      verify(mockGithubClient, never()).createReview(anyInt(), any());
    }

    @Test
    @DisplayName("Doit publier seulement le résumé quand le diff est null")
    @Disabled("A voir si le structuredDiff peut etre null ou pas.")
    void shouldPublishOnlySummaryWhenDiffIsNull() {
      // Given
      ReviewResult reviewResult = createValidReviewResult();
      DiffAnalysisBundle diffAnalysisBundle = new DiffAnalysisBundle(null, "some diff content");

      // When
      publisher.publish(TEST_PR_NUMBER, reviewResult, diffAnalysisBundle);

      // Then
      verify(mockGithubClient).postIssueComment(eq(TEST_PR_NUMBER), any());
      verify(mockGithubClient, never()).createReview(anyInt(), any());
    }

    @Test
    @DisplayName("Doit ignorer les issues null dans la liste")
    void shouldIgnoreNullIssuesInList() {
      // Given
      ReviewResult reviewResult = new ReviewResult();
      reviewResult.summary = "Test summary";
      reviewResult.issues =
          Arrays.asList(
              createValidIssue("src/Test.java", 10, "ERROR", "Test issue"),
              null,
              createValidIssue("src/Test.java", 20, "WARNING", "Another issue"));

      DiffAnalysisBundle diffAnalysisBundle = createValidDiffBundle();
      GitHubReviewPublisher publisherWithMockedMapper = createPublisherWithMockedMapper();
      when(mockPositionMapper.positionFor(anyString(), anyInt())).thenReturn(5);

      // When
      publisherWithMockedMapper.publish(TEST_PR_NUMBER, reviewResult, diffAnalysisBundle);

      // Then
      verify(mockGithubClient).createReview(eq(TEST_PR_NUMBER), reviewCommentsCaptor.capture());
      List<GithubClient.ReviewComment> reviewComments = reviewCommentsCaptor.getValue();
      assertEquals(2, reviewComments.size()); // Only non-null issues
    }

    @Test
    @DisplayName("Doit gérer les issues qui ne peuvent pas être mappées")
    void shouldHandleUnmappableIssues() {
      // Given
      ReviewResult reviewResult = createValidReviewResult();
      DiffAnalysisBundle diffAnalysisBundle = createValidDiffBundle();

      GitHubReviewPublisher publisherWithMockedMapper = createPublisherWithMockedMapper();
      when(mockPositionMapper.positionFor(anyString(), anyInt())).thenReturn(-1); // Cannot map

      // When
      publisherWithMockedMapper.publish(TEST_PR_NUMBER, reviewResult, diffAnalysisBundle);

      // Then
      verify(mockGithubClient).postIssueComment(eq(TEST_PR_NUMBER), any());
      verify(mockGithubClient, never()).createReview(anyInt(), any()); // No inline comments created
    }
  }

  @Nested
  @DisplayName("Construction du contenu des commentaires")
  class CommentContentBuildingTests {

    @Test
    @DisplayName("Doit construire un commentaire inline complet avec tous les champs")
    void shouldBuildCompleteInlineComment() {
      // Given
      ReviewResult.Issue issue = new ReviewResult.Issue();
      issue.file = "src/main/java/Test.java";
      issue.start_line = 10;
      issue.severity = "error";
      issue.title = "Null pointer risk";
      issue.rationale = "This code may throw NullPointerException";
      issue.suggestion = "Add null check before accessing the object";
      issue.references =
          Arrays.asList(
              "https://docs.oracle.com/javase/8/docs/api/java/lang/NullPointerException.html");

      ReviewResult reviewResult = new ReviewResult();
      reviewResult.summary = "Test";
      reviewResult.issues = Collections.singletonList(issue);

      DiffAnalysisBundle diffAnalysisBundle = createValidDiffBundle();
      GitHubReviewPublisher publisherWithMockedMapper = createPublisherWithMockedMapper();
      when(mockPositionMapper.positionFor("src/main/java/Test.java", 10)).thenReturn(5);

      // When
      publisherWithMockedMapper.publish(TEST_PR_NUMBER, reviewResult, diffAnalysisBundle);

      // Then
      verify(mockGithubClient).createReview(eq(TEST_PR_NUMBER), reviewCommentsCaptor.capture());
      List<GithubClient.ReviewComment> reviewComments = reviewCommentsCaptor.getValue();

      assertEquals(1, reviewComments.size());
      String commentBody = reviewComments.get(0).body();

      assertThat(commentBody)
          .contains("**ERROR** Null pointer risk")
          .contains("This code may throw NullPointerException")
          .contains("**Suggestion:**")
          .contains("Add null check before accessing the object")
          .contains(
              "**References:** https://docs.oracle.com/javase/8/docs/api/java/lang/NullPointerException.html");
    }

    @Test
    @DisplayName("Doit construire un commentaire minimal avec seulement les champs requis")
    void shouldBuildMinimalInlineComment() {
      // Given
      ReviewResult.Issue issue = new ReviewResult.Issue();
      issue.file = "src/main/java/Test.java";
      issue.start_line = 10;
      issue.severity = null; // Will default to "info"
      issue.title = null; // Will default to ""
      issue.rationale = null;
      issue.suggestion = "";
      issue.references = null;

      ReviewResult reviewResult = new ReviewResult();
      reviewResult.summary = "Test";
      reviewResult.issues = Collections.singletonList(issue);

      DiffAnalysisBundle diffAnalysisBundle = createValidDiffBundle();
      GitHubReviewPublisher publisherWithMockedMapper = createPublisherWithMockedMapper();
      when(mockPositionMapper.positionFor("src/main/java/Test.java", 10)).thenReturn(5);

      // When
      publisherWithMockedMapper.publish(TEST_PR_NUMBER, reviewResult, diffAnalysisBundle);

      // Then
      verify(mockGithubClient).createReview(eq(TEST_PR_NUMBER), reviewCommentsCaptor.capture());
      List<GithubClient.ReviewComment> reviewComments = reviewCommentsCaptor.getValue();

      assertEquals(1, reviewComments.size());
      String commentBody = reviewComments.get(0).body();

      assertThat(commentBody)
          .contains("**INFO**") // Default severity
          .doesNotContain("**Suggestion:**") // Empty suggestion not included
          .doesNotContain("**References:**"); // Null references not included
    }

    @Test
    @DisplayName("Doit gérer les références multiples")
    void shouldHandleMultipleReferences() {
      // Given
      ReviewResult.Issue issue = new ReviewResult.Issue();
      issue.file = "src/main/java/Test.java";
      issue.start_line = 10;
      issue.title = "Security issue";
      issue.references =
          Arrays.asList(
              "https://owasp.org/www-project-top-ten/",
              "https://cwe.mitre.org/data/definitions/89.html",
              "https://docs.oracle.com/en/java/javase/11/security/");

      ReviewResult reviewResult = new ReviewResult();
      reviewResult.summary = "Test";
      reviewResult.issues = Collections.singletonList(issue);

      DiffAnalysisBundle diffAnalysisBundle = createValidDiffBundle();
      GitHubReviewPublisher publisherWithMockedMapper = createPublisherWithMockedMapper();
      when(mockPositionMapper.positionFor("src/main/java/Test.java", 10)).thenReturn(5);

      // When
      publisherWithMockedMapper.publish(TEST_PR_NUMBER, reviewResult, diffAnalysisBundle);

      // Then
      verify(mockGithubClient).createReview(eq(TEST_PR_NUMBER), reviewCommentsCaptor.capture());
      List<GithubClient.ReviewComment> reviewComments = reviewCommentsCaptor.getValue();

      String commentBody = reviewComments.get(0).body();
      assertThat(commentBody)
          .contains(
              "**References:** https://owasp.org/www-project-top-ten/, https://cwe.mitre.org/data/definitions/89.html, https://docs.oracle.com/en/java/javase/11/security/");
    }
  }

  @Nested
  @DisplayName("Gestion des résumés")
  class SummaryHandlingTests {

    @Test
    @DisplayName("Doit utiliser le message par défaut quand le résumé est null")
    void shouldUseDefaultMessageWhenSummaryIsNull() {
      // Given
      ReviewResult reviewResult = new ReviewResult();
      reviewResult.summary = null;
      reviewResult.issues = Collections.emptyList();

      // When
      publisher.publish(TEST_PR_NUMBER, reviewResult, null);

      // Then
      verify(mockGithubClient).postIssueComment(eq(TEST_PR_NUMBER), summaryCaptor.capture());
      String summaryComment = summaryCaptor.getValue();
      assertThat(summaryComment).contains("No summary");
    }

    @Test
    @DisplayName("Doit utiliser le message par défaut quand le résumé est vide")
    void shouldUseDefaultMessageWhenSummaryIsBlank() {
      // Given
      ReviewResult reviewResult = new ReviewResult();
      reviewResult.summary = "   \n\t  ";
      reviewResult.issues = Collections.emptyList();

      // When
      publisher.publish(TEST_PR_NUMBER, reviewResult, null);

      // Then
      verify(mockGithubClient).postIssueComment(eq(TEST_PR_NUMBER), summaryCaptor.capture());
      String summaryComment = summaryCaptor.getValue();
      assertThat(summaryComment).contains("No summary");
    }

    @Test
    @DisplayName("Doit nettoyer les espaces en début et fin de résumé")
    void shouldTrimSummaryWhitespace() {
      // Given
      ReviewResult reviewResult = new ReviewResult();
      reviewResult.summary = "  \n  Cleaned summary content  \n  ";
      reviewResult.issues = Collections.emptyList();

      // When
      publisher.publish(TEST_PR_NUMBER, reviewResult, null);

      // Then
      verify(mockGithubClient).postIssueComment(eq(TEST_PR_NUMBER), summaryCaptor.capture());
      String summaryComment = summaryCaptor.getValue();
      assertThat(summaryComment).contains("Cleaned summary content");
    }
  }
}
