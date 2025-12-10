package com.ghiloufi.aicode.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.ReviewState;
import com.ghiloufi.aicode.core.infrastructure.persistence.entity.ReviewEntity;
import com.ghiloufi.aicode.core.infrastructure.persistence.entity.ReviewIssueEntity;
import com.ghiloufi.aicode.core.infrastructure.persistence.repository.ReviewJpaRepository;
import java.math.BigDecimal;
import java.util.List;
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

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestConfig.class)
@DisplayName("Database Migration V3 Integration Tests - Confidence Scoring & Fix Application")
class DatabaseMigrationV3IntegrationTest {

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

  @Autowired private ReviewJpaRepository jpaRepository;

  @BeforeEach
  void setUp() {
    jpaRepository.deleteAll();
  }

  @Test
  @DisplayName("should_persist_confidence_score_within_valid_range")
  void should_persist_confidence_score_within_valid_range() {
    final ReviewEntity review = createReviewWithConfidenceFields();
    final ReviewIssueEntity issue = review.getIssues().get(0);

    issue.setConfidenceScore(new BigDecimal("0.85"));
    issue.setConfidenceExplanation(
        "High confidence: Clear SQL injection vulnerability with well-established pattern");

    final ReviewEntity savedReview = jpaRepository.save(review);
    jpaRepository.flush();

    final ReviewEntity retrievedReview = jpaRepository.findById(savedReview.getId()).orElseThrow();
    final ReviewIssueEntity retrievedIssue = retrievedReview.getIssues().get(0);

    assertThat(retrievedIssue.getConfidenceScore()).isEqualByComparingTo(new BigDecimal("0.85"));
    assertThat(retrievedIssue.getConfidenceExplanation())
        .contains("High confidence")
        .contains("SQL injection");
  }

  @Test
  @DisplayName("should_validate_confidence_score_boundaries")
  void should_validate_confidence_score_boundaries() {
    final ReviewEntity review1 = createReviewWithConfidenceFields();
    review1.getIssues().get(0).setConfidenceScore(new BigDecimal("0.00"));
    final ReviewEntity saved1 = jpaRepository.save(review1);
    assertThat(saved1.getIssues().get(0).getConfidenceScore())
        .isEqualByComparingTo(new BigDecimal("0.00"));

    final ReviewEntity review2 = createReviewWithConfidenceFields();
    review2.getIssues().get(0).setConfidenceScore(new BigDecimal("1.00"));
    final ReviewEntity saved2 = jpaRepository.save(review2);
    assertThat(saved2.getIssues().get(0).getConfidenceScore())
        .isEqualByComparingTo(new BigDecimal("1.00"));

    final ReviewEntity review3 = createReviewWithConfidenceFields();
    review3.getIssues().get(0).setConfidenceScore(new BigDecimal("0.60"));
    final ReviewEntity saved3 = jpaRepository.save(review3);
    assertThat(saved3.getIssues().get(0).getConfidenceScore())
        .isEqualByComparingTo(new BigDecimal("0.60"));
  }

  @Test
  @DisplayName("should_allow_null_confidence_score_for_backward_compatibility")
  void should_allow_null_confidence_score_for_backward_compatibility() {
    final ReviewEntity review = createReviewWithConfidenceFields();
    final ReviewIssueEntity issue = review.getIssues().get(0);

    issue.setConfidenceScore(null);
    issue.setConfidenceExplanation(null);

    final ReviewEntity savedReview = jpaRepository.save(review);
    final ReviewIssueEntity retrievedIssue = savedReview.getIssues().get(0);

    assertThat(retrievedIssue.getConfidenceScore()).isNull();
    assertThat(retrievedIssue.getConfidenceExplanation()).isNull();
  }

  @Test
  @DisplayName("should_support_isHighConfidence_helper_method")
  void should_support_isHighConfidence_helper_method() {
    final ReviewEntity review = createReviewWithConfidenceFields();
    final ReviewIssueEntity issue = review.getIssues().get(0);

    issue.setConfidenceScore(new BigDecimal("0.85"));
    assertThat(issue.isHighConfidence()).isTrue();

    issue.setConfidenceScore(new BigDecimal("0.70"));
    assertThat(issue.isHighConfidence()).isTrue();

    issue.setConfidenceScore(new BigDecimal("0.69"));
    assertThat(issue.isHighConfidence()).isFalse();

    issue.setConfidenceScore(new BigDecimal("0.50"));
    assertThat(issue.isHighConfidence()).isFalse();

    issue.setConfidenceScore(null);
    assertThat(issue.isHighConfidence()).isFalse();
  }

  @Test
  @DisplayName("should_handle_multiple_issues_with_different_confidence_levels")
  void should_handle_multiple_issues_with_different_confidence_levels() {
    final ReviewEntity review = createReviewWithMultipleIssues();

    final ReviewEntity savedReview = jpaRepository.save(review);
    final List<ReviewIssueEntity> issues = savedReview.getIssues();

    assertThat(issues).hasSize(3);

    final ReviewIssueEntity highConfidenceIssue =
        issues.stream()
            .filter(i -> i.getTitle().equals("Critical security issue"))
            .findFirst()
            .orElseThrow();
    assertThat(highConfidenceIssue.getConfidenceScore())
        .isEqualByComparingTo(new BigDecimal("0.95"));
    assertThat(highConfidenceIssue.isHighConfidence()).isTrue();

    final ReviewIssueEntity mediumConfidenceIssue =
        issues.stream()
            .filter(i -> i.getTitle().equals("Code smell detected"))
            .findFirst()
            .orElseThrow();
    assertThat(mediumConfidenceIssue.getConfidenceScore())
        .isEqualByComparingTo(new BigDecimal("0.65"));
    assertThat(mediumConfidenceIssue.isHighConfidence()).isFalse();

    final ReviewIssueEntity lowConfidenceIssue =
        issues.stream()
            .filter(i -> i.getTitle().equals("Potential improvement"))
            .findFirst()
            .orElseThrow();
    assertThat(lowConfidenceIssue.getConfidenceScore())
        .isEqualByComparingTo(new BigDecimal("0.45"));
    assertThat(lowConfidenceIssue.isHighConfidence()).isFalse();
  }

  @Test
  @DisplayName("should_persist_large_text_fields_for_explanations")
  void should_persist_large_text_fields_for_explanations() {
    final ReviewEntity review = createReviewWithConfidenceFields();
    final ReviewIssueEntity issue = review.getIssues().get(0);

    final String largeExplanation = "This is a very detailed confidence explanation. ".repeat(100);

    issue.setConfidenceExplanation(largeExplanation);

    final ReviewEntity savedReview = jpaRepository.save(review);
    jpaRepository.flush();

    final ReviewEntity retrievedReview = jpaRepository.findById(savedReview.getId()).orElseThrow();
    final ReviewIssueEntity retrievedIssue = retrievedReview.getIssues().get(0);

    assertThat(retrievedIssue.getConfidenceExplanation()).hasSize(largeExplanation.length());
  }

  private ReviewEntity createReviewWithConfidenceFields() {
    final ReviewEntity review = new ReviewEntity();
    review.setRepositoryId("test-repo");
    review.setChangeRequestId("123");
    review.setStatus(ReviewState.IN_PROGRESS);
    review.setProvider("GITHUB");

    final ReviewIssueEntity issue =
        ReviewIssueEntity.builder()
            .review(review)
            .filePath("src/main/java/TestFile.java")
            .startLine(42)
            .severity("HIGH")
            .title("Security vulnerability detected")
            .suggestion("Fix the SQL injection vulnerability")
            .build();

    review.setIssues(List.of(issue));
    return review;
  }

  private ReviewEntity createReviewWithMultipleIssues() {
    final ReviewEntity review = new ReviewEntity();
    review.setRepositoryId("test-repo");
    review.setChangeRequestId("456");
    review.setStatus(ReviewState.IN_PROGRESS);
    review.setProvider("GITHUB");

    final ReviewIssueEntity issue1 =
        ReviewIssueEntity.builder()
            .review(review)
            .filePath("src/Security.java")
            .startLine(10)
            .severity("CRITICAL")
            .title("Critical security issue")
            .suggestion("Fix immediately")
            .confidenceScore(new BigDecimal("0.95"))
            .confidenceExplanation("Clear SQL injection pattern")
            .build();

    final ReviewIssueEntity issue2 =
        ReviewIssueEntity.builder()
            .review(review)
            .filePath("src/Service.java")
            .startLine(25)
            .severity("MEDIUM")
            .title("Code smell detected")
            .suggestion("Consider refactoring")
            .confidenceScore(new BigDecimal("0.65"))
            .confidenceExplanation("Possible code duplication")
            .build();

    final ReviewIssueEntity issue3 =
        ReviewIssueEntity.builder()
            .review(review)
            .filePath("src/Utils.java")
            .startLine(50)
            .severity("LOW")
            .title("Potential improvement")
            .suggestion("Could use Java 21 features")
            .confidenceScore(new BigDecimal("0.45"))
            .confidenceExplanation("Stylistic preference, context-dependent")
            .build();

    review.setIssues(List.of(issue1, issue2, issue3));
    return review;
  }
}
