package com.ghiloufi.security.adapter.semgrep;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SemgrepStart(int line, int col) {}
