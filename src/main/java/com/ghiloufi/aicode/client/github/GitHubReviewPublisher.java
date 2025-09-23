package com.ghiloufi.aicode.client.github;

import com.ghiloufi.aicode.domain.model.DiffAnalysisBundle;
import com.ghiloufi.aicode.domain.model.GitDiffDocument;
import com.ghiloufi.aicode.domain.model.ReviewResult;
import com.ghiloufi.aicode.client.github.GithubClient;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Service responsable de la publication des résultats d'analyse de code sur GitHub.
 *
 * <p>Cette classe permet de publier les commentaires de review générés par l'IA sous forme de
 * commentaire de synthèse et de commentaires inline sur une Pull Request GitHub.
 *
 * <p>Le processus de publication comprend deux étapes principales :
 *
 * <ul>
 *   <li>Publication d'un commentaire de synthèse contenant le résumé global de l'analyse
 *   <li>Publication de commentaires inline pour chaque issue identifiée dans le code
 * </ul>
 *
 * @author Generated AI Code Review
 * @since 1.0
 */
@Service
@ConditionalOnProperty(name = "app.mode", havingValue = "github")
public class GitHubReviewPublisher {

  private static final Logger log = LoggerFactory.getLogger(GitHubReviewPublisher.class);

  /** Titre par défaut pour le résumé d'analyse */
  private static final String DEFAULT_SUMMARY_TITLE = "🤖 AI Review Summary";

  /** Message affiché lorsqu'aucun résumé n'est disponible */
  private static final String NO_SUMMARY_MESSAGE = "No summary";

  /** Template pour l'en-tête des commentaires inline */
  private static final String INLINE_COMMENT_HEADER_TEMPLATE = "**%s** %s\n\n";

  /** Template pour la section suggestion des commentaires inline */
  private static final String SUGGESTION_SECTION_TEMPLATE = "**Suggestion:**\n%s\n";

  /** Template pour la section références des commentaires inline */
  private static final String REFERENCES_SECTION_TEMPLATE = "\n**References:** %s";

  private final GithubClient githubClient;

  /**
   * Construit un nouveau publisher GitHub avec le client spécifié.
   *
   * @param githubClient le client GitHub utilisé pour les interactions avec l'API
   * @throws IllegalArgumentException si le client GitHub est null
   */
  @Autowired
  public GitHubReviewPublisher(GithubClient githubClient) {
    if (githubClient == null) {
      throw new IllegalArgumentException("GithubClient ne peut pas être null");
    }
    this.githubClient = githubClient;
  }

  /**
   * Normalise une chaîne de caractères en retournant une valeur par défaut si elle est null.
   *
   * @param value la valeur à normaliser
   * @param defaultValue la valeur par défaut si la valeur est null
   * @return la valeur normalisée
   */
  private static String normalizeString(String value, String defaultValue) {
    return (value == null) ? defaultValue : value;
  }

  /**
   * Publie les résultats d'une analyse de code sur une Pull Request GitHub.
   *
   * <p>Cette méthode publie un commentaire de synthèse contenant le résumé global de l'analyse,
   * puis tente de publier des commentaires inline pour chaque issue identifiée. Si le mapping des
   * positions échoue pour certains commentaires, ils seront uniquement inclus dans le résumé.
   *
   * @param pullRequestNumber le numéro de la Pull Request cible
   * @param reviewResult le résultat de l'analyse contenant le résumé et les issues
   * @param diffAnalysisBundle le bundle contenant le diff unifié nécessaire pour le mapping des
   *     positions
   */
  public void publish(
      int pullRequestNumber, ReviewResult reviewResult, DiffAnalysisBundle diffAnalysisBundle) {
    if (reviewResult == null) {
      log.warn("ReviewResult est null, rien à publier.");
      return;
    }

    log.info("Début de la publication des résultats pour la PR #{}", pullRequestNumber);

    publishSummaryComment(pullRequestNumber, reviewResult);
    publishInlineComments(pullRequestNumber, reviewResult, diffAnalysisBundle);

    log.info("Publication terminée pour la PR #{}", pullRequestNumber);
  }

  /**
   * Publie le commentaire de synthèse sur la Pull Request.
   *
   * <p>Le commentaire de synthèse contient le résumé global de l'analyse ainsi que le nombre total
   * d'issues identifiées.
   *
   * @param pullRequestNumber le numéro de la Pull Request
   * @param reviewResult le résultat de l'analyse
   */
  private void publishSummaryComment(int pullRequestNumber, ReviewResult reviewResult) {
    log.debug("Publication du commentaire de synthèse pour la PR #{}", pullRequestNumber);

    String summaryContent = buildSummaryContent(reviewResult);
    githubClient.postIssueComment(pullRequestNumber, summaryContent);

    log.debug("Commentaire de synthèse publié avec succès");
  }

  /**
   * Construit le contenu du commentaire de synthèse.
   *
   * @param reviewResult le résultat de l'analyse
   * @return le contenu markdown du commentaire de synthèse
   */
  private String buildSummaryContent(ReviewResult reviewResult) {
    String summaryText = extractSummaryText(reviewResult);
    int issuesCount = getIssuesCount(reviewResult);

    return new StringBuilder()
        .append("### ")
        .append(DEFAULT_SUMMARY_TITLE)
        .append("\n\n")
        .append(summaryText)
        .append("\n\n")
        .append("**Findings:** ")
        .append(issuesCount)
        .append(" issue(s)")
        .append("\n")
        .toString();
  }

  /**
   * Extrait le texte de résumé du résultat d'analyse.
   *
   * @param reviewResult le résultat de l'analyse
   * @return le texte de résumé nettoyé ou le message par défaut
   */
  private String extractSummaryText(ReviewResult reviewResult) {
    if (reviewResult.summary == null || reviewResult.summary.isBlank()) {
      return NO_SUMMARY_MESSAGE;
    }
    return reviewResult.summary.trim();
  }

  /**
   * Compte le nombre d'issues dans le résultat d'analyse.
   *
   * @param reviewResult le résultat de l'analyse
   * @return le nombre d'issues, 0 si la liste est null
   */
  private int getIssuesCount(ReviewResult reviewResult) {
    return reviewResult.issues != null ? reviewResult.issues.size() : 0;
  }

  /**
   * Publie les commentaires inline sur la Pull Request.
   *
   * <p>Pour chaque issue identifiée, cette méthode tente de mapper la position de ligne vers une
   * position GitHub API et crée un commentaire inline. Les issues qui ne peuvent pas être mappées
   * sont signalées dans les logs.
   *
   * @param pullRequestNumber le numéro de la Pull Request
   * @param reviewResult le résultat de l'analyse contenant les issues
   * @param diffAnalysisBundle le bundle contenant le diff pour le mapping des positions
   */
  private void publishInlineComments(
      int pullRequestNumber, ReviewResult reviewResult, DiffAnalysisBundle diffAnalysisBundle) {
    if (!canPublishInlineComments(diffAnalysisBundle)) {
      log.warn(
          "Diff bundle manquant : publication inline impossible. Les findings restent dans le résumé.");
      return;
    }

    if (reviewResult.issues == null || reviewResult.issues.isEmpty()) {
      log.debug("Aucune issue à publier en commentaire inline");
      return;
    }

    log.debug("Tentative de publication de {} commentaires inline", reviewResult.issues.size());

    List<GithubClient.ReviewComment> reviewComments =
        buildInlineComments(reviewResult, diffAnalysisBundle);

    if (!reviewComments.isEmpty()) {
      githubClient.createReview(pullRequestNumber, reviewComments);
      log.info("{} commentaires inline publiés avec succès", reviewComments.size());
    } else {
      log.warn("Aucun commentaire inline n'a pu être mappé sur des positions valides");
    }
  }

  /**
   * Vérifie si les commentaires inline peuvent être publiés.
   *
   * @param diffAnalysisBundle le bundle contenant le diff
   * @return true si les commentaires inline peuvent être publiés, false sinon
   */
  private boolean canPublishInlineComments(DiffAnalysisBundle diffAnalysisBundle) {
    return diffAnalysisBundle != null && diffAnalysisBundle.structuredDiff() != null;
  }

  /**
   * Construit la liste des commentaires inline à partir des issues.
   *
   * @param reviewResult le résultat de l'analyse
   * @param diffAnalysisBundle le bundle contenant le diff pour le mapping
   * @return la liste des commentaires inline valides
   */
  private List<GithubClient.ReviewComment> buildInlineComments(
      ReviewResult reviewResult, DiffAnalysisBundle diffAnalysisBundle) {
    GitHubDiffPositionMapper positionMapper =
        createPositionMapper(diffAnalysisBundle.structuredDiff());
    List<GithubClient.ReviewComment> comments = new ArrayList<>();

    for (ReviewResult.Issue issue : reviewResult.issues) {
      if (issue == null) {
        continue;
      }

      GithubClient.ReviewComment comment = createInlineComment(issue, positionMapper);
      if (comment != null) {
        comments.add(comment);
      }
    }

    return comments;
  }

  /**
   * Crée un commentaire inline pour une issue spécifique.
   *
   * @param issue l'issue pour laquelle créer le commentaire
   * @param positionMapper le mapper pour convertir les numéros de ligne en positions GitHub
   * @return le commentaire inline créé, ou null si la position ne peut pas être mappée
   */
  private GithubClient.ReviewComment createInlineComment(
      ReviewResult.Issue issue, GitHubDiffPositionMapper positionMapper) {
    String filePath = normalizeString(issue.file, "");
    int startLine = issue.start_line;

    int position = positionMapper.positionFor(filePath, startLine);

    if (position <= 0) {
      log.warn(
          "Impossible de mapper la position inline pour {}:{} ; sera inclus uniquement dans le résumé",
          filePath,
          startLine);
      return null;
    }

    String commentBody = buildInlineCommentContent(issue);
    return new GithubClient.ReviewComment(filePath, position, commentBody);
  }

  /**
   * Construit le contenu d'un commentaire inline pour une issue.
   *
   * <p>Le commentaire inclut la sévérité, le titre, la justification, les suggestions
   * d'amélioration et les références le cas échéant.
   *
   * @param issue l'issue pour laquelle construire le commentaire
   * @return le contenu markdown du commentaire inline
   */
  private String buildInlineCommentContent(ReviewResult.Issue issue) {
    StringBuilder commentBuilder = new StringBuilder();

    // En-tête avec sévérité et titre
    appendCommentHeader(commentBuilder, issue);

    // Justification
    appendRationale(commentBuilder, issue);

    // Suggestion
    appendSuggestion(commentBuilder, issue);

    // Références
    appendReferences(commentBuilder, issue);

    return commentBuilder.toString().trim();
  }

  /**
   * Ajoute l'en-tête du commentaire (sévérité et titre).
   *
   * @param builder le builder pour construire le commentaire
   * @param issue l'issue contenant les informations
   */
  private void appendCommentHeader(StringBuilder builder, ReviewResult.Issue issue) {
    String severity = normalizeString(issue.severity, "info").toUpperCase();
    String title = normalizeString(issue.title, "");

    builder.append(String.format(INLINE_COMMENT_HEADER_TEMPLATE, severity, title));
  }

  /**
   * Ajoute la section justification au commentaire.
   *
   * @param builder le builder pour construire le commentaire
   * @param issue l'issue contenant les informations
   */
  private void appendRationale(StringBuilder builder, ReviewResult.Issue issue) {
    String rationale = normalizeString(issue.rationale, "");

    if (!rationale.isBlank()) {
      builder.append(rationale).append("\n\n");
    }
  }

  /**
   * Ajoute la section suggestion au commentaire.
   *
   * @param builder le builder pour construire le commentaire
   * @param issue l'issue contenant les informations
   */
  private void appendSuggestion(StringBuilder builder, ReviewResult.Issue issue) {
    String suggestion = normalizeString(issue.suggestion, "").trim();

    if (!suggestion.isBlank()) {
      builder.append(String.format(SUGGESTION_SECTION_TEMPLATE, suggestion));
    }
  }

  /**
   * Ajoute la section références au commentaire.
   *
   * @param builder le builder pour construire le commentaire
   * @param issue l'issue contenant les informations
   */
  private void appendReferences(StringBuilder builder, ReviewResult.Issue issue) {
    if (issue.references != null && !issue.references.isEmpty()) {
      String referencesText = String.join(", ", issue.references);
      builder.append(String.format(REFERENCES_SECTION_TEMPLATE, referencesText));
    }
  }

  /**
   * Créer le position mapper.
   *
   * @param diff la valeur du unified diff
   * @return le position mapper
   */
  protected GitHubDiffPositionMapper createPositionMapper(GitDiffDocument diff) {
    return new GitHubDiffPositionMapper(diff);
  }
}
