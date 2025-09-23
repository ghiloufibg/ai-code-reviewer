package com.ghiloufi.aicode.config;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/** Test configuration providing mock LangChain4j beans for testing. */
@TestConfiguration
@Profile("test")
public class TestLangChainConfig {

  @Bean
  @Primary
  public ChatLanguageModel testChatLanguageModel() {
    return new ChatLanguageModel() {
      @Override
      public Response<AiMessage> generate(List<ChatMessage> messages) {
        // Return a mock JSON response for testing
        String mockResponse =
            """
                    {
                        "summary": "Test review completed successfully",
                        "issues": [],
                        "non_blocking_notes": []
                    }
                    """;
        AiMessage message = AiMessage.from(mockResponse);
        return Response.from(message);
      }
    };
  }

  @Bean
  @Primary
  public StreamingChatLanguageModel testStreamingChatLanguageModel() {
    return new StreamingChatLanguageModel() {
      @Override
      public void generate(
          List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {
        // Simulate streaming response
        String mockResponse =
            """
                    {
                        "summary": "Test streaming review completed successfully",
                        "issues": [],
                        "non_blocking_notes": []
                    }
                    """;

        // Simulate chunked response
        String[] chunks = mockResponse.split("(?<=\\n)");
        for (String chunk : chunks) {
          if (!chunk.trim().isEmpty()) {
            handler.onNext(chunk);
          }
        }

        AiMessage finalMessage = AiMessage.from(mockResponse);
        handler.onComplete(Response.from(finalMessage));
      }
    };
  }
}
