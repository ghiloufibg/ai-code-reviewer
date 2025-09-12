package com.ghiloufi.aicode;

import com.ghiloufi.aicode.core.DiffCollectionService;
import com.ghiloufi.aicode.core.GitHubReviewPublisher;
import com.ghiloufi.aicode.core.ReviewResultMerger;
import com.ghiloufi.aicode.domain.DiffAnalysisBundle;
import com.ghiloufi.aicode.domain.ReviewResult;
import com.ghiloufi.aicode.github.GithubClient;
import com.ghiloufi.aicode.llm.LlmClient;
import com.ghiloufi.aicode.llm.LlmReviewValidator;
import com.ghiloufi.aicode.llm.PromptBuilder;
import com.ghiloufi.aicode.sast.StaticAnalysisRunner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application implements CommandLineRunner {
  private static final Logger log = LoggerFactory.getLogger(Application.class);

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @Override
  public void run(String... args) throws Exception {
    Map<String, String> cfg = parseArgs(args);
    String repo = cfg.getOrDefault("--repo", System.getenv().getOrDefault("GITHUB_REPOSITORY", ""));
    int pr =
        Integer.parseInt(cfg.getOrDefault("--pr", System.getenv().getOrDefault("PR_NUMBER", "0")));
    String model =
        cfg.getOrDefault(
            "--model", System.getenv().getOrDefault("MODEL", "qwen2.5-coder:7b-instruct"));
    String ollama =
        cfg.getOrDefault(
            "--ollama", System.getenv().getOrDefault("OLLAMA_HOST", "http://127.0.0.1:11434"));
    String mode = cfg.getOrDefault("--mode", "github");
    int maxLines = Integer.parseInt(cfg.getOrDefault("--max-lines", "1500"));
    int context = Integer.parseInt(cfg.getOrDefault("--context", "5"));
    int timeout = Integer.parseInt(cfg.getOrDefault("--timeout", "45"));
    GithubClient gh = new GithubClient(repo, System.getenv("GITHUB_TOKEN"));
    DiffCollectionService diff = new DiffCollectionService(context);
    StaticAnalysisRunner sast = new StaticAnalysisRunner();
    PromptBuilder pb = new PromptBuilder();
    LlmClient llm = new LlmClient(ollama, model, Duration.ofSeconds(timeout));
    ReviewResultMerger aggr = new ReviewResultMerger();
    GitHubReviewPublisher pub = new GitHubReviewPublisher(gh);
    DiffAnalysisBundle bundle =
        "local".equals(mode)
            ? diff.collectFromLocalGit("HEAD~1", "HEAD")
            : diff.collectFromGitHub(gh, pr);
    var staticReports = sast.runAndCollect();
    var chunks = bundle.splitByMaxLines(maxLines);
    LlmReviewValidator validator = new LlmReviewValidator();
    List<ReviewResult> results = new ArrayList<>();
    int idx = 0;
    for (var chunk : chunks) {
      String user =
          pb.buildUserMessage(
              repo,
              "main",
              "17",
              "maven",
              chunk.toUnifiedString(),
              staticReports,
              bundle.getProjectConfiguration(),
              bundle.getTestStatus());
      String resp = llm.review(PromptBuilder.SYSTEM_PROMPT, user);
      if (!validator.isValid(PromptBuilder.OUTPUT_SCHEMA_JSON, resp)) {
        resp =
            llm.review(
                PromptBuilder.SYSTEM_PROMPT,
                user + " Return ONLY JSON complying with the schema above.");
      }
      ReviewResult rr = ReviewResult.fromJson(resp);
      results.add(rr);
      idx++;
    }
    ReviewResult merged = aggr.merge(results);
    Path out = Path.of("target/artifacts");
    Files.createDirectories(out);
    Files.writeString(out.resolve("review.json"), merged.toJson());
    Files.writeString(out.resolve("prompt.txt"), "(prompt omitted) ");
    Files.writeString(out.resolve("diff.patch"), bundle.getUnifiedDiffString());
    if ("github".equals(mode) && pr > 0) {
      pub.publish(pr, merged, bundle);
    } else {
      System.out.println(merged.toPrettyJson());
    }
  }

  private Map<String, String> parseArgs(String[] args) {
    Map<String, String> m = new HashMap<>();
    String k = null;
    for (String a : args) {
      if (a.startsWith("--")) {
        k = a;
        m.putIfAbsent(k, "");
      } else if (k != null) {
        m.put(k, a);
        k = null;
      }
    }
    return m;
  }
}
