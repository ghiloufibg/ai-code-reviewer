package com.ghiloufi.aicode.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests unitaires pour la classe GitFileModification.
 *
 * <p>Couvre tous les scénarios : construction, copie, détection de types de modifications, méthodes
 * utilitaires, et cas edge pour assurer la robustesse.
 */
@DisplayName("GitFileModification Tests")
class GitFileModificationTest {

  private GitFileModification emptyFile;
  private GitFileModification modifiedFile;
  private GitFileModification renamedFile;
  private GitFileModification newFile;
  private GitFileModification deletedFile;

  private DiffHunkBlock sampleDiffHunkBlock1;
  private DiffHunkBlock sampleDiffHunkBlock2;
  private DiffHunkBlock largeDiffHunkBlock;

  @BeforeEach
  void setUp() {
    // Hunks de test
    sampleDiffHunkBlock1 =
        createHunk(10, 3, 10, 4, Arrays.asList("-old line", "+new line", " context"));
    sampleDiffHunkBlock2 = createHunk(20, 2, 21, 2, Arrays.asList(" context", "+added line"));
    largeDiffHunkBlock = createLargeHunk(100);

    // Fichier vide
    emptyFile = new GitFileModification();

    // Fichier modifié standard
    modifiedFile = createModifiedFile();

    // Fichier renommé
    renamedFile = createRenamedFile();

    // Nouveau fichier
    newFile = createNewFile();

    // Fichier supprimé
    deletedFile = createDeletedFile();
  }

  /** Crée un fichier modifié standard pour les tests. */
  private GitFileModification createModifiedFile() {
    GitFileModification file =
        new GitFileModification("src/main/java/Test.java", "src/main/java/Test.java");
    file.diffHunkBlocks.add(sampleDiffHunkBlock1);
    file.diffHunkBlocks.add(sampleDiffHunkBlock2);
    return file;
  }

  /** Crée un fichier renommé pour les tests. */
  private GitFileModification createRenamedFile() {
    GitFileModification file =
        new GitFileModification("src/main/java/OldName.java", "src/main/java/NewName.java");
    file.diffHunkBlocks.add(createHunk(5, 1, 5, 1, Arrays.asList("+renamed content")));
    return file;
  }

  /** Crée un nouveau fichier pour les tests. */
  private GitFileModification createNewFile() {
    GitFileModification file = new GitFileModification("/dev/null", "new_file.java");
    file.diffHunkBlocks.add(
        createHunk(0, 0, 1, 3, Arrays.asList("+public class NewFile {", "+    // content", "+")));
    return file;
  }

  /** Crée un fichier supprimé pour les tests. */
  private GitFileModification createDeletedFile() {
    GitFileModification file = new GitFileModification("deleted_file.java", "/dev/null");
    file.diffHunkBlocks.add(
        createHunk(
            1, 3, 0, 0, Arrays.asList("-deleted line 1", "-deleted line 2", "-deleted line 3")));
    return file;
  }

  /** Crée un hunk de test. */
  private DiffHunkBlock createHunk(
      int oldStart, int oldCount, int newStart, int newCount, List<String> lines) {
    DiffHunkBlock diffHunkBlock = new DiffHunkBlock();
    diffHunkBlock.oldStart = oldStart;
    diffHunkBlock.oldCount = oldCount;
    diffHunkBlock.newStart = newStart;
    diffHunkBlock.newCount = newCount;
    diffHunkBlock.lines = new ArrayList<>(lines);
    return diffHunkBlock;
  }

  /** Crée un gros hunk pour les tests de performance. */
  private DiffHunkBlock createLargeHunk(int lineCount) {
    List<String> lines = new ArrayList<>();
    for (int i = 0; i < lineCount; i++) {
      lines.add(" line " + i);
    }
    return createHunk(1, lineCount, 1, lineCount, lines);
  }

  @Nested
  @DisplayName("Tests de construction")
  class ConstructionTests {

    @Test
    @DisplayName("Constructeur par défaut doit initialiser les champs")
    void defaultConstructor_ShouldInitializeFields() {
      // When
      GitFileModification file = new GitFileModification();

      // Then
      assertNull(file.oldPath);
      assertNull(file.newPath);
      assertNotNull(file.diffHunkBlocks);
      assertTrue(file.diffHunkBlocks.isEmpty());
    }

    @Test
    @DisplayName("Constructeur avec chemins doit initialiser correctement")
    void constructorWithPaths_ShouldInitializeCorrectly() {
      // Given
      String oldPath = "src/main/java/Test.java";
      String newPath = "src/main/java/NewTest.java";

      // When
      GitFileModification file = new GitFileModification(oldPath, newPath);

      // Then
      assertEquals(oldPath, file.oldPath);
      assertEquals(newPath, file.newPath);
      assertNotNull(file.diffHunkBlocks);
      assertTrue(file.diffHunkBlocks.isEmpty());
    }
  }

  @Nested
  @DisplayName("Tests de shallowCopyWithSingleHunk")
  class ShallowCopyTests {

    @Test
    @DisplayName("Doit créer une copie avec un seul hunk")
    void shallowCopyWithSingleHunk_ShouldCreateCopyWithSingleHunk() {
      // When
      GitFileModification copy = modifiedFile.shallowCopyWithSingleHunk(sampleDiffHunkBlock1);

      // Then
      assertEquals(modifiedFile.oldPath, copy.oldPath);
      assertEquals(modifiedFile.newPath, copy.newPath);
      assertEquals(1, copy.diffHunkBlocks.size());
      assertSame(
          sampleDiffHunkBlock1, copy.diffHunkBlocks.get(0)); // Référence partagée (shallow copy)

      // Original non modifié
      assertEquals(2, modifiedFile.diffHunkBlocks.size());
    }

    @Test
    @DisplayName("Doit lancer IllegalArgumentException si hunk est null")
    void shallowCopyWithSingleHunk_ShouldThrowException_WhenHunkIsNull() {
      // When & Then
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> modifiedFile.shallowCopyWithSingleHunk(null));

      assertEquals("Le hunk ne peut pas être null", exception.getMessage());
    }

    @Test
    @DisplayName("Doit fonctionner avec un fichier vide")
    void shallowCopyWithSingleHunk_ShouldWorkWithEmptyFile() {
      // Given
      emptyFile.oldPath = "test.java";
      emptyFile.newPath = "test.java";

      // When
      GitFileModification copy = emptyFile.shallowCopyWithSingleHunk(sampleDiffHunkBlock1);

      // Then
      assertEquals("test.java", copy.oldPath);
      assertEquals("test.java", copy.newPath);
      assertEquals(1, copy.diffHunkBlocks.size());
      assertSame(sampleDiffHunkBlock1, copy.diffHunkBlocks.get(0));
    }

    @Test
    @DisplayName("Modifications du hunk doivent affecter l'original (shallow copy)")
    void shallowCopyWithSingleHunk_ModificationsShouldAffectOriginal() {
      // Given
      GitFileModification copy = modifiedFile.shallowCopyWithSingleHunk(sampleDiffHunkBlock1);

      // When - Modifier le hunk via la copie
      copy.diffHunkBlocks.get(0).lines.add("nouvelle ligne");

      // Then - L'original doit être affecté
      assertTrue(sampleDiffHunkBlock1.lines.contains("nouvelle ligne"));
    }
  }

  @Nested
  @DisplayName("Tests de createDeepCopyWithSingleHunk")
  class DeepCopyTests {

    @Test
    @DisplayName("Doit créer une copie profonde avec un seul hunk")
    void createDeepCopyWithSingleHunk_ShouldCreateDeepCopy() {
      // When
      GitFileModification copy = modifiedFile.createDeepCopyWithSingleHunk(sampleDiffHunkBlock1);

      // Then
      assertEquals(modifiedFile.oldPath, copy.oldPath);
      assertEquals(modifiedFile.newPath, copy.newPath);
      assertEquals(1, copy.diffHunkBlocks.size());

      DiffHunkBlock copiedDiffHunkBlock = copy.diffHunkBlocks.get(0);
      assertNotSame(sampleDiffHunkBlock1, copiedDiffHunkBlock); // Instances différentes
      assertEquals(sampleDiffHunkBlock1.oldStart, copiedDiffHunkBlock.oldStart);
      assertEquals(sampleDiffHunkBlock1.lines.size(), copiedDiffHunkBlock.lines.size());
    }

    @Test
    @DisplayName("Modifications du hunk ne doivent pas affecter l'original (deep copy)")
    void createDeepCopyWithSingleHunk_ModificationsShouldNotAffectOriginal() {
      // Given
      int originalSize = sampleDiffHunkBlock1.lines.size();
      GitFileModification copy = modifiedFile.createDeepCopyWithSingleHunk(sampleDiffHunkBlock1);

      // When - Modifier le hunk via la copie
      copy.diffHunkBlocks.get(0).lines.add("nouvelle ligne");

      // Then - L'original ne doit pas être affecté
      assertEquals(originalSize, sampleDiffHunkBlock1.lines.size());
      assertFalse(sampleDiffHunkBlock1.lines.contains("nouvelle ligne"));
    }

    @Test
    @DisplayName("Doit lancer IllegalArgumentException si hunk est null")
    void createDeepCopyWithSingleHunk_ShouldThrowException_WhenHunkIsNull() {
      // When & Then
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> modifiedFile.createDeepCopyWithSingleHunk(null));

      assertEquals("Le hunk ne peut pas être null", exception.getMessage());
    }
  }

  // ===== MÉTHODES UTILITAIRES DE TEST =====

  @Nested
  @DisplayName("Tests de détection de type de modification")
  class ModificationTypeTests {

    @Test
    @DisplayName("isRenamed doit détecter les renommages")
    void isRenamed_ShouldDetectRenames() {
      // Then
      assertTrue(renamedFile.isRenamed());
      assertFalse(modifiedFile.isRenamed());
      assertFalse(newFile.isRenamed()); // /dev/null -> file.java n'est PAS un rename
      assertFalse(deletedFile.isRenamed()); // file.java -> /dev/null n'est PAS un rename
    }

    @Test
    @DisplayName("isRenamed doit retourner false pour les nouveaux fichiers")
    void isRenamed_ShouldReturnFalse_ForNewFiles() {
      // Given
      GitFileModification newFile1 = new GitFileModification("/dev/null", "created.java");
      GitFileModification newFile2 = new GitFileModification(null, "created2.java");
      GitFileModification newFile3 = new GitFileModification("", "created3.java");

      // Then
      assertFalse(newFile1.isRenamed());
      assertFalse(newFile2.isRenamed());
      assertFalse(newFile3.isRenamed());
    }

    @Test
    @DisplayName("isRenamed doit retourner false pour les fichiers supprimés")
    void isRenamed_ShouldReturnFalse_ForDeletedFiles() {
      // Given
      GitFileModification deletedFile1 = new GitFileModification("removed.java", "/dev/null");
      GitFileModification deletedFile2 = new GitFileModification("removed2.java", null);
      GitFileModification deletedFile3 = new GitFileModification("removed3.java", "");

      // Then
      assertFalse(deletedFile1.isRenamed());
      assertFalse(deletedFile2.isRenamed());
      assertFalse(deletedFile3.isRenamed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/dev/null"})
    @NullAndEmptySource
    @DisplayName("isNewFile doit détecter les nouveaux fichiers")
    void isNewFile_ShouldDetectNewFiles(String oldPath) {
      // Given
      GitFileModification file = new GitFileModification(oldPath, "new.java");

      // Then
      assertTrue(file.isNewFile());
    }

    @Test
    @DisplayName("isNewFile doit retourner false pour fichiers existants")
    void isNewFile_ShouldReturnFalse_ForExistingFiles() {
      assertFalse(modifiedFile.isNewFile());
      assertFalse(renamedFile.isNewFile());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/dev/null"})
    @NullAndEmptySource
    @DisplayName("isDeleted doit détecter les fichiers supprimés")
    void isDeleted_ShouldDetectDeletedFiles(String newPath) {
      // Given
      GitFileModification file = new GitFileModification("old.java", newPath);

      // Then
      assertTrue(file.isDeleted());
    }

    @Test
    @DisplayName("isDeleted doit retourner false pour fichiers non supprimés")
    void isDeleted_ShouldReturnFalse_ForNonDeletedFiles() {
      assertFalse(modifiedFile.isDeleted());
      assertFalse(renamedFile.isDeleted());
      assertFalse(newFile.isDeleted());
    }

    @Test
    @DisplayName("hasModifications doit détecter la présence de hunks")
    void hasModifications_ShouldDetectHunksPresence() {
      assertTrue(modifiedFile.hasModifications());
      assertFalse(emptyFile.hasModifications());
    }

    @Test
    @DisplayName("hasModifications doit gérer les listes null")
    void hasModifications_ShouldHandleNullHunks() {
      // Given
      GitFileModification fileWithNullHunks = new GitFileModification();
      fileWithNullHunks.diffHunkBlocks = null;

      // Then
      assertFalse(fileWithNullHunks.hasModifications());
    }
  }

  @Nested
  @DisplayName("Tests des méthodes de comptage")
  class CountingMethodsTests {

    @Test
    @DisplayName("getTotalLineCount doit compter correctement les lignes")
    void getTotalLineCount_ShouldCountLinesCorrectly() {
      // Then
      assertEquals(5, modifiedFile.getTotalLineCount()); // 3 + 2 lignes
      assertEquals(0, emptyFile.getTotalLineCount());
    }

    @Test
    @DisplayName("getTotalLineCount doit gérer les hunks avec lines null")
    void getTotalLineCount_ShouldHandleHunksWithNullLines() {
      // Given
      DiffHunkBlock diffHunkBlockWithNullLines = new DiffHunkBlock();
      diffHunkBlockWithNullLines.lines = null;

      GitFileModification file = new GitFileModification();
      file.diffHunkBlocks.add(diffHunkBlockWithNullLines);

      // Then
      assertEquals(0, file.getTotalLineCount());
    }

    @Test
    @DisplayName("getTotalLineCount doit gérer les hunks null")
    void getTotalLineCount_ShouldHandleNullHunks() {
      // Given
      GitFileModification file = new GitFileModification();
      file.diffHunkBlocks = null;

      // Then
      assertEquals(0, file.getTotalLineCount());
    }

    @Test
    @DisplayName("getHunkCount doit compter correctement les hunks")
    void getHunkCount_ShouldCountHunksCorrectly() {
      assertEquals(2, modifiedFile.getHunkCount());
      assertEquals(0, emptyFile.getHunkCount());
    }

    @Test
    @DisplayName("getHunkCount doit gérer les hunks null")
    void getHunkCount_ShouldHandleNullHunks() {
      // Given
      GitFileModification file = new GitFileModification();
      file.diffHunkBlocks = null;

      // Then
      assertEquals(0, file.getHunkCount());
    }
  }

  @Nested
  @DisplayName("Tests des utilitaires de chemin")
  class PathUtilityTests {

    @ParameterizedTest
    @CsvSource({
      "'src/main/java/Test.java', '.java'",
      "'script.sh', '.sh'",
      "'README.md', '.md'",
      "'Dockerfile', ''",
      "'path/file.backup.txt', '.txt'",
      "'file.', ''",
      "'.gitignore', '.gitignore'"
    })
    @DisplayName("getFileExtension doit extraire correctement l'extension")
    void getFileExtension_ShouldExtractExtensionCorrectly(
        String filePath, String expectedExtension) {
      // Given
      GitFileModification file = new GitFileModification("old", filePath);

      // Then
      assertEquals(expectedExtension, file.getFileExtension());
    }

    @Test
    @DisplayName("getFileExtension doit gérer les chemins null")
    void getFileExtension_ShouldHandleNullPaths() {
      // Given
      GitFileModification file = new GitFileModification();
      file.oldPath = null;
      file.newPath = null;

      // Then
      assertEquals("", file.getFileExtension());
    }

    @ParameterizedTest
    @CsvSource({
      "'src/main/java/Test.java', 'Test.java'",
      "'script.sh', 'script.sh'",
      "'path/to/file.txt', 'file.txt'",
      "'C:\\\\Windows\\\\file.exe', 'file.exe'",
      "'single_file', 'single_file'"
    })
    @DisplayName("getFileName doit extraire correctement le nom de fichier")
    void getFileName_ShouldExtractFileNameCorrectly(String filePath, String expectedFileName) {
      // Given
      GitFileModification file = new GitFileModification("old", filePath);

      // Then
      assertEquals(expectedFileName, file.getFileName());
    }

    @Test
    @DisplayName("getFileName doit gérer les chemins null")
    void getFileName_ShouldHandleNullPaths() {
      // Given
      GitFileModification file = new GitFileModification();
      file.oldPath = null;
      file.newPath = null;

      // Then
      assertEquals("", file.getFileName());
    }

    @Test
    @DisplayName("getEffectivePath doit prioriser newPath")
    void getEffectivePath_ShouldPrioritizeNewPath() {
      assertEquals("src/main/java/Test.java", modifiedFile.getEffectivePath());
      assertEquals("src/main/java/NewName.java", renamedFile.getEffectivePath());
    }

    @Test
    @DisplayName("getEffectivePath doit utiliser oldPath si newPath est null")
    void getEffectivePath_ShouldUseOldPath_WhenNewPathIsNull() {
      // Given
      GitFileModification file = new GitFileModification();
      file.oldPath = "old.java";
      file.newPath = null;

      // Then
      assertEquals("old.java", file.getEffectivePath());
    }
  }

  @Nested
  @DisplayName("Tests de deepCopy complète")
  class CompleteDeepCopyTests {

    @Test
    @DisplayName("deepCopy doit créer une copie complètement indépendante")
    void deepCopy_ShouldCreateCompletelyIndependentCopy() {
      // When
      GitFileModification copy = modifiedFile.deepCopy();

      // Then
      assertEquals(modifiedFile.oldPath, copy.oldPath);
      assertEquals(modifiedFile.newPath, copy.newPath);
      assertEquals(modifiedFile.diffHunkBlocks.size(), copy.diffHunkBlocks.size());

      // Vérifier que les hunks sont différentes instances
      for (int i = 0; i < modifiedFile.diffHunkBlocks.size(); i++) {
        assertNotSame(modifiedFile.diffHunkBlocks.get(i), copy.diffHunkBlocks.get(i));
        assertEquals(
            modifiedFile.diffHunkBlocks.get(i).lines.size(),
            copy.diffHunkBlocks.get(i).lines.size());
      }
    }

    @Test
    @DisplayName("deepCopy - modifications ne doivent pas affecter l'original")
    void deepCopy_ModificationsShouldNotAffectOriginal() {
      // Given
      GitFileModification copy = modifiedFile.deepCopy();
      int originalHunksCount = modifiedFile.diffHunkBlocks.size();
      int originalLinesCount = modifiedFile.diffHunkBlocks.get(0).lines.size();

      // When - Modifier la copie
      copy.diffHunkBlocks.add(createHunk(30, 1, 30, 1, Arrays.asList("+new line")));
      copy.diffHunkBlocks.get(0).lines.add("modified line");

      // Then - L'original ne doit pas être affecté
      assertEquals(originalHunksCount, modifiedFile.diffHunkBlocks.size());
      assertEquals(originalLinesCount, modifiedFile.diffHunkBlocks.get(0).lines.size());
    }

    @Test
    @DisplayName("deepCopy doit gérer les hunks null")
    void deepCopy_ShouldHandleNullHunks() {
      // Given
      GitFileModification file = new GitFileModification("old", "new");
      file.diffHunkBlocks = null;

      // When
      GitFileModification copy = file.deepCopy();

      // Then
      assertEquals("old", copy.oldPath);
      assertEquals("new", copy.newPath);
      assertTrue(copy.diffHunkBlocks.isEmpty());
    }
  }

  @Nested
  @DisplayName("Tests des méthodes standard (toString, equals, hashCode)")
  class StandardMethodsTests {

    @Test
    @DisplayName("toString doit identifier les nouveaux fichiers")
    void toString_ShouldIdentifyNewFiles() {
      String result = newFile.toString();
      assertTrue(result.contains("NEW:"));
      assertTrue(result.contains("new_file.java"));
    }

    @Test
    @DisplayName("toString doit identifier les fichiers supprimés")
    void toString_ShouldIdentifyDeletedFiles() {
      String result = deletedFile.toString();
      assertTrue(result.contains("DELETED:"));
      assertTrue(result.contains("deleted_file.java"));
    }

    @Test
    @DisplayName("toString doit identifier les fichiers renommés")
    void toString_ShouldIdentifyRenamedFiles() {
      String result = renamedFile.toString();
      assertTrue(result.contains("RENAMED:"));
      assertTrue(result.contains("OldName.java -> src/main/java/NewName.java"));
    }

    @Test
    @DisplayName("toString doit identifier les fichiers modifiés")
    void toString_ShouldIdentifyModifiedFiles() {
      String result = modifiedFile.toString();
      assertTrue(result.contains("MODIFIED:"));
      assertTrue(result.contains("Test.java"));
      assertTrue(result.contains("2 hunk(s)"));
      assertTrue(result.contains("5 ligne(s)"));
    }

    @Test
    @DisplayName("equals doit comparer correctement le contenu")
    void equals_ShouldCompareContentCorrectly() {
      // Given
      GitFileModification file1 = createModifiedFile();
      GitFileModification file2 = createModifiedFile();
      GitFileModification different = createRenamedFile();

      // Then
      assertEquals(file1, file2);
      assertNotEquals(file1, different);
      assertNotEquals(file1, null);
      assertNotEquals(file1, "string");
    }

    @Test
    @DisplayName("hashCode doit être cohérent avec equals")
    void hashCode_ShouldBeConsistentWithEquals() {
      // Given
      GitFileModification file1 = createModifiedFile();
      GitFileModification file2 = createModifiedFile();

      // Then
      assertEquals(file1.hashCode(), file2.hashCode());
    }

    @Test
    @DisplayName("equals doit gérer l'auto-comparaison")
    void equals_ShouldHandleSelfComparison() {
      assertTrue(modifiedFile.equals(modifiedFile));
    }
  }

  @Nested
  @DisplayName("Tests de performance et cas limites")
  class PerformanceAndEdgeCaseTests {

    @Test
    @DisplayName("Doit gérer un fichier avec de nombreux hunks")
    void shouldHandleFileWithManyHunks() {
      // Given
      GitFileModification file = new GitFileModification("large.java", "large.java");
      for (int i = 0; i < 1000; i++) {
        file.diffHunkBlocks.add(createHunk(i * 10, 1, i * 10, 1, Arrays.asList("+line " + i)));
      }

      // When & Then
      assertEquals(1000, file.getHunkCount());
      assertEquals(1000, file.getTotalLineCount());
      assertTrue(file.hasModifications());

      // Test de performance pour deepCopy
      long startTime = System.currentTimeMillis();
      GitFileModification copy = file.deepCopy();
      long endTime = System.currentTimeMillis();

      assertTrue(endTime - startTime < 1000, "deepCopy ne devrait pas prendre plus d'1 seconde");
      assertEquals(1000, copy.getHunkCount());
    }

    @Test
    @DisplayName("Doit gérer un hunk avec beaucoup de lignes")
    void shouldHandleHunkWithManyLines() {
      // Given
      GitFileModification file = new GitFileModification("huge.java", "huge.java");
      file.diffHunkBlocks.add(largeDiffHunkBlock);

      // When & Then
      assertEquals(1, file.getHunkCount());
      assertEquals(100, file.getTotalLineCount());

      GitFileModification copy = file.shallowCopyWithSingleHunk(largeDiffHunkBlock);
      assertEquals(100, copy.getTotalLineCount());
    }

    @Test
    @DisplayName("Doit gérer les caractères spéciaux dans les chemins")
    void shouldHandleSpecialCharactersInPaths() {
      // Given
      String pathWithSpecialChars = "src/main/java/Tëst_Filé@#$.java";
      GitFileModification file =
          new GitFileModification(pathWithSpecialChars, pathWithSpecialChars);

      // Then
      assertEquals(".java", file.getFileExtension());
      assertEquals("Tëst_Filé@#$.java", file.getFileName());
      assertFalse(file.isRenamed());
    }

    @Test
    @DisplayName("Doit préserver l'ordre des hunks lors des copies")
    void shouldPreserveHunkOrderDuringCopies() {
      // Given
      GitFileModification original = new GitFileModification("ordered.java", "ordered.java");
      for (int i = 0; i < 10; i++) {
        DiffHunkBlock diffHunkBlock = createHunk(i * 10, 1, i * 10, 1, Arrays.asList("line " + i));
        original.diffHunkBlocks.add(diffHunkBlock);
      }

      // When
      GitFileModification deepCopy = original.deepCopy();

      // Then
      for (int i = 0; i < 10; i++) {
        assertEquals("line " + i, original.diffHunkBlocks.get(i).lines.get(0));
        assertEquals("line " + i, deepCopy.diffHunkBlocks.get(i).lines.get(0));
      }
    }
  }
}
