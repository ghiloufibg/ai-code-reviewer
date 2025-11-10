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
