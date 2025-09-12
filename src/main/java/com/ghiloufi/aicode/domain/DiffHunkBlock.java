package com.ghiloufi.aicode.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Représente un bloc de modifications (hunk) dans un diff Git.
 *
 * <p>Un hunk est un groupe contigu de lignes modifiées dans un fichier. Il contient toutes les
 * informations nécessaires pour localiser et appliquer les modifications :
 *
 * <ul>
 *   <li>Position et nombre de lignes dans l'ancienne version du fichier
 *   <li>Position et nombre de lignes dans la nouvelle version du fichier
 *   <li>Le contenu exact des lignes avec leurs préfixes (-, +, espace)
 * </ul>
 *
 * <p>Le format d'un hunk suit la spécification Git unified diff :
 *
 * <pre>
 * @@ -oldStart,oldCount +newStart,newCount @@
 * -ligne supprimée
 * +ligne ajoutée
 *  ligne inchangée (contexte)
 * </pre>
 *
 * <p><strong>Note :</strong> Cette classe utilise des champs publics pour maintenir la
 * compatibilité avec le code existant. Dans une nouvelle architecture, l'encapsulation serait
 * préférable.
 *
 * @author Ghiloufi AI Code
 * @version 1.0
 * @since 1.0
 * @see GitFileModification
 * @see GitDiffDocument
 */
public class DiffHunkBlock {

  /**
   * Numéro de ligne de début dans l'ancienne version du fichier.
   *
   * <p>Correspond au premier paramètre après le '-' dans l'en-tête du hunk. Par exemple, dans
   * {@code @@ -10,5 +12,7 @@}, oldStart = 10.
   *
   * <p><strong>Attention :</strong> Les numéros de ligne Git commencent à 1, pas à 0. Une valeur de
   * 0 peut indiquer un fichier vide ou une insertion au début du fichier.
   */
  public int oldStart;

  /**
   * Nombre de lignes concernées dans l'ancienne version du fichier.
   *
   * <p>Correspond au second paramètre après le '-' dans l'en-tête du hunk. Par exemple, dans
   * {@code @@ -10,5 +12,7 @@}, oldCount = 5.
   *
   * <p>Cette valeur représente le nombre de lignes de contexte plus le nombre de lignes supprimées
   * dans cette section.
   */
  public int oldCount;

  /**
   * Numéro de ligne de début dans la nouvelle version du fichier.
   *
   * <p>Correspond au premier paramètre après le '+' dans l'en-tête du hunk. Par exemple, dans
   * {@code @@ -10,5 +12,7 @@}, newStart = 12.
   */
  public int newStart;

  /**
   * Nombre de lignes concernées dans la nouvelle version du fichier.
   *
   * <p>Correspond au second paramètre après le '+' dans l'en-tête du hunk. Par exemple, dans
   * {@code @@ -10,5 +12,7 @@}, newCount = 7.
   *
   * <p>Cette valeur représente le nombre de lignes de contexte plus le nombre de lignes ajoutées
   * dans cette section.
   */
  public int newCount;

  /**
   * Liste des lignes du hunk avec leurs préfixes de modification.
   *
   * <p>Chaque ligne commence par un préfixe indiquant son type :
   *
   * <ul>
   *   <li><strong>'-'</strong> : Ligne supprimée (présente dans l'ancienne version)
   *   <li><strong>'+'</strong> : Ligne ajoutée (présente dans la nouvelle version)
   *   <li><strong>' '</strong> (espace) : Ligne de contexte (inchangée)
   * </ul>
   *
   * <p>Exemple :
   *
   * <pre>
   * [" public class Test {",
   *  "-    int x = 5;",
   *  "+    int x = 10;",
   *  " }"]
   * </pre>
   *
   * <p>Les lignes sont dans l'ordre d'apparition dans le fichier.
   */
  public List<String> lines = new ArrayList<>();

  /**
   * Constructeur par défaut.
   *
   * <p>Crée un hunk vide avec une liste de lignes initialisée.
   */
  public DiffHunkBlock() {
    // Constructeur vide - les champs sont initialisés inline
  }

  /**
   * Constructeur avec paramètres de position.
   *
   * @param oldStart Position de début dans l'ancienne version
   * @param oldCount Nombre de lignes dans l'ancienne version
   * @param newStart Position de début dans la nouvelle version
   * @param newCount Nombre de lignes dans la nouvelle version
   */
  public DiffHunkBlock(int oldStart, int oldCount, int newStart, int newCount) {
    this.oldStart = oldStart;
    this.oldCount = oldCount;
    this.newStart = newStart;
    this.newCount = newCount;
  }

  /**
   * Constructeur complet avec lignes.
   *
   * @param oldStart Position de début dans l'ancienne version
   * @param oldCount Nombre de lignes dans l'ancienne version
   * @param newStart Position de début dans la nouvelle version
   * @param newCount Nombre de lignes dans la nouvelle version
   * @param lines Liste des lignes du hunk (sera copiée)
   */
  public DiffHunkBlock(int oldStart, int oldCount, int newStart, int newCount, List<String> lines) {
    this.oldStart = oldStart;
    this.oldCount = oldCount;
    this.newStart = newStart;
    this.newCount = newCount;
    this.lines = lines != null ? new ArrayList<>(lines) : new ArrayList<>();
  }

  /**
   * Retourne le nombre total de lignes dans ce hunk.
   *
   * @return Le nombre de lignes (ajoutées + supprimées + contexte)
   */
  public int getLineCount() {
    return lines != null ? lines.size() : 0;
  }

  /**
   * Compte le nombre de lignes ajoutées (préfixe '+').
   *
   * @return Le nombre de lignes commençant par '+'
   */
  public int getAddedLinesCount() {
    if (lines == null) {
      return 0;
    }

    return (int) lines.stream().filter(line -> line.startsWith("+")).count();
  }

  /**
   * Compte le nombre de lignes supprimées (préfixe '-').
   *
   * @return Le nombre de lignes commençant par '-'
   */
  public int getDeletedLinesCount() {
    if (lines == null) {
      return 0;
    }

    return (int) lines.stream().filter(line -> line.startsWith("-")).count();
  }

  /**
   * Compte le nombre de lignes de contexte (préfixe ' ').
   *
   * @return Le nombre de lignes commençant par un espace
   */
  public int getContextLinesCount() {
    if (lines == null) {
      return 0;
    }

    return (int) lines.stream().filter(line -> line.startsWith(" ")).count();
  }

  /**
   * Vérifie si ce hunk contient uniquement des ajouts.
   *
   * @return true si toutes les lignes non-contexte sont des ajouts
   */
  public boolean isAdditionOnly() {
    return getDeletedLinesCount() == 0 && getAddedLinesCount() > 0;
  }

  /**
   * Vérifie si ce hunk contient uniquement des suppressions.
   *
   * @return true si toutes les lignes non-contexte sont des suppressions
   */
  public boolean isDeletionOnly() {
    return getAddedLinesCount() == 0 && getDeletedLinesCount() > 0;
  }

  /**
   * Vérifie si ce hunk est vide (aucune ligne).
   *
   * @return true si aucune ligne n'est présente
   */
  public boolean isEmpty() {
    return lines == null || lines.isEmpty();
  }

  /**
   * Vérifie si ce hunk contient des modifications (ajouts ou suppressions).
   *
   * @return true si au moins une ligne est ajoutée ou supprimée
   */
  public boolean hasModifications() {
    return getAddedLinesCount() > 0 || getDeletedLinesCount() > 0;
  }

  /**
   * Retourne une copie des lignes ajoutées sans leur préfixe '+'.
   *
   * @return Liste des lignes ajoutées (contenu seulement)
   */
  public List<String> getAddedLinesContent() {
    if (lines == null) {
      return new ArrayList<>();
    }

    return lines.stream()
        .filter(line -> line.startsWith("+"))
        .map(line -> line.substring(1)) // Retirer le préfixe '+'
        .toList();
  }

  /**
   * Retourne une copie des lignes supprimées sans leur préfixe '-'.
   *
   * @return Liste des lignes supprimées (contenu seulement)
   */
  public List<String> getDeletedLinesContent() {
    if (lines == null) {
      return new ArrayList<>();
    }

    return lines.stream()
        .filter(line -> line.startsWith("-"))
        .map(line -> line.substring(1)) // Retirer le préfixe '-'
        .toList();
  }

  /**
   * Retourne une copie des lignes de contexte sans leur préfixe ' '.
   *
   * @return Liste des lignes de contexte (contenu seulement)
   */
  public List<String> getContextLinesContent() {
    if (lines == null) {
      return new ArrayList<>();
    }

    return lines.stream()
        .filter(line -> line.startsWith(" "))
        .map(line -> line.substring(1)) // Retirer le préfixe ' '
        .toList();
  }

  /**
   * Génère l'en-tête de ce hunk au format Git standard.
   *
   * @return L'en-tête au format "@@ -oldStart,oldCount +newStart,newCount @@"
   */
  public String generateHeader() {
    return String.format("@@ -%d,%d +%d,%d @@", oldStart, oldCount, newStart, newCount);
  }

  /**
   * Vérifie si les compteurs de lignes sont cohérents avec le contenu.
   *
   * <p>Valide que :
   *
   * <ul>
   *   <li>oldCount = lignes supprimées + lignes de contexte
   *   <li>newCount = lignes ajoutées + lignes de contexte
   * </ul>
   *
   * @return true si les compteurs sont cohérents, false sinon
   */
  public boolean isValid() {
    if (lines == null) {
      return oldCount == 0 && newCount == 0;
    }

    int contextLines = getContextLinesCount();
    int addedLines = getAddedLinesCount();
    int deletedLines = getDeletedLinesCount();

    return (deletedLines + contextLines == oldCount) && (addedLines + contextLines == newCount);
  }

  /**
   * Crée une copie profonde de ce hunk.
   *
   * @return Une nouvelle instance avec le même contenu
   */
  public DiffHunkBlock deepCopy() {
    DiffHunkBlock copy = new DiffHunkBlock();
    copy.oldStart = this.oldStart;
    copy.oldCount = this.oldCount;
    copy.newStart = this.newStart;
    copy.newCount = this.newCount;
    copy.lines = this.lines != null ? new ArrayList<>(this.lines) : new ArrayList<>();
    return copy;
  }

  /**
   * Retourne une représentation textuelle de ce hunk.
   *
   * @return Description concise du hunk avec ses statistiques
   */
  @Override
  public String toString() {
    if (isEmpty()) {
      return "DiffHunkBlock[vide]";
    }

    return String.format(
        "DiffHunkBlock[%s, +%d/-%d lignes, %d contexte]",
        generateHeader(), getAddedLinesCount(), getDeletedLinesCount(), getContextLinesCount());
  }

  /**
   * Vérifie l'égalité basée sur tous les champs.
   *
   * @param obj L'objet à comparer
   * @return true si les hunks sont identiques
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    DiffHunkBlock other = (DiffHunkBlock) obj;
    return oldStart == other.oldStart
        && oldCount == other.oldCount
        && newStart == other.newStart
        && newCount == other.newCount
        && Objects.equals(lines, other.lines);
  }

  /**
   * Calcule le hash code basé sur tous les champs.
   *
   * @return Hash code de ce hunk
   */
  @Override
  public int hashCode() {
    return Objects.hash(oldStart, oldCount, newStart, newCount, lines);
  }
}
