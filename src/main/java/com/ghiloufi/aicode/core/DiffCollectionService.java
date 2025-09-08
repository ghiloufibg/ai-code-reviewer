package com.ghiloufi.aicode.core;

import com.ghiloufi.aicode.domain.DiffAnalysisBundle;
import com.ghiloufi.aicode.domain.UnifiedDiff;
import com.ghiloufi.aicode.github.GithubClient;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Service responsible for collecting unified diffs from various sources and converting them into
 * structured, exploitable format.
 *
 * <p>Supports two main collection modes: - GitHub API: Fetches diffs from Pull Requests via GitHub
 * client - Local Git: Executes git diff commands locally between commits/branches
 */
public class DiffCollectionService {

  private final int contextLines;
  private final UnifiedDiffParser diffParser;

  /**
   * Creates a new diff collection service with specified context lines.
   *
   * @param contextLines number of context lines to include around changes in diffs
   */
  public DiffCollectionService(int contextLines) {
    this.contextLines = contextLines;
    this.diffParser = new UnifiedDiffParser();
  }

  /**
   * Collects diff from a GitHub Pull Request and converts it to a DiffBundle.
   *
   * @param githubClient the GitHub client to use for API calls
   * @param pullRequestNumber the Pull Request number to fetch diff from
   * @return DiffBundle containing both raw and parsed diff data
   * @throws RuntimeException if GitHub API call fails
   */
  public DiffAnalysisBundle collectFromGitHub(GithubClient githubClient, int pullRequestNumber) {
    String rawDiff = fetchDiffFromGitHub(githubClient, pullRequestNumber);
    return createDiffBundle(rawDiff);
  }

  /**
   * Collects diff from local Git repository between two commits/branches.
   *
   * @param baseCommit the base commit or branch to compare from
   * @param headCommit the head commit or branch to compare to
   * @return DiffBundle containing both raw and parsed diff data
   * @throws RuntimeException if git command execution fails
   */
  public DiffAnalysisBundle collectFromLocalGit(String baseCommit, String headCommit) {
    String rawDiff = executeGitDiffCommand(baseCommit, headCommit);
    return createDiffBundle(rawDiff);
  }

  /**
   * Fetches unified diff from GitHub API for the specified Pull Request.
   *
   * @param githubClient the GitHub client to use for API calls
   * @param pullRequestNumber the Pull Request number
   * @return raw unified diff as string
   * @throws RuntimeException if GitHub API call fails
   */
  private String fetchDiffFromGitHub(GithubClient githubClient, int pullRequestNumber) {
    try {
      return githubClient.fetchPrUnifiedDiff(pullRequestNumber, contextLines);
    } catch (Exception e) {
      throw new RuntimeException("Failed to fetch diff from GitHub PR #" + pullRequestNumber, e);
    }
  }

  /**
   * Executes git diff command locally to generate unified diff.
   *
   * @param baseCommit the base commit or branch
   * @param headCommit the head commit or branch
   * @return raw unified diff as string from git command output
   * @throws RuntimeException if git command fails or process execution fails
   */
  private String executeGitDiffCommand(String baseCommit, String headCommit) {
    try {
      Process gitProcess = createGitDiffProcess(baseCommit, headCommit);
      String diffOutput = readProcessOutput(gitProcess);
      waitForProcessCompletion(gitProcess);
      return diffOutput;
    } catch (Exception e) {
      throw new RuntimeException(
          "Git diff command failed for " + baseCommit + ".." + headCommit, e);
    }
  }

  /**
   * Creates and starts a git diff process with appropriate parameters.
   *
   * @param baseCommit the base commit or branch
   * @param headCommit the head commit or branch
   * @return started Process executing the git diff command
   * @throws IOException if process creation fails
   */
  private Process createGitDiffProcess(String baseCommit, String headCommit) throws IOException {
    return new ProcessBuilder("git", "diff", "--unified=" + contextLines, baseCommit, headCommit)
        .redirectErrorStream(true)
        .start();
  }

  /**
   * Reads all output from a process input stream as UTF-8 string.
   *
   * @param process the process to read output from
   * @return process output as UTF-8 string
   * @throws IOException if reading from process fails
   */
  private String readProcessOutput(Process process) throws IOException {
    return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
  }

  /**
   * Waits for process completion and handles interruption gracefully.
   *
   * @param process the process to wait for
   * @throws InterruptedException if waiting is interrupted
   */
  private void waitForProcessCompletion(Process process) throws InterruptedException {
    process.waitFor();
  }

  /**
   * Creates a DiffBundle by parsing raw diff text into structured format.
   *
   * @param rawDiff the raw unified diff text to parse
   * @return DiffBundle containing both raw and parsed diff data
   */
  private DiffAnalysisBundle createDiffBundle(String rawDiff) {
    UnifiedDiff parsedDiff = diffParser.parse(rawDiff);
    return new DiffAnalysisBundle(parsedDiff, rawDiff);
  }
}
