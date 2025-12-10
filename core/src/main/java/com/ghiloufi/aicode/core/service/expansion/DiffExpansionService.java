package com.ghiloufi.aicode.core.service.expansion;

import com.ghiloufi.aicode.core.config.ContextRetrievalConfig;
import com.ghiloufi.aicode.core.domain.model.DiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.DiffExpansionResult;
import com.ghiloufi.aicode.core.domain.model.ExpandedFileContext;
import com.ghiloufi.aicode.core.domain.model.GitFileModification;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.port.output.SCMPort;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public final class DiffExpansionService {

  private final SCMPort scmPort;
  private final ContextRetrievalConfig config;

  public Mono<DiffExpansionResult> expandDiff(final DiffAnalysisBundle bundle) {
    if (!config.isDiffExpansionEnabled()) {
      log.debug("Diff expansion disabled");
      return Mono.just(DiffExpansionResult.disabled());
    }

    final List<GitFileModification> allFiles = bundle.structuredDiff().files;
    final List<GitFileModification> modifiedFiles = extractFilesNeedingExpansion(allFiles);
    final int newFilesSkipped = countNewFiles(allFiles);

    if (modifiedFiles.isEmpty()) {
      if (newFilesSkipped > 0) {
        log.debug(
            "No files to expand: {} new file(s) skipped (full content already in diff)",
            newFilesSkipped);
      }
      return Mono.just(DiffExpansionResult.empty());
    }

    final var expansionConfig = config.diffExpansion();
    final List<String> filesToExpand =
        modifiedFiles.stream()
            .map(GitFileModification::getEffectivePath)
            .filter(this::shouldIncludeFile)
            .limit(expansionConfig.maxFilesToExpand())
            .toList();

    final int totalCandidates = modifiedFiles.size();
    log.info(
        "Expanding {} of {} modified files ({} new files skipped - full content in diff)",
        filesToExpand.size(),
        totalCandidates,
        newFilesSkipped);

    final int concurrency = Math.min(filesToExpand.size(), expansionConfig.maxFilesToExpand());
    return Flux.fromIterable(filesToExpand)
        .flatMap(path -> fetchFileContent(bundle.repositoryIdentifier(), path), concurrency)
        .collectList()
        .map(
            expanded -> {
              final int totalRequested = totalCandidates + newFilesSkipped;
              final int skipped = totalRequested - expanded.size();
              final String skipReason =
                  buildSkipReason(totalRequested, expansionConfig, newFilesSkipped);
              return new DiffExpansionResult(
                  expanded, totalRequested, expanded.size(), skipped, skipReason);
            });
  }

  private List<GitFileModification> extractFilesNeedingExpansion(
      final List<GitFileModification> files) {
    return files.stream()
        .filter(file -> !file.isNewFile())
        .filter(file -> !file.isDeleted())
        .toList();
  }

  private int countNewFiles(final List<GitFileModification> files) {
    return (int) files.stream().filter(GitFileModification::isNewFile).count();
  }

  private String buildSkipReason(
      final int totalRequested,
      final ContextRetrievalConfig.DiffExpansionConfig expansionConfig,
      final int newFilesSkipped) {
    if (totalRequested > expansionConfig.maxFilesToExpand()) {
      return "max files limit";
    }
    if (newFilesSkipped > 0) {
      return newFilesSkipped + " new file(s) skipped (full content in diff)";
    }
    return null;
  }

  private boolean shouldIncludeFile(final String filePath) {
    final var expansionConfig = config.diffExpansion();
    final int lastDot = filePath.lastIndexOf('.');
    if (lastDot > 0) {
      final String ext = filePath.substring(lastDot);
      if (expansionConfig.excludedExtensions().contains(ext)) {
        log.trace("Excluding file {} due to extension {}", filePath, ext);
        return false;
      }
    }
    return true;
  }

  private Mono<ExpandedFileContext> fetchFileContent(
      final RepositoryIdentifier repo, final String filePath) {
    return scmPort
        .getFileContent(repo, filePath)
        .map(content -> createExpandedContext(filePath, content))
        .onErrorResume(
            error -> {
              log.warn("Failed to fetch file {}: {}", filePath, error.getMessage());
              return Mono.just(ExpandedFileContext.empty(filePath));
            });
  }

  private ExpandedFileContext createExpandedContext(final String filePath, final String content) {
    final var expansionConfig = config.diffExpansion();
    final long lineCount = content.lines().count();

    if (lineCount > expansionConfig.maxLineCount()) {
      final String truncatedContent =
          content.lines().limit(expansionConfig.maxLineCount()).collect(Collectors.joining("\n"));
      return ExpandedFileContext.truncated(filePath, truncatedContent, (int) lineCount);
    }

    return ExpandedFileContext.of(filePath, content);
  }
}
