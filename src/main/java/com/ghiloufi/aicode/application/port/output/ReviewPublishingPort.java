package com.ghiloufi.aicode.application.port.output;

import com.ghiloufi.aicode.domain.entity.CodeReview;
import com.ghiloufi.aicode.domain.entity.ReviewResult;
import reactor.core.publisher.Mono;

/**
 * Output port for publishing review results.
 *
 * <p>Abstracts the mechanism of publishing review results to various destinations (GitHub PR,
 * console, etc.).
 */
public interface ReviewPublishingPort {

  /** Publishes review results. */
  Mono<Void> publishReview(CodeReview review, ReviewResult result);

  /** Checks if publishing is supported for the given review. */
  boolean supports(CodeReview review);
}
