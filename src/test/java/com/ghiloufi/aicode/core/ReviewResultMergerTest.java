package com.ghiloufi.aicode.core;

import static org.junit.jupiter.api.Assertions.*;

import com.ghiloufi.aicode.domain.ReviewResult;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests unitaires pour la classe IssueAggregator.
 *
 * <p>Couvre les cas normaux, les cas limites et les cas d'erreur pour assurer la robustesse de la
 * fusion des résultats d'analyse.
 */
@DisplayName("IssueAggregator Tests")
class ReviewResultMergerTest {

  private ReviewResultMerger aggregator;

  @BeforeEach
  void setUp() {
    aggregator = new ReviewResultMerger();
  }

  /** Crée un ReviewResult avec un résumé seulement. */
  private ReviewResult createReviewResult(String summary) {
    ReviewResult result = new ReviewResult();
    result.summary = summary;
    return result;
  }

  /** Crée un ReviewResult avec résumé, une issue et une note. */
  private ReviewResult createReviewResult(
      String summary, ReviewResult.Issue issue, ReviewResult.Note note) {
    ReviewResult result = createReviewResult(summary);
    if (issue != null) result.issues.add(issue);
    if (note != null) result.non_blocking_notes.add(note);
    return result;
  }

  /** Crée un ReviewResult avec résumé et une issue. */
  private ReviewResult createReviewResult(String summary, ReviewResult.Issue issue) {
    return createReviewResult(summary, issue, null);
  }

  // ===== MÉTHODES UTILITAIRES =====

  /** Crée un ReviewResult avec résumé, deux issues et une note. */
  private ReviewResult createReviewResult(
      String summary,
      ReviewResult.Issue issue1,
      ReviewResult.Issue issue2,
      ReviewResult.Note note) {
    ReviewResult result = createReviewResult(summary);
    if (issue1 != null) result.issues.add(issue1);
    if (issue2 != null) result.issues.add(issue2);
    if (note != null) result.non_blocking_notes.add(note);
    return result;
  }

  /** Crée un ReviewResult avec plusieurs issues et notes. */
  private ReviewResult createReviewResultWithMultipleItems(
      String summary, List<String> issueTitles, List<String> noteTexts) {
    ReviewResult result = createReviewResult(summary);

    for (int i = 0; i < issueTitles.size(); i++) {
      result.issues.add(createIssue("file.java", i + 1, "INFO", issueTitles.get(i)));
    }

    for (int i = 0; i < noteTexts.size(); i++) {
      result.non_blocking_notes.add(createNote("file.java", i + 1, noteTexts.get(i)));
    }

    return result;
  }

  /** Crée une Issue de test. */
  private ReviewResult.Issue createIssue(String file, int line, String severity, String title) {
    ReviewResult.Issue issue = new ReviewResult.Issue();
    issue.file = file;
    issue.start_line = line;
    issue.end_line = line;
    issue.severity = severity;
    issue.title = title;
    issue.rule_id = "TEST_RULE_" + line;
    issue.rationale = "Rationale for " + title;
    issue.suggestion = "Suggestion for " + title;
    issue.references = Arrays.asList("https://example.com/rule/" + line);
    issue.hunk_index = 0;
    return issue;
  }

  /** Crée une Note de test. */
  private ReviewResult.Note createNote(String file, int line, String noteText) {
    ReviewResult.Note note = new ReviewResult.Note();
    note.file = file;
    note.line = line;
    note.note = noteText;
    return note;
  }

  @Nested
  @DisplayName("Tests de la méthode merge")
  class MergeTests {

    @Test
    @DisplayName("Doit lancer IllegalArgumentException si la liste est null")
    void merge_ShouldThrowException_WhenListIsNull() {
      // Given
      List<ReviewResult> nullList = null;

      // When & Then
      IllegalArgumentException exception =
          assertThrows(IllegalArgumentException.class, () -> aggregator.merge(nullList).block());

      assertEquals(
          "La liste des résultats d'analyse ne peut pas être null", exception.getMessage());
    }

    @Test
    @DisplayName("Doit retourner un résultat vide si la liste est vide")
    void merge_ShouldReturnEmptyResult_WhenListIsEmpty() {
      // Given
      List<ReviewResult> emptyList = new ArrayList<>();

      // When
      ReviewResult result = aggregator.merge(emptyList).block();

      // Then
      assertNotNull(result);
      assertNull(result.summary);
      assertTrue(result.issues.isEmpty());
      assertTrue(result.non_blocking_notes.isEmpty());
    }

    @Test
    @DisplayName("Doit fusionner correctement un seul résultat")
    void merge_ShouldHandleSingleResult_Correctly() {
      // Given
      ReviewResult singleResult =
          createReviewResult(
              "Analyse terminée",
              createIssue("file1.java", 10, "ERROR", "Erreur critique"),
              createNote("file1.java", 5, "Note informative"));
      List<ReviewResult> singleList = Arrays.asList(singleResult);

      // When
      ReviewResult result = aggregator.merge(singleList).block();

      // Then
      assertEquals("Analyse terminée", result.summary);
      assertEquals(1, result.issues.size());
      assertEquals(1, result.non_blocking_notes.size());
      assertEquals("Erreur critique", result.issues.get(0).title);
      assertEquals("Note informative", result.non_blocking_notes.get(0).note);
    }

    @Test
    @DisplayName("Doit fusionner correctement plusieurs résultats complets")
    void merge_ShouldMergeMultipleCompleteResults_Correctly() {
      // Given
      ReviewResult result1 =
          createReviewResult(
              "Première analyse",
              createIssue("file1.java", 10, "ERROR", "Erreur 1"),
              createNote("file1.java", 5, "Note 1"));

      ReviewResult result2 =
          createReviewResult(
              "Seconde analyse",
              createIssue("file2.java", 20, "WARNING", "Attention 2"),
              createNote("file2.java", 15, "Note 2"));

      List<ReviewResult> results = Arrays.asList(result1, result2);

      // When
      ReviewResult merged = aggregator.merge(results).block();

      // Then
      assertEquals("Première analyse Seconde analyse", merged.summary);
      assertEquals(2, merged.issues.size());
      assertEquals(2, merged.non_blocking_notes.size());

      // Vérifier l'ordre de préservation
      assertEquals("Erreur 1", merged.issues.get(0).title);
      assertEquals("Attention 2", merged.issues.get(1).title);
      assertEquals("Note 1", merged.non_blocking_notes.get(0).note);
      assertEquals("Note 2", merged.non_blocking_notes.get(1).note);
    }

    @Test
    @DisplayName("Doit ignorer les résumés null ou vides")
    void merge_ShouldIgnoreNullAndEmptySummaries() {
      // Given
      ReviewResult result1 = createReviewResult(null);
      ReviewResult result2 = createReviewResult("");
      ReviewResult result3 = createReviewResult("   ");
      ReviewResult result4 = createReviewResult("Résumé valide");
      ReviewResult result5 = createReviewResult("Autre résumé");

      List<ReviewResult> results = Arrays.asList(result1, result2, result3, result4, result5);

      // When
      ReviewResult merged = aggregator.merge(results).block();

      // Then
      assertEquals("Résumé valide Autre résumé", merged.summary);
    }

    @Test
    @DisplayName("Doit gérer les résultats avec des listes null")
    void merge_ShouldHandleNullLists_Gracefully() {
      // Given
      ReviewResult resultWithNullLists = new ReviewResult();
      resultWithNullLists.summary = "Test";
      resultWithNullLists.issues = null;
      resultWithNullLists.non_blocking_notes = null;

      ReviewResult normalResult =
          createReviewResult(
              "Normal",
              createIssue("file.java", 1, "INFO", "Info"),
              createNote("file.java", 1, "Note"));

      List<ReviewResult> results = Arrays.asList(resultWithNullLists, normalResult);

      // When
      ReviewResult merged = aggregator.merge(results).block();

      // Then
      assertEquals("Test Normal", merged.summary);
      assertEquals(1, merged.issues.size());
      assertEquals(1, merged.non_blocking_notes.size());
    }

    @Test
    @DisplayName("Doit ignorer les ReviewResult null dans la liste")
    void merge_ShouldIgnoreNullReviewResults() {
      // Given
      ReviewResult validResult = createReviewResult("Valide");
      List<ReviewResult> resultsWithNull =
          Arrays.asList(validResult, null, createReviewResult("Autre valide"), null);

      // When
      ReviewResult merged = aggregator.merge(resultsWithNull).block();

      // Then
      assertEquals("Valide Autre valide", merged.summary);
    }

    @Test
    @DisplayName("Doit fusionner de nombreux résultats sans problème de performance")
    void merge_ShouldHandleLargeNumberOfResults() {
      // Given
      List<ReviewResult> manyResults = new ArrayList<>();
      for (int i = 0; i < 1000; i++) {
        manyResults.add(
            createReviewResult(
                "Résumé " + i,
                createIssue("file" + i + ".java", i, "INFO", "Issue " + i),
                createNote("file" + i + ".java", i, "Note " + i)));
      }

      // When
      long startTime = System.currentTimeMillis();
      ReviewResult merged = aggregator.merge(manyResults).block();
      long endTime = System.currentTimeMillis();

      // Then
      assertTrue(endTime - startTime < 1000, "La fusion ne devrait pas prendre plus d'1 seconde");
      assertEquals(1000, merged.issues.size());
      assertEquals(1000, merged.non_blocking_notes.size());
      assertTrue(merged.summary.contains("Résumé 0"));
      assertTrue(merged.summary.contains("Résumé 999"));
    }
  }

  @Nested
  @DisplayName("Tests de la méthode getStatistics")
  class GetStatisticsTests {

    @Test
    @DisplayName("Doit lancer IllegalArgumentException si result est null")
    void getStatistics_ShouldThrowException_WhenResultIsNull() {
      // When & Then
      IllegalArgumentException exception =
          assertThrows(IllegalArgumentException.class, () -> aggregator.getStatistics(null));

      assertEquals("Le résultat d'analyse ne peut pas être null", exception.getMessage());
    }

    @Test
    @DisplayName("Doit retourner les bonnes statistiques pour un résultat vide")
    void getStatistics_ShouldReturnCorrectStats_ForEmptyResult() {
      // Given
      ReviewResult emptyResult = new ReviewResult();

      // When
      String stats = aggregator.getStatistics(emptyResult);

      // Then
      assertEquals("Statistiques: 0 issues, 0 notes, résumé: absent", stats);
    }

    @Test
    @DisplayName("Doit retourner les bonnes statistiques pour un résultat complet")
    void getStatistics_ShouldReturnCorrectStats_ForCompleteResult() {
      // Given
      ReviewResult result =
          createReviewResult(
              "Résumé présent",
              createIssue("file1.java", 1, "ERROR", "Issue 1"),
              createIssue("file2.java", 2, "WARNING", "Issue 2"),
              createNote("file1.java", 1, "Note 1"));

      // When
      String stats = aggregator.getStatistics(result);

      // Then
      assertEquals("Statistiques: 2 issues, 1 notes, résumé: présent", stats);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Doit considérer les résumés vides comme absents")
    void getStatistics_ShouldConsiderEmptySummariesAsAbsent(String summary) {
      // Given
      ReviewResult result = new ReviewResult();
      result.summary = summary;

      // When
      String stats = aggregator.getStatistics(result);

      // Then
      assertTrue(stats.contains("résumé: absent"));
    }

    @Test
    @DisplayName("Doit gérer les listes null gracieusement")
    void getStatistics_ShouldHandleNullLists_Gracefully() {
      // Given
      ReviewResult result = new ReviewResult();
      result.summary = "Test";
      result.issues = null;
      result.non_blocking_notes = null;

      // When
      String stats = aggregator.getStatistics(result);

      // Then
      assertEquals("Statistiques: 0 issues, 0 notes, résumé: présent", stats);
    }
  }

  @Nested
  @DisplayName("Tests d'intégration")
  class IntegrationTests {

    @Test
    @DisplayName("Doit préserver l'ordre des éléments lors de la fusion")
    void integration_ShouldPreserveOrderDuringMerge() {
      // Given
      List<ReviewResult> results =
          Arrays.asList(
              createReviewResultWithMultipleItems(
                  "Premier",
                  Arrays.asList("Issue A", "Issue B"),
                  Arrays.asList("Note A", "Note B")),
              createReviewResultWithMultipleItems(
                  "Second",
                  Arrays.asList("Issue C", "Issue D"),
                  Arrays.asList("Note C", "Note D")));

      // When
      ReviewResult merged = aggregator.merge(results).block();

      // Then
      assertEquals("Premier Second", merged.summary);

      // Vérifier l'ordre des issues
      assertEquals("Issue A", merged.issues.get(0).title);
      assertEquals("Issue B", merged.issues.get(1).title);
      assertEquals("Issue C", merged.issues.get(2).title);
      assertEquals("Issue D", merged.issues.get(3).title);

      // Vérifier l'ordre des notes
      assertEquals("Note A", merged.non_blocking_notes.get(0).note);
      assertEquals("Note B", merged.non_blocking_notes.get(1).note);
      assertEquals("Note C", merged.non_blocking_notes.get(2).note);
      assertEquals("Note D", merged.non_blocking_notes.get(3).note);
    }

    @Test
    @DisplayName("Workflow complet: fusion puis statistiques")
    void integration_CompleteFusionAndStatisticsWorkflow() {
      // Given
      List<ReviewResult> results =
          Arrays.asList(
              createReviewResult(
                  "Analyse 1", createIssue("file1.java", 10, "ERROR", "Erreur critique")),
              createReviewResult(
                  "Analyse 2",
                  createIssue("file2.java", 20, "WARNING", "Attention"),
                  createNote("file2.java", 21, "Suggestion d'amélioration")),
              createReviewResult(
                  "Analyse 3", createIssue("file3.java", 30, "ERROR", "Note informative")));

      // When
      ReviewResult merged = aggregator.merge(results).block();
      String stats = aggregator.getStatistics(merged);

      // Then
      assertEquals("Analyse 1 Analyse 2 Analyse 3", merged.summary);
      assertEquals("Statistiques: 3 issues, 1 notes, résumé: présent", stats);
      assertEquals(4, merged.getTotalItemCount()); // Utilise la méthode de ReviewResult
      assertTrue(merged.hasContent()); // Utilise la méthode de ReviewResult
    }
  }
}
