package com.ghiloufi.aicode.agentworker.repository;

public record CloneRequest(
    String repositoryUrl, String branch, String targetDirectory, int depth, String authToken) {

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String repositoryUrl;
    private String branch;
    private String targetDirectory;
    private int depth = 1;
    private String authToken;

    public Builder repositoryUrl(String repositoryUrl) {
      this.repositoryUrl = repositoryUrl;
      return this;
    }

    public Builder branch(String branch) {
      this.branch = branch;
      return this;
    }

    public Builder targetDirectory(String targetDirectory) {
      this.targetDirectory = targetDirectory;
      return this;
    }

    public Builder depth(int depth) {
      this.depth = depth;
      return this;
    }

    public Builder authToken(String authToken) {
      this.authToken = authToken;
      return this;
    }

    public CloneRequest build() {
      return new CloneRequest(repositoryUrl, branch, targetDirectory, depth, authToken);
    }
  }
}
