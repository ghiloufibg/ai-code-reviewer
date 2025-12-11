package com.ghiloufi.aicode.core.application.service.context;

import com.ghiloufi.aicode.core.domain.model.ContextMatch;
import com.ghiloufi.aicode.core.domain.model.DiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.MatchReason;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DiffFileReferenceExtractor {

  private static final Pattern IMPORT_PATTERN =
      Pattern.compile("^\\+\\s*import\\s+([a-zA-Z0-9_.]+);", Pattern.MULTILINE);
  private static final Pattern QUALIFIED_NAME_PATTERN =
      Pattern.compile("([a-zA-Z][a-zA-Z0-9_]*+\\.)+([A-Z][a-zA-Z0-9_]*+)");

  public List<ContextMatch> extractReferences(final DiffAnalysisBundle bundle) {
    if (bundle.rawDiffText() == null || bundle.rawDiffText().isBlank()) {
      return List.of();
    }

    final Set<String> uniqueReferences = new HashSet<>();
    final List<ContextMatch> matches = new ArrayList<>();

    final String[] lines = bundle.rawDiffText().split("\n");
    for (final String line : lines) {
      if (line.startsWith("+")) {
        extractFromLine(line, uniqueReferences, matches);
      }
    }

    return matches;
  }

  private void extractFromLine(
      final String line, final Set<String> uniqueReferences, final List<ContextMatch> matches) {

    final Matcher importMatcher = IMPORT_PATTERN.matcher(line);
    while (importMatcher.find()) {
      final String importPath = importMatcher.group(1);
      if (uniqueReferences.add(importPath)) {
        matches.add(createMatch(importPath, line));
      }
    }

    final Matcher qualifiedMatcher = QUALIFIED_NAME_PATTERN.matcher(line);
    while (qualifiedMatcher.find()) {
      final String qualifiedName = qualifiedMatcher.group(0);
      if (uniqueReferences.add(qualifiedName)) {
        matches.add(createMatch(qualifiedName, line));
      }
    }
  }

  private ContextMatch createMatch(final String reference, final String evidence) {
    final String filePath = convertToFilePath(reference);
    return new ContextMatch(
        filePath,
        MatchReason.FILE_REFERENCE,
        MatchReason.FILE_REFERENCE.getBaseConfidence(),
        evidence.trim());
  }

  private String convertToFilePath(final String reference) {
    return reference.replace('.', '/') + ".java";
  }
}
