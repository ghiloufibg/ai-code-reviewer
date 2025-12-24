package com.ghiloufi.aicode.agentworker.analysis;

import java.util.List;

public enum TestFramework {
  MAVEN("pom.xml", List.of("mvn", "test", "-B", "-q")),
  GRADLE("build.gradle", List.of("./gradlew", "test", "--no-daemon", "-q")),
  GRADLE_KTS("build.gradle.kts", List.of("./gradlew", "test", "--no-daemon", "-q")),
  NPM("package.json", List.of("npm", "test", "--", "--ci")),
  YARN("yarn.lock", List.of("yarn", "test", "--ci")),
  PYTEST("pytest.ini", List.of("pytest", "-v", "--tb=short")),
  PYTHON_SETUP("setup.py", List.of("python", "-m", "pytest", "-v")),
  GO_MOD("go.mod", List.of("go", "test", "./...", "-v")),
  CARGO("Cargo.toml", List.of("cargo", "test")),
  DOTNET("*.csproj", List.of("dotnet", "test", "--verbosity", "minimal"));

  private final String markerFile;
  private final List<String> testCommand;

  TestFramework(String markerFile, List<String> testCommand) {
    this.markerFile = markerFile;
    this.testCommand = testCommand;
  }

  public String getMarkerFile() {
    return markerFile;
  }

  public List<String> getTestCommand() {
    return testCommand;
  }
}
