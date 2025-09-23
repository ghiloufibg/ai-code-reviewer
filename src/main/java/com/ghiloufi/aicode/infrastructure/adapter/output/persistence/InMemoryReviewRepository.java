package com.ghiloufi.aicode.infrastructure.adapter.output.persistence;

import com.ghiloufi.aicode.domain.entity.CodeReview;
import com.ghiloufi.aicode.domain.repository.ReviewRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of ReviewRepository.
 *
 * <p>This is a temporary implementation for demonstration.
 * In production, this would be replaced with a proper
 * database adapter (JPA, MongoDB, etc.).
 */
@Repository
@Slf4j
public class InMemoryReviewRepository implements ReviewRepository {

    private final Map<UUID, CodeReview> reviews = new ConcurrentHashMap<>();

    @Override
    public Mono<CodeReview> save(CodeReview review) {
        return Mono.fromCallable(() -> {
            reviews.put(review.getId(), review);
            log.debug("Saved review: {}", review.getId());
            return review;
        });
    }

    @Override
    public Mono<CodeReview> findById(UUID id) {
        return Mono.fromCallable(() -> reviews.get(id))
            .doOnNext(review -> log.debug("Found review: {}", id))
            .doOnSuccess(review -> {
                if (review == null) {
                    log.debug("Review not found: {}", id);
                }
            });
    }

    @Override
    public Flux<CodeReview> findByRepository(String repository) {
        return Flux.fromIterable(reviews.values())
            .filter(review -> repository.equals(review.getRepositoryInfo().repository()))
            .doOnNext(review -> log.debug("Found review for repository {}: {}", repository, review.getId()));
    }

    @Override
    public Flux<CodeReview> findByStatus(CodeReview.ReviewStatus status) {
        return Flux.fromIterable(reviews.values())
            .filter(review -> status == review.getStatus())
            .doOnNext(review -> log.debug("Found review with status {}: {}", status, review.getId()));
    }

    @Override
    public Mono<Void> delete(UUID id) {
        return Mono.fromRunnable(() -> {
            reviews.remove(id);
            log.debug("Deleted review: {}", id);
        });
    }

    @Override
    public Mono<Boolean> existsById(UUID id) {
        return Mono.fromCallable(() -> reviews.containsKey(id));
    }
}