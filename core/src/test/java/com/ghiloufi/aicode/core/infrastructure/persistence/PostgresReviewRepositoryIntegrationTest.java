package com.ghiloufi.aicode.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.model.ReviewState;
import com.ghiloufi.aicode.core.infrastructure.persistence.repository.ReviewJpaRepository;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestConfig.class)
@DisplayName("PostgreSQL Review Repository Integration Tests")
class PostgresReviewRepositoryIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:15-alpine")
          .withDatabaseName("ai_code_reviewer_test")
          .withUsername("test_user")
          .withPassword("test_password");

  @DynamicPropertySource
  static void configureProperties(final DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.flyway.enabled", () -> "true");
  }

  @Autowired private PostgresReviewRepository repository;

  @Autowired private ReviewJpaRepository jpaRepository;

  @BeforeEach
  void setUp() {
    jpaRepository.deleteAll();
  }

  @Test
  @DisplayName("should save review with issues and notes")
  void should_save_review_with_issues_and_notes() {
    final String reviewId = "test-repo_123_github";
    final ReviewResult result = createSampleReviewResult();

    StepVerifier.create(repository.save(reviewId, result)).verifyComplete();

    StepVerifier.create(repository.findById(reviewId))
        .assertNext(
            optionalResult -> {
              assertThat(optionalResult).isPresent();
              final ReviewResult savedResult = optionalResult.get();
              assertThat(savedResult.getSummary()).isEqualTo("Test review summary");
              assertThat(savedResult.getIssues()).hasSize(2);
              assertThat(savedResult.getNonBlockingNotes()).hasSize(1);
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("should update review state")
  void should_update_review_state() {
    final String reviewId = "test-repo_456_github";
    final ReviewResult result = createSampleReviewResult();

    StepVerifier.create(
            repository
                .save(reviewId, result)
                .then(repository.updateState(reviewId, ReviewState.IN_PROGRESS)))
        .verifyComplete();

    StepVerifier.create(repository.getState(reviewId))
        .assertNext(
            optionalState -> {
              assertThat(optionalState).isPresent();
              assertThat(optionalState.get().state()).isEqualTo(ReviewState.IN_PROGRESS);
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("should update review result and state")
  void should_update_review_result_and_state() {
    final String reviewId = "test-repo_789_github";
    final ReviewResult initialResult = createSampleReviewResult();

    final ReviewResult.Issue newIssue =
        ReviewResult.Issue.issueBuilder()
            .file("UpdatedFile.java")
            .startLine(99)
            .severity("HIGH")
            .title("Updated issue")
            .suggestion("Fix this updated issue")
            .build();

    final ReviewResult updatedResult =
        ReviewResult.builder().summary("Updated summary").issues(List.of(newIssue)).build();

    StepVerifier.create(
            repository
                .save(reviewId, initialResult)
                .then(
                    repository.updateResultAndState(
                        reviewId, updatedResult, ReviewState.COMPLETED)))
        .verifyComplete();

    StepVerifier.create(repository.findById(reviewId))
        .assertNext(
            optionalResult -> {
              assertThat(optionalResult).isPresent();
              final ReviewResult result = optionalResult.get();
              assertThat(result.getSummary()).isEqualTo("Updated summary");
              assertThat(result.getIssues()).hasSize(1);
              assertThat(result.getIssues().get(0).getFile()).isEqualTo("UpdatedFile.java");
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("should return empty optional when review not found")
  void should_return_empty_optional_when_review_not_found() {
    final String nonExistentReviewId = UUID.randomUUID().toString();

    StepVerifier.create(repository.findById(nonExistentReviewId))
        .assertNext(optionalResult -> assertThat(optionalResult).isEmpty())
        .verifyComplete();
  }

  @Test
  @DisplayName("should set completed timestamp when state is terminal")
  void should_set_completed_timestamp_when_state_is_terminal() {
    final String reviewId = "test-repo_complete_github";
    final ReviewResult result = createSampleReviewResult();

    StepVerifier.create(
            repository
                .save(reviewId, result)
                .then(repository.updateState(reviewId, ReviewState.COMPLETED)))
        .verifyComplete();

    final var entity = jpaRepository.findAll().get(0);
    assertThat(entity.getCompletedAt()).isNotNull();
    assertThat(entity.getStatus()).isEqualTo(ReviewState.COMPLETED);
  }

  @Test
  @DisplayName("should handle concurrent saves")
  void should_handle_concurrent_saves() {
    final String reviewId1 = "test-repo_concurrent1_github";
    final String reviewId2 = "test-repo_concurrent2_github";
    final ReviewResult result1 = createSampleReviewResult();
    final ReviewResult result2 = createSampleReviewResult();

    StepVerifier.create(
            repository.save(reviewId1, result1).and(repository.save(reviewId2, result2)))
        .verifyComplete();

    assertThat(jpaRepository.count()).isEqualTo(2);
  }

  @Test
  @DisplayName("should cleanup old reviews")
  void should_cleanup_old_reviews() {
    final PostgresReviewRepository repositoryWithShortRetention =
        new PostgresReviewRepository(jpaRepository, Duration.ofMillis(1));

    final String reviewId = "test-repo_old_github";
    final ReviewResult result = createSampleReviewResult();

    StepVerifier.create(repositoryWithShortRetention.save(reviewId, result)).verifyComplete();

    assertThat(jpaRepository.count()).isEqualTo(1);

    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    repositoryWithShortRetention.cleanupOldReviews();

    assertThat(jpaRepository.count()).isEqualTo(0);
  }

  @Test
  @DisplayName("should store and retrieve raw LLM JSON response")
  void should_store_and_retrieve_raw_llm_json_response() {
    final String reviewId = "test-repo_rawjson_github";
    final String rawJson =
        """
        {
          "summary": "Test review summary",
          "issues": [
            {
              "file": "TestFile.java",
              "start_line": 10,
              "severity": "MEDIUM",
              "title": "Test issue 1",
              "suggestion": "Fix this issue"
            }
          ],
          "non_blocking_notes": []
        }
        """;

    final ReviewResult result = createSampleReviewResult().withRawLlmResponse(rawJson);

    StepVerifier.create(repository.save(reviewId, result)).verifyComplete();

    StepVerifier.create(repository.findById(reviewId))
        .assertNext(
            optionalResult -> {
              assertThat(optionalResult).isPresent();
              final ReviewResult savedResult = optionalResult.get();
              assertThat(savedResult.getRawLlmResponse()).isNotNull();
              assertThat(savedResult.getRawLlmResponse()).isEqualTo(rawJson);
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("should preserve raw JSON when updating review")
  void should_preserve_raw_json_when_updating_review() {
    final String reviewId = "test-repo_update_rawjson_github";
    final String initialRawJson = "{\"summary\": \"Initial\"}";
    final String updatedRawJson = "{\"summary\": \"Updated\"}";

    final ReviewResult initialResult =
        createSampleReviewResult().withRawLlmResponse(initialRawJson);

    final ReviewResult updatedResult =
        ReviewResult.builder().summary("Updated summary").rawLlmResponse(updatedRawJson).build();

    StepVerifier.create(
            repository
                .save(reviewId, initialResult)
                .then(
                    repository.updateResultAndState(
                        reviewId, updatedResult, ReviewState.COMPLETED)))
        .verifyComplete();

    StepVerifier.create(repository.findById(reviewId))
        .assertNext(
            optionalResult -> {
              assertThat(optionalResult).isPresent();
              final ReviewResult result = optionalResult.get();
              assertThat(result.getRawLlmResponse()).isEqualTo(updatedRawJson);
              assertThat(result.getSummary()).isEqualTo("Updated summary");
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("should handle null raw LLM response gracefully")
  void should_handle_null_raw_llm_response_gracefully() {
    final String reviewId = "test-repo_null_rawjson_github";
    final ReviewResult result = createSampleReviewResult().withRawLlmResponse(null);

    StepVerifier.create(repository.save(reviewId, result)).verifyComplete();

    StepVerifier.create(repository.findById(reviewId))
        .assertNext(
            optionalResult -> {
              assertThat(optionalResult).isPresent();
              final ReviewResult savedResult = optionalResult.get();
              assertThat(savedResult.getRawLlmResponse()).isNull();
            })
        .verifyComplete();
  }

  private ReviewResult createSampleReviewResult() {
    final ReviewResult.Issue issue1 =
        ReviewResult.Issue.issueBuilder()
            .file("TestFile.java")
            .startLine(10)
            .severity("MEDIUM")
            .title("Test issue 1")
            .suggestion("Fix this issue")
            .build();

    final ReviewResult.Issue issue2 =
        ReviewResult.Issue.issueBuilder()
            .file("AnotherFile.java")
            .startLine(20)
            .severity("LOW")
            .title("Test issue 2")
            .suggestion("Consider refactoring")
            .build();

    final ReviewResult.Note note =
        ReviewResult.Note.noteBuilder()
            .file("TestFile.java")
            .line(15)
            .note("Good implementation")
            .build();

    return ReviewResult.builder()
        .summary("Test review summary")
        .issues(List.of(issue1, issue2))
        .nonBlockingNotes(List.of(note))
        .build();
  }
}
