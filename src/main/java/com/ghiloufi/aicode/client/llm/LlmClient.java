package com.ghiloufi.aicode.client.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

/**
 * Client réactif pour interagir avec un modèle de langage (LLM) via LangChain4j.
 *
 * <p>Cette classe fournit une interface réactive pour envoyer des requêtes à un LLM et recevoir des
 * réponses formatées en JSON avec support du streaming. Elle utilise LangChain4j pour abstraction
 * des différents providers LLM (OpenAI, Ollama, etc.).
 *
 * <p><strong>Architecture :</strong> Le client utilise LangChain4j avec une couche réactive pour
 * maintenir la compatibilité avec l'architecture Spring WebFlux existante.
 *
 * <p><strong>Configuration :</strong> LangChain4j est configuré via application.properties avec
 * support pour multiple providers (OpenAI, Ollama, etc.)
 *
 * <p><strong>Format de réponse attendu :</strong> Le LLM doit retourner une réponse JSON conforme
 * au schéma défini.
 *
 * @version 3.0
 * @since 1.0
 */
@Service
@Slf4j
public class LlmClient {

  // Configuration par défaut
  private static final String JSON_INSTRUCTION =
      " Return ONLY JSON complying with the schema below.";

  private final ChatLanguageModel chatModel;
  private final StreamingChatLanguageModel streamingChatModel;
  private final ObjectMapper objectMapper;
  private final Duration timeout;
  private final String model;
  private final String baseUrl;

  /**
   * Construit un nouveau client LLM réactif avec LangChain4j.
   *
   * @param chatModel Le modèle de chat LangChain4j
   * @param streamingChatModel Le modèle de chat streaming (optionnel)
   * @param timeoutSeconds La durée maximale d'attente pour une réponse en secondes
   */
  public LlmClient(
      @Autowired(required = false) ChatLanguageModel chatModel,
      @Autowired(required = false) StreamingChatLanguageModel streamingChatModel,
      @Value("${langchain4j.timeout:45}") int timeoutSeconds) {

    // Allow both to be null for test environments
    if (chatModel == null && streamingChatModel == null) {
      log.warn("Aucun modèle LangChain4j configuré - mode test ou configuration incomplète");
    }

    this.chatModel = chatModel;
    this.streamingChatModel = streamingChatModel;
    this.timeout = Duration.ofSeconds(timeoutSeconds);
    this.objectMapper = new ObjectMapper();

    // Pour compatibilité avec l'interface existante
    this.model = "langchain4j-model";
    this.baseUrl = "langchain4j-provider";

    log.info(
        "LlmClient LangChain4j initialisé - Chat: {}, Streaming: {}, Timeout: {}s",
        chatModel != null ? "activé" : "désactivé",
        streamingChatModel != null ? "activé" : "désactivé",
        timeout.getSeconds());
  }

  /**
   * Effectue une revue réactive en envoyant des prompts système et utilisateur au LLM.
   *
   * <p>Cette méthode utilise LangChain4j pour envoyer une requête au LLM configuré. La réponse est
   * traitée de manière réactive pour maintenir la compatibilité.
   *
   * @param systemPrompt Le prompt système définissant le contexte et les instructions
   * @param userPrompt Le prompt utilisateur contenant le contenu à analyser
   * @return Un Mono contenant la réponse du LLM
   */
  public Mono<String> review(String systemPrompt, String userPrompt) {
    return Mono.fromCallable(
            () -> {
              validatePrompts(systemPrompt, userPrompt);

              if (chatModel == null) {
                // Return a mock response for test environments
                log.debug("ChatModel non configuré, retour d'une réponse mock");
                return createMockResponse();
              }

              SystemMessage system = SystemMessage.from(systemPrompt + JSON_INSTRUCTION);
              UserMessage user = UserMessage.from(userPrompt);

              // Use List of ChatMessage for the API
              java.util.List<dev.langchain4j.data.message.ChatMessage> messages =
                  java.util.Arrays.asList(system, user);

              Response<AiMessage> response = chatModel.generate(messages);
              return response.content().text();
            })
        .subscribeOn(Schedulers.boundedElastic())
        .timeout(timeout)
        .retryWhen(Retry.backoff(2, Duration.ofMillis(500)))
        .doOnNext(response -> log.trace("Réponse brute du LLM: {}", response))
        .map(this::extractContentFromResponse)
        .doOnError(error -> log.error("Erreur lors de la communication avec le LLM", error))
        .onErrorMap(
            throwable ->
                new LlmClientException("Erreur lors de la communication avec le LLM", throwable));
  }

  /**
   * Effectue une revue avec streaming des réponses.
   *
   * @param systemPrompt Le prompt système
   * @param userPrompt Le prompt utilisateur
   * @return Un Flux de chunks de réponse
   */
  public Flux<String> reviewStream(String systemPrompt, String userPrompt) {
    return Mono.fromCallable(
            () -> {
              validatePrompts(systemPrompt, userPrompt);

              if (streamingChatModel == null) {
                log.debug(
                    "StreamingChatLanguageModel n'est pas configuré, utilisation d'une réponse mock streamée");
                return Flux.just(createMockResponse().split("\\s+"))
                    .delayElements(Duration.ofMillis(10))
                    .map(chunk -> chunk + " ");
              }

              return createStreamingFlux(systemPrompt, userPrompt);
            })
        .flatMapMany(flux -> flux)
        .timeout(timeout)
        .retryWhen(Retry.backoff(2, Duration.ofMillis(500)))
        .filter(chunk -> !chunk.trim().isEmpty())
        .doOnError(error -> log.error("Erreur lors du streaming LLM", error))
        .onErrorResume(
            throwable -> {
              log.error("Erreur fatale lors du streaming, tentative de récupération", throwable);
              return Flux.error(new LlmClientException("Erreur lors du streaming LLM", throwable));
            });
  }

  /** Crée un Flux streaming avec LangChain4j. */
  private Flux<String> createStreamingFlux(String systemPrompt, String userPrompt) {
    return Flux.create(
        sink -> {
          try {
            SystemMessage system = SystemMessage.from(systemPrompt + JSON_INSTRUCTION);
            UserMessage user = UserMessage.from(userPrompt);

            // Use List of ChatMessage for the API
            java.util.List<dev.langchain4j.data.message.ChatMessage> messages =
                java.util.Arrays.asList(system, user);

            StringBuilder fullResponse = new StringBuilder();

            streamingChatModel.generate(
                messages,
                new dev.langchain4j.model.StreamingResponseHandler<
                    dev.langchain4j.data.message.AiMessage>() {
                  @Override
                  public void onNext(String chunk) {
                    if (chunk != null && !chunk.isEmpty()) {
                      fullResponse.append(chunk);
                      sink.next(chunk);
                    }
                  }

                  @Override
                  public void onComplete(
                      dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage>
                          response) {
                    sink.complete();
                  }

                  @Override
                  public void onError(Throwable error) {
                    sink.error(new LlmClientException("Erreur lors du streaming", error));
                  }
                });
          } catch (Exception e) {
            sink.error(new LlmClientException("Erreur lors de l'initialisation du streaming", e));
          }
        });
  }

  /** Extrait le contenu de la réponse LLM (maintient la compatibilité avec l'ancienne logique). */
  private String extractContentFromResponse(String responseBody) {
    if (responseBody == null || responseBody.trim().isEmpty()) {
      return responseBody;
    }

    try {
      // Tenter de parser comme JSON pour extraire le contenu si nécessaire
      JsonNode rootNode = objectMapper.readTree(responseBody);

      // Format standard OpenAI (si la réponse contient une structure JSON)
      if (rootNode.has("choices")
          && rootNode.get("choices").isArray()
          && !rootNode.get("choices").isEmpty()) {
        JsonNode firstChoice = rootNode.get("choices").get(0);
        if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
          String content = firstChoice.get("message").get("content").asText();
          log.debug("Contenu extrait du format OpenAI");
          return content;
        }
      }

      // Si c'est déjà du JSON simple, le retourner tel quel
      log.debug("Utilisation de la réponse directe du LLM");
      return responseBody;
    } catch (JsonProcessingException e) {
      // Si ce n'est pas du JSON, retourner tel quel
      log.debug("Réponse non-JSON, retour direct");
      return responseBody;
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
   * Retourne l'URL de base configurée (pour compatibilité).
   *
   * @return L'URL de base de l'API
   */
  public String getBaseUrl() {
    return baseUrl;
  }

  /**
   * Retourne le nom du modèle configuré (pour compatibilité).
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

  /** Crée une réponse mock pour les environnements de test. */
  private String createMockResponse() {
    return """
            {
                "summary": "Mock review completed successfully",
                "issues": [],
                "non_blocking_notes": []
            }
            """;
  }
}
