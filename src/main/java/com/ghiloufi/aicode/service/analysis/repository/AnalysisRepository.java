package com.ghiloufi.aicode.service.analysis.repository;

import com.ghiloufi.aicode.api.model.AnalysisStatus;
import com.ghiloufi.aicode.service.analysis.model.AnalysisEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * In-memory repository for storing and managing analysis entities. Thread-safe implementation using
 * ConcurrentHashMap.
 */
@Repository
@Slf4j
public class AnalysisRepository {

  private final ConcurrentMap<String, AnalysisEntity> analyses = new ConcurrentHashMap<>();

  /** Store a new analysis entity */
  public void save(AnalysisEntity analysis) {
    analyses.put(analysis.getId(), analysis);
    log.debug("Stored analysis: {} with status: {}", analysis.getId(), analysis.getStatus());
  }

  /** Find analysis by ID */
  public Optional<AnalysisEntity> findById(String id) {
    return Optional.ofNullable(analyses.get(id));
  }

  /** Update existing analysis */
  public void update(AnalysisEntity analysis) {
    if (analyses.containsKey(analysis.getId())) {
      analyses.put(analysis.getId(), analysis);
      log.debug("Updated analysis: {} with status: {}", analysis.getId(), analysis.getStatus());
    } else {
      log.warn("Attempted to update non-existent analysis: {}", analysis.getId());
    }
  }

  /** Delete analysis by ID */
  public boolean deleteById(String id) {
    AnalysisEntity removed = analyses.remove(id);
    if (removed != null) {
      log.debug("Deleted analysis: {}", id);
      return true;
    }
    return false;
  }

  /** Check if analysis exists */
  public boolean existsById(String id) {
    return analyses.containsKey(id);
  }

  /** Get all analyses (for debugging/monitoring) */
  public List<AnalysisEntity> findAll() {
    return analyses.values().stream()
        .sorted((a1, a2) -> a2.getCreatedAt().compareTo(a1.getCreatedAt()))
        .collect(Collectors.toList());
  }

  /** Find analyses by status */
  public List<AnalysisEntity> findByStatus(AnalysisStatus.StatusEnum status) {
    return analyses.values().stream()
        .filter(analysis -> analysis.getStatus() == status)
        .sorted((a1, a2) -> a2.getCreatedAt().compareTo(a1.getCreatedAt()))
        .collect(Collectors.toList());
  }

  /** Find analyses older than specified minutes (for cleanup) */
  public List<AnalysisEntity> findOlderThan(int minutes) {
    LocalDateTime cutoff = LocalDateTime.now().minusMinutes(minutes);
    return analyses.values().stream()
        .filter(analysis -> analysis.getCreatedAt().isBefore(cutoff))
        .collect(Collectors.toList());
  }

  /** Get current size of stored analyses */
  public int size() {
    return analyses.size();
  }

  /** Clear all analyses (for testing) */
  public void clear() {
    analyses.clear();
    log.debug("Cleared all analyses from repository");
  }

  /** Get statistics about stored analyses */
  public RepositoryStats getStats() {
    if (analyses.isEmpty()) {
      return new RepositoryStats(0, 0, 0, 0, 0, 0);
    }

    long pending =
        analyses.values().stream()
            .mapToLong(a -> a.getStatus() == AnalysisStatus.StatusEnum.PENDING ? 1 : 0)
            .sum();

    long inProgress =
        analyses.values().stream()
            .mapToLong(a -> a.getStatus() == AnalysisStatus.StatusEnum.IN_PROGRESS ? 1 : 0)
            .sum();

    long completed =
        analyses.values().stream()
            .mapToLong(a -> a.getStatus() == AnalysisStatus.StatusEnum.COMPLETED ? 1 : 0)
            .sum();

    long failed =
        analyses.values().stream()
            .mapToLong(a -> a.getStatus() == AnalysisStatus.StatusEnum.FAILED ? 1 : 0)
            .sum();

    long cancelled =
        analyses.values().stream()
            .mapToLong(a -> a.getStatus() == AnalysisStatus.StatusEnum.CANCELLED ? 1 : 0)
            .sum();

    return new RepositoryStats(
        analyses.size(),
        (int) pending,
        (int) inProgress,
        (int) completed,
        (int) failed,
        (int) cancelled);
  }

  /** Statistics about the repository state */
  public record RepositoryStats(
      int total, int pending, int inProgress, int completed, int failed, int cancelled) {}
}
