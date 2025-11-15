package com.ghiloufi.aicode.core.service.validation;

public final class ReviewResultSchema {

  private ReviewResultSchema() {}

  public static final String SCHEMA =
      """
      {
        "$schema": "http://json-schema.org/draft-07/schema#",
        "type": "object",
        "required": ["summary", "issues", "non_blocking_notes"],
        "properties": {
          "summary": {
            "type": "string",
            "description": "Overall summary of the code review"
          },
          "issues": {
            "type": "array",
            "description": "List of blocking issues found in the code",
            "items": {
              "type": "object",
              "required": ["file", "start_line", "severity", "title", "suggestion"],
              "properties": {
                "file": {
                  "type": "string",
                  "description": "File path where the issue was found"
                },
                "start_line": {
                  "type": "integer",
                  "minimum": 1,
                  "description": "Line number where the issue starts"
                },
                "severity": {
                  "type": "string",
                  "enum": ["critical", "major", "minor", "info"],
                  "description": "Severity level of the issue"
                },
                "title": {
                  "type": "string",
                  "description": "Brief title describing the issue"
                },
                "suggestion": {
                  "type": "string",
                  "description": "Suggested fix or improvement"
                },
                "confidenceScore": {
                  "type": "number",
                  "minimum": 0.0,
                  "maximum": 1.0,
                  "description": "AI confidence level (0.0-1.0) indicating certainty about the issue"
                },
                "confidenceExplanation": {
                  "type": "string",
                  "description": "Brief explanation of the confidence score considering pattern clarity, context completeness, and false positive risk"
                },
                "suggestedFix": {
                  "type": "string",
                  "description": "Base64-encoded markdown diff block showing the fix. The LLM should: 1) Create markdown diff with ```diff markers, 2) Base64-encode the entire diff, 3) Put the Base64 string here. Only provide for high-confidence issues (score >= 0.7) where an automated fix is safe and clear."
                }
              },
              "additionalProperties": false
            }
          },
          "non_blocking_notes": {
            "type": "array",
            "description": "List of non-blocking observations or suggestions",
            "items": {
              "type": "object",
              "required": ["file", "line", "note"],
              "properties": {
                "file": {
                  "type": "string",
                  "description": "File path where the note applies"
                },
                "line": {
                  "type": "integer",
                  "minimum": 1,
                  "description": "Line number for the note"
                },
                "note": {
                  "type": "string",
                  "description": "The observation or suggestion"
                }
              },
              "additionalProperties": false
            }
          }
        },
        "additionalProperties": false
      }
      """;
}
