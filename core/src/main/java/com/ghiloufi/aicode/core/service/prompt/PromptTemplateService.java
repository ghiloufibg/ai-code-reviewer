package com.ghiloufi.aicode.core.service.prompt;

import com.ghiloufi.aicode.core.config.PromptProperties;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PromptTemplateService {

  private final PromptProperties promptProperties;

  public PromptTemplateService(final PromptProperties promptProperties) {
    this.promptProperties =
        Objects.requireNonNull(promptProperties, "PromptProperties cannot be null");
  }

  public String compileSystemPrompt() {
    return promptProperties.getSystem();
  }

  public String compileFixGenerationInstructions() {
    return promptProperties.getFixGeneration();
  }

  public String compileConfidenceInstructions() {
    return promptProperties.getConfidence();
  }

  public String compileSchema() {
    return promptProperties.getSchema();
  }

  public String compileOutputRequirements() {
    return promptProperties.getOutputRequirements();
  }
}
