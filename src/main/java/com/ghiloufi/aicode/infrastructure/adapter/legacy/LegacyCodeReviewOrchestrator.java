package com.ghiloufi.aicode.infrastructure.adapter.legacy;

import com.ghiloufi.aicode.application.command.StartReviewCommand;
import com.ghiloufi.aicode.application.port.input.ReviewManagementPort;
import com.ghiloufi.aicode.domain.value.RepositoryInfo;
import com.ghiloufi.aicode.domain.value.ReviewConfiguration;
import com.ghiloufi.aicode.infrastructure.config.ApplicationConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Legacy compatibility layer for the orchestrator.
 *
 * <p>This adapter maintains the same interface as the old CodeReviewOrchestrator while delegating
 * to the new Clean Architecture implementation.
 */
@Service("codeReviewOrchestrator") // Use same bean name as original
@RequiredArgsConstructor
@Slf4j
public class LegacyCodeReviewOrchestrator {

  private final ReviewManagementPort reviewManagementPort;
  private final ApplicationConfig applicationConfig;

  /**
   * Executes code review with legacy ApplicationConfig. Maintains compatibility with existing
   * controllers.
   */
  public Mono<Void> executeCodeReview(ApplicationConfig config) {
    log.info("Executing code review (legacy compatibility mode)");

    // Convert legacy config to new domain objects
    RepositoryInfo repositoryInfo = createRepositoryInfo(config);
    ReviewConfiguration reviewConfiguration = createReviewConfiguration(config);

    StartReviewCommand command = new StartReviewCommand(repositoryInfo, reviewConfiguration);

    // Execute through new architecture
    return reviewManagementPort
        .startReview(command)
        .doOnSuccess(review -> log.info("Review started: {}", review.getId()))
        .then();
  }

  /** Creates RepositoryInfo from legacy config. */
  private RepositoryInfo createRepositoryInfo(ApplicationConfig config) {
    if ("github".equals(config.getMode())) {
      return RepositoryInfo.forGitHubPR(config.getRepository(), config.getPullRequestNumber());
    } else {
      return RepositoryInfo.forLocalCommits(
          config.getRepository(),
          config.getFromCommit(),
          config.getToCommit(),
          config.getDefaultBranch());
    }
  }

  /** Creates ReviewConfiguration from legacy config. */
  private ReviewConfiguration createReviewConfiguration(ApplicationConfig config) {
    return new ReviewConfiguration(
        config.getModel(),
        config.getOllamaHost(),
        config.getTimeoutSeconds(),
        config.getMaxLinesPerChunk(),
        config.getContextLines(),
        config.getJavaVersion(),
        config.getBuildSystem(),
        true, // enable static analysis
        true, // enable LLM analysis
        false // enable security analysis
        );
  }
}
