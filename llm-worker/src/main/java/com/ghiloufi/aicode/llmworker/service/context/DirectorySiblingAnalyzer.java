package com.ghiloufi.aicode.llmworker.service.context;

import com.ghiloufi.aicode.core.domain.model.ContextMatch;
import com.ghiloufi.aicode.core.domain.model.GitFileModification;
import com.ghiloufi.aicode.core.domain.model.MatchReason;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class DirectorySiblingAnalyzer {

  private static final double NAME_SIMILARITY_BOOST = 0.05;

  public List<ContextMatch> analyzeSiblings(
      final List<GitFileModification> modifiedFiles, final List<String> repositoryFiles) {

    final List<ContextMatch> matches = new ArrayList<>();

    for (final GitFileModification modifiedFile : modifiedFiles) {
      final String modifiedPath = modifiedFile.getEffectivePath();
      final Path modifiedPathObj = Paths.get(modifiedPath);
      final Path modifiedDirectory = modifiedPathObj.getParent();

      if (modifiedDirectory == null) {
        continue;
      }

      for (final String repositoryFile : repositoryFiles) {
        if (repositoryFile.equals(modifiedPath)) {
          continue;
        }

        final Path repositoryPath = Paths.get(repositoryFile);
        final Path repositoryDirectory = repositoryPath.getParent();

        if (modifiedDirectory.equals(repositoryDirectory)) {
          matches.add(createSiblingMatch(modifiedFile, repositoryFile));
        }
      }
    }

    return matches;
  }

  private ContextMatch createSiblingMatch(
      final GitFileModification modifiedFile, final String siblingFile) {
    final double confidence = calculateConfidence(modifiedFile.getEffectivePath(), siblingFile);
    final String evidence =
        String.format(
            "Sibling of %s in directory %s",
            modifiedFile.getEffectivePath(), getDirectory(siblingFile));

    return new ContextMatch(siblingFile, MatchReason.SIBLING_FILE, confidence, evidence);
  }

  private double calculateConfidence(final String modifiedFile, final String siblingFile) {
    double baseConfidence = MatchReason.SIBLING_FILE.getBaseConfidence();

    if (hasRelatedName(modifiedFile, siblingFile)) {
      baseConfidence += NAME_SIMILARITY_BOOST;
    }

    return Math.min(baseConfidence, 1.0);
  }

  private boolean hasRelatedName(final String file1, final String file2) {
    final String name1 = extractBaseName(file1);
    final String name2 = extractBaseName(file2);

    return name1.toLowerCase().contains(name2.toLowerCase())
        || name2.toLowerCase().contains(name1.toLowerCase());
  }

  private String extractBaseName(final String filePath) {
    final Path path = Paths.get(filePath);
    final Path fileNamePath = path.getFileName();
    if (fileNamePath == null) {
      return "";
    }
    final String fileName = fileNamePath.toString();
    final int dotIndex = fileName.lastIndexOf('.');
    return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
  }

  private String getDirectory(final String filePath) {
    final Path path = Paths.get(filePath);
    final Path parent = path.getParent();
    return parent != null ? parent.toString() : "";
  }
}
