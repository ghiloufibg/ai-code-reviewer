package com.ghiloufi.aicode.core;

import com.ghiloufi.aicode.domain.ReviewResult;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Agrégateur responsable de la fusion de plusieurs résultats d'analyse de code en un seul résultat
 * consolidé pour produire une review finale.
 *
 * <p>Cette classe permet de combiner les résultats partiels d'analyse provenant de différentes
 * sources ou sections de code en un rapport unifié.
 *
 * @author Ghiloufi
 * @version 1.0
 * @since 1.0
 */
@Component
public class ReviewResultMerger {

  /** Séparateur utilisé pour joindre les résumés multiples */
  private static final String SUMMARY_SEPARATOR = " ";

  /**
   * Fusionne une liste de résultats d'analyse en un seul résultat consolidé.
   * Version synchrone pour compatibilité ascendante.
   *
   * @param reviewParts Liste des résultats d'analyse à fusionner
   * @return Un nouveau {@link ReviewResult} contenant tous les éléments fusionnés
   */
  public ReviewResult merge(List<ReviewResult> reviewParts) {
    return mergeReactive(reviewParts).block();
  }

  /**
   * Fusionne une liste de résultats d'analyse en un seul résultat consolidé de manière réactive.
   *
   * <p>Cette méthode effectue les opérations suivantes :
   *
   * <ul>
   *   <li>Concatène tous les résumés non vides en un résumé global
   *   <li>Agrège toutes les issues détectées
   *   <li>Combine toutes les notes non-bloquantes
   * </ul>
   *
   * @param reviewParts Liste des résultats d'analyse à fusionner
   * @return Un Mono<ReviewResult> contenant tous les éléments fusionnés
   */
  public Mono<ReviewResult> mergeReactive(List<ReviewResult> reviewParts) {
    return Mono.fromCallable(() -> {
      validateInput(reviewParts);

      if (reviewParts.isEmpty()) {
        return createEmptyResult();
      }

      ReviewResult consolidatedResult = new ReviewResult();

      String mergedSummary = mergeSummaries(reviewParts);
      consolidatedResult.summary = mergedSummary;

      mergeIssues(reviewParts, consolidatedResult);
      mergeNonBlockingNotes(reviewParts, consolidatedResult);

      return consolidatedResult;
    });
  }

  /**
   * Fusionne un flux de résultats d'analyse en un seul résultat consolidé.
   *
   * @param reviewPartsFlux Flux des résultats d'analyse à fusionner
   * @return Un Mono<ReviewResult> contenant tous les éléments fusionnés
   */
  public Mono<ReviewResult> mergeFlux(Flux<ReviewResult> reviewPartsFlux) {
    return reviewPartsFlux
        .collectList()
        .flatMap(this::mergeReactive);
  }

  /**
   * Valide les paramètres d'entrée de la méthode merge.
   *
   * @param reviewParts Liste à valider
   * @throws IllegalArgumentException si reviewParts est null
   */
  private void validateInput(List<ReviewResult> reviewParts) {
    if (reviewParts == null) {
      throw new IllegalArgumentException("La liste des résultats d'analyse ne peut pas être null");
    }
  }

  /**
   * Crée un résultat vide pour les cas où aucun résultat n'est à fusionner.
   *
   * @return Un nouveau {@link ReviewResult} vide
   */
  private ReviewResult createEmptyResult() {
    return new ReviewResult();
  }

  /**
   * Fusionne tous les résumés des résultats d'analyse en un seul résumé.
   *
   * <p>Les résumés null ou vides sont ignorés. Les résumés valides sont concaténés avec un espace
   * comme séparateur.
   *
   * @param reviewParts Liste des résultats contenant les résumés à fusionner
   * @return Le résumé fusionné, trimé des espaces en début/fin. Retourne une chaîne vide si aucun
   *     résumé valide n'est trouvé.
   */
  private String mergeSummaries(List<ReviewResult> reviewParts) {
    return reviewParts.stream()
        .filter(Objects::nonNull)
        .map(result -> result.summary)
        .filter(summary -> summary != null && !summary.isBlank())
        .collect(Collectors.joining(SUMMARY_SEPARATOR))
        .trim();
  }

  /**
   * Fusionne toutes les issues des résultats d'analyse dans le résultat consolidé.
   *
   * <p>Cette méthode agrège toutes les issues détectées dans les différents résultats partiels, en
   * préservant leur ordre d'apparition.
   *
   * @param reviewParts Liste des résultats contenant les issues à fusionner
   * @param consolidatedResult Résultat de destination où ajouter les issues
   */
  private void mergeIssues(List<ReviewResult> reviewParts, ReviewResult consolidatedResult) {
    reviewParts.stream()
        .filter(Objects::nonNull)
        .forEach(
            result -> {
              if (result.issues != null) {
                consolidatedResult.issues.addAll(result.issues);
              }
            });
  }

  /**
   * Fusionne toutes les notes non-bloquantes des résultats d'analyse dans le résultat consolidé.
   *
   * <p>Cette méthode agrège toutes les notes non-bloquantes détectées dans les différents résultats
   * partiels, en préservant leur ordre d'apparition.
   *
   * @param reviewParts Liste des résultats contenant les notes à fusionner
   * @param consolidatedResult Résultat de destination où ajouter les notes
   */
  private void mergeNonBlockingNotes(
      List<ReviewResult> reviewParts, ReviewResult consolidatedResult) {
    reviewParts.stream()
        .filter(Objects::nonNull)
        .forEach(
            result -> {
              if (result.non_blocking_notes != null) {
                consolidatedResult.non_blocking_notes.addAll(result.non_blocking_notes);
              }
            });
  }

  /**
   * Retourne des statistiques sur le résultat de fusion.
   *
   * <p>Méthode utilitaire pour obtenir un aperçu rapide du contenu d'un résultat fusionné.
   *
   * @param result Le résultat d'analyse à analyser
   * @return Une chaîne contenant les statistiques (nombre d'issues, notes, etc.)
   * @throws IllegalArgumentException si result est null
   */
  public String getStatistics(ReviewResult result) {
    if (result == null) {
      throw new IllegalArgumentException("Le résultat d'analyse ne peut pas être null");
    }

    int issuesCount = result.issues != null ? result.issues.size() : 0;
    int notesCount = result.non_blocking_notes != null ? result.non_blocking_notes.size() : 0;
    boolean hasSummary = result.summary != null && !result.summary.isBlank();

    return String.format(
        "Statistiques: %d issues, %d notes, résumé: %s",
        issuesCount, notesCount, hasSummary ? "présent" : "absent");
  }
}
