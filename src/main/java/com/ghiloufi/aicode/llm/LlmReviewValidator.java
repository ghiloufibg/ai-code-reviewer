package com.ghiloufi.aicode.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.*;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validateur JSON pour vérifier la conformité des réponses LLM.
 *
 * <p>Cette classe fournit des fonctionnalités de validation JSON Schema pour s'assurer que les
 * réponses du modèle de langage (LLM) respectent le format attendu. Elle utilise JSON Schema
 * Draft-07 pour la validation.
 *
 * <p><strong>Utilisation principale :</strong> Valider que les réponses JSON du LLM lors des revues
 * de code correspondent au schéma défini, garantissant ainsi la cohérence et la fiabilité du
 * parsing des résultats.
 *
 * <p><strong>Caractéristiques :</strong>
 *
 * <ul>
 *   <li>Support de JSON Schema Draft-07
 *   <li>Validation stricte avec messages d'erreur détaillés
 *   <li>Gestion robuste des erreurs de parsing
 *   <li>API simple pour validation rapide
 * </ul>
 *
 * <p><strong>Format du schéma attendu :</strong> Le schéma doit être au format JSON Schema
 * Draft-07. Pour le plugin AI Code Reviewer, le schéma typique définit la structure des issues de
 * code avec leurs sévérités, localisations et suggestions.
 *
 * <p><strong>Exemple d'utilisation :</strong>
 *
 * <pre>{@code
 * JsonValidator validator = new JsonValidator();
 *
 * String schema = """
 *     {
 *       "type": "object",
 *       "properties": {
 *         "summary": {"type": "string"},
 *         "issues": {
 *           "type": "array",
 *           "items": {
 *             "type": "object",
 *             "required": ["file", "severity", "title"]
 *           }
 *         }
 *       },
 *       "required": ["summary", "issues"]
 *     }
 *     """;
 *
 * String llmResponse = "{"summary": "...", "issues": [...]}";
 *
 * if (validator.isValid(schema, llmResponse)) {
 *     // La réponse est valide, procéder au traitement
 * } else {
 *     // Obtenir les détails des erreurs
 *     ValidationResult result = validator.validateWithDetails(schema, llmResponse);
 *     System.out.println("Erreurs: " + result.getErrors());
 * }
 * }</pre>
 *
 * @version 1.0
 * @since 1.0
 * @see com.networknt.schema.JsonSchema
 * @see com.ghiloufi.aicode.llm.PromptBuilder#OUTPUT_SCHEMA_JSON
 */
public class LlmReviewValidator {

  private static final Logger logger = LoggerFactory.getLogger(LlmReviewValidator.class);

  /**
   * Version du JSON Schema utilisée (Draft-07).
   *
   * <p>Draft-07 offre un bon équilibre entre fonctionnalités et compatibilité pour la validation
   * des réponses LLM.
   */
  private static final SpecVersion.VersionFlag SCHEMA_VERSION = SpecVersion.VersionFlag.V7;

  private final ObjectMapper objectMapper;
  private final JsonSchemaFactory schemaFactory;

  /**
   * Construit un nouveau validateur JSON avec configuration par défaut.
   *
   * <p>Utilise JSON Schema Draft-07 et un ObjectMapper standard pour le parsing et la validation.
   */
  public LlmReviewValidator() {
    this.objectMapper = new ObjectMapper();
    this.schemaFactory = JsonSchemaFactory.getInstance(SCHEMA_VERSION);
    logger.debug("JsonValidator initialisé avec JSON Schema {}", SCHEMA_VERSION);
  }

  /**
   * Construit un nouveau validateur avec un ObjectMapper personnalisé.
   *
   * <p>Permet d'utiliser une configuration Jackson spécifique si nécessaire (par exemple, pour
   * ignorer les propriétés inconnues).
   *
   * @param objectMapper L'ObjectMapper à utiliser pour le parsing
   * @throws IllegalArgumentException si objectMapper est null
   */
  public LlmReviewValidator(ObjectMapper objectMapper) {
    Objects.requireNonNull(objectMapper, "ObjectMapper ne peut pas être null");
    this.objectMapper = objectMapper;
    this.schemaFactory = JsonSchemaFactory.getInstance(SCHEMA_VERSION);
    logger.debug("JsonValidator initialisé avec ObjectMapper personnalisé");
  }

  /**
   * Valide un document JSON contre un schéma.
   *
   * <p>Cette méthode effectue une validation simple et retourne uniquement un booléen indiquant si
   * le document est valide. Pour obtenir les détails des erreurs, utilisez {@link
   * #validateWithDetails(String, String)}.
   *
   * <p><strong>Comportement :</strong>
   *
   * <ul>
   *   <li>Retourne {@code true} si le document est valide
   *   <li>Retourne {@code false} si le document est invalide
   *   <li>Retourne {@code false} en cas d'erreur de parsing
   * </ul>
   *
   * @param schemaJson Le schéma JSON Schema en format chaîne
   * @param data Le document JSON à valider
   * @return {@code true} si le document est valide, {@code false} sinon
   */
  public boolean isValid(String schemaJson, String data) {
    try {
      ValidationResult result = performValidation(schemaJson, data);
      return result.isValid();
    } catch (Exception e) {
      logger.debug("Validation échouée avec exception", e);
      return false;
    }
  }

  /**
   * Valide un document JSON et retourne les détails du résultat.
   *
   * <p>Cette méthode fournit des informations détaillées sur la validation, incluant les messages
   * d'erreur spécifiques si le document est invalide.
   *
   * @param schemaJson Le schéma JSON Schema en format chaîne
   * @param data Le document JSON à valider
   * @return Un {@link ValidationResult} contenant le statut et les détails
   * @throws ReviewValidationException si une erreur survient lors de la validation
   */
  public ValidationResult validateWithDetails(String schemaJson, String data) {
    try {
      return performValidation(schemaJson, data);
    } catch (Exception e) {
      String errorMessage = "Erreur lors de la validation JSON";
      logger.error(errorMessage, e);
      throw new ReviewValidationException(errorMessage, e);
    }
  }

  /**
   * Effectue la validation interne.
   *
   * @param schemaJson Le schéma JSON
   * @param data Les données à valider
   * @return Le résultat de validation
   */
  private ValidationResult performValidation(String schemaJson, String data)
      throws JsonProcessingException {

    validateInputs(schemaJson, data);

    logger.trace(
        "Début de la validation - Schéma: {} caractères, Données: {} caractères",
        schemaJson.length(),
        data.length());

    // Parser le schéma
    JsonSchema schema = parseSchema(schemaJson);

    // Parser les données
    JsonNode dataNode = parseData(data);

    // Effectuer la validation
    Set<ValidationMessage> errors = schema.validate(dataNode);

    // Construire le résultat
    ValidationResult result = new ValidationResult(errors.isEmpty(), errors);

    if (result.isValid()) {
      logger.debug("Validation réussie");
    } else {
      logger.debug("Validation échouée avec {} erreur(s)", errors.size());
      if (logger.isTraceEnabled()) {
        errors.forEach(error -> logger.trace("Erreur de validation: {}", error));
      }
    }

    return result;
  }

  /**
   * Parse le schéma JSON Schema.
   *
   * @param schemaJson Le schéma en format chaîne
   * @return L'objet JsonSchema
   */
  private JsonSchema parseSchema(String schemaJson) throws JsonProcessingException {
    try {
      JsonNode schemaNode = objectMapper.readTree(schemaJson);
      return schemaFactory.getSchema(schemaNode);
    } catch (JsonProcessingException e) {
      logger.error("Erreur lors du parsing du schéma JSON", e);
      throw new ReviewValidationException("Le schéma JSON est invalide", e);
    }
  }

  /**
   * Parse les données JSON à valider.
   *
   * @param data Les données en format chaîne
   * @return Le JsonNode représentant les données
   */
  private JsonNode parseData(String data) throws JsonProcessingException {
    try {
      return objectMapper.readTree(data);
    } catch (JsonProcessingException e) {
      logger.error("Erreur lors du parsing des données JSON", e);
      throw new ReviewValidationException("Les données JSON sont invalides", e);
    }
  }

  /**
   * Valide les paramètres d'entrée.
   *
   * @param schemaJson Le schéma à valider
   * @param data Les données à valider
   */
  private void validateInputs(String schemaJson, String data) {
    if (schemaJson == null || schemaJson.isBlank()) {
      throw new IllegalArgumentException("Le schéma JSON ne peut pas être null ou vide");
    }
    if (data == null || data.isBlank()) {
      throw new IllegalArgumentException("Les données JSON ne peuvent pas être null ou vides");
    }
  }

  /**
   * Valide un JsonNode contre un schéma.
   *
   * <p>Cette méthode est utile quand les données sont déjà parsées en JsonNode, évitant ainsi un
   * double parsing.
   *
   * @param schemaJson Le schéma JSON Schema
   * @param dataNode Le JsonNode à valider
   * @return {@code true} si valide, {@code false} sinon
   */
  public boolean isValid(String schemaJson, JsonNode dataNode) {
    try {
      Objects.requireNonNull(dataNode, "Le JsonNode ne peut pas être null");

      JsonSchema schema = parseSchema(schemaJson);
      Set<ValidationMessage> errors = schema.validate(dataNode);

      return errors.isEmpty();
    } catch (Exception e) {
      logger.debug("Validation du JsonNode échouée", e);
      return false;
    }
  }

  /**
   * Retourne la version du JSON Schema utilisée.
   *
   * @return La version du schema (Draft-07)
   */
  public String getSchemaVersion() {
    return SCHEMA_VERSION.toString();
  }

  /**
   * Résultat détaillé d'une validation JSON.
   *
   * <p>Cette classe encapsule le résultat d'une validation, incluant le statut et les messages
   * d'erreur détaillés.
   */
  public static class ValidationResult {
    private final boolean valid;
    private final Set<ValidationMessage> validationMessages;
    private final List<String> errorMessages;

    /**
     * Construit un résultat de validation.
     *
     * @param valid Le statut de validation
     * @param validationMessages Les messages de validation
     */
    public ValidationResult(boolean valid, Set<ValidationMessage> validationMessages) {
      this.valid = valid;
      this.validationMessages = new HashSet<>(validationMessages);
      this.errorMessages =
          validationMessages.stream()
              .map(ValidationMessage::getMessage)
              .collect(Collectors.toList());
    }

    /**
     * Indique si la validation a réussi.
     *
     * @return {@code true} si valide, {@code false} sinon
     */
    public boolean isValid() {
      return valid;
    }

    /**
     * Retourne les messages d'erreur de validation.
     *
     * @return Une liste des messages d'erreur (vide si valide)
     */
    public List<String> getErrors() {
      return new ArrayList<>(errorMessages);
    }

    /**
     * Retourne les messages de validation détaillés.
     *
     * @return L'ensemble des messages de validation
     */
    public Set<ValidationMessage> getValidationMessages() {
      return new HashSet<>(validationMessages);
    }

    /**
     * Retourne le nombre d'erreurs.
     *
     * @return Le nombre d'erreurs de validation
     */
    public int getErrorCount() {
      return errorMessages.size();
    }

    /**
     * Retourne une représentation textuelle du résultat.
     *
     * @return Une chaîne décrivant le résultat
     */
    @Override
    public String toString() {
      if (valid) {
        return "ValidationResult[valid=true]";
      }
      return String.format(
          "ValidationResult[valid=false, errors=%d, messages=%s]",
          errorMessages.size(), errorMessages);
    }
  }
}
