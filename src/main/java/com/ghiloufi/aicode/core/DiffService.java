package com.ghiloufi.aicode.core;

import com.ghiloufi.aicode.domain.DiffBundle;
import com.ghiloufi.aicode.github.GithubClient;
import com.ghiloufi.aicode.util.UnifiedDiffParser;
import java.nio.charset.StandardCharsets;

public class DiffService {

  private final int context;
  private final UnifiedDiffParser diffParser;

  public DiffService(int contextLines) {
    this.context = contextLines;
    diffParser = new UnifiedDiffParser();
  }

  public DiffBundle collectFromGithub(GithubClient gh, int pr) {
    String diff = gh.fetchPrUnifiedDiff(pr, context);
    return new DiffBundle(diffParser.parse(diff), diff);
  }

  public DiffBundle collectFromGit(String base, String head) {
    try {
      Process p =
          new ProcessBuilder("git", "diff", "--unified=" + context, base, head)
              .redirectErrorStream(true)
              .start();
      String diff = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      p.waitFor();
      return new DiffBundle(diffParser.parse(diff), diff);
    } catch (Exception e) {
      throw new RuntimeException("git diff failed", e);
    }
  }
}
