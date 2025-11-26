package com.ghiloufi.aicode.core.infrastructure.adapter.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAIStreamResponse(String id, String object, List<Choice> choices) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Choice(int index, Delta delta, @JsonProperty("finish_reason") String finishReason) {

    public Optional<String> extractContent() {
      return Optional.ofNullable(delta).map(Delta::content);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Delta(String role, String content) {}

  public Optional<String> extractFirstContent() {
    return Optional.ofNullable(choices)
        .filter(c -> !c.isEmpty())
        .map(c -> c.get(0))
        .flatMap(Choice::extractContent);
  }

  public boolean hasContent() {
    return extractFirstContent().filter(c -> !c.isEmpty()).isPresent();
  }
}
