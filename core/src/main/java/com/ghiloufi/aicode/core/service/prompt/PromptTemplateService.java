package com.ghiloufi.aicode.core.service.prompt;

import com.ghiloufi.aicode.core.config.PromptPropertiesFactory;
import com.ghiloufi.aicode.core.config.PromptPropertiesFactory.PromptPropertiesAdapter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PromptTemplateService {

  @NonNull private final PromptPropertiesFactory factory;

  public String compileSystemPrompt() {
    return getActivePromptProperties().getSystem();
  }

  public String compileFixGenerationInstructions() {
    return getActivePromptProperties().getFixGeneration();
  }

  public String compileConfidenceInstructions() {
    return getActivePromptProperties().getConfidence();
  }

  public String compileSchema() {
    return getActivePromptProperties().getSchema();
  }

  public String compileSchemaReminder() {
    return getActivePromptProperties().getSchemaReminder();
  }

  public String compileOutputRequirements() {
    return getActivePromptProperties().getOutputRequirements();
  }

  private PromptPropertiesAdapter getActivePromptProperties() {
    return factory.getActivePromptProperties();
  }
}
