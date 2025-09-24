package com.ghiloufi.aicode.infrastructure.adapter.output.external;

import com.ghiloufi.aicode.application.port.output.ReviewPublishingPort;
import com.ghiloufi.aicode.domain.entity.CodeReview;
import com.ghiloufi.aicode.domain.entity.ReviewResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Infrastructure adapter that publishes review results to the console.
 *
 * <p>This is a simple implementation for development and testing purposes that outputs review
 * results to the console/logs instead of external systems like GitHub.
 */
@Component
@Slf4j
public class ConsoleReviewPublishingAdapter implements ReviewPublishingPort {

  @Override
  public Mono<Void> publishReview(CodeReview review, ReviewResult result) {
    log.info(
        "Publishing review results for repository: {}",
        review.getRepositoryInfo().getDisplayName());

    return Mono.fromRunnable(
            () -> {
              System.out.println("\n" + "=".repeat(80));
              System.out.println("CODE REVIEW RESULTS");
              System.out.println("=".repeat(80));
              System.out.println("Repository: " + review.getRepositoryInfo().getDisplayName());
              System.out.println("Review ID: " + review.getId());
              System.out.println("Status: " + review.getStatus());
              System.out.println("Configuration: " + review.getConfiguration().getDisplayName());

              if (result != null) {
                System.out.println("\nReview Results:");
                System.out.println("- Summary: " + result.getSummary());
                System.out.println("- Quality: " + result.getQuality().getDisplayName());
                System.out.println("- Issue Summary: " + result.getIssueSummary());
                System.out.println("- Passed: " + (result.isPassed() ? "YES" : "NO"));

                if (!result.getIssues().isEmpty()) {
                  System.out.println("\nIssues Found:");
                  result
                      .getIssues()
                      .forEach(
                          issue ->
                              System.out.println(
                                  "  * ["
                                      + issue.severity()
                                      + "] "
                                      + issue.title()
                                      + " (Line: "
                                      + issue.startLine()
                                      + ", File: "
                                      + issue.file()
                                      + ")"));
                }

                if (!result.getNonBlockingNotes().isEmpty()) {
                  System.out.println("\nNotes:");
                  result.getNonBlockingNotes().forEach(note -> System.out.println("- " + note));
                }
              }

              System.out.println("=".repeat(80));
              log.info("Review results published to console successfully");
            })
        .then()
        .doOnError(error -> log.error("Failed to publish review results", error));
  }

  @Override
  public boolean supports(CodeReview review) {
    // This console adapter supports all types of reviews
    return true;
  }
}
