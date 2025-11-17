package com.ghiloufi.security.model;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SecurityAnalysisRequestTest {

  private Validator validator;

  @BeforeEach
  void setup() {
    final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  void should_pass_validation_with_valid_request() {
    final SecurityAnalysisRequest request =
        new SecurityAnalysisRequest("public class Test {}", "java", "Test.java");

    final Set<ConstraintViolation<SecurityAnalysisRequest>> violations =
        validator.validate(request);

    assertThat(violations).isEmpty();
  }

  @Test
  void should_fail_validation_when_code_is_blank() {
    final SecurityAnalysisRequest request = new SecurityAnalysisRequest("", "java", "Test.java");

    final Set<ConstraintViolation<SecurityAnalysisRequest>> violations =
        validator.validate(request);

    assertThat(violations).isNotEmpty();
    assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("code"));
  }

  @Test
  void should_fail_validation_when_language_is_blank() {
    final SecurityAnalysisRequest request =
        new SecurityAnalysisRequest("public class Test {}", "", "Test.java");

    final Set<ConstraintViolation<SecurityAnalysisRequest>> violations =
        validator.validate(request);

    assertThat(violations).isNotEmpty();
    assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("language"));
  }

  @Test
  void should_fail_validation_when_filename_is_blank() {
    final SecurityAnalysisRequest request =
        new SecurityAnalysisRequest("public class Test {}", "java", "");

    final Set<ConstraintViolation<SecurityAnalysisRequest>> violations =
        validator.validate(request);

    assertThat(violations).isNotEmpty();
    assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("filename"));
  }
}
