package com.ghiloufi.aicode.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Représente les modifications apportées à un fichier spécifique dans un diff Git.
 *
 * <p>Cette classe encapsule toutes les informations relatives aux changements d'un fichier unique,
 * incluant :
 *
 * <ul>
 *   <li>Les chemins avant et après modification (pour gérer les renommages)
 *   <li>La liste des hunks (blocs de modifications) contenus dans ce fichier
 * </ul>
 *
 * <p>La classe fournit des utilitaires pour créer des copies avec un seul hunk, ce qui est
 * particulièrement utile lors de la division de gros diffs en chunks.
 *
 * <p><strong>Note :</strong> Cette classe utilise des champs publics pour maintenir la
 * compatibilité avec le code existant. Dans une nouvelle architecture, l'encapsulation serait
 * préférable.
 *
 * @version 1.0
 * @since 1.0
 * @see DiffHunkBlock
 * @see GitDiffDocument
 */
public class GitFileModification {

  /**
   * Chemin du fichier avant les modifications.
   *
   * <p>Pour un fichier nouvellement créé, ce chemin peut pointer vers {@code /dev/null} ou être
   * identique au nouveau chemin selon la convention Git utilisée.
   */
  public String oldPath;

  /**
   * Chemin du fichier après les modifications.
   *
   * <p>Pour un fichier supprimé, ce chemin peut pointer vers {@code /dev/null} ou être identique à
   * l'ancien chemin selon la convention Git utilisée.
   */
  public String newPath;

  /**
   * Liste des hunks (blocs de modifications) de ce fichier.
   *
   * <p>Chaque hunk représente un bloc contigu de lignes modifiées. La liste est ordonnée selon
   * l'ordre d'apparition dans le fichier.
   *
   * @see DiffHunkBlock
   */
  public List<DiffHunkBlock> diffHunkBlocks = new ArrayList<>();

  /**
   * Constructeur par défaut.
   *
   * <p>Crée une instance vide avec une liste de hunks initialisée.
   */
  public GitFileModification() {
    // Constructeur vide - les champs sont initialisés inline
  }

  /**
   * Constructeur avec chemins spécifiés.
   *
   * @param oldPath Chemin du fichier avant modification
   * @param newPath Chemin du fichier après modification
   */
  public GitFileModification(String oldPath, String newPath) {
    this.oldPath = oldPath;
    this.newPath = newPath;
  }

  /**
   * Crée une copie superficielle de cette modification de fichier contenant uniquement le hunk
   * spécifié.
   *
   * <p>Cette méthode est particulièrement utile pour :
   *
   * <ul>
   *   <li>Diviser un fichier avec plusieurs hunks en morceaux plus petits
   *   <li>Traiter les hunks individuellement lors de l'analyse
   *   <li>Respecter les limites de taille lors du traitement par batch
   * </ul>
   *
   * <p><strong>Comportement :</strong>
   *
   * <ul>
   *   <li>Les chemins {@code oldPath} et {@code newPath} sont copiés par référence
   *   <li>Une nouvelle liste {@code hunks} est créée contenant uniquement le hunk spécifié
   *   <li>Le hunk lui-même n'est pas cloné (copie superficielle)
   * </ul>
   *
   * @param diffHunkBlock Le hunk à inclure dans la copie. Ne doit pas être null.
   * @return Une nouvelle instance de {@code GitFileModification} contenant les mêmes chemins mais
   *     uniquement le hunk spécifié.
   * @throws IllegalArgumentException si hunk est null
   * @see #createDeepCopyWithSingleHunk(DiffHunkBlock)
   */
  public GitFileModification shallowCopyWithSingleHunk(DiffHunkBlock diffHunkBlock) {
    validateHunk(diffHunkBlock);

    GitFileModification copy = new GitFileModification();
    copy.oldPath = this.oldPath;
    copy.newPath = this.newPath;
    copy.diffHunkBlocks = new ArrayList<>();
    copy.diffHunkBlocks.add(diffHunkBlock);

    return copy;
  }

  /**
   * Crée une copie profonde de cette modification de fichier contenant uniquement le hunk spécifié
   * (cloné).
   *
   * <p>Contrairement à {@link #shallowCopyWithSingleHunk(DiffHunkBlock)}, cette méthode clone
   * également le contenu du hunk pour éviter les modifications partagées.
   *
   * @param diffHunkBlock Le hunk à cloner et inclure dans la copie. Ne doit pas être null.
   * @return Une nouvelle instance avec le hunk complètement isolé
   * @throws IllegalArgumentException si hunk est null
   * @see #shallowCopyWithSingleHunk(DiffHunkBlock)
   */
  public GitFileModification createDeepCopyWithSingleHunk(DiffHunkBlock diffHunkBlock) {
    validateHunk(diffHunkBlock);

    GitFileModification copy = new GitFileModification();
    copy.oldPath = this.oldPath;
    copy.newPath = this.newPath;
    copy.diffHunkBlocks = new ArrayList<>();
    copy.diffHunkBlocks.add(cloneHunk(diffHunkBlock));

    return copy;
  }

  /**
   * Valide qu'un hunk n'est pas null.
   *
   * @param diffHunkBlock Le hunk à valider
   * @throws IllegalArgumentException si hunk est null
   */
  private void validateHunk(DiffHunkBlock diffHunkBlock) {
    if (diffHunkBlock == null) {
      throw new IllegalArgumentException("Le hunk ne peut pas être null");
    }
  }

  /**
   * Clone un hunk en créant une copie profonde de son contenu.
   *
   * @param original Le hunk à cloner
   * @return Une nouvelle instance de Hunk avec le même contenu
   */
  private DiffHunkBlock cloneHunk(DiffHunkBlock original) {
    DiffHunkBlock clone = new DiffHunkBlock();
    clone.oldStart = original.oldStart;
    clone.oldCount = original.oldCount;
    clone.newStart = original.newStart;
    clone.newCount = original.newCount;
    clone.lines = new ArrayList<>(original.lines); // Copie profonde de la liste
    return clone;
  }

  /**
   * Vérifie si ce fichier a été renommé.
   *
   * @return true si l'ancien et le nouveau chemin sont différents, false sinon
   */
  public boolean isRenamed() {
    return !Objects.equals(oldPath, newPath) && !isNewFile() && !isDeleted();
  }

  /**
   * Vérifie si ce fichier a été nouvellement créé.
   *
   * <p>Un fichier est considéré comme nouveau si l'ancien chemin est null, vide, ou pointe vers
   * {@code /dev/null}.
   *
   * @return true si le fichier est nouvellement créé, false sinon
   */
  public boolean isNewFile() {
    return oldPath == null || oldPath.isEmpty() || "/dev/null".equals(oldPath);
  }

  /**
   * Vérifie si ce fichier a été supprimé.
   *
   * <p>Un fichier est considéré comme supprimé si le nouveau chemin est null, vide, ou pointe vers
   * {@code /dev/null}.
   *
   * @return true si le fichier a été supprimé, false sinon
   */
  public boolean isDeleted() {
    return newPath == null || newPath.isEmpty() || "/dev/null".equals(newPath);
  }

  /**
   * Vérifie si ce fichier contient des modifications.
   *
   * @return true si au moins un hunk est présent, false sinon
   */
  public boolean hasModifications() {
    return diffHunkBlocks != null && !diffHunkBlocks.isEmpty();
  }

  /**
   * Calcule le nombre total de lignes modifiées dans ce fichier.
   *
   * @return La somme des lignes de tous les hunks de ce fichier
   */
  public int getTotalLineCount() {
    if (diffHunkBlocks == null) {
      return 0;
    }

    return diffHunkBlocks.stream()
        .mapToInt(hunk -> hunk.lines != null ? hunk.lines.size() : 0)
        .sum();
  }

  /**
   * Retourne le nombre de hunks dans ce fichier.
   *
   * @return Le nombre de blocs de modifications
   */
  public int getHunkCount() {
    return diffHunkBlocks != null ? diffHunkBlocks.size() : 0;
  }

  /**
   * Retourne l'extension du fichier basée sur le nouveau chemin.
   *
   * @return L'extension avec le point (ex: ".java"), ou chaîne vide si pas d'extension
   */
  public String getFileExtension() {
    String path = newPath != null ? newPath : oldPath;
    if (path == null) {
      return "";
    }

    int lastDotIndex = path.lastIndexOf('.');
    int lastSlashIndex = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));

    // Vérifier que le point est après le dernier séparateur de répertoire
    if (lastDotIndex > lastSlashIndex && lastDotIndex < path.length() - 1) {
      return path.substring(lastDotIndex);
    }

    return "";
  }

  /**
   * Retourne le nom du fichier sans le chemin basé sur le nouveau chemin.
   *
   * @return Le nom du fichier sans répertoire
   */
  public String getFileName() {
    String path = newPath != null ? newPath : oldPath;
    if (path == null) {
      return "";
    }

    int lastSlashIndex = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
    return path.substring(lastSlashIndex + 1);
  }

  /**
   * Retourne le chemin effectif du fichier (priorité au nouveau chemin).
   *
   * @return Le nouveau chemin s'il existe, sinon l'ancien chemin
   */
  public String getEffectivePath() {
    return newPath != null ? newPath : oldPath;
  }

  /**
   * Crée une copie profonde complète de cette modification de fichier.
   *
   * @return Une nouvelle instance avec tout le contenu cloné
   */
  public GitFileModification deepCopy() {
    GitFileModification copy = new GitFileModification();
    copy.oldPath = this.oldPath;
    copy.newPath = this.newPath;
    copy.diffHunkBlocks = new ArrayList<>();

    if (this.diffHunkBlocks != null) {
      for (DiffHunkBlock diffHunkBlock : this.diffHunkBlocks) {
        copy.diffHunkBlocks.add(cloneHunk(diffHunkBlock));
      }
    }

    return copy;
  }

  /**
   * Retourne une représentation textuelle de cette modification de fichier.
   *
   * @return Description concise des modifications du fichier
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("GitFileModification[");

    if (isNewFile()) {
      sb.append("NEW: ").append(newPath);
    } else if (isDeleted()) {
      sb.append("DELETED: ").append(oldPath);
    } else if (isRenamed()) {
      sb.append("RENAMED: ").append(oldPath).append(" -> ").append(newPath);
    } else {
      sb.append("MODIFIED: ").append(getEffectivePath());
    }

    sb.append(", ").append(getHunkCount()).append(" hunk(s)");
    sb.append(", ").append(getTotalLineCount()).append(" ligne(s)");
    sb.append("]");

    return sb.toString();
  }

  /**
   * Vérifie l'égalité basée sur les chemins et le contenu des hunks.
   *
   * @param obj L'objet à comparer
   * @return true si les modifications sont identiques
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    GitFileModification other = (GitFileModification) obj;
    return Objects.equals(oldPath, other.oldPath)
        && Objects.equals(newPath, other.newPath)
        && Objects.equals(diffHunkBlocks, other.diffHunkBlocks);
  }

  /**
   * Calcule le hash code basé sur les chemins et hunks.
   *
   * @return Hash code de cette modification
   */
  @Override
  public int hashCode() {
    return Objects.hash(oldPath, newPath, diffHunkBlocks);
  }
}
