package com.ghiloufi.aicode.gateway.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.infrastructure.observability.CorrelationIdHolder;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("CorrelationIdWebFilter Tests")
final class CorrelationIdWebFilterTest {

  private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
  private static final Pattern UUID_PATTERN =
      Pattern.compile("^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$");

  private CorrelationIdWebFilter filter;

  @BeforeEach
  void setUp() {
    filter = new CorrelationIdWebFilter();
  }

  @Nested
  @DisplayName("Correlation ID Extraction")
  final class CorrelationIdExtractionTests {

    @Test
    @DisplayName("should_use_provided_correlation_id_from_header")
    void should_use_provided_correlation_id_from_header() {
      final String providedCorrelationId = "test-correlation-id-12345";
      final MockServerHttpRequest request =
          MockServerHttpRequest.get("/api/v1/test")
              .header(CORRELATION_ID_HEADER, providedCorrelationId)
              .build();
      final MockServerWebExchange exchange = MockServerWebExchange.from(request);
      final TestWebFilterChain chain = new TestWebFilterChain();

      StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

      assertThat(exchange.getResponse().getHeaders().getFirst(CORRELATION_ID_HEADER))
          .isEqualTo(providedCorrelationId);
    }

    @Test
    @DisplayName("should_generate_uuid_when_header_missing")
    void should_generate_uuid_when_header_missing() {
      final MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/test").build();
      final MockServerWebExchange exchange = MockServerWebExchange.from(request);
      final TestWebFilterChain chain = new TestWebFilterChain();

      StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

      final String generatedId =
          exchange.getResponse().getHeaders().getFirst(CORRELATION_ID_HEADER);
      assertThat(generatedId).isNotNull();
      assertThat(generatedId).matches(UUID_PATTERN);
    }

    @Test
    @DisplayName("should_generate_uuid_when_header_blank")
    void should_generate_uuid_when_header_blank() {
      final MockServerHttpRequest request =
          MockServerHttpRequest.get("/api/v1/test").header(CORRELATION_ID_HEADER, "   ").build();
      final MockServerWebExchange exchange = MockServerWebExchange.from(request);
      final TestWebFilterChain chain = new TestWebFilterChain();

      StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

      final String generatedId =
          exchange.getResponse().getHeaders().getFirst(CORRELATION_ID_HEADER);
      assertThat(generatedId).isNotNull();
      assertThat(generatedId).matches(UUID_PATTERN);
    }

    @Test
    @DisplayName("should_generate_uuid_when_header_empty")
    void should_generate_uuid_when_header_empty() {
      final MockServerHttpRequest request =
          MockServerHttpRequest.get("/api/v1/test").header(CORRELATION_ID_HEADER, "").build();
      final MockServerWebExchange exchange = MockServerWebExchange.from(request);
      final TestWebFilterChain chain = new TestWebFilterChain();

      StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

      final String generatedId =
          exchange.getResponse().getHeaders().getFirst(CORRELATION_ID_HEADER);
      assertThat(generatedId).isNotNull();
      assertThat(generatedId).matches(UUID_PATTERN);
    }
  }

  @Nested
  @DisplayName("Response Header Propagation")
  final class ResponseHeaderPropagationTests {

    @Test
    @DisplayName("should_add_correlation_id_to_response_headers")
    void should_add_correlation_id_to_response_headers() {
      final String correlationId = "response-header-test-id";
      final MockServerHttpRequest request =
          MockServerHttpRequest.get("/api/v1/test")
              .header(CORRELATION_ID_HEADER, correlationId)
              .build();
      final MockServerWebExchange exchange = MockServerWebExchange.from(request);
      final TestWebFilterChain chain = new TestWebFilterChain();

      StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

      assertThat(exchange.getResponse().getHeaders().containsKey(CORRELATION_ID_HEADER)).isTrue();
      assertThat(exchange.getResponse().getHeaders().getFirst(CORRELATION_ID_HEADER))
          .isEqualTo(correlationId);
    }
  }

  @Nested
  @DisplayName("Reactor Context Propagation")
  final class ReactorContextPropagationTests {

    @Test
    @DisplayName("should_propagate_correlation_id_to_reactor_context")
    void should_propagate_correlation_id_to_reactor_context() {
      final String correlationId = "context-propagation-id";
      final MockServerHttpRequest request =
          MockServerHttpRequest.get("/api/v1/test")
              .header(CORRELATION_ID_HEADER, correlationId)
              .build();
      final MockServerWebExchange exchange = MockServerWebExchange.from(request);

      final AtomicReference<String> capturedCorrelationId = new AtomicReference<>();

      final WebFilterChain chain =
          ex ->
              Mono.deferContextual(
                  ctx -> {
                    capturedCorrelationId.set(
                        CorrelationIdHolder.getCorrelationId(ctx).orElse(null));
                    return Mono.empty();
                  });

      StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

      assertThat(capturedCorrelationId.get()).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("should_propagate_generated_correlation_id_to_reactor_context")
    void should_propagate_generated_correlation_id_to_reactor_context() {
      final MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/test").build();
      final MockServerWebExchange exchange = MockServerWebExchange.from(request);

      final AtomicReference<String> capturedCorrelationId = new AtomicReference<>();

      final WebFilterChain chain =
          ex ->
              Mono.deferContextual(
                  ctx -> {
                    capturedCorrelationId.set(
                        CorrelationIdHolder.getCorrelationId(ctx).orElse(null));
                    return Mono.empty();
                  });

      StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

      assertThat(capturedCorrelationId.get()).isNotNull();
      assertThat(capturedCorrelationId.get()).matches(UUID_PATTERN);
    }
  }

  @Nested
  @DisplayName("Filter Chain Invocation")
  final class FilterChainInvocationTests {

    @Test
    @DisplayName("should_invoke_filter_chain")
    void should_invoke_filter_chain() {
      final MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/test").build();
      final MockServerWebExchange exchange = MockServerWebExchange.from(request);
      final TestWebFilterChain chain = new TestWebFilterChain();

      StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

      assertThat(chain.wasFilterCalled()).isTrue();
    }

    @Test
    @DisplayName("should_work_with_all_http_methods")
    void should_work_with_all_http_methods() {
      final TestWebFilterChain chain = new TestWebFilterChain();

      final MockServerHttpRequest postRequest = MockServerHttpRequest.post("/api/v1/test").build();
      final MockServerWebExchange postExchange = MockServerWebExchange.from(postRequest);
      StepVerifier.create(filter.filter(postExchange, chain)).verifyComplete();
      assertThat(postExchange.getResponse().getHeaders().containsKey(CORRELATION_ID_HEADER))
          .isTrue();

      final MockServerHttpRequest putRequest = MockServerHttpRequest.put("/api/v1/test").build();
      final MockServerWebExchange putExchange = MockServerWebExchange.from(putRequest);
      StepVerifier.create(filter.filter(putExchange, chain)).verifyComplete();
      assertThat(putExchange.getResponse().getHeaders().containsKey(CORRELATION_ID_HEADER))
          .isTrue();

      final MockServerHttpRequest deleteRequest =
          MockServerHttpRequest.delete("/api/v1/test").build();
      final MockServerWebExchange deleteExchange = MockServerWebExchange.from(deleteRequest);
      StepVerifier.create(filter.filter(deleteExchange, chain)).verifyComplete();
      assertThat(deleteExchange.getResponse().getHeaders().containsKey(CORRELATION_ID_HEADER))
          .isTrue();
    }
  }

  private static final class TestWebFilterChain implements WebFilterChain {
    private boolean filterCalled = false;

    @Override
    public Mono<Void> filter(final org.springframework.web.server.ServerWebExchange exchange) {
      filterCalled = true;
      return Mono.empty();
    }

    boolean wasFilterCalled() {
      return filterCalled;
    }
  }
}
