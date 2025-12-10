package com.ghiloufi.aicode.llmworker.service;

import com.ghiloufi.aicode.llmworker.schema.ReviewResultSchema;

public record AsyncReviewResult(ReviewResultSchema schema, int filesAnalyzed) {}
