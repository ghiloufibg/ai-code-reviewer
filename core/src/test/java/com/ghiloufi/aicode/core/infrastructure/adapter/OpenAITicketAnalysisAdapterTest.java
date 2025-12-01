package com.ghiloufi.aicode.core.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghiloufi.aicode.core.config.TicketAnalysisPromptProperties;
import com.ghiloufi.aicode.core.domain.model.TicketBusinessContext;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

final class OpenAITicketAnalysisAdapterTest {

  private MockWebServer mockServer;
  private OpenAITicketAnalysisAdapter adapter;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() throws Exception {
    mockServer = new MockWebServer();
    mockServer.start();
    final TicketAnalysisPromptProperties promptProperties =
        new TicketAnalysisPromptProperties("Analyze this ticket and extract structured data");
    adapter =
        new OpenAITicketAnalysisAdapter(
            mockServer.url("/v1").toString(),
            "gpt-4o-mini",
            "test-api-key",
            objectMapper,
            promptProperties);
  }

  @AfterEach
  void tearDown() throws Exception {
    mockServer.shutdown();
  }

  @Nested
  final class AnalyzeTicketTests {

    @Test
    void should_parse_complete_analysis_response() {
      final String jsonResponse =
          """
          {"objective":"Implement user login","acceptanceCriteria":["Users can login","Sessions persist"],"businessRules":["Max 3 attempts"],"technicalNotes":["Use JWT"]}""";
      final String sseResponse = createSseResponse(jsonResponse);

      mockServer.enqueue(
          new MockResponse().setHeader("Content-Type", "text/event-stream").setBody(sseResponse));

      final TicketBusinessContext rawContext =
          new TicketBusinessContext("TM-123", "Add login", "Implement login functionality");

      StepVerifier.create(adapter.analyzeTicket(rawContext))
          .assertNext(
              analysis -> {
                assertThat(analysis.ticketId()).isEqualTo("TM-123");
                assertThat(analysis.title()).isEqualTo("Add login");
                assertThat(analysis.objective()).isEqualTo("Implement user login");
                assertThat(analysis.acceptanceCriteria())
                    .containsExactly("Users can login", "Sessions persist");
                assertThat(analysis.businessRules()).containsExactly("Max 3 attempts");
                assertThat(analysis.technicalNotes()).containsExactly("Use JWT");
              })
          .verifyComplete();
    }

    @Test
    void should_handle_partial_analysis_response() {
      final String jsonResponse =
          """
          {"objective":"Fix bug","acceptanceCriteria":[],"businessRules":[],"technicalNotes":[]}""";
      final String sseResponse = createSseResponse(jsonResponse);

      mockServer.enqueue(
          new MockResponse().setHeader("Content-Type", "text/event-stream").setBody(sseResponse));

      final TicketBusinessContext rawContext =
          new TicketBusinessContext("TM-456", "Bug fix", "Fix the login issue");

      StepVerifier.create(adapter.analyzeTicket(rawContext))
          .assertNext(
              analysis -> {
                assertThat(analysis.objective()).isEqualTo("Fix bug");
                assertThat(analysis.acceptanceCriteria()).isEmpty();
                assertThat(analysis.businessRules()).isEmpty();
                assertThat(analysis.technicalNotes()).isEmpty();
              })
          .verifyComplete();
    }

    @Test
    void should_return_empty_analysis_when_ticket_is_empty() {
      final TicketBusinessContext emptyContext = TicketBusinessContext.empty();

      StepVerifier.create(adapter.analyzeTicket(emptyContext))
          .assertNext(
              analysis -> {
                assertThat(analysis.isEmpty()).isTrue();
                assertThat(analysis.objective()).isEmpty();
              })
          .verifyComplete();
    }

    @Test
    void should_return_empty_analysis_when_ticket_has_no_description() {
      final TicketBusinessContext noDescContext =
          new TicketBusinessContext("TM-789", "Title only", null);

      StepVerifier.create(adapter.analyzeTicket(noDescContext))
          .assertNext(
              analysis -> {
                assertThat(analysis.ticketId()).isEqualTo("TM-789");
                assertThat(analysis.objective()).isEmpty();
              })
          .verifyComplete();
    }

    @Test
    void should_return_empty_analysis_on_malformed_json_response() {
      final String sseResponse = createSseResponse("not-valid-json");

      mockServer.enqueue(
          new MockResponse().setHeader("Content-Type", "text/event-stream").setBody(sseResponse));

      final TicketBusinessContext rawContext =
          new TicketBusinessContext("TM-123", "Feature", "Description");

      StepVerifier.create(adapter.analyzeTicket(rawContext))
          .assertNext(
              analysis -> {
                assertThat(analysis.ticketId()).isEqualTo("TM-123");
                assertThat(analysis.objective()).isEmpty();
              })
          .verifyComplete();
    }

    @Test
    void should_return_empty_analysis_on_server_error() {
      mockServer.enqueue(new MockResponse().setResponseCode(500));

      final TicketBusinessContext rawContext =
          new TicketBusinessContext("TM-123", "Feature", "Description");

      StepVerifier.create(adapter.analyzeTicket(rawContext))
          .assertNext(
              analysis -> {
                assertThat(analysis.ticketId()).isEqualTo("TM-123");
                assertThat(analysis.objective()).isEmpty();
              })
          .verifyComplete();
    }

    @Test
    void should_concatenate_streaming_chunks() {
      final String sseResponse =
          """
          data: {"id":"1","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"{\\"objective\\":\\""},"finish_reason":null}]}

          data: {"id":"1","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"Test objective"},"finish_reason":null}]}

          data: {"id":"1","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"\\",\\"acceptanceCriteria\\":[],\\"businessRules\\":[],\\"technicalNotes\\":[]}"},"finish_reason":null}]}

          data: [DONE]

          """;

      mockServer.enqueue(
          new MockResponse().setHeader("Content-Type", "text/event-stream").setBody(sseResponse));

      final TicketBusinessContext rawContext =
          new TicketBusinessContext("TM-123", "Feature", "Description");

      StepVerifier.create(adapter.analyzeTicket(rawContext))
          .assertNext(analysis -> assertThat(analysis.objective()).isEqualTo("Test objective"))
          .verifyComplete();
    }
  }

  @Nested
  final class RequestFormattingTests {

    @Test
    void should_send_correct_request_format() throws Exception {
      final String sseResponse = "data: [DONE]\n\n";
      mockServer.enqueue(
          new MockResponse().setHeader("Content-Type", "text/event-stream").setBody(sseResponse));

      final TicketBusinessContext rawContext =
          new TicketBusinessContext("TM-123", "Test Title", "Test Description");

      adapter.analyzeTicket(rawContext).block();

      final RecordedRequest request = mockServer.takeRequest();
      final String body = request.getBody().readUtf8();

      assertThat(body).contains("\"model\":\"gpt-4o-mini\"");
      assertThat(body).contains("\"stream\":true");
      assertThat(body).contains("\"temperature\":0.2");
      assertThat(body).contains("\"messages\":");
      assertThat(body).contains("TICKET ID: TM-123");
      assertThat(body).contains("TITLE: Test Title");
      assertThat(body).contains("Test Description");
    }

    @Test
    void should_include_system_prompt_from_properties() throws Exception {
      final String sseResponse = "data: [DONE]\n\n";
      mockServer.enqueue(
          new MockResponse().setHeader("Content-Type", "text/event-stream").setBody(sseResponse));

      final TicketBusinessContext rawContext =
          new TicketBusinessContext("TM-123", "Title", "Description");

      adapter.analyzeTicket(rawContext).block();

      final RecordedRequest request = mockServer.takeRequest();
      final String body = request.getBody().readUtf8();

      assertThat(body).contains("Analyze this ticket and extract structured data");
    }

    @Test
    void should_send_authorization_header() throws Exception {
      final String sseResponse = "data: [DONE]\n\n";
      mockServer.enqueue(
          new MockResponse().setHeader("Content-Type", "text/event-stream").setBody(sseResponse));

      final TicketBusinessContext rawContext =
          new TicketBusinessContext("TM-123", "Title", "Description");

      adapter.analyzeTicket(rawContext).block();

      final RecordedRequest request = mockServer.takeRequest();
      assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test-api-key");
    }
  }

  @Nested
  final class ProviderInfoTests {

    @Test
    void should_return_provider_name() {
      assertThat(adapter.getProviderName()).isEqualTo("openai");
    }

    @Test
    void should_return_model_name() {
      assertThat(adapter.getModelName()).isEqualTo("gpt-4o-mini");
    }
  }

  private String createSseResponse(final String content) {
    final StringBuilder response = new StringBuilder();
    for (final char c : content.toCharArray()) {
      final String escaped =
          switch (c) {
            case '"' -> "\\\"";
            case '\\' -> "\\\\";
            case '\n' -> "\\n";
            default -> String.valueOf(c);
          };
      response
          .append(
              "data: {\"id\":\"1\",\"object\":\"chat.completion.chunk\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"")
          .append(escaped)
          .append("\"},\"finish_reason\":null}]}\n\n");
    }
    response.append("data: [DONE]\n\n");
    return response.toString();
  }
}
