package com.ghiloufi.aicode.llmworker.processor;

import com.ghiloufi.aicode.llmworker.schema.ReviewResultSchema;

public interface ReviewService {

  ReviewResultSchema performReview(String userPrompt);
}
