package com.ghiloufi.aicode.application.port.output;

import com.ghiloufi.aicode.domain.entity.DiffAnalysis;
import com.ghiloufi.aicode.domain.value.RepositoryInfo;
import reactor.core.publisher.Mono;

/**
 * Output port for collecting diff data from various sources.
 *
 * <p>Abstracts the mechanism of retrieving code differences,
 * whether from GitHub API, local Git, or other sources.
 */
public interface DiffCollectionPort {

    /**
     * Collects diff analysis from the specified repository information.
     */
    Mono<DiffAnalysis> collectDiff(RepositoryInfo repositoryInfo);

    /**
     * Checks if diff collection is supported for the given repository info.
     */
    boolean supports(RepositoryInfo repositoryInfo);
}