package com.ghiloufi.aicode.core.service.prompt;

public record ReviewPromptResult(String systemPrompt, String userPrompt) {

  public int totalLength() {
    return systemPrompt.length() + userPrompt.length();
  }

  public boolean hasSystemPrompt() {
    return systemPrompt != null && !systemPrompt.isBlank();
  }

  public boolean hasUserPrompt() {
    return userPrompt != null && !userPrompt.isBlank();
  }
}
