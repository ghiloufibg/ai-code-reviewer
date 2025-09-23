package com.ghiloufi.aicode.domain.repository;

import com.ghiloufi.aicode.domain.value.ReviewConfiguration;
import reactor.core.publisher.Mono;

/**
 * Repository interface for ReviewConfiguration.
 *
 * <p>Manages persistence of review configurations for different repositories or teams.
 */
public interface ConfigurationRepository {

  /** Gets the configuration for a repository. */
  Mono<ReviewConfiguration> findByRepository(String repository);

  /** Saves configuration for a repository. */
  Mono<ReviewConfiguration> save(String repository, ReviewConfiguration configuration);

  /** Gets the default configuration. */
  Mono<ReviewConfiguration> getDefault();

  /** Deletes configuration for a repository. */
  Mono<Void> delete(String repository);
}
