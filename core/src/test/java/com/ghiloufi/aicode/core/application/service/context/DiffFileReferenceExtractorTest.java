package com.ghiloufi.aicode.core.application.service.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.ContextMatch;
import com.ghiloufi.aicode.core.domain.model.DiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.GitDiffDocument;
import com.ghiloufi.aicode.core.domain.model.GitFileModification;
import com.ghiloufi.aicode.core.domain.model.MatchReason;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DiffFileReferenceExtractor Tests")
final class DiffFileReferenceExtractorTest {

  private DiffFileReferenceExtractor extractor;
  private RepositoryIdentifier testRepo;

  @BeforeEach
  final void setUp() {
    extractor = new DiffFileReferenceExtractor();
    testRepo = RepositoryIdentifier.create(SourceProvider.GITLAB, "test/repo");
  }

  @Test
  @DisplayName("should_extract_import_references_from_diff")
  final void should_extract_import_references_from_diff() {
    final String rawDiff =
        """
        diff --git a/src/main/java/com/example/Service.java b/src/main/java/com/example/Service.java
        index abc123..def456 100644
        --- a/src/main/java/com/example/Service.java
        +++ b/src/main/java/com/example/Service.java
        @@ -1,5 +1,6 @@
         package com.example;

        +import com.example.util.Helper;
         import java.util.List;

         public class Service {
        """;

    final GitFileModification modification =
        new GitFileModification(
            "src/main/java/com/example/Service.java", "src/main/java/com/example/Service.java");
    final GitDiffDocument gitDiff = new GitDiffDocument(List.of(modification));
    final DiffAnalysisBundle bundle =
        new DiffAnalysisBundle(testRepo, gitDiff, rawDiff, null, null);

    final List<ContextMatch> matches = extractor.extractReferences(bundle);

    assertThat(matches).isNotEmpty();
    assertThat(matches)
        .anyMatch(
            match ->
                match.filePath().equals("com/example/util/Helper.java")
                    && match.reason() == MatchReason.FILE_REFERENCE);
  }

  @Test
  @DisplayName("should_extract_qualified_class_references_from_diff")
  final void should_extract_qualified_class_references_from_diff() {
    final String rawDiff =
        """
        diff --git a/src/main/java/com/example/Controller.java b/src/main/java/com/example/Controller.java
        index abc123..def456 100644
        --- a/src/main/java/com/example/Controller.java
        +++ b/src/main/java/com/example/Controller.java
        @@ -10,1 +10,1 @@
        -  private Service service;
        +  private com.example.service.UserService userService;
        """;

    final GitFileModification modification =
        new GitFileModification(
            "src/main/java/com/example/Controller.java",
            "src/main/java/com/example/Controller.java");
    final GitDiffDocument gitDiff = new GitDiffDocument(List.of(modification));
    final DiffAnalysisBundle bundle =
        new DiffAnalysisBundle(testRepo, gitDiff, rawDiff, null, null);

    final List<ContextMatch> matches = extractor.extractReferences(bundle);

    assertThat(matches).isNotEmpty();
    assertThat(matches)
        .anyMatch(
            match ->
                match.filePath().equals("com/example/service/UserService.java")
                    && match.reason() == MatchReason.FILE_REFERENCE);
  }

  @Test
  @DisplayName("should_ignore_removed_lines_in_diff")
  final void should_ignore_removed_lines_in_diff() {
    final String rawDiff =
        """
        diff --git a/src/main/java/com/example/Service.java b/src/main/java/com/example/Service.java
        index abc123..def456 100644
        --- a/src/main/java/com/example/Service.java
        +++ b/src/main/java/com/example/Service.java
        @@ -1,5 +1,5 @@
         package com.example;

        -import com.example.old.LegacyHelper;
        +import com.example.util.NewHelper;
         import java.util.List;

         public class Service {
        """;

    final GitFileModification modification =
        new GitFileModification(
            "src/main/java/com/example/Service.java", "src/main/java/com/example/Service.java");
    final GitDiffDocument gitDiff = new GitDiffDocument(List.of(modification));
    final DiffAnalysisBundle bundle =
        new DiffAnalysisBundle(testRepo, gitDiff, rawDiff, null, null);

    final List<ContextMatch> matches = extractor.extractReferences(bundle);

    assertThat(matches).noneMatch(match -> match.filePath().contains("LegacyHelper"));
    assertThat(matches).anyMatch(match -> match.filePath().contains("NewHelper"));
  }

  @Test
  @DisplayName("should_return_empty_list_when_no_references_found")
  final void should_return_empty_list_when_no_references_found() {
    final String rawDiff =
        """
        diff --git a/README.md b/README.md
        index abc123..def456 100644
        --- a/README.md
        +++ b/README.md
        @@ -1,1 +1,1 @@
        -# Old Title
        +# New Title
        """;

    final GitFileModification modification = new GitFileModification("README.md", "README.md");
    final GitDiffDocument gitDiff = new GitDiffDocument(List.of(modification));
    final DiffAnalysisBundle bundle =
        new DiffAnalysisBundle(testRepo, gitDiff, rawDiff, null, null);

    final List<ContextMatch> matches = extractor.extractReferences(bundle);

    assertThat(matches).isEmpty();
  }

  @Test
  @DisplayName("should_have_correct_confidence_score_for_file_references")
  final void should_have_correct_confidence_score_for_file_references() {
    final String rawDiff =
        """
        diff --git a/src/Service.java b/src/Service.java
        index abc123..def456 100644
        --- a/src/Service.java
        +++ b/src/Service.java
        @@ -1,1 +1,2 @@
         package com.example;
        +import com.example.Helper;
        """;

    final GitFileModification modification =
        new GitFileModification("src/Service.java", "src/Service.java");
    final GitDiffDocument gitDiff = new GitDiffDocument(List.of(modification));
    final DiffAnalysisBundle bundle =
        new DiffAnalysisBundle(testRepo, gitDiff, rawDiff, null, null);

    final List<ContextMatch> matches = extractor.extractReferences(bundle);

    assertThat(matches).isNotEmpty();
    assertThat(matches.get(0).confidence())
        .isEqualTo(MatchReason.FILE_REFERENCE.getBaseConfidence());
  }
}
