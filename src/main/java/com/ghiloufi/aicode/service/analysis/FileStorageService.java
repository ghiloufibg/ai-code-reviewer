package com.ghiloufi.aicode.service.analysis;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service for storing and retrieving uploaded files for analysis. Handles file content extraction
 * and provides unified diff generation.
 */
@Service
@Slf4j
public class FileStorageService {

  @Value("${analysis.storage.path:${java.io.tmpdir}/ai-code-reviewer}")
  private String storagePath;

  // In-memory storage for file contents (analysisId -> Map<fileName, content>)
  private final Map<String, Map<String, String>> fileContents = new ConcurrentHashMap<>();

  /** Store uploaded files and extract their content for analysis */
  public Mono<Map<String, String>> storeAndExtractFiles(
      String analysisId, List<Flux<org.springframework.http.codec.multipart.Part>> files) {
    log.debug("Storing files for analysis: {}", analysisId);

    return Flux.fromIterable(files)
        .flatMap(flux -> flux)
        .filter(part -> "file".equals(part.name()))
        .cast(FilePart.class)
        .flatMap(this::extractFileContent)
        .collectMap(FileContent::fileName, FileContent::content)
        .doOnNext(
            contentMap -> {
              fileContents.put(analysisId, contentMap);
              log.info("Stored {} files for analysis {}", contentMap.size(), analysisId);
            })
        .doOnError(error -> log.error("Error storing files for analysis {}", analysisId, error));
  }

  /** Get stored file contents for an analysis */
  public Map<String, String> getFileContents(String analysisId) {
    return fileContents.get(analysisId);
  }

  /** Extract content from a single uploaded file */
  private Mono<FileContent> extractFileContent(FilePart filePart) {
    return filePart
        .content()
        .map(
            dataBuffer -> {
              byte[] bytes = new byte[dataBuffer.readableByteCount()];
              dataBuffer.read(bytes);
              return new String(bytes, StandardCharsets.UTF_8);
            })
        .reduce("", String::concat)
        .map(content -> new FileContent(filePart.filename(), content))
        .doOnNext(
            fileContent ->
                log.debug(
                    "Extracted content from file: {} ({} characters)",
                    fileContent.fileName(),
                    fileContent.content().length()));
  }

  /**
   * Generate a unified diff format from uploaded files This simulates a git diff for the LLM to
   * analyze
   */
  public String generateUnifiedDiff(Map<String, String> fileContents) {
    StringBuilder diffBuilder = new StringBuilder();

    for (Map.Entry<String, String> entry : fileContents.entrySet()) {
      String fileName = entry.getKey();
      String content = entry.getValue();

      // Create a unified diff format showing the entire file as "added"
      diffBuilder
          .append("diff --git a/")
          .append(fileName)
          .append(" b/")
          .append(fileName)
          .append("\n");
      diffBuilder.append("new file mode 100644\n");
      diffBuilder
          .append("index 0000000..")
          .append(Integer.toHexString(content.hashCode()))
          .append("\n");
      diffBuilder.append("--- /dev/null\n");
      diffBuilder.append("+++ b/").append(fileName).append("\n");

      // Split content into lines and add with "+" prefix
      String[] lines = content.split("\n");
      diffBuilder.append("@@ -0,0 +1,").append(lines.length).append(" @@\n");

      for (String line : lines) {
        diffBuilder.append("+").append(line).append("\n");
      }

      diffBuilder.append("\n");
    }

    log.debug("Generated unified diff ({} characters)", diffBuilder.length());
    return diffBuilder.toString();
  }

  /**
   * Create a simplified diff showing only the files being analyzed This is more appropriate for
   * uploaded files vs git diffs
   */
  public String generateSimplifiedContent(Map<String, String> fileContents) {
    StringBuilder contentBuilder = new StringBuilder();

    contentBuilder.append("Files being analyzed:\n\n");

    for (Map.Entry<String, String> entry : fileContents.entrySet()) {
      String fileName = entry.getKey();
      String content = entry.getValue();

      contentBuilder.append("=== ").append(fileName).append(" ===\n");
      contentBuilder.append(content);
      contentBuilder.append("\n\n");
    }

    log.debug("Generated simplified content ({} characters)", contentBuilder.length());
    return contentBuilder.toString();
  }

  /** Clean up stored files for an analysis */
  public void cleanup(String analysisId) {
    Map<String, String> removed = fileContents.remove(analysisId);
    if (removed != null) {
      log.debug("Cleaned up {} files for analysis {}", removed.size(), analysisId);
    }
  }

  /** Get file count for an analysis */
  public int getFileCount(String analysisId) {
    Map<String, String> contents = fileContents.get(analysisId);
    return contents != null ? contents.size() : 0;
  }

  /** Calculate total content size for an analysis */
  public long getTotalSize(String analysisId) {
    Map<String, String> contents = fileContents.get(analysisId);
    if (contents == null) return 0;

    return contents.values().stream().mapToLong(String::length).sum();
  }

  /** Record class for file content */
  public record FileContent(String fileName, String content) {}
}
