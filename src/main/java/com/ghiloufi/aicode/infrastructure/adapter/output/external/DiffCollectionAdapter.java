package com.ghiloufi.aicode.infrastructure.adapter.output.external;

import com.ghiloufi.aicode.application.port.output.DiffCollectionPort;
import com.ghiloufi.aicode.domain.entity.DiffAnalysis;
import com.ghiloufi.aicode.domain.entity.FileModification;
import com.ghiloufi.aicode.domain.entity.DiffHunk;
import com.ghiloufi.aicode.domain.value.RepositoryInfo;
import com.ghiloufi.aicode.infrastructure.adapter.output.external.github.GitHubAdapter;
import com.ghiloufi.aicode.infrastructure.adapter.output.filesystem.LocalGitAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Infrastructure adapter for collecting diffs from various sources.
 *
 * <p>This adapter coordinates between GitHub API and local Git
 * to collect diff data based on the repository information.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DiffCollectionAdapter implements DiffCollectionPort {

    private final GitHubAdapter gitHubAdapter;
    private final LocalGitAdapter localGitAdapter;

    @Override
    public Mono<DiffAnalysis> collectDiff(RepositoryInfo repositoryInfo) {
        log.info("Collecting diff for {}", repositoryInfo.getDisplayName());

        if (repositoryInfo.isGitHubMode()) {
            return gitHubAdapter.getDiff(repositoryInfo.repository(), repositoryInfo.pullRequestNumber())
                .map(this::parseDiff);
        } else {
            return localGitAdapter.getDiff(repositoryInfo.fromCommit(), repositoryInfo.toCommit())
                .map(this::parseDiff);
        }
    }

    @Override
    public boolean supports(RepositoryInfo repositoryInfo) {
        return repositoryInfo.isGitHubMode() || repositoryInfo.isLocalMode();
    }

    /**
     * Parses raw diff into structured DiffAnalysis.
     * This is a simplified implementation - in reality you'd use the existing UnifiedDiffParser.
     */
    private DiffAnalysis parseDiff(String rawDiff) {
        // Simplified parsing - in production, use the existing UnifiedDiffParser
        // For now, create a minimal structure
        var fileModifications = java.util.List.of(
            new FileModification(
                "example.java",
                "example.java",
                java.util.List.of(
                    new DiffHunk(1, 1, 1, 1, java.util.List.of("- old line", "+ new line"))
                ),
                FileModification.ModificationType.MODIFIED
            )
        );

        return new DiffAnalysis(rawDiff, fileModifications);
    }
}