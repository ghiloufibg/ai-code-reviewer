package com.ghiloufi.aicode.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Client pour interagir avec un modèle de langage (LLM) via API HTTP.
 *
 * <p>Cette classe fournit une interface pour envoyer des requêtes à un LLM et recevoir des réponses
 * formatées en JSON. Elle est principalement utilisée pour effectuer des revues de code
 * automatisées dans le contexte du plugin AI Code Reviewer.
 *
 * <p><strong>Architecture :</strong> Le client communique avec une API REST compatible avec le
 * format de chat standard (système/utilisateur). L'API doit accepter des messages avec des rôles et
 * retourner une réponse JSON.
 *
 * <p><strong>Configuration :</strong>
 *
 * <ul>
 *   <li>URL de base : L'endpoint de l'API LLM
 *   <li>Modèle : Le nom/ID du modèle à utiliser
 *   <li>Timeout : Durée maximale d'attente pour une réponse
 * </ul>
 *
 * <p><strong>Format de réponse attendu :</strong> Le LLM doit retourner une réponse JSON contenant
 * soit un champ "message.content", soit un champ "content" direct.
 *
 * <p><strong>Exemple d'utilisation :</strong>
 *
 * <pre>{@code
 * LlmClient client = new LlmClient(
 *     "http://localhost:11434",
 *     "codellama:13b",
 *     Duration.ofSeconds(30)
 * );
 *
 * String systemPrompt = "You are a code reviewer. Analyze the following code.";
 * String userPrompt = "Review this Java method: public void process() {...}";
 *
 * String review = client.review(systemPrompt, userPrompt);
 * System.out.println("Review: " + review);
 *
 * // N'oubliez pas de fermer le client
 * client.close();
 * }</pre>
 *
 * @version 1.0
 * @since 1.0
 */
@Service
public class LlmClient implements Closeable {

  private static final Logger logger = LoggerFactory.getLogger(LlmClient.class);

  // Constantes pour l'API
  private static final String API_ENDPOINT = "v1/chat/completions";
  private static final String CONTENT_TYPE_HEADER = "Content-Type";
  private static final String APPLICATION_JSON = "application/json";

  // Constantes pour le payload JSON
  private static final String FIELD_MODEL = "model";
  private static final String FIELD_MESSAGES = "messages";
  private static final String FIELD_OPTIONS = "options";
  private static final String FIELD_TEMPERATURE = "temperature";
  private static final String FIELD_ROLE = "role";
  private static final String FIELD_CONTENT = "content";
  private static final String FIELD_MESSAGE = "message";

  // Valeurs des rôles
  private static final String ROLE_SYSTEM = "system";
  private static final String ROLE_USER = "user";

  // Configuration par défaut
  private static final double DEFAULT_TEMPERATURE = 0.1;
  private static final String JSON_INSTRUCTION =
      " Return ONLY JSON complying with the schema below.";

  private final String baseUrl;
  private final String model;
  private final Duration timeout;
  private final CloseableHttpClient httpClient;
  private final ObjectMapper objectMapper;

  /**
   * Construit un nouveau client LLM.
   *
   * <p>Le client est configuré avec une URL de base pour l'API, le nom du modèle à utiliser, et un
   * timeout pour les requêtes.
   *
   * @param baseUrl L'URL de base de l'API LLM (ex: "http://localhost:11434")
   * @param model Le nom ou ID du modèle LLM à utiliser (ex: "codellama:13b")
   * @param timeoutSeconds La durée maximale d'attente pour une réponse en secondes
   * @throws IllegalArgumentException si un paramètre est null ou invalide
   */
  @Autowired
  public LlmClient(@Value("${app.llm.baseUrl:http://localhost:1234}") String baseUrl,
                   @Value("${app.llm.model:deepseek-coder-6.7b-instruct}") String model,
                   @Value("${app.llm.timeoutSeconds:45}") int timeoutSeconds) {
    Duration timeout = Duration.ofSeconds(timeoutSeconds);
    validateConstructorParameters(baseUrl, model, timeout);

    this.baseUrl = normalizeBaseUrl(baseUrl);
    this.model = model;
    this.timeout = timeout;
    this.objectMapper = new ObjectMapper();
    this.httpClient = createConfiguredHttpClient(timeout);

    logger.info(
        "LlmClient initialisé - URL: {}, Modèle: {}, Timeout: {}s",
        this.baseUrl,
        this.model,
        timeout.getSeconds());
  }

  /**
   * Effectue une revue en envoyant des prompts système et utilisateur au LLM.
   *
   * <p>Cette méthode envoie une requête au LLM avec un prompt système qui définit le contexte et un
   * prompt utilisateur qui contient le contenu à analyser. Le LLM est configuré pour retourner une
   * réponse JSON avec une température basse (0.1) pour des résultats déterministes.
   *
   * <p><strong>Format de la requête :</strong>
   *
   * <pre>{@code
   * {
   *   "model": "codellama:13b",
   *   "messages": [
   *     {"role": "system", "content": "...instructions + JSON schema..."},
   *     {"role": "user", "content": "...code à analyser..."}
   *   ],
   *   "options": {
   *     "temperature": 0.1
   *   }
   * }
   * }</pre>
   *
   * <p><strong>Traitement de la réponse :</strong>
   *
   * <ol>
   *   <li>Si la réponse contient "message.content", cette valeur est extraite
   *   <li>Sinon, si elle contient "content", cette valeur est extraite
   *   <li>Sinon, le corps brut de la réponse est retourné
   * </ol>
   *
   * @param systemPrompt Le prompt système définissant le contexte et les instructions
   * @param userPrompt Le prompt utilisateur contenant le contenu à analyser
   * @return La réponse du LLM, extraite selon le format de réponse
   * @throws LlmClientException si la requête échoue ou si les paramètres sont invalides
   * @throws IllegalArgumentException si un prompt est null ou vide
   */
  public String review(String systemPrompt, String userPrompt) {
    validatePrompts(systemPrompt, userPrompt);

    String url = buildApiUrl();
    logger.debug("Envoi de la requête de review au LLM - URL: {}", url);

    HttpPost request = createPostRequest(url);

    try {
      Map<String, Object> payload = buildReviewPayload(systemPrompt, userPrompt);
      setJsonEntity(request, payload);

      return executeRequestAndExtractResponse(request);

    } catch (IOException e) {
      String errorMessage = "Erreur lors de la communication avec le LLM";
      logger.error(errorMessage, e);
      throw new LlmClientException(errorMessage, e);
    } catch (Exception e) {
      String errorMessage = "Erreur inattendue lors de la review";
      logger.error(errorMessage, e);
      throw new LlmClientException(errorMessage, e);
    }
  }

  /** Construit l'URL complète de l'API. */
  private String buildApiUrl() {
    return baseUrl + API_ENDPOINT;
  }

  /** Normalise l'URL de base en s'assurant qu'elle se termine par '/'. */
  private String normalizeBaseUrl(String url) {
    return url.endsWith("/") ? url : url + "/";
  }

  /** Crée un client HTTP configuré avec le timeout spécifié. */
  private CloseableHttpClient createConfiguredHttpClient(Duration timeout) {
    RequestConfig requestConfig =
        RequestConfig.custom()
            .setConnectionRequestTimeout(Timeout.of(timeout.toMillis(), TimeUnit.MILLISECONDS))
            .setResponseTimeout(Timeout.of(timeout.toMillis(), TimeUnit.MILLISECONDS))
            .build();

    return HttpClients.custom().setDefaultRequestConfig(requestConfig).build();
  }

  /** Crée une requête POST configurée. */
  private HttpPost createPostRequest(String url) {
    HttpPost request = new HttpPost(url);
    request.addHeader(CONTENT_TYPE_HEADER, APPLICATION_JSON);
    return request;
  }

  /** Configure l'entité JSON de la requête. */
  private void setJsonEntity(HttpPost request, Map<String, Object> payload) throws IOException {
    String jsonPayload = objectMapper.writeValueAsString(payload);
    logger.trace("Payload JSON: {}", jsonPayload);
    request.setEntity(new StringEntity(jsonPayload, ContentType.APPLICATION_JSON));
  }

  /**
   * Construit le payload pour la requête de review.
   *
   * <p>Note: Les espaces dans les clés sont intentionnels pour maintenir la compatibilité avec
   * l'implémentation existante.
   */
  private Map<String, Object> buildReviewPayload(String systemPrompt, String userPrompt) {
    // Construction du message système avec l'instruction JSON
    String fullSystemPrompt = systemPrompt + JSON_INSTRUCTION;

    // Messages avec les espaces dans les clés pour compatibilité
    List<Map<String, String>> messages =
        List.of(
            Map.of(FIELD_ROLE, ROLE_SYSTEM, FIELD_CONTENT, fullSystemPrompt),
            Map.of("role", "user", "content", userPrompt) // Espaces intentionnels
            );

    // Options avec les espaces dans les clés pour compatibilité
    Map<String, Object> options = Map.of("temperature", DEFAULT_TEMPERATURE);

    // Payload principal
    Map<String, Object> payload = new HashMap<>();
    payload.put(FIELD_MODEL, model);
    payload.put(FIELD_MESSAGES, messages);
    payload.put("temperature", DEFAULT_TEMPERATURE);
    //payload.put("max_tokens", "300");
    //payload.put(" options", options); // Espace intentionnel

    return payload;
  }

  /** Exécute la requête et extrait la réponse selon le format. */
  private String executeRequestAndExtractResponse(HttpPost request)
      throws IOException, ParseException {
    try (CloseableHttpResponse response = httpClient.execute(request)) {
      validateHttpResponse(response);

      String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
      logger.trace("Réponse brute du LLM: {}", responseBody);

      return extractContentFromResponse(responseBody);
    }
  }

  /** Valide le code de statut HTTP de la réponse. */
  private void validateHttpResponse(CloseableHttpResponse response) {
    int statusCode = response.getCode();
    if (statusCode < HttpStatus.SC_OK || statusCode >= HttpStatus.SC_MULTIPLE_CHOICES) {
      String errorMessage =
          String.format("Le LLM a retourné un code d'erreur HTTP: %d", statusCode);
      logger.error(errorMessage);
      throw new LlmClientException(errorMessage);
    }
  }

  /**
   * Extrait le contenu de la réponse JSON selon le format.
   *
   * <p>Maintient la compatibilité avec les espaces dans les noms de champs.
   */
  private String extractContentFromResponse(String responseBody) throws IOException {
    JsonNode rootNode = objectMapper.readTree(responseBody);

    // Vérifier d'abord avec les espaces (compatibilité)
    if (rootNode.has(" message")) {
      JsonNode messageNode = rootNode.get(" message");
      if (messageNode.has(" content")) {
        String content = messageNode.get(" content").asText();
        logger.debug("Contenu extrait du champ ' message. content'");
        return content;
      }
    }

    // Ensuite sans espaces (cas normal)
    if (rootNode.has(FIELD_MESSAGE)) {
      JsonNode messageNode = rootNode.get(FIELD_MESSAGE);
      if (messageNode.has(FIELD_CONTENT)) {
        String content = messageNode.get(FIELD_CONTENT).asText();
        logger.debug("Contenu extrait du champ 'message.content'");
        return content;
      }
    }

    // Vérifier le champ content avec espace
    if (rootNode.has(" content")) {
      String content = rootNode.get(" content").asText();
      logger.debug("Contenu extrait du champ ' content'");
      return content;
    }

    // Vérifier le champ content sans espace
    if (rootNode.has(FIELD_CONTENT)) {
      String content = rootNode.get(FIELD_CONTENT).asText();
      logger.debug("Contenu extrait du champ 'content'");
      return content;
    }

    // Si aucun champ standard n'est trouvé, retourner le corps brut
    logger.warn("Format de réponse non reconnu, retour du corps brut");
    return responseBody;
  }

  /** Valide les paramètres du constructeur. */
  private void validateConstructorParameters(String baseUrl, String model, Duration timeout) {
    if (baseUrl == null || baseUrl.isBlank()) {
      throw new IllegalArgumentException("L'URL de base ne peut pas être null ou vide");
    }
    if (model == null || model.isBlank()) {
      throw new IllegalArgumentException("Le modèle ne peut pas être null ou vide");
    }
    if (timeout == null) {
      throw new IllegalArgumentException("Le timeout ne peut pas être null");
    }
    if (timeout.isNegative() || timeout.isZero()) {
      throw new IllegalArgumentException("Le timeout doit être positif");
    }
  }

  /** Valide les prompts système et utilisateur. */
  private void validatePrompts(String systemPrompt, String userPrompt) {
    if (systemPrompt == null || systemPrompt.isBlank()) {
      throw new IllegalArgumentException("Le prompt système ne peut pas être null ou vide");
    }
    if (userPrompt == null || userPrompt.isBlank()) {
      throw new IllegalArgumentException("Le prompt utilisateur ne peut pas être null ou vide");
    }
  }

  /**
   * Ferme les ressources du client.
   *
   * <p>Cette méthode doit être appelée lorsque le client n'est plus nécessaire pour libérer les
   * ressources système (connexions HTTP, threads, etc.).
   *
   * @throws IOException si une erreur survient lors de la fermeture
   */
  @Override
  public void close() throws IOException {
    if (httpClient != null) {
      httpClient.close();
      logger.info("LlmClient fermé pour le modèle: {}", model);
    }
  }

  /**
   * Retourne l'URL de base configurée.
   *
   * @return L'URL de base de l'API
   */
  public String getBaseUrl() {
    return baseUrl;
  }

  /**
   * Retourne le nom du modèle configuré.
   *
   * @return Le nom ou ID du modèle
   */
  public String getModel() {
    return model;
  }

  /**
   * Retourne le timeout configuré.
   *
   * @return La durée du timeout
   */
  public Duration getTimeout() {
    return timeout;
  }
}
