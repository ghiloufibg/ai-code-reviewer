package com.ghiloufi.aicode.domain;

import java.util.*;

/**
 * Représente un diff Git unifié contenant tous les fichiers modifiés d'un commit ou d'une révision.
 *
 * <p>Cette classe encapsule un ensemble de modifications de fichiers et fournit des méthodes pour
 * générer le format textuel unifié Git standard. Le format unifié Git est la représentation
 * textuelle standard des différences entre versions de fichiers utilisée par Git.
 *
 * <p>Structure du format unifié Git :
 *
 * <pre>
 * --- a/ancien/fichier.txt
 * +++ b/nouveau/fichier.txt
 * @@ -ligne_debut,nb_lignes +ligne_debut,nb_lignes @@
 * -ligne supprimée
 * +ligne ajoutée
 *  ligne de contexte
 * </pre>
 *
 * <p>La classe permet de :
 *
 * <ul>
 *   <li>Stocker plusieurs fichiers modifiés dans un seul diff
 *   <li>Générer la représentation textuelle complète au format Git
 *   <li>Analyser et manipuler les modifications de manière structurée
 * </ul>
 *
 * <p><strong>Note :</strong> Cette classe utilise des champs publics pour maintenir la
 * compatibilité avec le code existant. Dans une nouvelle architecture, l'encapsulation serait
 * préférable.
 *
 * @version 1.0
 * @since 1.0
 * @see GitFileModification
 * @see DiffHunkBlock
 * @see DiffAnalysisBundle
 */
public class GitDiffDocument {

  /** Préfixe pour les en-têtes de fichiers dans l'ancienne version */
  private static final String OLD_FILE_PREFIX = "--- a/";
  /** Préfixe pour les en-têtes de fichiers dans la nouvelle version */
  private static final String NEW_FILE_PREFIX = "+++ b/";
  /** Marqueur de début et fin de l'en-tête de hunk */
  private static final String HUNK_MARKER = "@@";
  /** Préfixe pour les informations d'ancienne version dans l'en-tête de hunk */
  private static final String OLD_VERSION_PREFIX = " -";
  /** Préfixe pour les informations de nouvelle version dans l'en-tête de hunk */
  private static final String NEW_VERSION_PREFIX = " +";
  /** Séparateur entre les numéros de ligne et le nombre de lignes */
  private static final String LINE_COUNT_SEPARATOR = ",";
  /** Suffixe ajouté après chaque ligne dans la sortie */
  private static final String LINE_SUFFIX = " ";
  /**
   * Liste des fichiers modifiés dans ce diff unifié.
   *
   * <p>Chaque élément représente un fichier qui a été modifié, ajouté, supprimé ou renommé. Les
   * fichiers sont généralement ordonnés selon l'ordre d'apparition dans le système de fichiers ou
   * selon l'ordre de traitement par Git.
   *
   * <p>La liste peut être vide si aucun fichier n'a été modifié, mais ne devrait jamais être null.
   *
   * @see GitFileModification
   */
  public List<GitFileModification> files = new ArrayList<>();

  /**
   * Constructeur par défaut.
   *
   * <p>Crée un diff vide avec une liste de fichiers initialisée.
   */
  public GitDiffDocument() {
    // Constructeur vide - les champs sont initialisés inline
  }

  /**
   * Constructeur avec une liste de fichiers spécifiée.
   *
   * @param files Liste des fichiers modifiés. Ne doit pas être null.
   * @throws IllegalArgumentException si files est null
   */
  public GitDiffDocument(List<GitFileModification> files) {
    Objects.requireNonNull(files, "La liste des fichiers ne peut pas être null");
    this.files.addAll(files);
  }

  /**
   * Génère la représentation textuelle de ce diff au format Git unifié standard.
   *
   * <p>Cette méthode produit une chaîne de caractères conforme au format unifié Git, incluant :
   *
   * <ul>
   *   <li>Les en-têtes de fichiers (--- a/... et +++ b/...)
   *   <li>Les en-têtes de hunks (@@ -x,y +a,b @@)
   *   <li>Le contenu des lignes avec leurs préfixes (-, +, espace)
   * </ul>
   *
   * <p><strong>Format de sortie :</strong>
   *
   * <pre>
   * --- a/fichier1.txt
   * +++ b/fichier1.txt
   * @@ -1,3 +1,4 @@
   * -ancienne ligne
   * +nouvelle ligne
   *  ligne de contexte
   * --- a/fichier2.txt
   * +++ b/fichier2.txt
   * @@ -10,2 +10,3 @@
   * +ligne ajoutée
   * </pre>
   *
   * <p><strong>Comportement :</strong>
   *
   * <ul>
   *   <li>Les fichiers sont traités dans l'ordre de la liste {@link #files}
   *   <li>Chaque fichier génère ses en-têtes suivis de tous ses hunks
   *   <li>Les hunks sont traités dans l'ordre de leur liste
   *   <li>Chaque ligne se termine par un espace (préservation du comportement original)
   * </ul>
   *
   * @return La représentation textuelle complète au format Git unifié. Retourne une chaîne vide si
   *     aucun fichier n'est présent.
   * @see GitFileModification
   * @see DiffHunkBlock
   */
  public String toUnifiedString() {
    if (files.isEmpty()) {
      return "";
    }

    StringBuilder diffBuilder = new StringBuilder();

    for (GitFileModification file : files) {
      appendFileToUnifiedString(diffBuilder, file);
    }

    return diffBuilder.toString();
  }

  /**
   * Ajoute la représentation d'un fichier au StringBuilder du diff unifié.
   *
   * <p>Cette méthode génère l'en-tête du fichier suivi de tous ses hunks.
   *
   * @param diffBuilder Le StringBuilder où ajouter le contenu
   * @param file Le fichier à traiter
   */
  private void appendFileToUnifiedString(StringBuilder diffBuilder, GitFileModification file) {
    appendFileHeaders(diffBuilder, file);
    appendFileHunks(diffBuilder, file);
  }

  /**
   * Ajoute les en-têtes d'un fichier (lignes --- et +++).
   *
   * <p>Génère les lignes d'en-tête au format :
   *
   * <pre>
   * --- a/ancien/chemin
   * +++ b/nouveau/chemin
   * </pre>
   *
   * @param diffBuilder Le StringBuilder où ajouter les en-têtes
   * @param file Le fichier dont générer les en-têtes
   */
  private void appendFileHeaders(StringBuilder diffBuilder, GitFileModification file) {
    diffBuilder.append(OLD_FILE_PREFIX).append(file.oldPath).append(LINE_SUFFIX);

    diffBuilder.append(NEW_FILE_PREFIX).append(file.newPath).append(LINE_SUFFIX);
  }

  /**
   * Ajoute tous les hunks d'un fichier au diff unifié.
   *
   * <p>Chaque hunk génère son en-tête suivi de toutes ses lignes.
   *
   * @param diffBuilder Le StringBuilder où ajouter les hunks
   * @param file Le fichier dont traiter les hunks
   */
  private void appendFileHunks(StringBuilder diffBuilder, GitFileModification file) {
    for (DiffHunkBlock hunk : file.diffHunkBlocks) {
      appendHunkToUnifiedString(diffBuilder, hunk);
    }
  }

  /**
   * Ajoute un hunk complet au diff unifié.
   *
   * <p>Génère l'en-tête du hunk suivi de toutes ses lignes.
   *
   * @param diffBuilder Le StringBuilder où ajouter le hunk
   * @param hunk Le hunk à traiter
   */
  private void appendHunkToUnifiedString(StringBuilder diffBuilder, DiffHunkBlock hunk) {
    appendHunkHeader(diffBuilder, hunk);
    appendHunkLines(diffBuilder, hunk);
  }

  /**
   * Ajoute l'en-tête d'un hunk au format Git standard.
   *
   * <p>Génère l'en-tête au format :
   *
   * <pre>
   * @@ -oldStart,oldCount +newStart,newCount @@
   * </pre>
   *
   * @param diffBuilder Le StringBuilder où ajouter l'en-tête
   * @param hunk Le hunk dont générer l'en-tête
   */
  private void appendHunkHeader(StringBuilder diffBuilder, DiffHunkBlock hunk) {
    diffBuilder
        .append(HUNK_MARKER)
        .append(OLD_VERSION_PREFIX)
        .append(hunk.oldStart)
        .append(LINE_COUNT_SEPARATOR)
        .append(hunk.oldCount)
        .append(NEW_VERSION_PREFIX)
        .append(hunk.newStart)
        .append(LINE_COUNT_SEPARATOR)
        .append(hunk.newCount)
        .append(LINE_SUFFIX)
        .append(HUNK_MARKER)
        .append(LINE_SUFFIX);
  }

  /**
   * Ajoute toutes les lignes d'un hunk au diff unifié.
   *
   * <p>Chaque ligne est ajoutée telle quelle avec un suffixe espace, préservant le comportement
   * original.
   *
   * @param diffBuilder Le StringBuilder où ajouter les lignes
   * @param hunk Le hunk dont traiter les lignes
   */
  private void appendHunkLines(StringBuilder diffBuilder, DiffHunkBlock hunk) {
    for (String line : hunk.lines) {
      diffBuilder.append(line).append(LINE_SUFFIX);
    }
  }

  /**
   * Ajoute un fichier modifié à ce diff.
   *
   * @param file Le fichier à ajouter. Ne doit pas être null.
   * @throws IllegalArgumentException si file est null
   */
  public void addFile(GitFileModification file) {
    Objects.requireNonNull(file, "Le fichier ne peut pas être null");
    files.add(file);
  }

  /**
   * Ajoute plusieurs fichiers modifiés à ce diff.
   *
   * @param filesToAdd Collection des fichiers à ajouter. Ne doit pas être null.
   * @throws IllegalArgumentException si filesToAdd est null ou contient des éléments null
   */
  public void addFiles(Collection<GitFileModification> filesToAdd) {
    Objects.requireNonNull(filesToAdd, "La collection de fichiers ne peut pas être null");

    for (GitFileModification file : filesToAdd) {
      if (file == null) {
        throw new IllegalArgumentException("La collection ne peut pas contenir d'éléments null");
      }
    }

    files.addAll(filesToAdd);
  }

  /**
   * Retourne une vue non-modifiable de la liste des fichiers.
   *
   * @return Liste en lecture seule des fichiers dans ce diff
   */
  public List<GitFileModification> getFiles() {
    return Collections.unmodifiableList(files);
  }

  /**
   * Vérifie si ce diff est vide (aucun fichier modifié).
   *
   * @return true si aucun fichier n'est présent, false sinon
   */
  public boolean isEmpty() {
    return files.isEmpty() || files.stream().allMatch(file -> file.diffHunkBlocks.isEmpty());
  }

  /**
   * Calcule le nombre total de fichiers dans ce diff.
   *
   * @return Le nombre de fichiers modifiés
   */
  public int getFileCount() {
    return files.size();
  }

  /**
   * Calcule le nombre total de hunks dans ce diff.
   *
   * @return La somme de tous les hunks de tous les fichiers
   */
  public int getTotalHunkCount() {
    return files.stream().mapToInt(file -> file.diffHunkBlocks.size()).sum();
  }

  /**
   * Calcule le nombre total de lignes modifiées dans ce diff.
   *
   * @return La somme de toutes les lignes de tous les hunks de tous les fichiers
   */
  public int getTotalLineCount() {
    return files.stream()
        .flatMap(file -> file.diffHunkBlocks.stream())
        .mapToInt(hunk -> hunk.lines.size())
        .sum();
  }

  /**
   * Recherche un fichier par son chemin (ancien ou nouveau).
   *
   * @param filePath Le chemin à rechercher. Ne doit pas être null.
   * @return Optional contenant le fichier si trouvé, Optional.empty() sinon
   * @throws IllegalArgumentException si filePath est null
   */
  public Optional<GitFileModification> findFileByPath(String filePath) {
    Objects.requireNonNull(filePath, "Le chemin de fichier ne peut pas être null");

    return files.stream()
        .filter(file -> filePath.equals(file.oldPath) || filePath.equals(file.newPath))
        .findFirst();
  }

  /**
   * Filtre les fichiers par extension.
   *
   * @param extension L'extension à rechercher (avec ou sans le point initial)
   * @return Liste des fichiers ayant l'extension spécifiée
   * @throws IllegalArgumentException si extension est null
   */
  public List<GitFileModification> getFilesByExtension(String extension) {
    Objects.requireNonNull(extension, "L'extension ne peut pas être null");

    String normalizedExtension = extension.startsWith(".") ? extension : "." + extension;

    return files.stream()
        .filter(
            file ->
                file.newPath.endsWith(normalizedExtension)
                    || file.oldPath.endsWith(normalizedExtension))
        .toList();
  }

  /**
   * Crée une copie profonde de ce diff unifié.
   *
   * @return Une nouvelle instance avec le même contenu mais complètement indépendante
   */
  public GitDiffDocument deepCopy() {
    GitDiffDocument copy = new GitDiffDocument();
    for (GitFileModification file : files) {
      copy.addFile(file.deepCopy());
    }
    return copy;
  }

  /**
   * Retourne une représentation textuelle concise de ce diff.
   *
   * @return Description du diff avec ses statistiques principales
   */
  @Override
  public String toString() {
    if (isEmpty()) {
      return "UnifiedDiff[vide]";
    }

    return String.format(
        "UnifiedDiff[%d fichier(s), %d hunk(s), %d ligne(s)]",
        getFileCount(), getTotalHunkCount(), getTotalLineCount());
  }

  /**
   * Vérifie l'égalité basée sur le contenu des fichiers.
   *
   * @param obj L'objet à comparer
   * @return true si les diffs ont le même contenu
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    GitDiffDocument other = (GitDiffDocument) obj;
    return Objects.equals(files, other.files);
  }

  /**
   * Calcule le hash code basé sur les fichiers.
   *
   * @return Hash code de ce diff
   */
  @Override
  public int hashCode() {
    return Objects.hash(files);
  }
}
