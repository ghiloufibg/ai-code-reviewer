package com.ghiloufi.aicode.gateway.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ObservabilityConfiguration Tests")
final class ObservabilityConfigurationTest {

  @Test
  @DisplayName("should_register_mdc_context_lifter_without_error")
  void should_register_mdc_context_lifter_without_error() {
    final ObservabilityConfiguration config = new ObservabilityConfiguration();

    assertThatCode(config::registerMdcContextLifter).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("should_unregister_mdc_context_lifter_without_error")
  void should_unregister_mdc_context_lifter_without_error() {
    final ObservabilityConfiguration config = new ObservabilityConfiguration();
    config.registerMdcContextLifter();

    assertThatCode(config::unregisterMdcContextLifter).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("should_handle_multiple_registrations")
  void should_handle_multiple_registrations() {
    final ObservabilityConfiguration config = new ObservabilityConfiguration();

    assertThatCode(
            () -> {
              config.registerMdcContextLifter();
              config.registerMdcContextLifter();
            })
        .doesNotThrowAnyException();

    config.unregisterMdcContextLifter();
  }

  @Test
  @DisplayName("should_handle_unregister_without_register")
  void should_handle_unregister_without_register() {
    final ObservabilityConfiguration config = new ObservabilityConfiguration();

    assertThatCode(config::unregisterMdcContextLifter).doesNotThrowAnyException();
  }
}
