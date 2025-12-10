package com.ghiloufi.aicode.llmworker.processor;

import com.ghiloufi.aicode.llmworker.schema.ReviewResultSchema;
import com.ghiloufi.aicode.llmworker.service.CodeReviewAiService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StructuredReviewService implements ReviewService {

  private final CodeReviewAiService aiService;

  public StructuredReviewService(ChatModel chatModel) {
    this.aiService = AiServices.builder(CodeReviewAiService.class).chatModel(chatModel).build();
    log.info("StructuredReviewService initialized with LangChain4j AI Service");
  }

  @Override
  public ReviewResultSchema performReview(String userPrompt) {
    log.debug("Performing structured review, prompt length: {} chars", userPrompt.length());

    final long startTime = System.currentTimeMillis();
    final ReviewResultSchema result = aiService.reviewCode(userPrompt);
    final long duration = System.currentTimeMillis() - startTime;

    logReviewCompletion(result, duration);
    return result;
  }

  @Override
  public ReviewResultSchema performReview(String systemPrompt, String userPrompt) {
    log.debug(
        "Performing structured review with custom prompts, system={} chars, user={} chars",
        systemPrompt.length(),
        userPrompt.length());

    final long startTime = System.currentTimeMillis();
    final ReviewResultSchema result =
        aiService.reviewCodeWithCustomPrompt(systemPrompt, userPrompt);
    final long duration = System.currentTimeMillis() - startTime;

    logReviewCompletion(result, duration);
    return result;
  }

  private void logReviewCompletion(final ReviewResultSchema result, final long duration) {
    log.info(
        "Review completed in {}ms: {} issues, {} notes",
        duration,
        result.issues() != null ? result.issues().size() : 0,
        result.nonBlockingNotes() != null ? result.nonBlockingNotes().size() : 0);
  }
}
