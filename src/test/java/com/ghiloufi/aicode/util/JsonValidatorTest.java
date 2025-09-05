package com.ghiloufi.aicode.util;

import com.ghiloufi.aicode.llm.PromptBuilder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class JsonValidatorTest {
  @Test
  void validateSchema() {
    String schema = PromptBuilder.OUTPUT_SCHEMA_JSON;
    String ok =
"""
{
  "summary":"s",
  "issues":[{"file":"f","start_line":1,"end_line":1,"severity":"info","rule_id":"JAVA.X.Y","title":"t","rationale":"r","suggestion":"s","references":[],"hunk_index":0}],
  "non_blocking_notes":[]
}
""";
    JsonValidator v = new JsonValidator();
    assertTrue(v.isValid(schema, ok));
  }
}
