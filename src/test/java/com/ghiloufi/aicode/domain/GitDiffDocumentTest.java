package com.ghiloufi.aicode.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Classe de test pour {@link GitDiffDocument}.
 *
 * <p>Couvre tous les scénarios de la gestion des diffs Git unifiés.
 */
@DisplayName("GitDiffDocument")
class GitDiffDocumentTest {

  private GitDiffDocument diffDocument;
  private GitFileModification fileModification1;
  private GitFileModification fileModification2;
  private DiffHunkBlock hunk1;
  private DiffHunkBlock hunk2;

  @BeforeEach
  void setUp() {
    diffDocument = new GitDiffDocument();

    // Préparation des hunks de test
    hunk1 = new DiffHunkBlock();
    hunk1.oldStart = 1;
    hunk1.oldCount = 3;
    hunk1.newStart = 1;
    hunk1.newCount = 4;
    hunk1.lines =
        Arrays.asList(
            "-ancienne ligne 1", "+nouvelle ligne 1", " ligne de contexte", "+ligne ajoutée");

    hunk2 = new DiffHunkBlock();
    hunk2.oldStart = 10;
    hunk2.oldCount = 2;
    hunk2.newStart = 10;
    hunk2.newCount = 3;
    hunk2.lines = Arrays.asList(" ligne de contexte 2", "-ligne supprimée", "+ligne remplacée");

    // Préparation des fichiers de test
    fileModification1 = new GitFileModification();
    fileModification1.oldPath = "src/main/java/OldClass.java";
    fileModification1.newPath = "src/main/java/NewClass.java";
    fileModification1.diffHunkBlocks = Arrays.asList(hunk1, hunk2);

    fileModification2 = new GitFileModification();
    fileModification2.oldPath = "src/test/java/TestClass.java";
    fileModification2.newPath = "src/test/java/TestClass.java";
    fileModification2.diffHunkBlocks = Collections.singletonList(hunk1);
  }

  @Nested
  @DisplayName("Constructeurs")
  class ConstructorsTest {

    @Test
    @DisplayName("Constructeur par défaut crée une liste vide")
    void testDefaultConstructor() {
      GitDiffDocument diff = new GitDiffDocument();
      assertNotNull(diff.files);
      assertTrue(diff.files.isEmpty());
    }

    @Test
    @DisplayName("Constructeur avec liste copie les fichiers")
    void testConstructorWithFiles() {
      List<GitFileModification> files = Arrays.asList(fileModification1, fileModification2);
      GitDiffDocument diff = new GitDiffDocument(files);

      assertNotNull(diff.files);
      assertEquals(2, diff.files.size());
      assertTrue(diff.files.contains(fileModification1));
      assertTrue(diff.files.contains(fileModification2));
    }

    @Test
    @DisplayName("Constructeur avec liste vide")
    void testConstructorWithEmptyList() {
      GitDiffDocument diff = new GitDiffDocument(new ArrayList<>());
      assertNotNull(diff.files);
      assertTrue(diff.files.isEmpty());
    }

    @Test
    @DisplayName("Constructeur rejette une liste null")
    void testConstructorWithNullList() {
      assertThrows(NullPointerException.class, () -> new GitDiffDocument(null));
    }
  }

  @Nested
  @DisplayName("Méthode toUnifiedString")
  class ToUnifiedStringTest {

    @Test
    @DisplayName("Retourne une chaîne vide pour un diff vide")
    void testEmptyDiff() {
      assertEquals("", diffDocument.toUnifiedString());
    }

    @Test
    @DisplayName("Génère le format unifié pour un fichier simple")
    void testSingleFile() {
      diffDocument.addFile(fileModification1);
      String result = diffDocument.toUnifiedString();

      // Vérifier les en-têtes de fichier
      assertTrue(result.contains("--- a/src/main/java/OldClass.java "));
      assertTrue(result.contains("+++ b/src/main/java/NewClass.java "));

      // Vérifier les en-têtes de hunk
      assertTrue(result.contains("@@ -1,3 +1,4 @@"));
      assertTrue(result.contains("@@ -10,2 +10,3 @@"));

      // Vérifier les lignes de contenu
      assertTrue(result.contains("-ancienne ligne 1 "));
      assertTrue(result.contains("+nouvelle ligne 1 "));
      assertTrue(result.contains(" ligne de contexte "));
    }

    @Test
    @DisplayName("Génère le format unifié pour plusieurs fichiers")
    void testMultipleFiles() {
      diffDocument.addFile(fileModification1);
      diffDocument.addFile(fileModification2);
      String result = diffDocument.toUnifiedString();

      // Vérifier la présence des deux fichiers
      assertTrue(result.contains("--- a/src/main/java/OldClass.java"));
      assertTrue(result.contains("--- a/src/test/java/TestClass.java"));
    }

    @Test
    @DisplayName("Préserve l'ordre des fichiers")
    void testFileOrder() {
      diffDocument.addFile(fileModification1);
      diffDocument.addFile(fileModification2);
      String result = diffDocument.toUnifiedString();

      int index1 = result.indexOf("src/main/java/OldClass.java");
      int index2 = result.indexOf("src/test/java/TestClass.java");
      assertTrue(index1 < index2);
    }

    @Test
    @DisplayName("Gère les fichiers sans hunks")
    void testFileWithoutHunks() {
      GitFileModification emptyFile = new GitFileModification();
      emptyFile.oldPath = "empty.txt";
      emptyFile.newPath = "empty.txt";
      emptyFile.diffHunkBlocks = new ArrayList<>();

      diffDocument.addFile(emptyFile);
      String result = diffDocument.toUnifiedString();

      assertTrue(result.contains("--- a/empty.txt"));
      assertTrue(result.contains("+++ b/empty.txt"));
      assertFalse(result.contains("@@"));
    }
  }

  @Nested
  @DisplayName("Gestion des fichiers")
  class FileManagementTest {

    @Test
    @DisplayName("addFile ajoute un fichier correctement")
    void testAddFile() {
      diffDocument.addFile(fileModification1);
      assertEquals(1, diffDocument.files.size());
      assertEquals(fileModification1, diffDocument.files.get(0));
    }

    @Test
    @DisplayName("addFile rejette null")
    void testAddFileNull() {
      assertThrows(NullPointerException.class, () -> diffDocument.addFile(null));
    }

    @Test
    @DisplayName("addFiles ajoute plusieurs fichiers")
    void testAddFiles() {
      List<GitFileModification> files = Arrays.asList(fileModification1, fileModification2);
      diffDocument.addFiles(files);
      assertEquals(2, diffDocument.files.size());
    }

    @Test
    @DisplayName("addFiles rejette une collection null")
    void testAddFilesNull() {
      assertThrows(NullPointerException.class, () -> diffDocument.addFiles(null));
    }

    @Test
    @DisplayName("addFiles rejette une collection avec éléments null")
    void testAddFilesWithNullElement() {
      List<GitFileModification> files = Arrays.asList(fileModification1, null);
      assertThrows(IllegalArgumentException.class, () -> diffDocument.addFiles(files));
    }

    @Test
    @DisplayName("getFiles retourne une liste non-modifiable")
    void testGetFilesUnmodifiable() {
      diffDocument.addFile(fileModification1);
      List<GitFileModification> files = diffDocument.getFiles();
      assertThrows(UnsupportedOperationException.class, () -> files.add(fileModification2));
    }
  }

  @Nested
  @DisplayName("Méthodes de calcul et recherche")
  class CalculationAndSearchTest {

    @BeforeEach
    void setUpWithFiles() {
      diffDocument.addFile(fileModification1);
      diffDocument.addFile(fileModification2);
    }

    @Test
    @DisplayName("isEmpty retourne true pour un diff vide")
    void testIsEmptyTrue() {
      GitDiffDocument emptyDiff = new GitDiffDocument();
      assertTrue(emptyDiff.isEmpty());
    }

    @Test
    @DisplayName("isEmpty retourne false pour un diff non vide")
    void testIsEmptyFalse() {
      assertFalse(diffDocument.isEmpty());
    }

    @Test
    @DisplayName("isEmpty retourne true si tous les fichiers sont sans hunks")
    void testIsEmptyWithEmptyFiles() {
      GitDiffDocument diff = new GitDiffDocument();
      GitFileModification emptyFile = new GitFileModification();
      emptyFile.diffHunkBlocks = new ArrayList<>();
      diff.addFile(emptyFile);
      assertTrue(diff.isEmpty());
    }

    @Test
    @DisplayName("getFileCount retourne le nombre correct de fichiers")
    void testGetFileCount() {
      assertEquals(2, diffDocument.getFileCount());
    }

    @Test
    @DisplayName("getTotalHunkCount calcule le total des hunks")
    void testGetTotalHunkCount() {
      // fileModification1 a 2 hunks, fileModification2 a 1 hunk
      assertEquals(3, diffDocument.getTotalHunkCount());
    }

    @Test
    @DisplayName("getTotalLineCount calcule le total des lignes")
    void testGetTotalLineCount() {
      // hunk1: 4 lignes, hunk2: 3 lignes
      // fileModification1: hunk1 + hunk2 = 7 lignes
      // fileModification2: hunk1 = 4 lignes
      // Total: 11 lignes
      assertEquals(11, diffDocument.getTotalLineCount());
    }

    @Test
    @DisplayName("findFileByPath trouve un fichier par l'ancien chemin")
    void testFindFileByOldPath() {
      Optional<GitFileModification> result =
          diffDocument.findFileByPath("src/main/java/OldClass.java");
      assertTrue(result.isPresent());
      assertEquals(fileModification1, result.get());
    }

    @Test
    @DisplayName("findFileByPath trouve un fichier par le nouveau chemin")
    void testFindFileByNewPath() {
      Optional<GitFileModification> result =
          diffDocument.findFileByPath("src/main/java/NewClass.java");
      assertTrue(result.isPresent());
      assertEquals(fileModification1, result.get());
    }

    @Test
    @DisplayName("findFileByPath retourne empty si non trouvé")
    void testFindFileByPathNotFound() {
      Optional<GitFileModification> result = diffDocument.findFileByPath("inexistant.txt");
      assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("findFileByPath rejette null")
    void testFindFileByPathNull() {
      assertThrows(NullPointerException.class, () -> diffDocument.findFileByPath(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {".java", "java"})
    @DisplayName("getFilesByExtension trouve les fichiers avec extension")
    void testGetFilesByExtension(String extension) {
      List<GitFileModification> javaFiles = diffDocument.getFilesByExtension(extension);
      assertEquals(2, javaFiles.size());
    }

    @Test
    @DisplayName("getFilesByExtension retourne liste vide si aucune correspondance")
    void testGetFilesByExtensionNoMatch() {
      List<GitFileModification> xmlFiles = diffDocument.getFilesByExtension(".xml");
      assertTrue(xmlFiles.isEmpty());
    }

    @Test
    @DisplayName("getFilesByExtension rejette null")
    void testGetFilesByExtensionNull() {
      assertThrows(NullPointerException.class, () -> diffDocument.getFilesByExtension(null));
    }
  }

  @Nested
  @DisplayName("Méthode deepCopy")
  class DeepCopyTest {

    @Test
    @DisplayName("deepCopy crée une copie indépendante")
    void testDeepCopy() {
      // Créer un mock simple de GitFileModification avec deepCopy
      GitFileModification file =
          new GitFileModification() {
            @Override
            public GitFileModification deepCopy() {
              GitFileModification copy = new GitFileModification();
              copy.oldPath = this.oldPath;
              copy.newPath = this.newPath;
              copy.diffHunkBlocks = new ArrayList<>(this.diffHunkBlocks);
              return copy;
            }
          };
      file.oldPath = "test.txt";
      file.newPath = "test.txt";
      file.diffHunkBlocks = new ArrayList<>();

      diffDocument.addFile(file);
      GitDiffDocument copy = diffDocument.deepCopy();

      assertNotSame(diffDocument, copy);
      assertNotSame(diffDocument.files, copy.files);
      assertEquals(diffDocument.getFileCount(), copy.getFileCount());
    }

    @Test
    @DisplayName("deepCopy d'un diff vide")
    void testDeepCopyEmpty() {
      GitDiffDocument copy = diffDocument.deepCopy();
      assertTrue(copy.isEmpty());
      assertNotSame(diffDocument, copy);
    }
  }

  @Nested
  @DisplayName("Méthodes Object")
  class ObjectMethodsTest {

    @Test
    @DisplayName("toString pour un diff vide")
    void testToStringEmpty() {
      assertEquals("UnifiedDiff[vide]", diffDocument.toString());
    }

    @Test
    @DisplayName("toString avec des fichiers")
    void testToStringWithFiles() {
      diffDocument.addFile(fileModification1);
      diffDocument.addFile(fileModification2);
      String result = diffDocument.toString();
      assertTrue(result.contains("2 fichier(s)"));
      assertTrue(result.contains("3 hunk(s)"));
      assertTrue(result.contains("11 ligne(s)"));
    }

    @Test
    @DisplayName("equals avec le même objet")
    void testEqualsSameObject() {
      assertEquals(diffDocument, diffDocument);
    }

    @Test
    @DisplayName("equals avec null")
    void testEqualsNull() {
      assertNotEquals(null, diffDocument);
    }

    @Test
    @DisplayName("equals avec un autre type")
    void testEqualsDifferentType() {
      assertNotEquals("string", diffDocument);
    }

    @Test
    @DisplayName("equals avec le même contenu")
    void testEqualsSameContent() {
      diffDocument.addFile(fileModification1);

      GitDiffDocument other = new GitDiffDocument();
      other.addFile(fileModification1);

      assertEquals(diffDocument, other);
    }

    @Test
    @DisplayName("equals avec contenu différent")
    void testEqualsDifferentContent() {
      diffDocument.addFile(fileModification1);

      GitDiffDocument other = new GitDiffDocument();
      other.addFile(fileModification2);

      assertNotEquals(diffDocument, other);
    }

    @Test
    @DisplayName("hashCode cohérent avec equals")
    void testHashCode() {
      diffDocument.addFile(fileModification1);

      GitDiffDocument other = new GitDiffDocument();
      other.addFile(fileModification1);

      assertEquals(diffDocument.hashCode(), other.hashCode());
    }

    @Test
    @DisplayName("hashCode différent pour contenu différent")
    void testHashCodeDifferent() {
      diffDocument.addFile(fileModification1);

      GitDiffDocument other = new GitDiffDocument();
      other.addFile(fileModification2);

      // Généralement différent, mais pas garanti
      assertNotEquals(diffDocument.hashCode(), other.hashCode());
    }
  }

  @Nested
  @DisplayName("Cas limites et edge cases")
  class EdgeCasesTest {

    @Test
    @DisplayName("Gère les chemins avec espaces")
    void testPathsWithSpaces() {
      GitFileModification file = new GitFileModification();
      file.oldPath = "path with spaces/file.txt";
      file.newPath = "new path/file.txt";
      file.diffHunkBlocks = new ArrayList<>();

      diffDocument.addFile(file);
      String result = diffDocument.toUnifiedString();

      assertTrue(result.contains("--- a/path with spaces/file.txt"));
      assertTrue(result.contains("+++ b/new path/file.txt"));
    }

    @Test
    @DisplayName("Gère les caractères spéciaux dans les chemins")
    void testSpecialCharactersInPaths() {
      GitFileModification file = new GitFileModification();
      file.oldPath = "src/café/fichier-été.txt";
      file.newPath = "src/café/fichier-été.txt";
      file.diffHunkBlocks = Arrays.asList(hunk1);

      diffDocument.addFile(file);
      String result = diffDocument.toUnifiedString();

      assertTrue(result.contains("src/café/fichier-été.txt"));
    }

    @Test
    @DisplayName("Gère les hunks avec des valeurs 0")
    void testHunkWithZeroValues() {
      DiffHunkBlock zeroHunk = new DiffHunkBlock();
      zeroHunk.oldStart = 0;
      zeroHunk.oldCount = 0;
      zeroHunk.newStart = 1;
      zeroHunk.newCount = 1;
      zeroHunk.lines = Collections.singletonList("+nouvelle ligne");

      GitFileModification file = new GitFileModification();
      file.oldPath = "new-file.txt";
      file.newPath = "new-file.txt";
      file.diffHunkBlocks = Collections.singletonList(zeroHunk);

      diffDocument.addFile(file);
      String result = diffDocument.toUnifiedString();

      assertTrue(result.contains("@@ -0,0 +1,1 @@"));
    }

    @Test
    @DisplayName("Performance avec beaucoup de fichiers")
    void testPerformanceWithManyFiles() {
      // Créer 1000 fichiers
      for (int i = 0; i < 1000; i++) {
        GitFileModification file = new GitFileModification();
        file.oldPath = "file" + i + ".txt";
        file.newPath = "file" + i + ".txt";
        file.diffHunkBlocks = Collections.singletonList(hunk1);
        diffDocument.addFile(file);
      }

      assertEquals(1000, diffDocument.getFileCount());

      // Vérifier que toUnifiedString se termine en temps raisonnable
      long startTime = System.currentTimeMillis();
      String result = diffDocument.toUnifiedString();
      long endTime = System.currentTimeMillis();

      assertNotNull(result);
      assertTrue(endTime - startTime < 1000, "La génération devrait prendre moins d'1 seconde");
    }
  }
}
