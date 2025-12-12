package com.ghiloufi.aicode.gateway.exception;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("CodeReviewControllerAdvice Tests")
final class CodeReviewControllerAdviceTest {

  private CodeReviewControllerAdvice advice;

  @BeforeEach
  void setUp() {
    advice = new CodeReviewControllerAdvice();
  }

  @Nested
  @DisplayName("IllegalArgumentException Handling")
  final class IllegalArgumentExceptionTests {

    @Test
    @DisplayName("should_return_bad_request_for_illegal_argument")
    void should_return_bad_request_for_illegal_argument() {
      final IllegalArgumentException exception =
          new IllegalArgumentException("Invalid provider: XYZ");

      final ResponseEntity<String> response = advice.handleIllegalArgumentException(exception);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isEqualTo("Invalid provider: XYZ");
    }

    @Test
    @DisplayName("should_handle_empty_message")
    void should_handle_empty_message() {
      final IllegalArgumentException exception = new IllegalArgumentException("");

      final ResponseEntity<String> response = advice.handleIllegalArgumentException(exception);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isEmpty();
    }

    @Test
    @DisplayName("should_handle_null_message")
    void should_handle_null_message() {
      final IllegalArgumentException exception = new IllegalArgumentException((String) null);

      final ResponseEntity<String> response = advice.handleIllegalArgumentException(exception);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNull();
    }
  }

  @Nested
  @DisplayName("ConstraintViolationException Handling")
  final class ConstraintViolationExceptionTests {

    @Test
    @DisplayName("should_return_bad_request_for_constraint_violation")
    void should_return_bad_request_for_constraint_violation() {
      final ConstraintViolation<?> violation =
          new TestConstraintViolation("pullRequestNumber must be positive");
      final ConstraintViolationException exception =
          new ConstraintViolationException("Validation failed", Set.of(violation));

      final ResponseEntity<String> response = advice.handleConstraintViolationException(exception);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).contains("Validation failed");
    }

    @Test
    @DisplayName("should_handle_empty_violations_set")
    void should_handle_empty_violations_set() {
      final ConstraintViolationException exception =
          new ConstraintViolationException("Empty violations", Set.of());

      final ResponseEntity<String> response = advice.handleConstraintViolationException(exception);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isEqualTo("Empty violations");
    }

    @Test
    @DisplayName("should_handle_multiple_violations")
    void should_handle_multiple_violations() {
      final ConstraintViolation<?> violation1 = new TestConstraintViolation("field1 is required");
      final ConstraintViolation<?> violation2 =
          new TestConstraintViolation("field2 must be positive");
      final ConstraintViolationException exception =
          new ConstraintViolationException("Multiple errors", Set.of(violation1, violation2));

      final ResponseEntity<String> response = advice.handleConstraintViolationException(exception);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).contains("Multiple errors");
    }
  }

  private static final class TestConstraintViolation implements ConstraintViolation<Object> {
    private final String message;

    TestConstraintViolation(final String message) {
      this.message = message;
    }

    @Override
    public String getMessage() {
      return message;
    }

    @Override
    public String getMessageTemplate() {
      return message;
    }

    @Override
    public Object getRootBean() {
      return null;
    }

    @Override
    public Class<Object> getRootBeanClass() {
      return Object.class;
    }

    @Override
    public Object getLeafBean() {
      return null;
    }

    @Override
    public Object[] getExecutableParameters() {
      return new Object[0];
    }

    @Override
    public Object getExecutableReturnValue() {
      return null;
    }

    @Override
    public Path getPropertyPath() {
      return null;
    }

    @Override
    public Object getInvalidValue() {
      return null;
    }

    @Override
    public jakarta.validation.metadata.ConstraintDescriptor<?> getConstraintDescriptor() {
      return null;
    }

    @Override
    public <U> U unwrap(final Class<U> type) {
      return null;
    }
  }
}
