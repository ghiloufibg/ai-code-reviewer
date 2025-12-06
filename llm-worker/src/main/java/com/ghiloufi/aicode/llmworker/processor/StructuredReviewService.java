package com.ghiloufi.aicode.llmworker.processor;

import com.ghiloufi.aicode.llmworker.schema.ReviewResultSchema;
import com.ghiloufi.aicode.llmworker.service.CodeReviewAiService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StructuredReviewService {

  private final CodeReviewAiService aiService;

  public StructuredReviewService(ChatLanguageModel chatModel) {
    this.aiService =
        AiServices.builder(CodeReviewAiService.class).chatLanguageModel(chatModel).build();
    log.info("StructuredReviewService initialized with LangChain4j AI Service");
  }

  public ReviewResultSchema performReview(String userPrompt) {
    log.debug("Performing structured review, prompt length: {} chars", userPrompt.length());

    long startTime = System.currentTimeMillis();
    ReviewResultSchema result = aiService.reviewCode(userPrompt);
    long duration = System.currentTimeMillis() - startTime;

    log.info(
        "Review completed in {}ms: {} issues, {} notes",
        duration,
        result.issues() != null ? result.issues().size() : 0,
        result.nonBlockingNotes() != null ? result.nonBlockingNotes().size() : 0);

    return result;
  }
}
