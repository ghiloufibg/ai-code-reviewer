package com.ghiloufi.aicode.core.service.expansion;

import com.ghiloufi.aicode.core.config.ContextRetrievalConfig;
import com.ghiloufi.aicode.core.domain.model.DiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.DiffExpansionResult;
import com.ghiloufi.aicode.core.domain.model.ExpandedFileContext;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.port.output.SCMPort;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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

    final List<String> filePaths = extractModifiedFilePaths(bundle);
    final int totalFiles = filePaths.size();

    if (filePaths.isEmpty()) {
      return Mono.just(DiffExpansionResult.empty());
    }

    final var expansionConfig = config.diffExpansion();
    final List<String> filesToExpand =
        filePaths.stream()
            .filter(this::shouldIncludeFile)
            .limit(expansionConfig.maxFilesToExpand())
            .toList();

    log.info("Expanding {} of {} modified files", filesToExpand.size(), totalFiles);

    return Flux.fromIterable(filesToExpand)
        .flatMap(path -> fetchFileContent(bundle.repositoryIdentifier(), path))
        .collectList()
        .map(
            expanded ->
                new DiffExpansionResult(
                    expanded,
                    totalFiles,
                    expanded.size(),
                    totalFiles - expanded.size(),
                    totalFiles > expansionConfig.maxFilesToExpand() ? "max files limit" : null));
  }

  private List<String> extractModifiedFilePaths(final DiffAnalysisBundle bundle) {
    return bundle.structuredDiff().files.stream()
        .map(file -> file.newPath)
        .filter(Objects::nonNull)
        .filter(path -> !path.equals("/dev/null"))
        .toList();
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
    final String[] lines = content.split("\n", -1);
    final int lineCount = lines.length;

    if (lineCount > expansionConfig.maxLineCount()) {
      final String truncatedContent =
          Arrays.stream(lines)
              .limit(expansionConfig.maxLineCount())
              .collect(Collectors.joining("\n"));
      return ExpandedFileContext.truncated(filePath, truncatedContent, lineCount);
    }

    return ExpandedFileContext.of(filePath, content);
  }
}
