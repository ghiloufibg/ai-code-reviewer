package com.ghiloufi.aicode.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Client réactif pour interagir avec l'API GitHub.
 *
 * <p>Cette classe fournit des méthodes réactives pour effectuer des opérations courantes sur les
 * Pull Requests GitHub, notamment :
 *
 * <ul>
 *   <li>Récupération des diffs unifiés de Pull Requests
 *   <li>Publication de commentaires sur les issues/PR
 *   <li>Création de reviews avec commentaires positionnés
 * </ul>
 *
 * <p><strong>Approche réactive :</strong> Utilise Spring WebClient pour des communications
 * non-bloquantes avec l'API GitHub, améliorant les performances et la scalabilité.
 *
 * <p><strong>Authentification :</strong> Le client supporte l'authentification via token GitHub
 * (Personal Access Token ou GitHub App Token). Si aucun token n'est fourni, les requêtes seront
 * effectuées de manière anonyme avec les limitations de taux associées.
 *
 * <p><strong>Exemple d'utilisation réactive :</strong>
 *
 * <pre>{@code
 * GithubClient client = new GithubClient("owner/repo", "ghp_token123");
 *
 * // Récupérer le diff d'une PR de manière réactive
 * client.fetchPrUnifiedDiffReactive(42, 3)
 *     .subscribe(diff -> processeDiff(diff));
 *
 * // Poster un commentaire de manière réactive
 * client.postIssueCommentReactive(42, "LGTM!")
 *     .subscribe();
 * }</pre>
 *
 * @version 2.0
 * @since 1.0
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "app.mode", havingValue = "github")
public class GithubClient {

  // Constantes pour l'API GitHub
  private static final String GITHUB_API_BASE_URL = "https://api.github.com";
  private static final String HEADER_ACCEPT = "Accept";
  private static final String HEADER_AUTHORIZATION = "Authorization";
  private static final String ACCEPT_DIFF = "application/vnd.github.v3.diff";
  private static final String ACCEPT_JSON = "application/vnd.github+json";
  private static final String BEARER_PREFIX = "Bearer ";

  // Constantes pour les endpoints
  private static final String ENDPOINT_PULL = "/repos/%s/pulls/%d";
  private static final String ENDPOINT_ISSUE_COMMENTS = "/repos/%s/issues/%d/comments";
  private static final String ENDPOINT_PULL_REVIEWS = "/repos/%s/pulls/%d/reviews";

  // Constantes pour les payloads
  private static final String FIELD_BODY = "body";
  private static final String FIELD_EVENT = "event";
  private static final String FIELD_COMMENTS = "comments";
  private static final String FIELD_PATH = "path";
  private static final String FIELD_POSITION = "position";
  private static final String EVENT_COMMENT = "COMMENT";

  private final String repository;
  private final String authToken;
  private final ObjectMapper objectMapper;
  private final WebClient webClient;
  private final Duration timeout;

  /**
   * Construit un nouveau client GitHub réactif.
   *
   * @param repository Le repository GitHub au format "owner/repo" (ex: "microsoft/vscode")
   * @param authToken Le token d'authentification GitHub (peut être null pour les requêtes anonymes)
   * @param timeoutSeconds Timeout pour les requêtes en secondes
   * @throws IllegalArgumentException si le repository est null ou vide
   */
  @Autowired
  public GithubClient(
      @Value("${app.repository:}") String repository,
      @Value("${GITHUB_TOKEN:}") String authToken,
      @Value("${app.github.timeoutSeconds:30}") int timeoutSeconds) {
    validateRepository(repository);

    this.repository = repository;
    this.authToken = authToken;
    this.timeout = Duration.ofSeconds(timeoutSeconds);
    this.objectMapper = new ObjectMapper();

    WebClient.Builder builder =
        WebClient.builder()
            .baseUrl(GITHUB_API_BASE_URL)
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024));

    if (authToken != null && !authToken.isBlank()) {
      builder.defaultHeader(HEADER_AUTHORIZATION, BEARER_PREFIX + authToken);
    }

    this.webClient = builder.build();

    log.info("GithubClient réactif initialisé pour le repository: {}", repository);
  }

  /**
   * Récupère le diff unifié d'une Pull Request de manière réactive.
   *
   * @param pullRequestNumber Le numéro de la Pull Request
   * @param contextLines Le nombre de lignes de contexte
   * @return Mono<String> contenant le diff unifié
   */
  public Mono<String> fetchPrUnifiedDiff(int pullRequestNumber, int contextLines) {
    return Mono.fromCallable(
            () -> {
              validatePullRequestNumber(pullRequestNumber);
              return pullRequestNumber;
            })
        .flatMap(
            prNumber -> {
              String endpoint = String.format(ENDPOINT_PULL, repository, prNumber);
              log.debug(
                  "Récupération du diff unifié pour PR #{} avec {} lignes de contexte",
                  prNumber,
                  contextLines);

              return webClient
                  .get()
                  .uri(endpoint)
                  .header(HEADER_ACCEPT, ACCEPT_DIFF)
                  .retrieve()
                  .bodyToMono(String.class)
                  .timeout(timeout)
                  .retryWhen(Retry.backoff(2, Duration.ofMillis(500)))
                  .onErrorMap(
                      throwable ->
                          new GithubClientException(
                              String.format(
                                  "Erreur lors de la récupération du diff pour la PR #%d",
                                  prNumber),
                              throwable));
            });
  }

  /**
   * Poste un commentaire sur une issue ou Pull Request de manière réactive.
   *
   * @param issueNumber Le numéro de l'issue ou de la Pull Request
   * @param commentBody Le contenu du commentaire
   * @return Mono<Void> qui se complète quand le commentaire est posté
   */
  public Mono<Void> postIssueComment(int issueNumber, String commentBody) {
    return Mono.fromCallable(
            () -> {
              validatePullRequestNumber(issueNumber);
              validateCommentBody(commentBody);
              return Map.of(FIELD_BODY, commentBody);
            })
        .flatMap(
            payload -> {
              String endpoint = String.format(ENDPOINT_ISSUE_COMMENTS, repository, issueNumber);
              log.debug("Publication d'un commentaire sur l'issue/PR #{}", issueNumber);

              return webClient
                  .post()
                  .uri(endpoint)
                  .header(HEADER_ACCEPT, ACCEPT_JSON)
                  .contentType(MediaType.APPLICATION_JSON)
                  .body(BodyInserters.fromValue(payload))
                  .retrieve()
                  .bodyToMono(Void.class)
                  .timeout(timeout)
                  .retryWhen(Retry.backoff(2, Duration.ofMillis(500)))
                  .doOnSuccess(
                      unused ->
                          log.info(
                              "Commentaire publié avec succès sur l'issue/PR #{}", issueNumber))
                  .onErrorMap(
                      throwable ->
                          new GithubClientException(
                              String.format(
                                  "Erreur lors de la publication du commentaire sur l'issue/PR #%d",
                                  issueNumber),
                              throwable));
            });
  }

  /**
   * Crée une review sur une Pull Request avec des commentaires positionnés de manière réactive.
   *
   * @param pullRequestNumber Le numéro de la Pull Request
   * @param comments Liste des commentaires à inclure dans la review
   * @return Mono<Void> qui se complète quand la review est créée
   */
  public Mono<Void> createReview(int pullRequestNumber, List<ReviewComment> comments) {
    return Mono.fromCallable(
            () -> {
              validatePullRequestNumber(pullRequestNumber);
              validateReviewComments(comments);
              return buildReviewPayload(comments);
            })
        .flatMap(
            payload -> {
              String endpoint = String.format(ENDPOINT_PULL_REVIEWS, repository, pullRequestNumber);
              log.debug(
                  "Création d'une review sur la PR #{} avec {} commentaire(s)",
                  pullRequestNumber,
                  comments.size());

              return webClient
                  .post()
                  .uri(endpoint)
                  .header(HEADER_ACCEPT, ACCEPT_JSON)
                  .contentType(MediaType.APPLICATION_JSON)
                  .body(BodyInserters.fromValue(payload))
                  .retrieve()
                  .bodyToMono(Void.class)
                  .timeout(timeout)
                  .retryWhen(Retry.backoff(2, Duration.ofMillis(500)))
                  .doOnSuccess(
                      unused ->
                          log.info(
                              "Review créée avec succès sur la PR #{} avec {} commentaire(s)",
                              pullRequestNumber,
                              comments.size()))
                  .onErrorMap(
                      throwable ->
                          new GithubClientException(
                              String.format(
                                  "Erreur lors de la création de la review sur la PR #%d",
                                  pullRequestNumber),
                              throwable));
            });
  }

  /** Construit le payload pour une review. */
  private Map<String, Object> buildReviewPayload(List<ReviewComment> comments) {
    try {
      List<Map<String, Object>> commentsList = new ArrayList<>();

      for (ReviewComment comment : comments) {
        commentsList.add(
            Map.of(
                FIELD_PATH, comment.path(),
                FIELD_POSITION, comment.position(),
                FIELD_BODY, comment.body()));
      }

      Map<String, Object> payload = new HashMap<>();
      payload.put(FIELD_EVENT, EVENT_COMMENT);
      payload.put(FIELD_COMMENTS, commentsList);

      return payload;
    } catch (Exception e) {
      throw new GithubClientException("Erreur lors de la construction du payload de review", e);
    }
  }

  /** Valide le format du repository. */
  private void validateRepository(String repo) {
    if (repo == null || repo.isBlank()) {
      throw new IllegalArgumentException("Le repository ne peut pas être null ou vide");
    }
    if (!repo.contains("/")) {
      throw new IllegalArgumentException("Le repository doit être au format 'owner/repo'");
    }
  }

  /** Valide le numéro de Pull Request/Issue. */
  private void validatePullRequestNumber(int number) {
    if (number <= 0) {
      throw new IllegalArgumentException(
          "Le numéro de PR/issue doit être positif, reçu: " + number);
    }
  }

  /** Valide le corps d'un commentaire. */
  private void validateCommentBody(String body) {
    if (body == null || body.isBlank()) {
      throw new IllegalArgumentException("Le corps du commentaire ne peut pas être null ou vide");
    }
  }

  /** Valide la liste des commentaires de review. */
  private void validateReviewComments(List<ReviewComment> comments) {
    if (comments == null || comments.isEmpty()) {
      throw new IllegalArgumentException("La liste des commentaires ne peut pas être null ou vide");
    }

    for (int i = 0; i < comments.size(); i++) {
      ReviewComment comment = comments.get(i);
      if (comment == null) {
        throw new IllegalArgumentException("Le commentaire à l'index " + i + " est null");
      }
      if (comment.path() == null || comment.path().isBlank()) {
        throw new IllegalArgumentException(
            "Le chemin du commentaire à l'index " + i + " est invalide");
      }
      if (comment.body() == null || comment.body().isBlank()) {
        throw new IllegalArgumentException(
            "Le corps du commentaire à l'index " + i + " est invalide");
      }
      if (comment.position() < 0) {
        throw new IllegalArgumentException(
            "La position du commentaire à l'index " + i + " doit être positive");
      }
    }
  }

  /**
   * Représente un commentaire dans une review de Pull Request.
   *
   * <p>Un commentaire de review est attaché à une ligne spécifique du diff et contient un message.
   *
   * @param path Le chemin du fichier concerné par le commentaire
   * @param position La position dans le diff unifié (numéro de ligne dans le diff)
   * @param body Le contenu du commentaire (supporte le Markdown)
   */
  public record ReviewComment(String path, int position, String body) {
    /**
     * Construit un nouveau commentaire de review.
     *
     * @param path Le chemin du fichier (ne peut pas être null ou vide)
     * @param position La position dans le diff (doit être >= 0)
     * @param body Le contenu du commentaire (ne peut pas être null ou vide)
     * @throws IllegalArgumentException si les paramètres sont invalides
     */
    public ReviewComment {
      Objects.requireNonNull(path, "Le chemin ne peut pas être null");
      Objects.requireNonNull(body, "Le corps du commentaire ne peut pas être null");
      if (path.isBlank()) {
        throw new IllegalArgumentException("Le chemin ne peut pas être vide");
      }
      if (body.isBlank()) {
        throw new IllegalArgumentException("Le corps du commentaire ne peut pas être vide");
      }
      if (position < 0) {
        throw new IllegalArgumentException(
            "La position doit être positive ou zéro, reçu: " + position);
      }
    }
  }

  /**
   * Exception spécifique aux erreurs du client GitHub.
   *
   * <p>Cette exception encapsule toutes les erreurs qui peuvent survenir lors de l'interaction avec
   * l'API GitHub.
   */
  public static class GithubClientException extends RuntimeException {

    /**
     * Construit une nouvelle exception avec un message.
     *
     * @param message Le message d'erreur
     */
    public GithubClientException(String message) {
      super(message);
    }

    /**
     * Construit une nouvelle exception avec un message et une cause.
     *
     * @param message Le message d'erreur
     * @param cause L'exception originale
     */
    public GithubClientException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
