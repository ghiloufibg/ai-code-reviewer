package com.ghiloufi.security.adapter.semgrep;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SemgrepExtra(String message, String severity, Map<String, Object> metadata) {}
