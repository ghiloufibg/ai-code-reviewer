package com.ghiloufi.aicode.core.service.prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ghiloufi.aicode.core.config.OptimizedPromptProperties;
import com.ghiloufi.aicode.core.config.PromptProperties;
import com.ghiloufi.aicode.core.config.PromptPropertiesFactory;
import com.ghiloufi.aicode.core.config.PromptVariantProperties;
import org.junit.jupiter.api.Test;

final class PromptTemplateServiceTest {

  @Test
  void should_throw_exception_when_prompt_properties_factory_is_null() {
    assertThatThrownBy(() -> new PromptTemplateService(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("factory");
  }

  @Test
  void should_return_system_prompt_from_properties() {
    final PromptPropertiesFactory factory = createFactory("system prompt", "", "", "", "");
    final PromptTemplateService service = new PromptTemplateService(factory);

    final String result = service.compileSystemPrompt();

    assertThat(result).isEqualTo("system prompt");
  }

  @Test
  void should_return_fix_generation_instructions_from_properties() {
    final PromptPropertiesFactory factory = createFactory("", "fix instructions", "", "", "");
    final PromptTemplateService service = new PromptTemplateService(factory);

    final String result = service.compileFixGenerationInstructions();

    assertThat(result).isEqualTo("fix instructions");
  }

  @Test
  void should_return_confidence_instructions_from_properties() {
    final PromptPropertiesFactory factory = createFactory("", "", "confidence guide", "", "");
    final PromptTemplateService service = new PromptTemplateService(factory);

    final String result = service.compileConfidenceInstructions();

    assertThat(result).isEqualTo("confidence guide");
  }

  @Test
  void should_return_schema_from_properties() {
    final PromptPropertiesFactory factory = createFactory("", "", "", "json schema", "");
    final PromptTemplateService service = new PromptTemplateService(factory);

    final String result = service.compileSchema();

    assertThat(result).isEqualTo("json schema");
  }

  @Test
  void should_return_output_requirements_from_properties() {
    final PromptPropertiesFactory factory = createFactory("", "", "", "", "output requirements");
    final PromptTemplateService service = new PromptTemplateService(factory);

    final String result = service.compileOutputRequirements();

    assertThat(result).isEqualTo("output requirements");
  }

  @Test
  void should_handle_empty_strings_in_properties() {
    final PromptPropertiesFactory factory = createFactory("", "", "", "", "");
    final PromptTemplateService service = new PromptTemplateService(factory);

    assertThat(service.compileSystemPrompt()).isEmpty();
    assertThat(service.compileFixGenerationInstructions()).isEmpty();
    assertThat(service.compileConfidenceInstructions()).isEmpty();
    assertThat(service.compileSchema()).isEmpty();
    assertThat(service.compileOutputRequirements()).isEmpty();
  }

  @Test
  void should_preserve_multiline_content_from_properties() {
    final String multilineContent = "Line 1\nLine 2\nLine 3";
    final PromptPropertiesFactory factory = createFactory(multilineContent, "", "", "", "");
    final PromptTemplateService service = new PromptTemplateService(factory);

    final String result = service.compileSystemPrompt();

    assertThat(result).isEqualTo(multilineContent);
    assertThat(result).contains("Line 1", "Line 2", "Line 3");
  }

  @Test
  void should_preserve_special_characters_in_properties() {
    final String contentWithSpecialChars = "Check: ✅ ❌ symbols and Unicode: 你好";
    final PromptPropertiesFactory factory = createFactory(contentWithSpecialChars, "", "", "", "");
    final PromptTemplateService service = new PromptTemplateService(factory);

    final String result = service.compileSystemPrompt();

    assertThat(result).isEqualTo(contentWithSpecialChars);
    assertThat(result).contains("✅", "❌", "你好");
  }

  private PromptPropertiesFactory createFactory(
      final String system,
      final String fixGeneration,
      final String confidence,
      final String schema,
      final String outputRequirements) {
    final PromptProperties currentProperties =
        new PromptProperties(system, fixGeneration, confidence, schema, outputRequirements);
    final OptimizedPromptProperties optimizedProperties =
        new OptimizedPromptProperties(
            system, fixGeneration, confidence, schema, outputRequirements);
    final PromptVariantProperties variantProperties =
        new PromptVariantProperties(PromptVariantProperties.Variant.CURRENT);
    return new PromptPropertiesFactory(variantProperties, currentProperties, optimizedProperties);
  }
}
