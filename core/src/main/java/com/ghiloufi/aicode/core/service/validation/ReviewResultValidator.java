package com.ghiloufi.aicode.core.service.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghiloufi.aicode.core.exception.JsonValidationException;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public final class ReviewResultValidator {

  private final ObjectMapper objectMapper;
  private final JsonSchema schema;

  public ReviewResultValidator(final ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    this.schema = factory.getSchema(ReviewResultSchema.SCHEMA);
  }

  public ValidationResult validate(final String jsonString) {
    final String validJson =
        Optional.ofNullable(jsonString)
            .filter(json -> !json.isBlank())
            .orElseThrow(() -> new IllegalArgumentException("JSON string cannot be null or empty"));

    try {
      final JsonNode jsonNode = objectMapper.readTree(validJson);
      final Set<ValidationMessage> validationMessages = schema.validate(jsonNode);

      if (validationMessages.isEmpty()) {
        return ValidationResult.valid();
      }

      final List<String> errors =
          validationMessages.stream().map(ValidationMessage::getMessage).toList();

      return ValidationResult.invalid(errors);
    } catch (final Exception e) {
      throw new JsonValidationException("Invalid JSON format: " + e.getMessage(), e);
    }
  }
}
