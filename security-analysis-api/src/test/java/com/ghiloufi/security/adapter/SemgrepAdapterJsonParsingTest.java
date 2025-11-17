package com.ghiloufi.security.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghiloufi.security.model.SecurityFinding;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Semgrep Adapter JSON Parsing Test")
class SemgrepAdapterJsonParsingTest {

  private SemgrepAdapter semgrepAdapter;

  @BeforeEach
  void setUp() {
    semgrepAdapter = new SemgrepAdapter(new ObjectMapper());
  }

  @Test
  @DisplayName("should_parse_sql_injection_finding_correctly")
  void should_parse_sql_injection_finding_correctly() {
    final String semgrepJson =
        """
        {
          "version": "1.143.1",
          "results": [{
            "check_id": "java.lang.security.audit.formatted-sql-string.formatted-sql-string",
            "path": "UserDAO.java",
            "start": {"line": 9, "col": 24, "offset": 346},
            "end": {"line": 9, "col": 48, "offset": 370},
            "extra": {
              "message": "SQL injection vulnerability detected",
              "severity": "ERROR",
              "metadata": {
                "cwe": ["CWE-89: SQL Injection"],
                "owasp": ["A03:2021 - Injection"]
              }
            }
          }],
          "errors": []
        }
        """;

    final List<SecurityFinding> findings =
        semgrepAdapter.parseSemgrepJson(semgrepJson, "UserDAO.java");

    assertThat(findings).hasSize(1);
    final SecurityFinding finding = findings.get(0);
    assertThat(finding.type())
        .isEqualTo("java.lang.security.audit.formatted-sql-string.formatted-sql-string");
    assertThat(finding.severity()).isEqualTo("HIGH");
    assertThat(finding.line()).isEqualTo(9);
    assertThat(finding.message()).isEqualTo("SQL injection vulnerability detected");
    assertThat(finding.cweId()).startsWith("CWE-89");
    assertThat(finding.owaspCategory()).isEqualTo("A03:2021 - Injection");
  }

  @Test
  @DisplayName("should_parse_weak_crypto_finding_correctly")
  void should_parse_weak_crypto_finding_correctly() {
    final String semgrepJson =
        """
        {
          "version": "1.143.1",
          "results": [{
            "check_id": "java.lang.security.audit.crypto.des-is-deprecated.des-is-deprecated",
            "path": "CryptoService.java",
            "start": {"line": 7, "col": 56, "offset": 211},
            "end": {"line": 7, "col": 61, "offset": 216},
            "extra": {
              "message": "DES is considered deprecated. AES is recommended",
              "severity": "ERROR",
              "metadata": {
                "cwe": ["CWE-327: Use of Broken Cryptography"],
                "owasp": ["A02:2021 - Cryptographic Failures"]
              }
            }
          }],
          "errors": []
        }
        """;

    final List<SecurityFinding> findings =
        semgrepAdapter.parseSemgrepJson(semgrepJson, "CryptoService.java");

    assertThat(findings).hasSize(1);
    final SecurityFinding finding = findings.get(0);
    assertThat(finding.type()).contains("des-is-deprecated");
    assertThat(finding.severity()).isEqualTo("HIGH");
    assertThat(finding.line()).isEqualTo(7);
    assertThat(finding.message()).contains("DES");
  }

  @Test
  @DisplayName("should_map_severity_levels_correctly")
  void should_map_severity_levels_correctly() {
    final String errorSeverity =
        """
        {
          "version": "1.143.1",
          "results": [{
            "check_id": "test.error",
            "path": "Test.java",
            "start": {"line": 1, "col": 1},
            "end": {"line": 1, "col": 1},
            "extra": {
              "message": "Error level",
              "severity": "ERROR",
              "metadata": {}
            }
          }],
          "errors": []
        }
        """;

    final List<SecurityFinding> errorFindings =
        semgrepAdapter.parseSemgrepJson(errorSeverity, "Test.java");
    assertThat(errorFindings.get(0).severity()).isEqualTo("HIGH");

    final String warningSeverity =
        """
        {
          "version": "1.143.1",
          "results": [{
            "check_id": "test.warning",
            "path": "Test.java",
            "start": {"line": 1, "col": 1},
            "end": {"line": 1, "col": 1},
            "extra": {
              "message": "Warning level",
              "severity": "WARNING",
              "metadata": {}
            }
          }],
          "errors": []
        }
        """;

    final List<SecurityFinding> warningFindings =
        semgrepAdapter.parseSemgrepJson(warningSeverity, "Test.java");
    assertThat(warningFindings.get(0).severity()).isEqualTo("MEDIUM");

    final String infoSeverity =
        """
        {
          "version": "1.143.1",
          "results": [{
            "check_id": "test.info",
            "path": "Test.java",
            "start": {"line": 1, "col": 1},
            "end": {"line": 1, "col": 1},
            "extra": {
              "message": "Info level",
              "severity": "INFO",
              "metadata": {}
            }
          }],
          "errors": []
        }
        """;

    final List<SecurityFinding> infoFindings =
        semgrepAdapter.parseSemgrepJson(infoSeverity, "Test.java");
    assertThat(infoFindings.get(0).severity()).isEqualTo("LOW");
  }

  @Test
  @DisplayName("should_handle_multiple_findings")
  void should_handle_multiple_findings() {
    final String multipleFindings =
        """
        {
          "version": "1.143.1",
          "results": [
            {
              "check_id": "finding.one",
              "path": "Test.java",
              "start": {"line": 1, "col": 1},
              "end": {"line": 1, "col": 1},
              "extra": {
                "message": "First finding",
                "severity": "ERROR",
                "metadata": {"cwe": ["CWE-1"], "owasp": ["A01"]}
              }
            },
            {
              "check_id": "finding.two",
              "path": "Test.java",
              "start": {"line": 2, "col": 1},
              "end": {"line": 2, "col": 1},
              "extra": {
                "message": "Second finding",
                "severity": "WARNING",
                "metadata": {"cwe": ["CWE-2"], "owasp": ["A02"]}
              }
            }
          ],
          "errors": []
        }
        """;

    final List<SecurityFinding> findings =
        semgrepAdapter.parseSemgrepJson(multipleFindings, "Test.java");

    assertThat(findings).hasSize(2);
    assertThat(findings.get(0).line()).isEqualTo(1);
    assertThat(findings.get(1).line()).isEqualTo(2);
  }

  @Test
  @DisplayName("should_return_empty_list_for_no_findings")
  void should_return_empty_list_for_no_findings() {
    final String noFindings =
        """
        {
          "version": "1.143.1",
          "results": [],
          "errors": []
        }
        """;

    final List<SecurityFinding> findings = semgrepAdapter.parseSemgrepJson(noFindings, "Test.java");

    assertThat(findings).isEmpty();
  }

  @Test
  @DisplayName("should_handle_invalid_json_gracefully")
  void should_handle_invalid_json_gracefully() {
    final String invalidJson = "{ invalid json }";

    final List<SecurityFinding> findings =
        semgrepAdapter.parseSemgrepJson(invalidJson, "Test.java");

    assertThat(findings).isEmpty();
  }

  @Test
  @DisplayName("should_extract_cwe_from_list")
  void should_extract_cwe_from_list() {
    final String cweList =
        """
        {
          "version": "1.143.1",
          "results": [{
            "check_id": "test.cwe",
            "path": "Test.java",
            "start": {"line": 1, "col": 1},
            "end": {"line": 1, "col": 1},
            "extra": {
              "message": "Test",
              "severity": "ERROR",
              "metadata": {
                "cwe": ["CWE-89: SQL Injection", "CWE-79: XSS"]
              }
            }
          }],
          "errors": []
        }
        """;

    final List<SecurityFinding> findings = semgrepAdapter.parseSemgrepJson(cweList, "Test.java");

    assertThat(findings.get(0).cweId()).isEqualTo("CWE-89: SQL Injection");
  }

  @Test
  @DisplayName("should_extract_owasp_from_list")
  void should_extract_owasp_from_list() {
    final String owaspList =
        """
        {
          "version": "1.143.1",
          "results": [{
            "check_id": "test.owasp",
            "path": "Test.java",
            "start": {"line": 1, "col": 1},
            "end": {"line": 1, "col": 1},
            "extra": {
              "message": "Test",
              "severity": "ERROR",
              "metadata": {
                "owasp": ["A03:2021 - Injection", "A01:2017 - Injection"]
              }
            }
          }],
          "errors": []
        }
        """;

    final List<SecurityFinding> findings = semgrepAdapter.parseSemgrepJson(owaspList, "Test.java");

    assertThat(findings.get(0).owaspCategory()).isEqualTo("A03:2021 - Injection");
  }
}
