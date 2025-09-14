package com.ghiloufi.aicode.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Client pour interagir avec l'API GitHub.
 *
 * <p>Cette classe fournit des méthodes pour effectuer des opérations courantes sur les Pull
 * Requests GitHub, notamment :
 *
 * <ul>
 *   <li>Récupération des diffs unifiés de Pull Requests
 *   <li>Publication de commentaires sur les issues/PR
 *   <li>Création de reviews avec commentaires positionnés
 * </ul>
 *
 * <p><strong>Authentification :</strong> Le client supporte l'authentification via token GitHub
 * (Personal Access Token ou GitHub App Token). Si aucun token n'est fourni, les requêtes seront
 * effectuées de manière anonyme avec les limitations de taux associées.
 *
 * <p><strong>Gestion des erreurs :</strong> Toutes les méthodes lancent des {@link
 * GithubClientException} en cas d'erreur, encapsulant l'exception originale.
 *
 * <p><strong>Exemple d'utilisation :</strong>
 *
 * <pre>{@code
 * GithubClient client = new GithubClient("owner/repo", "ghp_token123");
 *
 * // Récupérer le diff d'une PR
 * String diff = client.fetchPrUnifiedDiff(42, 3);
 *
 * // Poster un commentaire
 * client.postIssueComment(42, "LGTM!");
 *
 * // Créer une review avec commentaires
 * List<ReviewComment> comments = List.of(
 *     new ReviewComment("src/Main.java", 10, "Consider using Optional here")
 * );
 * client.createReview(42, comments);
 * }</pre>
 *
 * @version 1.0
 * @since 1.0
 */
@Service
@ConditionalOnProperty(name = "app.mode", havingValue = "github")
public class GithubClient {

  private static final Logger logger = LoggerFactory.getLogger(GithubClient.class);

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
  protected CloseableHttpClient httpClient;

  /**
   * Construit un nouveau client GitHub.
   *
   * @param repository Le repository GitHub au format "owner/repo" (ex: "microsoft/vscode")
   * @param authToken Le token d'authentification GitHub (peut être null pour les requêtes anonymes)
   * @throws IllegalArgumentException si le repository est null ou vide
   */
  @Autowired
  public GithubClient(@Value("${app.repository:}") String repository,
                     @Value("${GITHUB_TOKEN:}") String authToken) {
    validateRepository(repository);

    this.repository = repository;
    this.authToken = authToken;
    this.objectMapper = new ObjectMapper();
    this.httpClient = HttpClients.createDefault();

    logger.info("GithubClient initialisé pour le repository: {}", repository);
  }

  /**
   * Récupère le diff unifié d'une Pull Request.
   *
   * <p>Cette méthode retourne le diff au format unifié Git standard, qui peut être parsé pour
   * analyser les modifications apportées dans la Pull Request.
   *
   * @param pullRequestNumber Le numéro de la Pull Request
   * @param contextLines Le nombre de lignes de contexte (non utilisé actuellement mais préservé
   *     pour compatibilité)
   * @return Le diff unifié sous forme de chaîne de caractères
   * @throws GithubClientException si la requête échoue ou si le numéro de PR est invalide
   * @throws IllegalArgumentException si pullRequestNumber est négatif ou zéro
   */
  public String fetchPrUnifiedDiff(int pullRequestNumber, int contextLines) {
    validatePullRequestNumber(pullRequestNumber);

    String url = buildUrl(ENDPOINT_PULL, pullRequestNumber);
    logger.debug(
        "Récupération du diff unifié pour PR #{} avec {} lignes de contexte",
        pullRequestNumber,
        contextLines);

    HttpGet request = createGetRequest(url, ACCEPT_DIFF);

    try {
      return executeRequestAndGetBody(request);
    } catch (IOException | ParseException e) {
      String errorMessage =
          String.format("Erreur lors de la récupération du diff pour la PR #%d", pullRequestNumber);
      logger.error(errorMessage, e);
      throw new GithubClientException(errorMessage, e);
    }
  }

  /**
   * Poste un commentaire sur une issue ou Pull Request.
   *
   * <p>Note : Dans l'API GitHub, les Pull Requests sont considérées comme des issues, donc cette
   * méthode fonctionne pour les deux.
   *
   * @param issueNumber Le numéro de l'issue ou de la Pull Request
   * @param commentBody Le contenu du commentaire (peut contenir du Markdown)
   * @throws GithubClientException si la requête échoue
   * @throws IllegalArgumentException si issueNumber est invalide ou si commentBody est null/vide
   */
  public void postIssueComment(int issueNumber, String commentBody) {
    validatePullRequestNumber(issueNumber);
    validateCommentBody(commentBody);

    String url = buildUrl(ENDPOINT_ISSUE_COMMENTS, issueNumber);
    logger.debug("Publication d'un commentaire sur l'issue/PR #{}", issueNumber);

    HttpPost request = createPostRequest(url, ACCEPT_JSON);

    try {
      Map<String, String> payload = Map.of(FIELD_BODY, commentBody);
      setJsonEntity(request, payload);

      try (CloseableHttpResponse response = httpClient.execute(request)) {
        validateResponse(response, "Publication du commentaire");
        logger.info("Commentaire publié avec succès sur l'issue/PR #{}", issueNumber);
      }
    } catch (IOException e) {
      String errorMessage =
          String.format(
              "Erreur lors de la publication du commentaire sur l'issue/PR #%d", issueNumber);
      logger.error(errorMessage, e);
      throw new GithubClientException(errorMessage, e);
    }
  }

  /**
   * Crée une review sur une Pull Request avec des commentaires positionnés.
   *
   * <p>Cette méthode permet de créer une review complète avec plusieurs commentaires attachés à des
   * lignes spécifiques du diff.
   *
   * @param pullRequestNumber Le numéro de la Pull Request
   * @param comments Liste des commentaires à inclure dans la review
   * @throws GithubClientException si la requête échoue
   * @throws IllegalArgumentException si pullRequestNumber est invalide ou si comments est null/vide
   */
  public void createReview(int pullRequestNumber, List<ReviewComment> comments) {
    validatePullRequestNumber(pullRequestNumber);
    validateReviewComments(comments);

    String url = buildUrl(ENDPOINT_PULL_REVIEWS, pullRequestNumber);
    logger.debug(
        "Création d'une review sur la PR #{} avec {} commentaire(s)",
        pullRequestNumber,
        comments.size());

    HttpPost request = createPostRequest(url, ACCEPT_JSON);

    try {
      Map<String, Object> payload = buildReviewPayload(comments);
      setJsonEntity(request, payload);

      try (CloseableHttpResponse response = httpClient.execute(request)) {
        validateResponse(response, "Création de la review");
        logger.info(
            "Review créée avec succès sur la PR #{} avec {} commentaire(s)",
            pullRequestNumber,
            comments.size());
      }
    } catch (IOException e) {
      String errorMessage =
          String.format("Erreur lors de la création de la review sur la PR #%d", pullRequestNumber);
      logger.error(errorMessage, e);
      throw new GithubClientException(errorMessage, e);
    }
  }

  /** Construit l'URL complète pour un endpoint GitHub. */
  private String buildUrl(String endpointTemplate, int number) {
    return GITHUB_API_BASE_URL + String.format(endpointTemplate, repository, number);
  }

  /** Crée une requête GET configurée avec les headers appropriés. */
  private HttpGet createGetRequest(String url, String acceptHeader) {
    HttpGet request = new HttpGet(url);
    request.addHeader(HEADER_ACCEPT, acceptHeader);
    addAuthorizationHeader(request);
    return request;
  }

  /** Crée une requête POST configurée avec les headers appropriés. */
  private HttpPost createPostRequest(String url, String acceptHeader) {
    HttpPost request = new HttpPost(url);
    request.addHeader(HEADER_ACCEPT, acceptHeader);
    addAuthorizationHeader(request);
    return request;
  }

  /** Ajoute le header d'autorisation si un token est disponible. */
  private void addAuthorizationHeader(org.apache.hc.core5.http.HttpMessage request) {
    if (authToken != null && !authToken.isBlank()) {
      request.addHeader(HEADER_AUTHORIZATION, BEARER_PREFIX + authToken);
    }
  }

  /** Configure l'entité JSON d'une requête POST. */
  private void setJsonEntity(HttpPost request, Object payload) throws IOException {
    String jsonPayload = objectMapper.writeValueAsString(payload);
    request.setEntity(new StringEntity(jsonPayload, ContentType.APPLICATION_JSON));
  }

  /** Exécute une requête GET et retourne le corps de la réponse. */
  private String executeRequestAndGetBody(HttpGet request) throws IOException, ParseException {
    try (CloseableHttpResponse response = httpClient.execute(request)) {
      return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
    }
  }

  /** Construit le payload pour une review. */
  private Map<String, Object> buildReviewPayload(List<ReviewComment> comments) {
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
  }

  /** Valide la réponse HTTP. */
  private void validateResponse(CloseableHttpResponse response, String operation) {
    int statusCode = response.getCode();
    if (statusCode < HttpStatus.SC_OK || statusCode >= HttpStatus.SC_MULTIPLE_CHOICES) {
      String errorMessage = String.format("%s échouée avec le code HTTP %d", operation, statusCode);
      logger.error(errorMessage);
      throw new GithubClientException(errorMessage);
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
   * Ferme les ressources du client.
   *
   * <p>Cette méthode devrait être appelée lorsque le client n'est plus nécessaire pour libérer les
   * ressources système.
   *
   * @throws IOException si une erreur survient lors de la fermeture
   */
  public void close() throws IOException {
    if (httpClient != null) {
      httpClient.close();
      logger.info("GithubClient fermé pour le repository: {}", repository);
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
