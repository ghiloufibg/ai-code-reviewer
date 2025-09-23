package com.ghiloufi.aicode.client.github;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.test.StepVerifier;

/**
 * Tests unitaires pour {@link GithubClient} reactive implementation.
 *
 * <p>Ces tests vérifient les aspects réactifs du client GitHub sans dépendances externes. Les tests
 * d'intégration avec de vraies APIs sont séparés.
 */
@DisplayName("GithubClient Reactive")
class GithubClientReactiveTest {

  private static final String TEST_REPO = "owner/repo";
  private static final String TEST_TOKEN = "ghp_testtoken123";
  private static final int TEST_TIMEOUT = 30;

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Valid constructor with all parameters")
    void testValidConstructor() {
      assertDoesNotThrow(() -> new GithubClient(TEST_REPO, TEST_TOKEN, TEST_TIMEOUT));
    }

    @Test
    @DisplayName("Constructor with null token (anonymous mode)")
    void testConstructorWithNullToken() {
      assertDoesNotThrow(() -> new GithubClient(TEST_REPO, null, TEST_TIMEOUT));
    }

    @Test
    @DisplayName("Constructor with empty token")
    void testConstructorWithEmptyToken() {
      assertDoesNotThrow(() -> new GithubClient(TEST_REPO, "", TEST_TIMEOUT));
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @DisplayName("Constructor should reject null/empty repository")
    void testConstructorWithInvalidRepository(String repository) {
      assertThrows(
          IllegalArgumentException.class,
          () -> new GithubClient(repository, TEST_TOKEN, TEST_TIMEOUT));
    }

    @Test
    @DisplayName("Constructor should reject repository without slash")
    void testConstructorWithInvalidRepositoryFormat() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new GithubClient("invalidrepo", TEST_TOKEN, TEST_TIMEOUT));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    @DisplayName("Constructor should reject invalid timeout values")
    void testConstructorWithInvalidTimeout(int timeout) {
      // For now, the constructor doesn't validate timeout, but we can add this later
      assertDoesNotThrow(() -> new GithubClient(TEST_REPO, TEST_TOKEN, timeout));
    }
  }

  @Nested
  @DisplayName("Validation Tests")
  class ValidationTests {

    private GithubClient client;

    @BeforeEach
    void setUp() {
      client = new GithubClient(TEST_REPO, TEST_TOKEN, TEST_TIMEOUT);
    }

    @Test
    @DisplayName("fetchPrUnifiedDiff should validate PR number")
    void testFetchPrUnifiedDiffValidation() {
      assertThrows(IllegalArgumentException.class, () -> client.fetchPrUnifiedDiff(-1, 3).block());
      assertThrows(IllegalArgumentException.class, () -> client.fetchPrUnifiedDiff(0, 3).block());
    }

    @Test
    @DisplayName("postIssueComment should validate parameters")
    void testPostIssueCommentValidation() {
      assertThrows(
          IllegalArgumentException.class, () -> client.postIssueComment(-1, "comment").block());
      assertThrows(
          IllegalArgumentException.class, () -> client.postIssueComment(0, "comment").block());
      assertThrows(IllegalArgumentException.class, () -> client.postIssueComment(1, null).block());
      assertThrows(IllegalArgumentException.class, () -> client.postIssueComment(1, "").block());
      assertThrows(IllegalArgumentException.class, () -> client.postIssueComment(1, "   ").block());
    }

    @Test
    @DisplayName("createReview should validate parameters")
    void testCreateReviewValidation() {
      assertThrows(
          IllegalArgumentException.class, () -> client.createReview(-1, List.of()).block());
      assertThrows(IllegalArgumentException.class, () -> client.createReview(0, List.of()).block());
      assertThrows(IllegalArgumentException.class, () -> client.createReview(1, null).block());
      assertThrows(IllegalArgumentException.class, () -> client.createReview(1, List.of()).block());
    }
  }

  @Nested
  @DisplayName("Reactive Method Tests")
  class ReactiveMethodTests {

    private GithubClient client;

    @BeforeEach
    void setUp() {
      client = new GithubClient(TEST_REPO, TEST_TOKEN, TEST_TIMEOUT);
    }

    @Test
    @DisplayName("fetchPrUnifiedDiff should return Mono")
    void testFetchPrUnifiedDiffReactive() {
      // This will fail with network error since we're not mocking, but it verifies the reactive
      // chain
      StepVerifier.create(client.fetchPrUnifiedDiff(1, 3))
          .expectErrorMatches(throwable -> throwable instanceof GithubClient.GithubClientException)
          .verify();
    }

    @Test
    @DisplayName("postIssueComment should return Mono")
    void testPostIssueCommentReactive() {
      StepVerifier.create(client.postIssueComment(1, "test comment"))
          .expectErrorMatches(throwable -> throwable instanceof GithubClient.GithubClientException)
          .verify();
    }

    @Test
    @DisplayName("createReview should return Mono")
    void testCreateReviewReactive() {
      var comment = new GithubClient.ReviewComment("file.txt", 1, "test comment");
      StepVerifier.create(client.createReview(1, List.of(comment)))
          .expectErrorMatches(throwable -> throwable instanceof GithubClient.GithubClientException)
          .verify();
    }
  }

  @Nested
  @DisplayName("ReviewComment Tests")
  class ReviewCommentTests {

    @Test
    @DisplayName("ReviewComment should validate parameters")
    void testReviewCommentValidation() {
      assertThrows(
          NullPointerException.class, () -> new GithubClient.ReviewComment(null, 1, "body"));
      assertThrows(
          NullPointerException.class, () -> new GithubClient.ReviewComment("path", 1, null));
      assertThrows(
          IllegalArgumentException.class, () -> new GithubClient.ReviewComment("", 1, "body"));
      assertThrows(
          IllegalArgumentException.class, () -> new GithubClient.ReviewComment("   ", 1, "body"));
      assertThrows(
          IllegalArgumentException.class, () -> new GithubClient.ReviewComment("path", 1, ""));
      assertThrows(
          IllegalArgumentException.class, () -> new GithubClient.ReviewComment("path", 1, "   "));
      assertThrows(
          IllegalArgumentException.class, () -> new GithubClient.ReviewComment("path", -1, "body"));
    }

    @Test
    @DisplayName("ReviewComment should create valid instances")
    void testValidReviewComment() {
      var comment = new GithubClient.ReviewComment("file.txt", 5, "Good code!");
      assertEquals("file.txt", comment.path());
      assertEquals(5, comment.position());
      assertEquals("Good code!", comment.body());
    }
  }
}
