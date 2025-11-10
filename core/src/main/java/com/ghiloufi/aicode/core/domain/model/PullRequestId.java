package com.ghiloufi.aicode.core.domain.model;

public record PullRequestId(int number) implements ChangeRequestIdentifier {

  public PullRequestId {
    if (number <= 0) {
      throw new IllegalArgumentException("Pull request number must be positive, got: " + number);
    }
  }

  @Override
  public SourceProvider getProvider() {
    return SourceProvider.GITHUB;
  }

  @Override
  public int getNumber() {
    return number;
  }

  @Override
  public String getDisplayName() {
    return "PR #" + number;
  }
}
