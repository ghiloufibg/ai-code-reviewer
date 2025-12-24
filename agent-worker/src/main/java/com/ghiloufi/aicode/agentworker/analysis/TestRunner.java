package com.ghiloufi.aicode.agentworker.analysis;

import com.ghiloufi.aicode.agentworker.config.AgentWorkerProperties;
import com.ghiloufi.aicode.agentworker.container.ContainerConfiguration;
import com.ghiloufi.aicode.agentworker.container.DockerContainerManager;
import com.ghiloufi.aicode.core.domain.model.TestResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestRunner {

  private final DockerContainerManager containerManager;
  private final AgentWorkerProperties properties;

  public TestExecutionResult runTests(Path repositoryPath) {
    if (!properties.getAnalysis().getTests().isEnabled()) {
      log.debug("Test execution is disabled");
      return TestExecutionResult.notExecuted("Test execution is disabled in configuration");
    }

    final var startTime = Instant.now();

    try {
      final var detectedFramework = detectFramework(repositoryPath);
      if (detectedFramework.isEmpty()) {
        log.info("No test framework detected in repository");
        return TestExecutionResult.notExecuted("No test framework detected");
      }

      final var framework = detectedFramework.get();
      log.info("Detected test framework: {}", framework);

      final var containerConfig = buildTestContainerConfig(repositoryPath, framework);
      final var result = containerManager.executeInContainer(containerConfig);

      final var duration = Duration.between(startTime, Instant.now());
      final var testResults = parseTestOutput(result.stdout(), framework);

      if (result.isSuccess()) {
        final int passed = (int) testResults.stream().filter(TestResult::isSuccess).count();
        final int failed = testResults.size() - passed;
        log.info(
            "Tests completed: {} total, {} passed, {} failed in {}ms",
            testResults.size(),
            passed,
            failed,
            duration.toMillis());
        return TestExecutionResult.success(
            framework,
            testResults,
            testResults.size(),
            passed,
            failed,
            0,
            duration,
            result.stdout());
      } else {
        log.warn("Test execution failed: {}", result.stderr());
        return TestExecutionResult.failure(
            framework, testResults, duration, result.stdout(), result.stderr());
      }

    } catch (Exception e) {
      log.error("Test execution error", e);
      return TestExecutionResult.notExecuted("Error: " + e.getMessage());
    }
  }

  private Optional<TestFramework> detectFramework(Path repositoryPath) {
    if (!properties.getAnalysis().getTests().isAutoDetect()) {
      return Optional.empty();
    }

    for (final var framework : TestFramework.values()) {
      final var markerFile = framework.getMarkerFile();
      if (markerFile.contains("*")) {
        try (final var files = Files.list(repositoryPath)) {
          final var pattern = markerFile.replace("*", "");
          if (files.anyMatch(p -> p.getFileName().toString().endsWith(pattern))) {
            return Optional.of(framework);
          }
        } catch (Exception e) {
          log.debug("Error scanning for {}: {}", markerFile, e.getMessage());
        }
      } else {
        if (Files.exists(repositoryPath.resolve(markerFile))) {
          return Optional.of(framework);
        }
      }
    }

    return Optional.empty();
  }

  private ContainerConfiguration buildTestContainerConfig(
      Path repositoryPath, TestFramework framework) {
    final var command = new ArrayList<String>();
    command.add("/bin/sh");
    command.add("-c");
    command.add("cd /workspace/repo && " + String.join(" ", framework.getTestCommand()));

    return ContainerConfiguration.builder()
        .imageName(properties.getDocker().getAnalysisImage())
        .memoryBytes(properties.getDocker().getResourceLimits().getMemoryBytes())
        .nanoCpus(properties.getDocker().getResourceLimits().getNanoCpus())
        .workspaceVolume(repositoryPath.getParent().toString())
        .command(command)
        .environment(Map.of("CI", "true"))
        .readOnly(false)
        .autoRemove(true)
        .noNewPrivileges(true)
        .build();
  }

  private List<TestResult> parseTestOutput(String output, TestFramework framework) {
    final List<TestResult> results = new ArrayList<>();

    if (output == null || output.isBlank()) {
      return results;
    }

    switch (framework) {
      case MAVEN, GRADLE, GRADLE_KTS -> parseMavenGradleOutput(output, results);
      case NPM, YARN -> parseJestOutput(output, results);
      case PYTEST, PYTHON_SETUP -> parsePytestOutput(output, results);
      case GO_MOD -> parseGoTestOutput(output, results);
      default -> log.debug("No parser available for framework: {}", framework);
    }

    return results;
  }

  private void parseMavenGradleOutput(String output, List<TestResult> results) {
    final var lines = output.split("\n");
    for (final var line : lines) {
      if (line.contains("Tests run:") || line.contains("test:")) {
        log.trace("Parsing test summary line: {}", line);
      }

      if (line.matches(".*\\[ERROR\\].*Test.*failed.*") || line.matches(".*FAILED.*")) {
        final var testName = extractTestName(line);
        results.add(TestResult.failed(testName, "unknown", extractFailureMessage(line), null));
      }
    }
  }

  private void parseJestOutput(String output, List<TestResult> results) {
    final var lines = output.split("\n");
    for (final var line : lines) {
      if (line.contains("✓") || line.contains("PASS")) {
        final var testName = line.replaceAll(".*[✓PASS]\\s*", "").trim();
        results.add(TestResult.passed(testName, "unknown", Duration.ZERO));
      } else if (line.contains("✕") || line.contains("FAIL")) {
        final var testName = line.replaceAll(".*[✕FAIL]\\s*", "").trim();
        results.add(TestResult.failed(testName, "unknown", "Test failed", null));
      }
    }
  }

  private void parsePytestOutput(String output, List<TestResult> results) {
    final var lines = output.split("\n");
    for (final var line : lines) {
      if (line.contains("PASSED")) {
        final var testName = line.split("\\s+")[0];
        results.add(TestResult.passed(testName, "unknown", Duration.ZERO));
      } else if (line.contains("FAILED")) {
        final var testName = line.split("\\s+")[0];
        results.add(TestResult.failed(testName, "unknown", "Test failed", null));
      }
    }
  }

  private void parseGoTestOutput(String output, List<TestResult> results) {
    final var lines = output.split("\n");
    for (final var line : lines) {
      if (line.startsWith("--- PASS:")) {
        final var testName = line.replace("--- PASS:", "").split("\\s")[0].trim();
        results.add(TestResult.passed(testName, "unknown", Duration.ZERO));
      } else if (line.startsWith("--- FAIL:")) {
        final var testName = line.replace("--- FAIL:", "").split("\\s")[0].trim();
        results.add(TestResult.failed(testName, "unknown", "Test failed", null));
      }
    }
  }

  private String extractTestName(String line) {
    final var parts = line.split("\\s+");
    for (final var part : parts) {
      if (part.contains("Test") || part.contains("test")) {
        return part.replaceAll("[^a-zA-Z0-9_]", "");
      }
    }
    return "UnknownTest";
  }

  private String extractFailureMessage(String line) {
    final var colonIndex = line.lastIndexOf(':');
    if (colonIndex > 0 && colonIndex < line.length() - 1) {
      return line.substring(colonIndex + 1).trim();
    }
    return line;
  }
}
