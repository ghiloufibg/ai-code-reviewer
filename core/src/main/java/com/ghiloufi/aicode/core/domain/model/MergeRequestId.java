package com.ghiloufi.aicode.core.domain.model;

public record MergeRequestId(int iid) implements ChangeRequestIdentifier {

  public MergeRequestId {
    if (iid <= 0) {
      throw new IllegalArgumentException("Merge request IID must be positive, got: " + iid);
    }
  }

  @Override
  public SourceProvider getProvider() {
    return SourceProvider.GITLAB;
  }

  @Override
  public int getNumber() {
    return iid;
  }

  @Override
  public String getDisplayName() {
    return "MR !" + iid;
  }
}
