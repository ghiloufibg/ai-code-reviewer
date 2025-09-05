package com.ghiloufi.aicode.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public class PromptBuilder {
  public static final String SYSTEM_PROMPT =
      """
            You are a rigorous code review assistant specialized in Java. Review ONLY the changes in this Pull Request and output STRICT JSON.
            Principles:
            - Comment only on changed lines and ±5 lines of context.
            - Prefer minimal, safe patches; avoid restyling unless it prevents defects.
            - If uncertain, use severity=info and explain briefly.
            - Never invent files or lines; ground all comments in provided diff.
            - Classify findings: critical  major  minor  info.
            - Categories: correctness, security, concurrency, performance, maintainability, test gaps.
            - Cite references (CWE/OWASP) when applicable.
            [Policy]
            - Use try-with-resources for IO/DB; avoid resource leaks.
            - Validate untrusted input; never build SQL with string concatenation (use PreparedStatement).
            - Avoid catching generic Exception without context.
            - Avoid mutable static state; ensure thread-safety.
            - Use equals() not '==' for objects; keep equals/hashCode consistent.
            - Don’t log secrets/PII.
            - In Spring, validate request inputs; set safe defaults.
            - Prefer immutable DTOs or defensive copies.
            - Severity guidance:
              - critical: exploitable security bug, data loss, crash under normal use
              - major: likely user-visible bug or strong correctness risk
              - minor: small defect or risky pattern
              - info: suggestion/clarification
            """;
  public static final String OUTPUT_SCHEMA_JSON =
      """
            { "summary":"string (1-3 sentences)", "issues":[ { "file":"string", "start_line":"int", "end_line":"int", "severity":"critical
            major
            minor
            info", "rule_id":"JAVA.<CATEGORY>.<CODE>", "title":"string", "rationale":"string", "suggestion":"string", "references":["string"], "hunk_index":"int" } ], "non_blocking_notes":[{"file":"string","line":"int","note":"string"}] }
            """;

  public String buildUserMessage(
      String repoName,
      String defaultBranch,
      String javaVersion,
      String buildSystem,
      String unifiedDiff,
      Map<String, Object> staticAnalysis,
      Map<String, Object> projectConfig,
      Map<String, Object> testStatus) {
    try {
      ObjectMapper om = new ObjectMapper();
      String sa = om.writerWithDefaultPrettyPrinter().writeValueAsString(staticAnalysis);
      String pc = om.writerWithDefaultPrettyPrinter().writeValueAsString(projectConfig);
      String ts = om.writerWithDefaultPrettyPrinter().writeValueAsString(testStatus);
      return """
                    [REPO]
                    name: %s
                    default_branch: %s
                    java_version: %s
                    build_system: %s
                    [/REPO]
                    [DIFF_UNIFIED]
                    %s
                    [/DIFF_UNIFIED]
                    [STATIC_ANALYSIS]
                    %s
                    [/STATIC_ANALYSIS]
                    [PROJECT_CONFIG]
                    %s
                    [/PROJECT_CONFIG]
                    [TEST_STATUS]
                    %s
                    [/TEST_STATUS]
                    [Few-shot Example]
                    [EXAMPLE_DIFF]
                    --- a/src/Foo.java
                    +++ b/src/Foo.java
                    @@ -10,6 +10,10 @@
                    + Connection c = DriverManager.getConnection(url+user+pass);
                    + Statement s = c.createStatement();
                    + ResultSet rs = s.executeQuery("SELECT * FROM users WHERE name = '" + input + "'");
                    + // no close
                    [/EXAMPLE_DIFF]
                    [EXAMPLE_OUTPUT]
                    {
                     "summary": "High risk SQL injection and resource leak introduced.",
                     "issues": [
                      {
                       "file": "src/Foo.java",
                       "start_line": 11,
                       "end_line": 14,
                       "severity": "critical",
                       "rule_id": "JAVA.SEC.SQL_INJECTION",
                       "title": "SQL injection via string concatenation",
                       "rationale": "User-supplied 'input' is concatenated into SQL.",
                       "suggestion": "Use PreparedStatement with placeholders and try-with-resources.",
                       "references": ["CWE-89","OWASP A03: Injection"],
                       "hunk_index": 0
                      },
                      {
                       "file": "src/Foo.java",
                       "start_line": 11,
                       "end_line": 14,
                       "severity": "major",
                       "rule_id": "JAVA.RES.LEAK",
                       "title": "Missing resource closing",
                       "rationale": "Connection/Statement/ResultSet not closed.",
                       "suggestion": "Wrap in try-with-resources.",
                       "references": ["CWE-772"],
                       "hunk_index": 0
                      }
                     ],
                     "non_blocking_notes": []
                    }
                    [/EXAMPLE_OUTPUT]
                    """
          .formatted(repoName, defaultBranch, javaVersion, buildSystem, unifiedDiff, sa, pc, ts);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
