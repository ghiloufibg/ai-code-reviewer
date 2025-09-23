package com.ghiloufi.aicode.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests unitaires pour la classe DiffHunkBlock.
 *
 * <p>Couvre tous les scénarios : construction, analyse de contenu, validation, méthodes
 * utilitaires, et cas edge pour assurer la robustesse.
 */
@DisplayName("DiffHunkBlock Tests")
class DiffHunkBlockTest {

  private DiffHunkBlock emptyHunk;
  private DiffHunkBlock additionOnlyHunk;
  private DiffHunkBlock deletionOnlyHunk;
  private DiffHunkBlock mixedHunk;
  private DiffHunkBlock contextOnlyHunk;
  private DiffHunkBlock invalidHunk;

  @BeforeEach
  void setUp() {
    // Hunk vide
    emptyHunk = new DiffHunkBlock();

    // Hunk avec seulement des ajouts
    additionOnlyHunk = createAdditionOnlyHunk();

    // Hunk avec seulement des suppressions
    deletionOnlyHunk = createDeletionOnlyHunk();

    // Hunk mixte (ajouts + suppressions + contexte)
    mixedHunk = createMixedHunk();

    // Hunk avec seulement du contexte
    contextOnlyHunk = createContextOnlyHunk();

    // Hunk avec des compteurs incohérents
    invalidHunk = createInvalidHunk();
  }

  /** Crée un hunk avec seulement des ajouts. */
  private DiffHunkBlock createAdditionOnlyHunk() {
    return new DiffHunkBlock(
        1, 0, 1, 3, Arrays.asList("+nouvelle ligne 1", "+nouvelle ligne 2", "+nouvelle ligne 3"));
  }

  /** Crée un hunk avec seulement des suppressions. */
  private DiffHunkBlock createDeletionOnlyHunk() {
    return new DiffHunkBlock(1, 2, 1, 0, Arrays.asList("-ancienne ligne 1", "-ancienne ligne 2"));
  }

  /** Crée un hunk mixte avec ajouts, suppressions et contexte. */
  private DiffHunkBlock createMixedHunk() {
    return new DiffHunkBlock(
        10,
        4,
        10,
        4,
        Arrays.asList(
            " ligne de contexte 1",
            "-ancienne ligne 1",
            "-ancienne ligne 2",
            "+nouvelle ligne 1",
            "+nouvelle ligne 2",
            " ligne de contexte 2"));
  }

  /** Crée un hunk avec seulement du contexte. */
  private DiffHunkBlock createContextOnlyHunk() {
    return new DiffHunkBlock(
        5, 3, 5, 3, Arrays.asList(" contexte ligne 1", " contexte ligne 2", " contexte ligne 3"));
  }

  /** Crée un hunk avec des compteurs incohérents. */
  private DiffHunkBlock createInvalidHunk() {
    // 1 ajout + 1 suppression + 1 contexte = 3 lignes
    // Mais oldCount=5 et newCount=10 (incohérent)
    return new DiffHunkBlock(1, 5, 1, 10, Arrays.asList("+ajout", "-suppression", " contexte"));
  }

  @Nested
  @DisplayName("Tests de construction")
  class ConstructionTests {

    @Test
    @DisplayName("Constructeur par défaut doit initialiser les champs")
    void defaultConstructor_ShouldInitializeFields() {
      // When
      DiffHunkBlock hunk = new DiffHunkBlock();

      // Then
      assertEquals(0, hunk.oldStart);
      assertEquals(0, hunk.oldCount);
      assertEquals(0, hunk.newStart);
      assertEquals(0, hunk.newCount);
      assertNotNull(hunk.lines);
      assertTrue(hunk.lines.isEmpty());
    }

    @Test
    @DisplayName("Constructeur avec positions doit initialiser correctement")
    void constructorWithPositions_ShouldInitializeCorrectly() {
      // When
      DiffHunkBlock hunk = new DiffHunkBlock(10, 5, 12, 7);

      // Then
      assertEquals(10, hunk.oldStart);
      assertEquals(5, hunk.oldCount);
      assertEquals(12, hunk.newStart);
      assertEquals(7, hunk.newCount);
      assertNotNull(hunk.lines);
      assertTrue(hunk.lines.isEmpty());
    }

    @Test
    @DisplayName("Constructeur complet doit copier les lignes")
    void fullConstructor_ShouldCopyLines() {
      // Given
      List<String> originalLines = Arrays.asList("-old line", "+new line", " context");

      // When
      DiffHunkBlock hunk = new DiffHunkBlock(1, 2, 1, 2, originalLines);

      // Then
      assertEquals(1, hunk.oldStart);
      assertEquals(2, hunk.oldCount);
      assertEquals(1, hunk.newStart);
      assertEquals(2, hunk.newCount);
      assertEquals(3, hunk.lines.size());

      // Vérifier que c'est une copie (pas la même référence)
      assertNotSame(originalLines, hunk.lines);
      assertEquals(originalLines, hunk.lines);
    }

    @Test
    @DisplayName("Constructeur complet doit gérer les lignes null")
    void fullConstructor_ShouldHandleNullLines() {
      // When
      DiffHunkBlock hunk = new DiffHunkBlock(1, 0, 1, 0, null);

      // Then
      assertNotNull(hunk.lines);
      assertTrue(hunk.lines.isEmpty());
    }
  }

  @Nested
  @DisplayName("Tests de comptage des lignes")
  class LineCountingTests {

    @Test
    @DisplayName("getLineCount doit retourner le nombre total de lignes")
    void getLineCount_ShouldReturnTotalLines() {
      assertEquals(0, emptyHunk.getLineCount());
      assertEquals(3, additionOnlyHunk.getLineCount());
      assertEquals(2, deletionOnlyHunk.getLineCount());
      assertEquals(6, mixedHunk.getLineCount());
    }

    @Test
    @DisplayName("getLineCount doit gérer les lignes null")
    void getLineCount_ShouldHandleNullLines() {
      // Given
      DiffHunkBlock hunkWithNullLines = new DiffHunkBlock();
      hunkWithNullLines.lines = null;

      // Then
      assertEquals(0, hunkWithNullLines.getLineCount());
    }

    @Test
    @DisplayName("getAddedLinesCount doit compter les lignes avec préfixe +")
    void getAddedLinesCount_ShouldCountPlusLines() {
      assertEquals(0, emptyHunk.getAddedLinesCount());
      assertEquals(3, additionOnlyHunk.getAddedLinesCount());
      assertEquals(0, deletionOnlyHunk.getAddedLinesCount());
      assertEquals(2, mixedHunk.getAddedLinesCount());
    }

    @Test
    @DisplayName("getDeletedLinesCount doit compter les lignes avec préfixe -")
    void getDeletedLinesCount_ShouldCountMinusLines() {
      assertEquals(0, emptyHunk.getDeletedLinesCount());
      assertEquals(0, additionOnlyHunk.getDeletedLinesCount());
      assertEquals(2, deletionOnlyHunk.getDeletedLinesCount());
      assertEquals(2, mixedHunk.getDeletedLinesCount());
    }

    @Test
    @DisplayName("getContextLinesCount doit compter les lignes avec préfixe espace")
    void getContextLinesCount_ShouldCountSpaceLines() {
      assertEquals(0, emptyHunk.getContextLinesCount());
      assertEquals(0, additionOnlyHunk.getContextLinesCount());
      assertEquals(0, deletionOnlyHunk.getContextLinesCount());
      assertEquals(2, mixedHunk.getContextLinesCount());
      assertEquals(3, contextOnlyHunk.getContextLinesCount());
    }

    @Test
    @DisplayName("Les comptages doivent gérer les lignes null")
    void countMethods_ShouldHandleNullLines() {
      // Given
      DiffHunkBlock hunkWithNullLines = new DiffHunkBlock();
      hunkWithNullLines.lines = null;

      // Then
      assertEquals(0, hunkWithNullLines.getAddedLinesCount());
      assertEquals(0, hunkWithNullLines.getDeletedLinesCount());
      assertEquals(0, hunkWithNullLines.getContextLinesCount());
    }
  }

  @Nested
  @DisplayName("Tests de détection de type de modification")
  class ModificationTypeTests {

    @Test
    @DisplayName("isAdditionOnly doit détecter les hunks avec seulement des ajouts")
    void isAdditionOnly_ShouldDetectAdditionOnlyHunks() {
      assertTrue(additionOnlyHunk.isAdditionOnly());
      assertFalse(deletionOnlyHunk.isAdditionOnly());
      assertFalse(mixedHunk.isAdditionOnly());
      assertFalse(emptyHunk.isAdditionOnly());
      assertFalse(contextOnlyHunk.isAdditionOnly());
    }

    @Test
    @DisplayName("isDeletionOnly doit détecter les hunks avec seulement des suppressions")
    void isDeletionOnly_ShouldDetectDeletionOnlyHunks() {
      assertFalse(additionOnlyHunk.isDeletionOnly());
      assertTrue(deletionOnlyHunk.isDeletionOnly());
      assertFalse(mixedHunk.isDeletionOnly());
      assertFalse(emptyHunk.isDeletionOnly());
      assertFalse(contextOnlyHunk.isDeletionOnly());
    }

    @Test
    @DisplayName("isEmpty doit détecter les hunks vides")
    void isEmpty_ShouldDetectEmptyHunks() {
      assertTrue(emptyHunk.isEmpty());
      assertFalse(additionOnlyHunk.isEmpty());
      assertFalse(deletionOnlyHunk.isEmpty());
      assertFalse(mixedHunk.isEmpty());
    }

    @Test
    @DisplayName("isEmpty doit gérer les lignes null")
    void isEmpty_ShouldHandleNullLines() {
      // Given
      DiffHunkBlock hunkWithNullLines = new DiffHunkBlock();
      hunkWithNullLines.lines = null;

      // Then
      assertTrue(hunkWithNullLines.isEmpty());
    }

    @Test
    @DisplayName("hasModifications doit détecter la présence de modifications")
    void hasModifications_ShouldDetectModifications() {
      assertFalse(emptyHunk.hasModifications());
      assertTrue(additionOnlyHunk.hasModifications());
      assertTrue(deletionOnlyHunk.hasModifications());
      assertTrue(mixedHunk.hasModifications());
      assertFalse(contextOnlyHunk.hasModifications());
    }
  }

  // ===== MÉTHODES UTILITAIRES DE TEST =====

  @Nested
  @DisplayName("Tests d'extraction de contenu")
  class ContentExtractionTests {

    @Test
    @DisplayName("getAddedLinesContent doit extraire les lignes ajoutées sans préfixe")
    void getAddedLinesContent_ShouldExtractAddedLinesWithoutPrefix() {
      // When
      List<String> addedContent = mixedHunk.getAddedLinesContent();

      // Then
      assertEquals(2, addedContent.size());
      assertEquals("nouvelle ligne 1", addedContent.get(0));
      assertEquals("nouvelle ligne 2", addedContent.get(1));
    }

    @Test
    @DisplayName("getDeletedLinesContent doit extraire les lignes supprimées sans préfixe")
    void getDeletedLinesContent_ShouldExtractDeletedLinesWithoutPrefix() {
      // When
      List<String> deletedContent = mixedHunk.getDeletedLinesContent();

      // Then
      assertEquals(2, deletedContent.size());
      assertEquals("ancienne ligne 1", deletedContent.get(0));
      assertEquals("ancienne ligne 2", deletedContent.get(1));
    }

    @Test
    @DisplayName("getContextLinesContent doit extraire les lignes de contexte sans préfixe")
    void getContextLinesContent_ShouldExtractContextLinesWithoutPrefix() {
      // When
      List<String> contextContent = mixedHunk.getContextLinesContent();

      // Then
      assertEquals(2, contextContent.size());
      assertEquals("ligne de contexte 1", contextContent.get(0));
      assertEquals("ligne de contexte 2", contextContent.get(1));
    }

    @Test
    @DisplayName("Les méthodes d'extraction doivent retourner des listes vides pour hunk vide")
    void extractionMethods_ShouldReturnEmptyLists_ForEmptyHunk() {
      assertTrue(emptyHunk.getAddedLinesContent().isEmpty());
      assertTrue(emptyHunk.getDeletedLinesContent().isEmpty());
      assertTrue(emptyHunk.getContextLinesContent().isEmpty());
    }

    @Test
    @DisplayName("Les méthodes d'extraction doivent gérer les lignes null")
    void extractionMethods_ShouldHandleNullLines() {
      // Given
      DiffHunkBlock hunkWithNullLines = new DiffHunkBlock();
      hunkWithNullLines.lines = null;

      // Then
      assertTrue(hunkWithNullLines.getAddedLinesContent().isEmpty());
      assertTrue(hunkWithNullLines.getDeletedLinesContent().isEmpty());
      assertTrue(hunkWithNullLines.getContextLinesContent().isEmpty());
    }

    @Test
    @DisplayName("Les méthodes d'extraction doivent gérer les préfixes manquants")
    void extractionMethods_ShouldHandleMissingPrefixes() {
      // Given
      DiffHunkBlock hunkWithInvalidPrefixes = new DiffHunkBlock();
      hunkWithInvalidPrefixes.lines = Arrays.asList("", "x", "ligne sans préfixe");

      // When & Then
      assertTrue(hunkWithInvalidPrefixes.getAddedLinesContent().isEmpty());
      assertTrue(hunkWithInvalidPrefixes.getDeletedLinesContent().isEmpty());
      assertTrue(hunkWithInvalidPrefixes.getContextLinesContent().isEmpty());
    }
  }

  @Nested
  @DisplayName("Tests de génération d'en-tête et validation")
  class HeaderAndValidationTests {

    @Test
    @DisplayName("generateHeader doit générer l'en-tête au format Git standard")
    void generateHeader_ShouldGenerateGitFormatHeader() {
      // Given
      DiffHunkBlock hunk = new DiffHunkBlock(10, 5, 12, 7);

      // When
      String header = hunk.generateHeader();

      // Then
      assertEquals("@@ -10,5 +12,7 @@", header);
    }

    @ParameterizedTest
    @CsvSource({
      "0, 0, 1, 3, '@@ -0,0 +1,3 @@'",
      "1, 1, 0, 0, '@@ -1,1 +0,0 @@'",
      "100, 50, 200, 75, '@@ -100,50 +200,75 @@'"
    })
    @DisplayName("generateHeader doit gérer différentes combinaisons de valeurs")
    void generateHeader_ShouldHandleDifferentValueCombinations(
        int oldStart, int oldCount, int newStart, int newCount, String expected) {
      // Given
      DiffHunkBlock hunk = new DiffHunkBlock(oldStart, oldCount, newStart, newCount);

      // When
      String header = hunk.generateHeader();

      // Then
      assertEquals(expected, header);
    }

    @Test
    @DisplayName("isValid doit valider la cohérence des compteurs")
    void isValid_ShouldValidateCountersCoherence() {
      assertTrue(additionOnlyHunk.isValid());
      assertTrue(deletionOnlyHunk.isValid());
      assertTrue(mixedHunk.isValid());
      assertTrue(contextOnlyHunk.isValid());
      assertTrue(emptyHunk.isValid());
      assertFalse(invalidHunk.isValid());
    }

    @Test
    @DisplayName("isValid doit gérer les lignes null")
    void isValid_ShouldHandleNullLines() {
      // Given
      DiffHunkBlock hunkWithNullLines = new DiffHunkBlock(0, 0, 0, 0);
      hunkWithNullLines.lines = null;

      // Then
      assertTrue(hunkWithNullLines.isValid());

      // Cas invalide avec lignes null mais compteurs non-zéro
      DiffHunkBlock invalidWithNull = new DiffHunkBlock(1, 1, 1, 1);
      invalidWithNull.lines = null;
      assertFalse(invalidWithNull.isValid());
    }

    @Test
    @DisplayName("isValid doit valider selon les règles Git")
    void isValid_ShouldValidateAccordingToGitRules() {
      // Given - Hunk valide : 2 supprimées + 1 contexte = 3 (oldCount)
      //                      1 ajoutée + 1 contexte = 2 (newCount)
      DiffHunkBlock validHunk =
          new DiffHunkBlock(
              5, 3, 5, 2, Arrays.asList("-supprimée 1", "-supprimée 2", " contexte", "+ajoutée"));

      // Given - Hunk invalide : compteurs incohérents
      DiffHunkBlock invalidHunk =
          new DiffHunkBlock(
              5,
              5,
              5,
              5,
              Arrays.asList(
                  "-supprimée",
                  "+ajoutée")); // 1 supprimée + 0 contexte != 5, 1 ajoutée + 0 contexte != 5

      // Then
      assertTrue(validHunk.isValid());
      assertFalse(invalidHunk.isValid());
    }
  }

  @Nested
  @DisplayName("Tests de deepCopy")
  class DeepCopyTests {

    @Test
    @DisplayName("deepCopy doit créer une copie complètement indépendante")
    void deepCopy_ShouldCreateCompletelyIndependentCopy() {
      // When
      DiffHunkBlock copy = mixedHunk.deepCopy();

      // Then
      assertEquals(mixedHunk.oldStart, copy.oldStart);
      assertEquals(mixedHunk.oldCount, copy.oldCount);
      assertEquals(mixedHunk.newStart, copy.newStart);
      assertEquals(mixedHunk.newCount, copy.newCount);
      assertEquals(mixedHunk.lines.size(), copy.lines.size());
      assertEquals(mixedHunk.lines, copy.lines);

      // Vérifier que les listes sont différentes instances
      assertNotSame(mixedHunk.lines, copy.lines);
    }

    @Test
    @DisplayName("deepCopy - modifications ne doivent pas affecter l'original")
    void deepCopy_ModificationsShouldNotAffectOriginal() {
      // Given
      DiffHunkBlock copy = mixedHunk.deepCopy();
      int originalSize = mixedHunk.lines.size();

      // When - Modifier la copie
      copy.lines.add("+nouvelle ligne dans la copie");
      copy.oldStart = 999;

      // Then - L'original ne doit pas être affecté
      assertEquals(originalSize, mixedHunk.lines.size());
      assertNotEquals(999, mixedHunk.oldStart);
      assertFalse(mixedHunk.lines.contains("+nouvelle ligne dans la copie"));
    }

    @Test
    @DisplayName("deepCopy doit gérer les hunks vides")
    void deepCopy_ShouldHandleEmptyHunks() {
      // When
      DiffHunkBlock copy = emptyHunk.deepCopy();

      // Then
      assertEquals(emptyHunk.oldStart, copy.oldStart);
      assertEquals(emptyHunk.oldCount, copy.oldCount);
      assertEquals(emptyHunk.newStart, copy.newStart);
      assertEquals(emptyHunk.newCount, copy.newCount);
      assertNotSame(emptyHunk.lines, copy.lines);
      assertTrue(copy.lines.isEmpty());
    }

    @Test
    @DisplayName("deepCopy doit gérer les lignes null")
    void deepCopy_ShouldHandleNullLines() {
      // Given
      DiffHunkBlock hunkWithNullLines = new DiffHunkBlock(1, 1, 1, 1);
      hunkWithNullLines.lines = null;

      // When
      DiffHunkBlock copy = hunkWithNullLines.deepCopy();

      // Then
      assertNotNull(copy.lines);
      assertTrue(copy.lines.isEmpty());
    }
  }

  @Nested
  @DisplayName("Tests des méthodes standard (toString, equals, hashCode)")
  class StandardMethodsTests {

    @Test
    @DisplayName("toString doit identifier les hunks vides")
    void toString_ShouldIdentifyEmptyHunks() {
      String result = emptyHunk.toString();
      assertTrue(result.contains("vide"));
    }

    @Test
    @DisplayName("toString doit afficher les statistiques pour hunks non-vides")
    void toString_ShouldShowStatistics_ForNonEmptyHunks() {
      String result = mixedHunk.toString();
      assertTrue(result.contains("@@ -10,4 +10,4 @@"));
      assertTrue(result.contains("+2"));
      assertTrue(result.contains("-2"));
      assertTrue(result.contains("2 contexte"));
    }

    @Test
    @DisplayName("toString doit gérer les hunks avec seulement des ajouts")
    void toString_ShouldHandleAdditionOnlyHunks() {
      String result = additionOnlyHunk.toString();
      assertTrue(result.contains("+3"));
      assertTrue(result.contains("-0"));
      assertTrue(result.contains("0 contexte"));
    }

    @Test
    @DisplayName("equals doit comparer correctement le contenu")
    void equals_ShouldCompareContentCorrectly() {
      // Given
      DiffHunkBlock hunk1 = createMixedHunk();
      DiffHunkBlock hunk2 = createMixedHunk();
      DiffHunkBlock different = createAdditionOnlyHunk();

      // Then
      assertEquals(hunk1, hunk2);
      assertNotEquals(hunk1, different);
      assertNotEquals(hunk1, null);
      assertNotEquals(hunk1, "string");
    }

    @Test
    @DisplayName("equals doit gérer l'auto-comparaison")
    void equals_ShouldHandleSelfComparison() {
      assertTrue(mixedHunk.equals(mixedHunk));
    }

    @Test
    @DisplayName("hashCode doit être cohérent avec equals")
    void hashCode_ShouldBeConsistentWithEquals() {
      // Given
      DiffHunkBlock hunk1 = createMixedHunk();
      DiffHunkBlock hunk2 = createMixedHunk();

      // Then
      assertEquals(hunk1.hashCode(), hunk2.hashCode());
    }

    @Test
    @DisplayName("equals doit détecter les différences dans tous les champs")
    void equals_ShouldDetectDifferencesInAllFields() {
      // Given
      DiffHunkBlock base = new DiffHunkBlock(1, 2, 3, 4, Arrays.asList("+test"));

      DiffHunkBlock diffOldStart = new DiffHunkBlock(999, 2, 3, 4, Arrays.asList("+test"));
      DiffHunkBlock diffOldCount = new DiffHunkBlock(1, 999, 3, 4, Arrays.asList("+test"));
      DiffHunkBlock diffNewStart = new DiffHunkBlock(1, 2, 999, 4, Arrays.asList("+test"));
      DiffHunkBlock diffNewCount = new DiffHunkBlock(1, 2, 3, 999, Arrays.asList("+test"));
      DiffHunkBlock diffLines = new DiffHunkBlock(1, 2, 3, 4, Arrays.asList("+different"));

      // Then
      assertNotEquals(base, diffOldStart);
      assertNotEquals(base, diffOldCount);
      assertNotEquals(base, diffNewStart);
      assertNotEquals(base, diffNewCount);
      assertNotEquals(base, diffLines);
    }
  }

  @Nested
  @DisplayName("Tests de performance et cas limites")
  class PerformanceAndEdgeCaseTests {

    @Test
    @DisplayName("Doit gérer un hunk avec de nombreuses lignes")
    void shouldHandleHunkWithManyLines() {
      // Given
      List<String> manyLines = new ArrayList<>();
      for (int i = 0; i < 10000; i++) {
        manyLines.add(
            i % 3 == 0 ? "+ajout " + i : i % 3 == 1 ? "-suppression " + i : " contexte " + i);
      }

      DiffHunkBlock largeHunk = new DiffHunkBlock(1, 6667, 1, 6667, manyLines);

      // When & Then
      assertEquals(10000, largeHunk.getLineCount());
      assertTrue(largeHunk.getAddedLinesCount() > 3000);
      assertTrue(largeHunk.getDeletedLinesCount() > 3000);
      assertTrue(largeHunk.getContextLinesCount() > 3000);

      // Test de performance pour deepCopy
      long startTime = System.currentTimeMillis();
      DiffHunkBlock copy = largeHunk.deepCopy();
      long endTime = System.currentTimeMillis();

      assertTrue(endTime - startTime < 1000, "deepCopy ne devrait pas prendre plus d'1 seconde");
      assertEquals(10000, copy.getLineCount());
    }

    @Test
    @DisplayName("Doit gérer les lignes avec caractères spéciaux")
    void shouldHandleLinesWithSpecialCharacters() {
      // Given
      List<String> specialLines =
          Arrays.asList(
              "+ligne avec émojis 🚀 et ñ",
              "-ligne avec <>&\"'",
              " ligne avec \t\n\r",
              "+中文字符",
              "-العربية");

      DiffHunkBlock specialHunk = new DiffHunkBlock(1, 2, 1, 3, specialLines);

      // When & Then
      assertEquals(5, specialHunk.getLineCount());
      assertEquals(2, specialHunk.getAddedLinesCount());
      assertEquals(2, specialHunk.getDeletedLinesCount());
      assertEquals(1, specialHunk.getContextLinesCount());

      List<String> addedContent = specialHunk.getAddedLinesContent();
      assertEquals("ligne avec émojis 🚀 et ñ", addedContent.get(0));
      assertEquals("中文字符", addedContent.get(1));
    }

    @Test
    @DisplayName("Doit gérer les hunks avec des valeurs limites")
    void shouldHandleHunksWithBoundaryValues() {
      // Given - Hunk avec des valeurs limites mais cohérentes
      DiffHunkBlock boundaryHunk = new DiffHunkBlock(Integer.MAX_VALUE, 0, Integer.MIN_VALUE, 0);

      // When & Then
      assertEquals("@@ -2147483647,0 +-2147483648,0 @@", boundaryHunk.generateHeader());
      assertTrue(boundaryHunk.isEmpty());
      assertTrue(boundaryHunk.isValid()); // 0 lignes pour tous les types = cohérent

      // Test avec des valeurs limites mais incohérentes
      DiffHunkBlock invalidBoundaryHunk =
          new DiffHunkBlock(Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0);
      assertFalse(
          invalidBoundaryHunk.isValid()); // MAX_VALUE lignes attendues mais 0 lignes réelles
    }

    @Test
    @DisplayName("Doit préserver l'ordre des lignes lors des opérations")
    void shouldPreserveLineOrderDuringOperations() {
      // Given
      List<String> orderedLines =
          Arrays.asList(
              " premier contexte",
              "-première suppression",
              "+premier ajout",
              " second contexte",
              "-seconde suppression",
              "+second ajout");

      DiffHunkBlock orderedHunk = new DiffHunkBlock(1, 4, 1, 4, orderedLines);

      // When
      List<String> addedContent = orderedHunk.getAddedLinesContent();
      List<String> deletedContent = orderedHunk.getDeletedLinesContent();
      List<String> contextContent = orderedHunk.getContextLinesContent();
      DiffHunkBlock copy = orderedHunk.deepCopy();

      // Then
      assertEquals(Arrays.asList("premier ajout", "second ajout"), addedContent);
      assertEquals(Arrays.asList("première suppression", "seconde suppression"), deletedContent);
      assertEquals(Arrays.asList("premier contexte", "second contexte"), contextContent);
      assertEquals(orderedLines, copy.lines);
    }
  }
}
