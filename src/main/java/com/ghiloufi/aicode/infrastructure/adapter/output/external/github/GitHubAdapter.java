package com.ghiloufi.aicode.infrastructure.adapter.output.external.github;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Infrastructure adapter for GitHub API integration.
 *
 * <p>This adapter handles communication with GitHub API to retrieve
 * pull request diffs and related information.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GitHubAdapter {

    /**
     * Retrieves diff from GitHub pull request.
     */
    public Mono<String> getDiff(String repository, Integer pullRequestNumber) {
        log.info("Fetching diff from GitHub PR {} in {}", pullRequestNumber, repository);

        // Simplified implementation - in production would use GitHub API client
        return Mono.just(
            "--- a/example.java\n" +
            "+++ b/example.java\n" +
            "@@ -1,1 +1,1 @@\n" +
            "- old line\n" +
            "+ new line\n"
        );
    }
}