package com.ghiloufi.aicode.core.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.application.service.context.ContextEnricher;
import com.ghiloufi.aicode.core.application.service.context.ContextOrchestrator;
import com.ghiloufi.aicode.core.config.ContextRetrievalConfig;
import com.ghiloufi.aicode.core.config.OptimizedPromptProperties;
import com.ghiloufi.aicode.core.config.PromptProperties;
import com.ghiloufi.aicode.core.config.PromptPropertiesFactory;
import com.ghiloufi.aicode.core.config.PromptVariantProperties;
import com.ghiloufi.aicode.core.config.PromptVariantProperties.Variant;
import com.ghiloufi.aicode.core.domain.model.*;
import com.ghiloufi.aicode.core.domain.port.output.ContextRetrievalStrategy;
import com.ghiloufi.aicode.core.domain.service.DiffFormatter;
import com.ghiloufi.aicode.core.service.diff.UnifiedDiffParser;
import com.ghiloufi.aicode.core.service.prompt.PromptBuilder;
import com.ghiloufi.aicode.core.service.prompt.PromptTemplateService;
import com.ghiloufi.aicode.core.service.prompt.ReviewPromptResult;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("Context-Aware Review Workflow Integration Test")
final class ContextAwareReviewWorkflowTest {

  private UnifiedDiffParser diffParser;
  private PromptBuilder promptBuilder;
  private ContextOrchestrator contextOrchestrator;
  private RepositoryIdentifier testRepo;

  @BeforeEach
  final void setUp() {
    diffParser = new UnifiedDiffParser();
    final PromptProperties currentProperties =
        new PromptProperties(
            "test system", "test fix", "test confidence", "test schema", "test output");
    final OptimizedPromptProperties optimizedProperties =
        new OptimizedPromptProperties(
            "test system", "test fix", "test confidence", "test schema", "test output");
    final PromptVariantProperties variantProperties = new PromptVariantProperties(Variant.CURRENT);
    final PromptPropertiesFactory factory =
        new PromptPropertiesFactory(variantProperties, currentProperties, optimizedProperties);
    final PromptTemplateService promptTemplateService = new PromptTemplateService(factory);
    promptBuilder = new PromptBuilder(new DiffFormatter(), promptTemplateService);

    final TestContextRetrievalStrategy testStrategy = new TestContextRetrievalStrategy();
    final ContextEnricher contextEnricher = new ContextEnricher();
    final ContextRetrievalConfig config =
        new ContextRetrievalConfig(
            true,
            30,
            List.of("test-strategy"),
            new ContextRetrievalConfig.RolloutConfig(100, false, 1000),
            null,
            null,
            null);

    contextOrchestrator = new ContextOrchestrator(List.of(testStrategy), contextEnricher, config);

    testRepo = RepositoryIdentifier.create(SourceProvider.GITLAB, "example/user-service");
  }

  @Test
  @DisplayName("should_enrich_medium_pr_with_context_and_include_in_prompt")
  final void should_enrich_medium_pr_with_context_and_include_in_prompt() throws Exception {
    final String mediumPRDiff = loadDiffFromClasspath("diff-samples/medium-pr.diff");

    final GitDiffDocument parsedDiff = diffParser.parse(mediumPRDiff);
    final DiffAnalysisBundle diffBundle =
        new DiffAnalysisBundle(testRepo, parsedDiff, mediumPRDiff, null);

    final Mono<EnrichedDiffAnalysisBundle> enrichedMono =
        contextOrchestrator.retrieveEnrichedContext(diffBundle);

    StepVerifier.create(enrichedMono)
        .assertNext(
            enrichedBundle -> {
              assertThat(enrichedBundle).isNotNull();
              assertThat(enrichedBundle.hasContext()).isTrue();
              assertThat(enrichedBundle.getContextMatchCount()).isEqualTo(4);

              final ReviewPromptResult result =
                  promptBuilder.buildStructuredReviewPrompt(
                      enrichedBundle,
                      ReviewConfiguration.defaults(),
                      TicketBusinessContext.empty());
              final String userPrompt = result.userPrompt();

              assertThat(userPrompt).contains("[CONTEXT]");
              assertThat(userPrompt)
                  .contains("Relevant files identified by context analysis (test-strategy)");
              assertThat(userPrompt)
                  .contains("src/main/java/com/example/repository/UserRepository.java");
              assertThat(userPrompt).contains("confidence: 0.95");
              assertThat(userPrompt).contains("reason: Direct import");
              assertThat(userPrompt).contains("Evidence: imported in UserService.java");

              assertThat(userPrompt).contains("src/main/java/com/example/model/User.java");
              assertThat(userPrompt).contains("confidence: 0.90");

              assertThat(userPrompt)
                  .contains("src/main/java/com/example/exception/UserNotFoundException.java");
              assertThat(userPrompt).contains("confidence: 0.85");

              assertThat(userPrompt).contains("src/test/java/com/example/service/UserServiceTest.java");
              assertThat(userPrompt).contains("confidence: 0.80");

              assertThat(userPrompt)
                  .contains(
                      "These files may provide important context for understanding the changes");

              assertThat(userPrompt).contains("[DIFF]");
              assertThat(userPrompt).contains("UserService.java");
            })
        .expectComplete()
        .verify(Duration.ofSeconds(5));
  }

  @Test
  @DisplayName("should_handle_large_pr_diff_and_skip_context_retrieval_when_exceeds_limit")
  final void should_handle_large_pr_diff_and_skip_context_retrieval_when_exceeds_limit()
      throws Exception {
    final String largePRDiff = loadDiffFromClasspath("diff-samples/large-pr.diff");

    final GitDiffDocument parsedDiff = diffParser.parse(largePRDiff);
    final DiffAnalysisBundle diffBundle =
        new DiffAnalysisBundle(testRepo, parsedDiff, largePRDiff, null);

    final ContextRetrievalConfig configWithLowLimit =
        new ContextRetrievalConfig(
            true,
            30,
            List.of("test-strategy"),
            new ContextRetrievalConfig.RolloutConfig(100, true, 100),
            null,
            null,
            null);

    final ContextOrchestrator orchestratorWithLimit =
        new ContextOrchestrator(
            List.of(new TestContextRetrievalStrategy()), new ContextEnricher(), configWithLowLimit);

    final Mono<EnrichedDiffAnalysisBundle> enrichedMono =
        orchestratorWithLimit.retrieveEnrichedContext(diffBundle);

    StepVerifier.create(enrichedMono)
        .assertNext(
            enrichedBundle -> {
              assertThat(enrichedBundle).isNotNull();
              assertThat(enrichedBundle.getContextMatchCount()).isEqualTo(0);

              final ReviewPromptResult result =
                  promptBuilder.buildStructuredReviewPrompt(
                      enrichedBundle,
                      ReviewConfiguration.defaults(),
                      TicketBusinessContext.empty());
              final String userPrompt = result.userPrompt();

              assertThat(userPrompt).doesNotContain("[CONTEXT]");
              assertThat(userPrompt).contains("[DIFF]");
            })
        .expectComplete()
        .verify(Duration.ofSeconds(5));
  }

  @Test
  @DisplayName("should_complete_review_gracefully_when_context_retrieval_returns_no_matches")
  final void should_complete_review_gracefully_when_context_retrieval_returns_no_matches()
      throws Exception {
    final String mediumPRDiff = loadDiffFromClasspath("diff-samples/medium-pr.diff");

    final GitDiffDocument parsedDiff = diffParser.parse(mediumPRDiff);
    final DiffAnalysisBundle diffBundle =
        new DiffAnalysisBundle(testRepo, parsedDiff, mediumPRDiff, null);

    final TestContextRetrievalStrategy emptyStrategy = new TestContextRetrievalStrategy();
    emptyStrategy.setMatchesToReturn(List.of());

    final ContextOrchestrator orchestratorWithEmptyStrategy =
        new ContextOrchestrator(
            List.of(emptyStrategy),
            new ContextEnricher(),
            new ContextRetrievalConfig(
                true,
                30,
                List.of("test-strategy"),
                new ContextRetrievalConfig.RolloutConfig(100, false, 1000),
                null,
                null,
                null));

    final Mono<EnrichedDiffAnalysisBundle> enrichedMono =
        orchestratorWithEmptyStrategy.retrieveEnrichedContext(diffBundle);

    StepVerifier.create(enrichedMono)
        .assertNext(
            enrichedBundle -> {
              assertThat(enrichedBundle).isNotNull();
              assertThat(enrichedBundle.getContextMatchCount()).isEqualTo(0);

              final ReviewPromptResult result =
                  promptBuilder.buildStructuredReviewPrompt(
                      enrichedBundle,
                      ReviewConfiguration.defaults(),
                      TicketBusinessContext.empty());
              final String userPrompt = result.userPrompt();

              assertThat(userPrompt).doesNotContain("[CONTEXT]");
              assertThat(userPrompt).contains("[DIFF]");
            })
        .expectComplete()
        .verify(Duration.ofSeconds(5));
  }

  private String loadDiffFromClasspath(final String path) throws Exception {
    final ClassPathResource resource = new ClassPathResource(path);
    return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
  }

  private static final class TestContextRetrievalStrategy implements ContextRetrievalStrategy {
    private List<ContextMatch> matchesToReturn =
        List.of(
            new ContextMatch(
                "src/main/java/com/example/repository/UserRepository.java",
                MatchReason.DIRECT_IMPORT,
                0.95,
                "imported in UserService.java"),
            new ContextMatch(
                "src/main/java/com/example/model/User.java",
                MatchReason.TYPE_REFERENCE,
                0.90,
                "used as type in UserService"),
            new ContextMatch(
                "src/main/java/com/example/exception/UserNotFoundException.java",
                MatchReason.DIRECT_IMPORT,
                0.85,
                "imported in UserService.java"),
            new ContextMatch(
                "src/test/java/com/example/service/UserServiceTest.java",
                MatchReason.GIT_COCHANGE_HIGH,
                0.80,
                "frequently changed together with UserService"));

    void setMatchesToReturn(final List<ContextMatch> matches) {
      this.matchesToReturn = matches;
    }

    @Override
    public Mono<ContextRetrievalResult> retrieveContext(final DiffAnalysisBundle diffBundle) {
      final ContextRetrievalMetadata metadata =
          new ContextRetrievalMetadata(
              "test-strategy",
              Duration.ofMillis(50),
              matchesToReturn.size(),
              matchesToReturn.size(),
              Map.of());

      return Mono.just(new ContextRetrievalResult(matchesToReturn, metadata));
    }

    @Override
    public String getStrategyName() {
      return "test-strategy";
    }

    @Override
    public int getPriority() {
      return 100;
    }
  }
}
