package com.ghiloufi.aicode.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@DisplayName("TicketAnalysisPromptProperties Configuration Binding Tests")
@SpringBootTest(classes = TicketAnalysisPromptPropertiesTest.TestConfig.class)
final class TicketAnalysisPromptPropertiesTest {

  @Autowired private TicketAnalysisPromptProperties properties;

  @Test
  @DisplayName("should_bind_system_prompt_from_configuration")
  void should_bind_system_prompt_from_configuration() {
    assertThat(properties.getSystem())
        .as("System prompt should be loaded from prompts.ticket-analysis.system")
        .isNotBlank();
  }

  @Test
  @DisplayName("should_contain_role_definition")
  void should_contain_role_definition() {
    assertThat(properties.getSystem()).contains("<role>");
  }

  @Test
  @DisplayName("should_contain_json_schema")
  void should_contain_json_schema() {
    assertThat(properties.getSystem()).contains("<schema>");
    assertThat(properties.getSystem()).contains("objective");
    assertThat(properties.getSystem()).contains("acceptanceCriteria");
    assertThat(properties.getSystem()).contains("businessRules");
    assertThat(properties.getSystem()).contains("technicalNotes");
  }

  @Test
  @DisplayName("should_contain_json_requirements")
  void should_contain_json_requirements() {
    assertThat(properties.getSystem()).contains("<json_requirements>");
    assertThat(properties.getSystem()).contains("MUST be pure JSON");
  }

  @Configuration
  @EnableConfigurationProperties(TicketAnalysisPromptProperties.class)
  static class TestConfig {}
}
