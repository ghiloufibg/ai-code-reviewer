package com.ghiloufi.aicode.agentworker.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CloneRequest")
final class CloneRequestTest {

  @Nested
  @DisplayName("builder")
  final class BuilderTests {

    @Test
    void should_build_clone_request_with_all_fields() {
      final var request =
          CloneRequest.builder()
              .repositoryUrl("https://github.com/owner/repo.git")
              .branch("main")
              .targetDirectory("/tmp/workspace")
              .depth(1)
              .authToken("token123")
              .build();

      assertThat(request.repositoryUrl()).isEqualTo("https://github.com/owner/repo.git");
      assertThat(request.branch()).isEqualTo("main");
      assertThat(request.targetDirectory()).isEqualTo("/tmp/workspace");
      assertThat(request.depth()).isEqualTo(1);
      assertThat(request.authToken()).isEqualTo("token123");
    }

    @Test
    void should_use_default_depth_of_1() {
      final var request =
          CloneRequest.builder()
              .repositoryUrl("https://github.com/owner/repo.git")
              .branch("main")
              .targetDirectory("/tmp/workspace")
              .build();

      assertThat(request.depth()).isEqualTo(1);
    }

    @Test
    void should_allow_null_auth_token() {
      final var request =
          CloneRequest.builder()
              .repositoryUrl("https://github.com/owner/repo.git")
              .branch("main")
              .targetDirectory("/tmp/workspace")
              .build();

      assertThat(request.authToken()).isNull();
    }

    @Test
    void should_allow_custom_depth() {
      final var request =
          CloneRequest.builder()
              .repositoryUrl("https://github.com/owner/repo.git")
              .branch("main")
              .targetDirectory("/tmp/workspace")
              .depth(10)
              .build();

      assertThat(request.depth()).isEqualTo(10);
    }
  }

  @Nested
  @DisplayName("record accessors")
  final class AccessorTests {

    @Test
    void should_provide_all_record_accessors() {
      final var request =
          new CloneRequest(
              "https://gitlab.com/owner/repo.git", "develop", "/workspace", 5, "secret");

      assertThat(request.repositoryUrl()).isEqualTo("https://gitlab.com/owner/repo.git");
      assertThat(request.branch()).isEqualTo("develop");
      assertThat(request.targetDirectory()).isEqualTo("/workspace");
      assertThat(request.depth()).isEqualTo(5);
      assertThat(request.authToken()).isEqualTo("secret");
    }
  }
}
