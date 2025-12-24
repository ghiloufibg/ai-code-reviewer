package com.ghiloufi.aicode.agentworker.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("TestFramework")
final class TestFrameworkTest {

  @ParameterizedTest
  @EnumSource(TestFramework.class)
  void should_have_marker_file_for_each_framework(final TestFramework framework) {
    assertThat(framework.getMarkerFile()).isNotNull().isNotBlank();
  }

  @ParameterizedTest
  @EnumSource(TestFramework.class)
  void should_have_test_command_for_each_framework(final TestFramework framework) {
    assertThat(framework.getTestCommand()).isNotNull().isNotEmpty();
  }

  @Test
  void maven_should_use_pom_xml_as_marker() {
    assertThat(TestFramework.MAVEN.getMarkerFile()).isEqualTo("pom.xml");
  }

  @Test
  void maven_should_use_mvn_test_command() {
    assertThat(TestFramework.MAVEN.getTestCommand()).containsExactly("mvn", "test", "-B", "-q");
  }

  @Test
  void gradle_should_use_build_gradle_as_marker() {
    assertThat(TestFramework.GRADLE.getMarkerFile()).isEqualTo("build.gradle");
  }

  @Test
  void gradle_kts_should_use_build_gradle_kts_as_marker() {
    assertThat(TestFramework.GRADLE_KTS.getMarkerFile()).isEqualTo("build.gradle.kts");
  }

  @Test
  void npm_should_use_package_json_as_marker() {
    assertThat(TestFramework.NPM.getMarkerFile()).isEqualTo("package.json");
  }

  @Test
  void pytest_should_use_pytest_ini_as_marker() {
    assertThat(TestFramework.PYTEST.getMarkerFile()).isEqualTo("pytest.ini");
  }

  @Test
  void go_mod_should_use_go_mod_as_marker() {
    assertThat(TestFramework.GO_MOD.getMarkerFile()).isEqualTo("go.mod");
  }

  @Test
  void dotnet_should_use_wildcard_csproj_as_marker() {
    assertThat(TestFramework.DOTNET.getMarkerFile()).isEqualTo("*.csproj");
  }

  @Test
  void cargo_should_use_cargo_toml_as_marker() {
    assertThat(TestFramework.CARGO.getMarkerFile()).isEqualTo("Cargo.toml");
  }
}
