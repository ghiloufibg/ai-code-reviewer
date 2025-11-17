package com.ghiloufi.security.adapter.semgrep;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SemgrepResult(
    @JsonProperty("check_id") String checkId,
    SemgrepStart start,
    SemgrepExtra extra,
    String path) {}
