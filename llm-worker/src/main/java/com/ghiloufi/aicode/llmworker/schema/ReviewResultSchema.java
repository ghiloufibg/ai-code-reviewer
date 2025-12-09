package com.ghiloufi.aicode.llmworker.schema;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;
import java.util.List;

public record ReviewResultSchema(
    @Description("1-2 sentence overview of the code changes and their quality") String summary,
    @Description("List of blocking issues that require fixes before merging")
        List<IssueSchema> issues,
    @JsonProperty("non_blocking_notes")
        @JsonAlias("nonBlockingNotes")
        @Description("List of non-blocking observations, suggestions, or improvements")
        List<NoteSchema> nonBlockingNotes) {}
