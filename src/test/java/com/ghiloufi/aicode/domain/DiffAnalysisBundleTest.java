package com.ghiloufi.aicode.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("DiffAnalysisBundle Tests")
class DiffAnalysisBundleTest {

  // ===============================
  // BUILDERS ET FACTORY METHODS
  // ===============================

  /** Builder pattern pour créer facilement des instances de test */
  public static class DiffAnalysisBundleTestBuilder {
    private UnifiedDiff structuredDiff = new UnifiedDiff();
    private String rawDiffText = "default raw diff text";

    public DiffAnalysisBundleTestBuilder withStructuredDiff(UnifiedDiff diff) {
      this.structuredDiff = diff;
      return this;
    }

    public DiffAnalysisBundleTestBuilder withRawDiffText(String text) {
      this.rawDiffText = text;
      return this;
    }

    public DiffAnalysisBundleTestBuilder withEmptyDiff() {
      this.structuredDiff = new UnifiedDiff();
      return this;
    }

    public DiffAnalysisBundleTestBuilder withFileCount(int fileCount) {
      UnifiedDiff diff = new UnifiedDiff();
      for (int i = 0; i < fileCount; i++) {
        FileDiff file = createTestFileDiff("file" + i + ".java", 1, 10);
        diff.files.add(file);
      }
      this.structuredDiff = diff;
      return this;
    }

    public DiffAnalysisBundle build() {
      return new DiffAnalysisBundle(structuredDiff, rawDiffText);
    }
  }

  /** Factory method pour créer un builder */
  public static DiffAnalysisBundleTestBuilder aDiffAnalysisBundle() {
    return new DiffAnalysisBundleTestBuilder();
  }

  /** Crée un FileDiff de test avec des paramètres configurables */
  public static FileDiff createTestFileDiff(String fileName, int hunkCount, int linesPerHunk) {
    FileDiff fileDiff = new FileDiff();
    fileDiff.oldPath = fileName;
    fileDiff.newPath = fileName;

    for (int i = 0; i < hunkCount; i++) {
      Hunk hunk = createTestHunk(i + 1, linesPerHunk);
      fileDiff.hunks.add(hunk);
    }

    return fileDiff;
  }

  /** Crée un Hunk de test avec des paramètres configurables */
  public static Hunk createTestHunk(int startLine, int lineCount) {
    Hunk hunk = new Hunk();
    hunk.oldStart = startLine;
    hunk.oldCount = lineCount;
    hunk.newStart = startLine;
    hunk.newCount = lineCount;

    for (int i = 0; i < lineCount; i++) {
      hunk.lines.add("+ ligne de test " + (startLine + i));
    }

    return hunk;
  }

  // ===============================
  // TESTS DE CONSTRUCTION
  // ===============================

  @Nested
  @DisplayName("Construction et Validation")
  class ConstructionTests {

    @Test
    @DisplayName("Construction valide avec paramètres corrects")
    void shouldCreateValidDiffAnalysisBundle() {
      // Given
      UnifiedDiff diff = new UnifiedDiff();
      String rawText = "raw diff content";

      // When
      DiffAnalysisBundle bundle = new DiffAnalysisBundle(diff, rawText);

      // Then
      assertNotNull(bundle);
      assertEquals(diff, bundle.structuredDiff());
      assertEquals(rawText, bundle.rawDiffText());
    }

    @Test
    @DisplayName("Échec si structuredDiff est null")
    void shouldFailWhenStructuredDiffIsNull() {
      // When & Then
      NullPointerException exception =
          assertThrows(
              NullPointerException.class, () -> new DiffAnalysisBundle(null, "valid text"));

      assertEquals("Le diff structuré ne peut pas être null", exception.getMessage());
    }

    @Test
    @DisplayName("Échec si rawDiffText est null")
    void shouldFailWhenRawDiffTextIsNull() {
      // When & Then
      NullPointerException exception =
          assertThrows(
              NullPointerException.class, () -> new DiffAnalysisBundle(new UnifiedDiff(), null));

      assertEquals("Le texte brut du diff ne peut pas être null", exception.getMessage());
    }

    @ParameterizedTest
    @DisplayName("Échec si rawDiffText est vide ou ne contient que des espaces")
    @ValueSource(strings = {"", "   ", "\t", "\n", "  \t\n  "})
    void shouldFailWhenRawDiffTextIsEmpty(String emptyText) {
      // When & Then
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new DiffAnalysisBundle(new UnifiedDiff(), emptyText));

      assertEquals("Le texte brut du diff ne peut pas être vide", exception.getMessage());
    }
  }

  // ===============================
  // TESTS DE DIVISION (SPLITTING)
  // ===============================

  @Nested
  @DisplayName("Division en Chunks")
  class SplittingTests {

    @Test
    @DisplayName("Division avec taille par défaut")
    void shouldSplitWithDefaultMaxLines() {
      // Given
      DiffAnalysisBundle bundle = aDiffAnalysisBundle().withFileCount(2).build();

      // When
      List<UnifiedDiff> chunks = bundle.splitByMaxLines();

      // Then
      assertNotNull(chunks);
      assertEquals(1, chunks.size()); // Avec 20 lignes total, devrait tenir dans un chunk
    }

    @ParameterizedTest
    @DisplayName("Division avec différentes tailles de chunks")
    @MethodSource("provideSplittingTestCases")
    void shouldSplitAccordingToMaxLines(
        int fileCount, int linesPerHunk, int maxLinesPerChunk, int expectedChunks) {
      // Given
      UnifiedDiff diff = new UnifiedDiff();
      for (int i = 0; i < fileCount; i++) {
        diff.files.add(createTestFileDiff("file" + i + ".java", 1, linesPerHunk));
      }

      DiffAnalysisBundle bundle = aDiffAnalysisBundle().withStructuredDiff(diff).build();

      // When
      List<UnifiedDiff> chunks = bundle.splitByMaxLines(maxLinesPerChunk);

      // Then
      assertEquals(expectedChunks, chunks.size());

      // Vérifier que chaque chunk respecte la limite (sauf le dernier hunk trop gros)
      for (UnifiedDiff chunk : chunks) {
        int chunkLineCount =
            chunk.files.stream()
                .flatMap(file -> file.hunks.stream())
                .mapToInt(hunk -> hunk.lines.size())
                .sum();

        // Un chunk peut dépasser si un seul hunk est plus gros que la limite
        boolean isValidChunk =
            chunkLineCount <= maxLinesPerChunk
                || chunk.files.stream().flatMap(f -> f.hunks.stream()).count() == 1;

        assertTrue(
            isValidChunk, "Chunk a " + chunkLineCount + " lignes, limite: " + maxLinesPerChunk);
      }
    }

    private static Stream<Arguments> provideSplittingTestCases() {
      return Stream.of(
          Arguments.of(1, 10, 20, 1), // Un fichier, 10 lignes, limite 20 → 1 chunk
          Arguments.of(3, 10, 25, 2), // 3 fichiers, 30 lignes total, limite 25 → 2 chunks
          Arguments.of(2, 50, 30, 2), // 2 fichiers, chacun dépasse la limite → 2 chunks
          Arguments.of(5, 5, 10, 3) // 5 fichiers, 25 lignes, limite 10 → 3 chunks
          );
    }

    @Test
    @DisplayName("Division d'un diff vide retourne liste vide")
    void shouldReturnEmptyListForEmptyDiff() {
      // Given
      DiffAnalysisBundle bundle = aDiffAnalysisBundle().withEmptyDiff().build();

      // When
      List<UnifiedDiff> chunks = bundle.splitByMaxLines(10);

      // Then
      assertTrue(chunks.isEmpty());
    }

    @Test
    @DisplayName("Échec si maxLinesPerChunk est invalide")
    void shouldFailWithInvalidMaxLines() {
      // Given
      DiffAnalysisBundle bundle = aDiffAnalysisBundle().build();

      // When & Then
      IllegalArgumentException exception =
          assertThrows(IllegalArgumentException.class, () -> bundle.splitByMaxLines(0));

      assertTrue(exception.getMessage().contains("doit être positif"));
    }
  }

  // ===============================
  // TESTS DE MÉTADONNÉES
  // ===============================

  @Nested
  @DisplayName("Métadonnées et Statistiques")
  class MetadataTests {

    @ParameterizedTest
    @DisplayName("Calcul du nombre total de lignes")
    @MethodSource("provideTotalLineCountTestCases")
    void shouldCalculateTotalLineCount(
        int fileCount, int hunksPerFile, int linesPerHunk, int expectedTotal) {
      // Given
      UnifiedDiff diff = new UnifiedDiff();
      for (int i = 0; i < fileCount; i++) {
        diff.files.add(createTestFileDiff("file" + i + ".java", hunksPerFile, linesPerHunk));
      }

      DiffAnalysisBundle bundle = aDiffAnalysisBundle().withStructuredDiff(diff).build();

      // When & Then
      assertEquals(expectedTotal, bundle.getTotalLineCount());
    }

    private static Stream<Arguments> provideTotalLineCountTestCases() {
      return Stream.of(
          Arguments.of(0, 0, 0, 0), // Diff vide
          Arguments.of(1, 1, 10, 10), // 1 fichier, 1 hunk, 10 lignes
          Arguments.of(2, 2, 5, 20), // 2 fichiers, 2 hunks chacun, 5 lignes par hunk
          Arguments.of(3, 1, 15, 45) // 3 fichiers, 1 hunk chacun, 15 lignes par hunk
          );
    }

    @Test
    @DisplayName("Calcul du nombre de fichiers modifiés")
    void shouldCalculateModifiedFileCount() {
      // Given
      DiffAnalysisBundle bundle = aDiffAnalysisBundle().withFileCount(5).build();

      // When & Then
      assertEquals(5, bundle.getModifiedFileCount());
    }

    @Test
    @DisplayName("Détection des modifications")
    void shouldDetectModifications() {
      // Given
      DiffAnalysisBundle emptyBundle = aDiffAnalysisBundle().withEmptyDiff().build();

      DiffAnalysisBundle nonEmptyBundle = aDiffAnalysisBundle().withFileCount(1).build();

      // When & Then
      assertFalse(emptyBundle.hasModifications());
      assertTrue(nonEmptyBundle.hasModifications());
    }

    @Test
    @DisplayName("Génération du résumé")
    void shouldGenerateCorrectSummary() {
      // Given
      DiffAnalysisBundle emptyBundle = aDiffAnalysisBundle().withEmptyDiff().build();

      DiffAnalysisBundle nonEmptyBundle = aDiffAnalysisBundle().withFileCount(3).build();

      // When & Then
      assertEquals("Diff vide - aucune modification", emptyBundle.getSummary());
      assertEquals("Diff: 3 fichier(s) modifié(s), 30 ligne(s) total", nonEmptyBundle.getSummary());
    }
  }

  // ===============================
  // TESTS D'INTÉGRATION
  // ===============================

  @Nested
  @DisplayName("Tests d'Intégration")
  class IntegrationTests {

    @Test
    @DisplayName("Workflow complet : création, division et analyse")
    void shouldHandleCompleteWorkflow() {
      // Given - Un diff complexe avec plusieurs fichiers
      UnifiedDiff diff = new UnifiedDiff();
      diff.files.add(createTestFileDiff("src/main/java/Service.java", 2, 15));
      diff.files.add(createTestFileDiff("src/test/java/ServiceTest.java", 1, 20));
      diff.files.add(createTestFileDiff("README.md", 1, 5));

      String rawText = "git diff output content...";

      // When
      DiffAnalysisBundle bundle = new DiffAnalysisBundle(diff, rawText);
      List<UnifiedDiff> chunks = bundle.splitByMaxLines(25);

      // Then
      assertEquals(3, bundle.getModifiedFileCount());
      assertEquals(55, bundle.getTotalLineCount());
      assertTrue(bundle.hasModifications());

      assertEquals(3, chunks.size()); // Doit être divisé en 3 chunks

      // Vérifier que tous les hunks sont préservés
      int totalHunksInChunks =
          chunks.stream()
              .flatMap(chunk -> chunk.files.stream())
              .mapToInt(file -> file.hunks.size())
              .sum();

      assertEquals(4, totalHunksInChunks); // 2 + 1 + 1 hunks originaux
    }

    @Test
    @DisplayName("Performance avec un gros diff")
    void shouldHandleLargeDiff() {
      // Given - Simuler un gros diff
      UnifiedDiff largeDiff = new UnifiedDiff();
      for (int i = 0; i < 100; i++) {
        largeDiff.files.add(createTestFileDiff("file" + i + ".java", 5, 20));
      }

      DiffAnalysisBundle bundle = aDiffAnalysisBundle().withStructuredDiff(largeDiff).build();

      // When - Mesurer le temps d'exécution
      long startTime = System.currentTimeMillis();
      List<UnifiedDiff> chunks = bundle.splitByMaxLines(500);
      long endTime = System.currentTimeMillis();

      // Then
      assertEquals(100, bundle.getModifiedFileCount());
      assertEquals(10000, bundle.getTotalLineCount()); // 100 * 5 * 20
      assertTrue(chunks.size() > 1);

      // La division ne devrait pas prendre plus d'une seconde
      assertTrue(
          endTime - startTime < 1000, "Division trop lente: " + (endTime - startTime) + "ms");
    }
  }

  // ===============================
  // TESTS DES MÉTHODES UTILITAIRES
  // ===============================

  @Nested
  @DisplayName("Méthodes Utilitaires")
  class UtilityMethodsTests {

    @Test
    @DisplayName("Méthode toString")
    void shouldProvideInformativeToString() {
      // Given
      DiffAnalysisBundle bundle =
          aDiffAnalysisBundle()
              .withFileCount(2)
              .withRawDiffText("raw content with specific length")
              .build();

      // When
      String result = bundle.toString();

      // Then
      assertTrue(result.contains("DiffAnalysisBundle"));
      assertTrue(result.contains("2 fichier(s)"));
      assertTrue(result.contains("20 ligne(s)"));
      assertTrue(result.contains("32 caractères")); // longueur du texte brut
    }

    @Test
    @DisplayName("Génération du diff unifié")
    void shouldGenerateUnifiedDiffString() {
      // Given
      UnifiedDiff diff = new UnifiedDiff();
      diff.files.add(createTestFileDiff("test.java", 1, 3));

      DiffAnalysisBundle bundle = aDiffAnalysisBundle().withStructuredDiff(diff).build();

      // When
      String unifiedString = bundle.getUnifiedDiffString();

      // Then
      assertNotNull(unifiedString);
      assertTrue(unifiedString.contains("--- a/test.java"));
      assertTrue(unifiedString.contains("+++ b/test.java"));
      assertTrue(unifiedString.contains("@@"));
    }

    @Test
    @DisplayName("Configuration et statut des tests (méthodes futures)")
    void shouldReturnEmptyConfigurationAndTestStatus() {
      // Given
      DiffAnalysisBundle bundle = aDiffAnalysisBundle().build();

      // When & Then
      assertTrue(bundle.getProjectConfiguration().isEmpty());
      assertTrue(bundle.getTestStatus().isEmpty());
    }
  }
}
