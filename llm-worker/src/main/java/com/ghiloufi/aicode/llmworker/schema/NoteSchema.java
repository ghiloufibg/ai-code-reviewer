package com.ghiloufi.aicode.llmworker.schema;

import dev.langchain4j.model.output.structured.Description;
import jakarta.validation.constraints.Min;

public record NoteSchema(
    @Description("File path where the observation was found") String file,
    @Description("Line number") @Min(1) int line,
    @Description("Non-blocking observation or suggestion") String note) {}
