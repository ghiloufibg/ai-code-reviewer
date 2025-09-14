package com.ghiloufi.aicode.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Client réactif pour interagir avec un modèle de langage (LLM) via API HTTP.
 *
 * <p>Cette classe fournit une interface réactive pour envoyer des requêtes à un LLM et recevoir
 * des réponses formatées en JSON avec support du streaming. Elle est principalement utilisée pour
 * effectuer des revues de code automatisées dans le contexte du plugin AI Code Reviewer.
 *
 * <p><strong>Architecture :</strong> Le client utilise WebClient de Spring WebFlux pour des communications
 * non-bloquantes. Il supporte le streaming des réponses LLM pour améliorer les performances.
 *
 * <p><strong>Configuration :</strong>
 *
 * <ul>
 *   <li>URL de base : L'endpoint de l'API LLM
 *   <li>Modèle : Le nom/ID du modèle à utiliser
 *   <li>Timeout : Durée maximale d'attente pour une réponse
 * </ul>
 *
 * <p><strong>Format de réponse attendu :</strong> Le LLM doit retourner une réponse JSON ou un stream
 * de chunks JSON pour les réponses streamées.
 *
 * @version 2.0
 * @since 1.0
 */
@Service
public class LlmClient {

  private static final Logger logger = LoggerFactory.getLogger(LlmClient.class);

  // Constantes pour l'API
  private static final String API_ENDPOINT = "v1/chat/completions";
  private static final String CONTENT_TYPE_HEADER = "Content-Type";
  private static final String APPLICATION_JSON = "application/json";

  // Constantes pour le payload JSON
  private static final String FIELD_MODEL = "model";
  private static final String FIELD_MESSAGES = "messages";
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
  private final WebClient webClient;
  private final ObjectMapper objectMapper;

  /**
   * Construit un nouveau client LLM réactif.
   *
   * <p>Le client est configuré avec une URL de base pour l'API, le nom du modèle à utiliser, et un
   * timeout pour les requêtes. Utilise WebClient pour les communications non-bloquantes.
   *
   * @param baseUrl L'URL de base de l'API LLM (ex: "http://localhost:11434")
   * @param model Le nom ou ID du modèle LLM à utiliser (ex: "codellama:13b")
   * @param timeoutSeconds La durée maximale d'attente pour une réponse en secondes
   * @throws IllegalArgumentException si un paramètre est null ou invalide
   */
  public LlmClient(@Value("${app.llm.baseUrl:http://localhost:1234}") String baseUrl,
                   @Value("${app.llm.model:deepseek-coder-6.7b-instruct}") String model,
                   @Value("${app.llm.timeoutSeconds:45}") int timeoutSeconds) {
    Duration timeout = Duration.ofSeconds(timeoutSeconds);
    validateConstructorParameters(baseUrl, model, timeout);

    this.baseUrl = normalizeBaseUrl(baseUrl);
    this.model = model;
    this.timeout = timeout;
    this.objectMapper = new ObjectMapper();
    this.webClient = WebClient.builder()
        .baseUrl(this.baseUrl)
        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
        .build();

    logger.info(
        "LlmClient réactif initialisé - URL: {}, Modèle: {}, Timeout: {}s",
        this.baseUrl,
        this.model,
        timeout.getSeconds());
  }

  /**
   * Effectue une revue en envoyant des prompts système et utilisateur au LLM.
   * Version synchrone pour compatibilité ascendante.
   *
   * @param systemPrompt Le prompt système définissant le contexte et les instructions
   * @param userPrompt Le prompt utilisateur contenant le contenu à analyser
   * @return La réponse du LLM, extraite selon le format de réponse
   * @throws LlmClientException si la requête échoue ou si les paramètres sont invalides
   */
  public String review(String systemPrompt, String userPrompt) {
    return reviewReactive(systemPrompt, userPrompt).block();
  }

  /**
   * Effectue une revue réactive en envoyant des prompts système et utilisateur au LLM.
   *
   * <p>Cette méthode envoie une requête au LLM avec un prompt système qui définit le contexte et un
   * prompt utilisateur qui contient le contenu à analyser. Le LLM est configuré pour retourner une
   * réponse JSON avec une température basse (0.1) pour des résultats déterministes.
   *
   * @param systemPrompt Le prompt système définissant le contexte et les instructions
   * @param userPrompt Le prompt utilisateur contenant le contenu à analyser
   * @return Un Mono contenant la réponse du LLM
   */
  public Mono<String> reviewReactive(String systemPrompt, String userPrompt) {
    return Mono.fromCallable(() -> {
      validatePrompts(systemPrompt, userPrompt);
      return buildReviewPayload(systemPrompt, userPrompt);
    })
    .flatMap(payload -> {
      logger.debug("Envoi de la requête de review au LLM réactif");

      return webClient.post()
          .uri(API_ENDPOINT)
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(payload))
          .retrieve()
          .bodyToMono(String.class)
          .timeout(timeout)
          .retryWhen(Retry.backoff(2, Duration.ofMillis(500)))
          .doOnNext(response -> logger.trace("Réponse brute du LLM: {}", response))
          .map(this::extractContentFromResponse)
          .doOnError(error -> logger.error("Erreur lors de la communication avec le LLM", error))
          .onErrorMap(throwable -> new LlmClientException("Erreur lors de la communication avec le LLM", throwable));
    });
  }

  /**
   * Effectue une revue avec streaming des réponses.
   *
   * @param systemPrompt Le prompt système
   * @param userPrompt Le prompt utilisateur
   * @return Un Flux de chunks de réponse
   */
  public Flux<String> reviewStream(String systemPrompt, String userPrompt) {
    return Mono.fromCallable(() -> {
      validatePrompts(systemPrompt, userPrompt);
      Map<String, Object> payload = buildReviewPayload(systemPrompt, userPrompt);
      payload.put("stream", true);
      return payload;
    })
    .flatMapMany(payload -> {
      logger.debug("Envoi de la requête de review streamée au LLM");

      return webClient.post()
          .uri(API_ENDPOINT)
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(payload))
          .retrieve()
          .bodyToFlux(String.class)
          .timeout(timeout)
          .retryWhen(Retry.backoff(2, Duration.ofMillis(500)))
          .filter(chunk -> !chunk.trim().isEmpty())
          .map(this::extractContentFromStreamChunk)
          .filter(content -> content != null && !content.isEmpty())
          .doOnError(error -> logger.error("Erreur lors du streaming LLM", error))
          .onErrorResume(throwable -> {
            logger.error("Erreur fatale lors du streaming, tentative de récupération", throwable);
            return Flux.error(new LlmClientException("Erreur lors du streaming LLM", throwable));
          });
    });
  }


  /** Normalise l'URL de base en s'assurant qu'elle se termine par '/'. */
  private String normalizeBaseUrl(String url) {
    return url.endsWith("/") ? url : url + "/";
  }




  /**
   * Construit le payload pour la requête de review.
   *
   * @param systemPrompt Le prompt système
   * @param userPrompt Le prompt utilisateur
   * @return Le payload JSON pour la requête
   */
  private Map<String, Object> buildReviewPayload(String systemPrompt, String userPrompt) {
    try {
      String fullSystemPrompt = systemPrompt + JSON_INSTRUCTION;

      List<Map<String, String>> messages = List.of(
          Map.of(FIELD_ROLE, ROLE_SYSTEM, FIELD_CONTENT, fullSystemPrompt),
          Map.of(FIELD_ROLE, ROLE_USER, FIELD_CONTENT, userPrompt)
      );

      Map<String, Object> payload = new HashMap<>();
      payload.put(FIELD_MODEL, model);
      payload.put(FIELD_MESSAGES, messages);
      payload.put(FIELD_TEMPERATURE, DEFAULT_TEMPERATURE);

      logger.trace("Payload construit: {}", objectMapper.writeValueAsString(payload));
      return payload;
    } catch (JsonProcessingException e) {
      logger.error("Erreur lors de la construction du payload", e);
      throw new LlmClientException("Erreur lors de la construction du payload", e);
    }
  }


  /**
   * Extrait le contenu de la réponse JSON selon le format.
   *
   * @param responseBody Le corps de la réponse brute
   * @return Le contenu extrait
   */
  private String extractContentFromResponse(String responseBody) {
    try {
      JsonNode rootNode = objectMapper.readTree(responseBody);

      // Format standard OpenAI
      if (rootNode.has("choices") && rootNode.get("choices").isArray() && !rootNode.get("choices").isEmpty()) {
        JsonNode firstChoice = rootNode.get("choices").get(0);
        if (firstChoice.has(FIELD_MESSAGE) && firstChoice.get(FIELD_MESSAGE).has(FIELD_CONTENT)) {
          String content = firstChoice.get(FIELD_MESSAGE).get(FIELD_CONTENT).asText();
          logger.debug("Contenu extrait du format OpenAI");
          return content;
        }
      }

      // Format direct message.content
      if (rootNode.has(FIELD_MESSAGE)) {
        JsonNode messageNode = rootNode.get(FIELD_MESSAGE);
        if (messageNode.has(FIELD_CONTENT)) {
          String content = messageNode.get(FIELD_CONTENT).asText();
          logger.debug("Contenu extrait du champ 'message.content'");
          return content;
        }
      }

      // Format direct content
      if (rootNode.has(FIELD_CONTENT)) {
        String content = rootNode.get(FIELD_CONTENT).asText();
        logger.debug("Contenu extrait du champ 'content'");
        return content;
      }

      // Si aucun champ standard n'est trouvé, retourner le corps brut
      logger.warn("Format de réponse non reconnu, retour du corps brut");
      return responseBody;
    } catch (JsonProcessingException e) {
      logger.error("Erreur lors du parsing JSON, retour du corps brut", e);
      return responseBody;
    }
  }

  /**
   * Extrait le contenu d'un chunk de streaming.
   *
   * @param chunk Le chunk de réponse
   * @return Le contenu extrait ou null si pas de contenu
   */
  private String extractContentFromStreamChunk(String chunk) {
    try {
      // Les chunks de streaming sont souvent préfixés par "data: "
      String jsonPart = chunk.startsWith("data: ") ? chunk.substring(6).trim() : chunk.trim();

      // Ignorer les chunks de fin
      if ("[DONE]".equals(jsonPart) || jsonPart.isEmpty()) {
        return null;
      }

      JsonNode chunkNode = objectMapper.readTree(jsonPart);

      // Format OpenAI streaming
      if (chunkNode.has("choices") && chunkNode.get("choices").isArray() && !chunkNode.get("choices").isEmpty()) {
        JsonNode firstChoice = chunkNode.get("choices").get(0);
        if (firstChoice.has("delta") && firstChoice.get("delta").has(FIELD_CONTENT)) {
          return firstChoice.get("delta").get(FIELD_CONTENT).asText();
        }
      }

      return null;
    } catch (JsonProcessingException e) {
      logger.trace("Chunk non-JSON ignoré: {}", chunk);
      return null;
    }
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
