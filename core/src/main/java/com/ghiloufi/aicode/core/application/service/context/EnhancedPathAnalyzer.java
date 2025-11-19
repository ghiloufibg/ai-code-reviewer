package com.ghiloufi.aicode.core.application.service.context;

import com.ghiloufi.aicode.core.domain.model.ContextMatch;
import com.ghiloufi.aicode.core.domain.model.GitFileModification;
import com.ghiloufi.aicode.core.domain.model.MatchReason;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class EnhancedPathAnalyzer {

  private static final Set<String> LAYER_KEYWORDS =
      Set.of(
          "controller",
          "service",
          "repository",
          "dao",
          "model",
          "entity",
          "dto",
          "mapper",
          "adapter",
          "port");

  public List<ContextMatch> analyzePathPatterns(
      final List<GitFileModification> modifiedFiles, final List<String> repositoryFiles) {

    final List<ContextMatch> matches = new ArrayList<>();

    for (final GitFileModification modifiedFile : modifiedFiles) {
      final String modifiedPath = modifiedFile.getEffectivePath();

      for (final String repositoryFile : repositoryFiles) {
        if (repositoryFile.equals(modifiedPath)) {
          continue;
        }

        analyzeFilePair(modifiedPath, repositoryFile, matches);
      }
    }

    return matches;
  }

  private void analyzeFilePair(
      final String modifiedPath, final String repositoryFile, final List<ContextMatch> matches) {

    if (isTestCounterpart(modifiedPath, repositoryFile)) {
      matches.add(createMatch(repositoryFile, MatchReason.TEST_COUNTERPART, modifiedPath));
      return;
    }

    if (isSamePackage(modifiedPath, repositoryFile)) {
      matches.add(createMatch(repositoryFile, MatchReason.SAME_PACKAGE, modifiedPath));
      return;
    }

    if (isRelatedLayer(modifiedPath, repositoryFile)) {
      matches.add(createMatch(repositoryFile, MatchReason.RELATED_LAYER, modifiedPath));
      return;
    }

    if (isParentPackage(modifiedPath, repositoryFile)) {
      matches.add(createMatch(repositoryFile, MatchReason.PARENT_PACKAGE, modifiedPath));
    }
  }

  private boolean isTestCounterpart(final String modifiedFile, final String repositoryFile) {
    final String modifiedBaseName = extractBaseName(modifiedFile);
    final String repositoryBaseName = extractBaseName(repositoryFile);

    final boolean modifiedIsTest =
        modifiedFile.contains("/test/") || modifiedBaseName.endsWith("Test");
    final boolean repositoryIsTest =
        repositoryFile.contains("/test/") || repositoryBaseName.endsWith("Test");

    if (modifiedIsTest && !repositoryIsTest) {
      return modifiedBaseName.replace("Test", "").equals(repositoryBaseName);
    }

    if (!modifiedIsTest && repositoryIsTest) {
      return repositoryBaseName.replace("Test", "").equals(modifiedBaseName);
    }

    return false;
  }

  private boolean isSamePackage(final String file1, final String file2) {
    final Path path1 = Paths.get(file1);
    final Path path2 = Paths.get(file2);

    final Path parent1 = path1.getParent();
    final Path parent2 = path2.getParent();

    return parent1 != null && parent2 != null && parent1.equals(parent2);
  }

  private boolean isRelatedLayer(final String file1, final String file2) {
    final String baseName1 = extractBaseName(file1);
    final String baseName2 = extractBaseName(file2);

    final String layer1 = extractLayer(file1);
    final String layer2 = extractLayer(file2);

    return !layer1.isEmpty()
        && !layer2.isEmpty()
        && !layer1.equals(layer2)
        && hasRelatedBaseName(baseName1, baseName2);
  }

  private boolean isParentPackage(final String file1, final String file2) {
    final Path path1 = Paths.get(file1);
    final Path path2 = Paths.get(file2);

    final Path parent1 = path1.getParent();
    final Path parent2 = path2.getParent();

    if (parent1 == null || parent2 == null) {
      return false;
    }

    return parent1.startsWith(parent2) || parent2.startsWith(parent1);
  }

  private String extractLayer(final String filePath) {
    final String lowerPath = filePath.toLowerCase();
    return LAYER_KEYWORDS.stream().filter(lowerPath::contains).findFirst().orElse("");
  }

  private boolean hasRelatedBaseName(final String name1, final String name2) {
    final String core1 = extractCoreName(name1);
    final String core2 = extractCoreName(name2);

    return core1.equals(core2);
  }

  private String extractCoreName(final String baseName) {
    String core = baseName;

    for (final String keyword : LAYER_KEYWORDS) {
      final String capitalized = capitalize(keyword);
      core = core.replace(capitalized, "");
    }

    return core;
  }

  private String capitalize(final String str) {
    if (str == null || str.isEmpty()) {
      return str;
    }
    return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
  }

  private String extractBaseName(final String filePath) {
    final Path path = Paths.get(filePath);
    final String fileName = path.getFileName().toString();
    final int dotIndex = fileName.lastIndexOf('.');
    return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
  }

  private ContextMatch createMatch(
      final String filePath, final MatchReason reason, final String modifiedFile) {
    final String evidence = String.format("Path pattern match with %s", modifiedFile);
    return new ContextMatch(filePath, reason, reason.getBaseConfidence(), evidence);
  }
}
