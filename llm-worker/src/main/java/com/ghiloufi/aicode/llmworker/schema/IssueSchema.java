package com.ghiloufi.aicode.llmworker.schema;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Min;

public record IssueSchema(
    @Description("File path where the issue was found") String file,
    @JsonProperty("start_line")
        @JsonAlias("startLine")
        @Description("Line number where the issue starts")
        @Min(1)
        int startLine,
    @Description("Severity level: critical, major, minor, or info") Severity severity,
    @Description("Brief title describing the issue (max 100 chars)") String title,
    @Description("Detailed explanation and suggested fix (max 500 chars)") String suggestion,
    @JsonProperty("confidence_score")
        @JsonAlias("confidenceScore")
        @Description("AI confidence level 0.0-1.0, or null if not applicable")
        @Nullable
        Double confidenceScore,
    @JsonProperty("confidence_explanation")
        @JsonAlias("confidenceExplanation")
        @Description("Explanation for the confidence score")
        @Nullable
        String confidenceExplanation) {}
