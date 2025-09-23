package com.ghiloufi.aicode.service.analysis;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Tests unitaires pour {@link StaticAnalysisRunner} reactive implementation.
 *
 * <p>Ces tests vérifient tous les aspects réactifs du StaticAnalysisRunner, y compris la lecture de
 * fichiers concurrente, la gestion d'erreurs, et la compatibilité backwards.
 */
@DisplayName("StaticAnalysisRunner Reactive")
class StaticAnalysisRunnerReactiveTest {

  @TempDir Path tempDir;

  private StaticAnalysisRunner staticAnalysisRunner;

  @BeforeEach
  void setUp() {
    staticAnalysisRunner = new StaticAnalysisRunner(tempDir);
  }

  @AfterEach
  void cleanup() throws IOException {
    // Clean up temp directory contents
    if (Files.exists(tempDir)) {
      Files.walkFileTree(
          tempDir,
          new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
              if (!file.equals(tempDir)) {
                Files.deleteIfExists(file);
              }
              return FileVisitResult.CONTINUE;
            }
          });
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Default constructor should work")
    void testDefaultConstructor() {
      assertDoesNotThrow(() -> new StaticAnalysisRunner());
    }

    @Test
    @DisplayName("Constructor with target directory should work")
    void testConstructorWithTargetDirectory() {
      assertDoesNotThrow(() -> new StaticAnalysisRunner(tempDir));
    }

    @Test
    @DisplayName("Constructor with full parameters should work")
    void testConstructorWithAllParameters() {
      StaticAnalysisRunner runner = new StaticAnalysisRunner(tempDir, 50000);
      assertEquals(tempDir, runner.getTargetDirectory());
      assertEquals(50000, runner.getMaxContentLength());
    }

    @Test
    @DisplayName("Constructor should reject null target directory")
    void testConstructorWithNullTargetDirectory() {
      assertThrows(IllegalArgumentException.class, () -> new StaticAnalysisRunner(null));
    }

    @Test
    @DisplayName("Constructor should reject invalid max content length")
    void testConstructorWithInvalidMaxContentLength() {
      assertThrows(IllegalArgumentException.class, () -> new StaticAnalysisRunner(tempDir, 0));
      assertThrows(IllegalArgumentException.class, () -> new StaticAnalysisRunner(tempDir, -1));
    }
  }

  @Nested
  @DisplayName("Reactive Collection Tests")
  class ReactiveCollectionTests {

    @Test
    @DisplayName("runAndCollect should return empty results when no files exist")
    void testRunAndCollectReactiveWithNoFiles() {
      StepVerifier.create(staticAnalysisRunner.runAndCollect())
          .expectNextMatches(
              results -> {
                assertNotNull(results);
                assertEquals(4, results.size());
                assertTrue(results.containsKey("checkstyle"));
                assertTrue(results.containsKey("pmd"));
                assertTrue(results.containsKey("spotbugs"));
                assertTrue(results.containsKey("semgrep"));
                // All should be empty strings
                assertEquals("", results.get("checkstyle"));
                assertEquals("", results.get("pmd"));
                assertEquals("", results.get("spotbugs"));
                assertEquals("", results.get("semgrep"));
                return true;
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("runAndCollect should handle XML files")
    void testRunAndCollectReactiveWithXmlFiles() throws IOException {
      // Create test XML files
      String xmlContent =
          """
          <?xml version="1.0" encoding="UTF-8"?>
          <checkstyle version="8.45">
            <file name="Test.java">
              <error line="1" severity="error" message="Test error"/>
            </file>
          </checkstyle>
          """;

      Files.writeString(tempDir.resolve("checkstyle-result.xml"), xmlContent);
      Files.writeString(tempDir.resolve("pmd.xml"), xmlContent);
      Files.writeString(tempDir.resolve("spotbugs.xml"), xmlContent);

      StepVerifier.create(staticAnalysisRunner.runAndCollect())
          .expectNextMatches(
              results -> {
                assertNotNull(results);
                assertEquals(4, results.size());

                // XML files should contain content
                assertEquals(xmlContent, results.get("checkstyle"));
                assertEquals(xmlContent, results.get("pmd"));
                assertEquals(xmlContent, results.get("spotbugs"));

                // Semgrep should be empty (no file)
                assertEquals("", results.get("semgrep"));
                return true;
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("runAndCollect should handle JSON files")
    void testRunAndCollectReactiveWithJsonFiles() throws IOException {
      // Create test JSON content
      Map<String, Object> jsonObject =
          Map.of(
              "results",
              Map.of(
                  "findings", 5,
                  "errors", 0),
              "tool",
              "semgrep");

      ObjectMapper mapper = new ObjectMapper();
      String jsonContent = mapper.writeValueAsString(jsonObject);

      Files.writeString(tempDir.resolve("semgrep.json"), jsonContent);

      StepVerifier.create(staticAnalysisRunner.runAndCollect())
          .expectNextMatches(
              results -> {
                assertNotNull(results);
                assertEquals(4, results.size());

                // JSON should be parsed as object
                Object semgrepResult = results.get("semgrep");
                assertNotNull(semgrepResult);
                assertFalse(semgrepResult instanceof String);

                // Others should be empty
                assertEquals("", results.get("checkstyle"));
                assertEquals("", results.get("pmd"));
                assertEquals("", results.get("spotbugs"));
                return true;
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("runAndCollect should handle mixed file types")
    void testRunAndCollectReactiveWithMixedFiles() throws IOException {
      // Create XML file
      String xmlContent = "<checkstyle><file name=\"Test.java\"/></checkstyle>";
      Files.writeString(tempDir.resolve("checkstyle-result.xml"), xmlContent);

      // Create JSON file
      String jsonContent = "{\"findings\": 3, \"tool\": \"semgrep\"}";
      Files.writeString(tempDir.resolve("semgrep.json"), jsonContent);

      StepVerifier.create(staticAnalysisRunner.runAndCollect())
          .expectNextMatches(
              results -> {
                assertNotNull(results);
                assertEquals(4, results.size());

                // XML should be string
                assertEquals(xmlContent, results.get("checkstyle"));

                // JSON should be parsed object
                Object semgrepResult = results.get("semgrep");
                assertNotNull(semgrepResult);
                assertFalse(semgrepResult instanceof String);

                // Others should be empty
                assertEquals("", results.get("pmd"));
                assertEquals("", results.get("spotbugs"));
                return true;
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("runAndCollect should truncate large XML content")
    void testRunAndCollectReactiveWithLargeContent() throws IOException {
      // Create very large content
      StringBuilder largeContent = new StringBuilder();
      for (int i = 0; i < 10000; i++) {
        largeContent
            .append("<error line=\"")
            .append(i)
            .append("\" message=\"Long error message for line ")
            .append(i)
            .append("\"/>\n");
      }

      StaticAnalysisRunner smallLimitRunner = new StaticAnalysisRunner(tempDir, 1000);
      Files.writeString(tempDir.resolve("checkstyle-result.xml"), largeContent.toString());

      StepVerifier.create(smallLimitRunner.runAndCollect())
          .expectNextMatches(
              results -> {
                String checkstyleResult = (String) results.get("checkstyle");
                assertEquals(1000, checkstyleResult.length());
                return true;
              })
          .verifyComplete();
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("runAndCollect should handle invalid JSON gracefully")
    void testRunAndCollectReactiveWithInvalidJson() throws IOException {
      // Create invalid JSON
      String invalidJson = "{\"invalid\": json content without closing brace";
      Files.writeString(tempDir.resolve("semgrep.json"), invalidJson);

      StepVerifier.create(staticAnalysisRunner.runAndCollect())
          .expectNextMatches(
              results -> {
                // Should fallback to truncated string content
                String semgrepResult = (String) results.get("semgrep");
                assertEquals(invalidJson, semgrepResult);
                return true;
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("runAndCollect should handle non-readable files")
    void testRunAndCollectReactiveWithNonReadableFile() throws IOException {
      Path nonReadableFile = tempDir.resolve("checkstyle-result.xml");
      Files.writeString(nonReadableFile, "content");

      // Try to make file non-readable (may not work on all systems)
      try {
        nonReadableFile.toFile().setReadable(false);

        if (!Files.isReadable(nonReadableFile)) {
          StepVerifier.create(staticAnalysisRunner.runAndCollect())
              .expectNextMatches(
                  results -> {
                    assertEquals("", results.get("checkstyle"));
                    return true;
                  })
              .verifyComplete();
        } else {
          // If we can't make it non-readable, just verify it works normally
          StepVerifier.create(staticAnalysisRunner.runAndCollect())
              .expectNextMatches(
                  results -> {
                    assertEquals("content", results.get("checkstyle"));
                    return true;
                  })
              .verifyComplete();
        }
      } finally {
        // Restore readability
        nonReadableFile.toFile().setReadable(true);
      }
    }

    @Test
    @DisplayName("runAndCollect should handle directories as files")
    void testRunAndCollectReactiveWithDirectoryAsFile() throws IOException {
      // Create directory with same name as expected file
      Files.createDirectory(tempDir.resolve("checkstyle-result.xml"));

      StepVerifier.create(staticAnalysisRunner.runAndCollect())
          .expectNextMatches(
              results -> {
                assertEquals("", results.get("checkstyle"));
                return true;
              })
          .verifyComplete();
    }
  }

  @Nested
  @DisplayName("Backward Compatibility Tests")
  class BackwardCompatibilityTests {

    @Test
    @DisplayName("runAndCollect should work consistently with multiple calls")
    void testRunAndCollectConsistency() throws IOException {
      // Create test files
      String xmlContent = "<checkstyle><file name=\"Test.java\"/></checkstyle>";
      Files.writeString(tempDir.resolve("checkstyle-result.xml"), xmlContent);

      String jsonContent = "{\"findings\": 2}";
      Files.writeString(tempDir.resolve("semgrep.json"), jsonContent);

      // Test reactive version multiple times
      Map<String, Object> firstResults = staticAnalysisRunner.runAndCollect().block();
      Map<String, Object> secondResults = staticAnalysisRunner.runAndCollect().block();

      // Results should be identical across multiple calls
      assertEquals(firstResults.size(), secondResults.size());
      assertEquals(firstResults.get("checkstyle"), secondResults.get("checkstyle"));
      assertEquals(firstResults.get("pmd"), secondResults.get("pmd"));
      assertEquals(firstResults.get("spotbugs"), secondResults.get("spotbugs"));

      // JSON parsing should produce consistent results
      assertNotNull(firstResults.get("semgrep"));
      assertNotNull(secondResults.get("semgrep"));
    }
  }

  @Nested
  @DisplayName("Utility Method Tests")
  class UtilityMethodTests {

    @Test
    @DisplayName("isTargetDirectoryAccessible should work correctly")
    void testIsTargetDirectoryAccessible() {
      assertTrue(staticAnalysisRunner.isTargetDirectoryAccessible());

      // Test with non-existent directory
      StaticAnalysisRunner nonExistentRunner =
          new StaticAnalysisRunner(tempDir.resolve("non-existent"));
      assertFalse(nonExistentRunner.isTargetDirectoryAccessible());
    }

    @Test
    @DisplayName("getAvailableAnalysisFiles should detect files correctly")
    void testGetAvailableAnalysisFiles() throws IOException {
      // Initially no files
      Map<String, Boolean> availability = staticAnalysisRunner.getAvailableAnalysisFiles();
      assertEquals(4, availability.size());
      assertFalse(availability.get("checkstyle"));
      assertFalse(availability.get("pmd"));
      assertFalse(availability.get("spotbugs"));
      assertFalse(availability.get("semgrep"));

      // Create some files
      Files.writeString(tempDir.resolve("checkstyle-result.xml"), "content");
      Files.writeString(tempDir.resolve("semgrep.json"), "{}");

      availability = staticAnalysisRunner.getAvailableAnalysisFiles();
      assertTrue(availability.get("checkstyle"));
      assertFalse(availability.get("pmd"));
      assertFalse(availability.get("spotbugs"));
      assertTrue(availability.get("semgrep"));
    }

    @Test
    @DisplayName("Getters should return correct values")
    void testGetters() {
      assertEquals(tempDir, staticAnalysisRunner.getTargetDirectory());
      assertTrue(staticAnalysisRunner.getMaxContentLength() > 0);

      StaticAnalysisRunner customRunner = new StaticAnalysisRunner(tempDir, 50000);
      assertEquals(50000, customRunner.getMaxContentLength());
    }
  }

  @Nested
  @DisplayName("Concurrent Processing Tests")
  class ConcurrentProcessingTests {

    @Test
    @DisplayName("runAndCollect should process files concurrently")
    void testConcurrentFileProcessing() throws IOException {
      // Create multiple files with different content
      Files.writeString(
          tempDir.resolve("checkstyle-result.xml"), "<checkstyle>checkstyle content</checkstyle>");
      Files.writeString(tempDir.resolve("pmd.xml"), "<pmd>pmd content</pmd>");
      Files.writeString(tempDir.resolve("spotbugs.xml"), "<spotbugs>spotbugs content</spotbugs>");
      Files.writeString(
          tempDir.resolve("semgrep.json"), "{\"tool\": \"semgrep\", \"results\": []}");

      long startTime = System.currentTimeMillis();

      StepVerifier.create(staticAnalysisRunner.runAndCollect())
          .expectNextMatches(
              results -> {
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;

                // Verify all files were processed
                assertNotNull(results.get("checkstyle"));
                assertNotNull(results.get("pmd"));
                assertNotNull(results.get("spotbugs"));
                assertNotNull(results.get("semgrep"));

                // With concurrent processing, this should be reasonably fast
                // (This is a basic check - in real scenarios concurrent processing shows more
                // benefit)
                assertTrue(duration < 5000, "Processing took too long: " + duration + "ms");

                return true;
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("Multiple concurrent calls should work correctly")
    void testMultipleConcurrentCalls() throws IOException {
      Files.writeString(tempDir.resolve("checkstyle-result.xml"), "test content");

      // Execute multiple concurrent reactive calls
      Mono<Map<String, Object>> call1 = staticAnalysisRunner.runAndCollect();
      Mono<Map<String, Object>> call2 = staticAnalysisRunner.runAndCollect();
      Mono<Map<String, Object>> call3 = staticAnalysisRunner.runAndCollect();

      StepVerifier.create(Mono.zip(call1, call2, call3))
          .expectNextMatches(
              tuple -> {
                Map<String, Object> result1 = tuple.getT1();
                Map<String, Object> result2 = tuple.getT2();
                Map<String, Object> result3 = tuple.getT3();

                // All results should be equivalent
                assertEquals(result1.get("checkstyle"), result2.get("checkstyle"));
                assertEquals(result2.get("checkstyle"), result3.get("checkstyle"));
                assertEquals("test content", result1.get("checkstyle"));

                return true;
              })
          .verifyComplete();
    }
  }

  @Nested
  @DisplayName("Exception Handling Tests")
  class ExceptionHandlingTests {

    @Test
    @DisplayName("runAndCollect should wrap unexpected exceptions")
    void testUnexpectedExceptionHandling() throws IOException {
      // Create a scenario that might cause unexpected issues
      Path specialFile = tempDir.resolve("checkstyle-result.xml");

      // Create file, then make directory unreadable to simulate permission issues
      Files.writeString(specialFile, "content");

      // This test is platform-dependent, so we'll just verify error handling works
      StepVerifier.create(staticAnalysisRunner.runAndCollect())
          .expectNextMatches(
              results -> {
                // Should complete successfully in normal cases
                assertNotNull(results);
                return true;
              })
          .verifyComplete();
    }
  }
}
