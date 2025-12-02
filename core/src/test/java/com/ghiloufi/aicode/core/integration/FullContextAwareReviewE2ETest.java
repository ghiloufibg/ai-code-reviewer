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
import com.ghiloufi.aicode.core.domain.model.ChangeRequestIdentifier;
import com.ghiloufi.aicode.core.domain.model.CommitInfo;
import com.ghiloufi.aicode.core.domain.model.CommitResult;
import com.ghiloufi.aicode.core.domain.model.ContextMatch;
import com.ghiloufi.aicode.core.domain.model.ContextRetrievalMetadata;
import com.ghiloufi.aicode.core.domain.model.ContextRetrievalResult;
import com.ghiloufi.aicode.core.domain.model.DiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.DiffExpansionResult;
import com.ghiloufi.aicode.core.domain.model.EnrichedDiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.ExpandedFileContext;
import com.ghiloufi.aicode.core.domain.model.GitDiffDocument;
import com.ghiloufi.aicode.core.domain.model.MatchReason;
import com.ghiloufi.aicode.core.domain.model.MergeRequestSummary;
import com.ghiloufi.aicode.core.domain.model.PolicyDocument;
import com.ghiloufi.aicode.core.domain.model.PolicyType;
import com.ghiloufi.aicode.core.domain.model.PrMetadata;
import com.ghiloufi.aicode.core.domain.model.PullRequestId;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.model.RepositoryInfo;
import com.ghiloufi.aicode.core.domain.model.RepositoryPolicies;
import com.ghiloufi.aicode.core.domain.model.ReviewConfiguration;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import com.ghiloufi.aicode.core.domain.model.TicketBusinessContext;
import com.ghiloufi.aicode.core.domain.port.output.ContextRetrievalStrategy;
import com.ghiloufi.aicode.core.domain.port.output.SCMPort;
import com.ghiloufi.aicode.core.domain.service.DiffFormatter;
import com.ghiloufi.aicode.core.service.diff.UnifiedDiffParser;
import com.ghiloufi.aicode.core.service.expansion.DiffExpansionService;
import com.ghiloufi.aicode.core.service.metadata.PrMetadataExtractor;
import com.ghiloufi.aicode.core.service.policy.RepositoryPolicyProvider;
import com.ghiloufi.aicode.core.service.prompt.PromptBuilder;
import com.ghiloufi.aicode.core.service.prompt.PromptTemplateService;
import com.ghiloufi.aicode.core.service.prompt.ReviewPromptResult;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("Full Context-Aware Review E2E Integration Test")
final class FullContextAwareReviewE2ETest {

  private UnifiedDiffParser diffParser;
  private PromptBuilder promptBuilder;
  private ContextOrchestrator contextOrchestrator;
  private DiffExpansionService diffExpansionService;
  private RepositoryPolicyProvider repositoryPolicyProvider;
  private PrMetadataExtractor prMetadataExtractor;
  private RepositoryIdentifier testRepo;
  private ContextRetrievalConfig config;
  private TestSCMPort testSCMPort;

  @BeforeEach
  final void setUp() {
    diffParser = new UnifiedDiffParser();

    final PromptProperties currentProperties =
        new PromptProperties(
            "test system prompt",
            "test fix instructions",
            "test confidence",
            "test schema",
            "test output");
    final OptimizedPromptProperties optimizedProperties =
        new OptimizedPromptProperties(
            "test system prompt",
            "test fix instructions",
            "test confidence",
            "test schema",
            "",
            "test output");
    final PromptVariantProperties variantProperties = new PromptVariantProperties(Variant.CURRENT);
    final PromptPropertiesFactory factory =
        new PromptPropertiesFactory(variantProperties, currentProperties, optimizedProperties);
    final PromptTemplateService promptTemplateService = new PromptTemplateService(factory);
    promptBuilder = new PromptBuilder(new DiffFormatter(), promptTemplateService);

    config = createFullConfig();

    final TestContextRetrievalStrategy testStrategy = new TestContextRetrievalStrategy();
    final ContextEnricher contextEnricher = new ContextEnricher();
    contextOrchestrator = new ContextOrchestrator(List.of(testStrategy), contextEnricher, config);

    testSCMPort = new TestSCMPort();
    diffExpansionService = new DiffExpansionService(testSCMPort, config);
    repositoryPolicyProvider = new RepositoryPolicyProvider(testSCMPort, config);
    prMetadataExtractor = new PrMetadataExtractor(testSCMPort, config);

    testRepo = RepositoryIdentifier.create(SourceProvider.GITHUB, "example/user-service");
  }

  @Nested
  @DisplayName("Full Workflow Integration")
  final class FullWorkflowIntegrationTest {

    @Test
    @DisplayName("should_build_complete_prompt_with_all_context_components")
    final void should_build_complete_prompt_with_all_context_components() throws Exception {
      final String diff = loadDiffFromClasspath("diff-samples/medium-pr.diff");
      final GitDiffDocument parsedDiff = diffParser.parse(diff);
      final DiffAnalysisBundle diffBundle =
          new DiffAnalysisBundle(testRepo, parsedDiff, diff, null);

      final Mono<EnrichedDiffAnalysisBundle> enrichedMono =
          contextOrchestrator.retrieveEnrichedContext(diffBundle);

      StepVerifier.create(enrichedMono)
          .assertNext(
              enrichedBundle -> {
                final DiffExpansionResult expansionResult =
                    diffExpansionService.expandDiff(diffBundle).block(Duration.ofSeconds(5));
                final RepositoryPolicies policies =
                    repositoryPolicyProvider.getPolicies(testRepo).block(Duration.ofSeconds(5));
                final PrMetadata prMetadata = createTestPrMetadata();

                final ReviewPromptResult result =
                    promptBuilder.buildStructuredReviewPrompt(
                        enrichedBundle,
                        ReviewConfiguration.defaults(),
                        TicketBusinessContext.empty(),
                        expansionResult,
                        prMetadata,
                        policies);
                final String userPrompt = result.userPrompt();

                assertThat(userPrompt).contains("[DIFF]");
                assertThat(userPrompt).contains("[/DIFF]");

                assertThat(userPrompt).contains("[CONTEXT]");
                assertThat(userPrompt).contains("test-strategy");

                assertThat(userPrompt).contains("[PR_METADATA]");
                assertThat(userPrompt).contains("Pull Request: Feature: Add user authentication");
                assertThat(userPrompt).contains("Author: john.doe");
                assertThat(userPrompt).contains("Branch: feature/auth → main");
                assertThat(userPrompt).contains("Labels: enhancement, backend");
                assertThat(userPrompt).contains("Recent Commits:");
                assertThat(userPrompt).contains("abc123: Add authentication service");

                assertThat(userPrompt).contains("[EXPANDED_FILES]");
                assertThat(userPrompt).contains("Full content of modified files");

                assertThat(userPrompt).contains("[POLICIES]");
                assertThat(userPrompt).contains("CONTRIBUTING.md");
                assertThat(userPrompt).contains("Repository guidelines");
              })
          .expectComplete()
          .verify(Duration.ofSeconds(10));
    }

    @Test
    @DisplayName("should_handle_ticket_context_in_full_workflow")
    final void should_handle_ticket_context_in_full_workflow() throws Exception {
      final String diff = loadDiffFromClasspath("diff-samples/medium-pr.diff");
      final GitDiffDocument parsedDiff = diffParser.parse(diff);
      final DiffAnalysisBundle diffBundle =
          new DiffAnalysisBundle(testRepo, parsedDiff, diff, null);

      final Mono<EnrichedDiffAnalysisBundle> enrichedMono =
          contextOrchestrator.retrieveEnrichedContext(diffBundle);

      StepVerifier.create(enrichedMono)
          .assertNext(
              enrichedBundle -> {
                final TicketBusinessContext ticketContext =
                    new TicketBusinessContext(
                        "JIRA-123",
                        "User Authentication Feature",
                        "Implement OAuth2 authentication flow");

                final ReviewPromptResult result =
                    promptBuilder.buildStructuredReviewPrompt(
                        enrichedBundle,
                        ReviewConfiguration.defaults(),
                        ticketContext,
                        DiffExpansionResult.empty(),
                        PrMetadata.empty(),
                        RepositoryPolicies.empty());
                final String userPrompt = result.userPrompt();

                assertThat(userPrompt).contains("BUSINESS CONTEXT FROM TICKET: JIRA-123");
                assertThat(userPrompt).contains("Feature/Fix: User Authentication Feature");
                assertThat(userPrompt).contains("Implement OAuth2 authentication flow");
                assertThat(userPrompt).contains("[REVIEW_FOCUS]");
                assertThat(userPrompt)
                    .contains("Verify code implements business requirements from ticket");
              })
          .expectComplete()
          .verify(Duration.ofSeconds(5));
    }
  }

  @Nested
  @DisplayName("Diff Expansion Integration")
  final class DiffExpansionIntegrationTest {

    @Test
    @DisplayName("should_expand_diff_with_file_contents")
    final void should_expand_diff_with_file_contents() throws Exception {
      final String diff = loadDiffFromClasspath("diff-samples/medium-pr.diff");
      final GitDiffDocument parsedDiff = diffParser.parse(diff);
      final DiffAnalysisBundle diffBundle =
          new DiffAnalysisBundle(testRepo, parsedDiff, diff, null);

      final Mono<DiffExpansionResult> expansionMono = diffExpansionService.expandDiff(diffBundle);

      StepVerifier.create(expansionMono)
          .assertNext(
              expansionResult -> {
                assertThat(expansionResult.hasExpandedFiles()).isTrue();
                assertThat(expansionResult.filesExpanded()).isGreaterThan(0);

                final ReviewPromptResult promptResult =
                    promptBuilder.buildStructuredReviewPrompt(
                        new EnrichedDiffAnalysisBundle(diffBundle),
                        ReviewConfiguration.defaults(),
                        TicketBusinessContext.empty(),
                        expansionResult,
                        PrMetadata.empty(),
                        RepositoryPolicies.empty());
                final String userPrompt = promptResult.userPrompt();

                assertThat(userPrompt).contains("[EXPANDED_FILES]");
                assertThat(userPrompt).contains("Full content of modified files");
                assertThat(userPrompt).contains("--- FILE:");
                assertThat(userPrompt).contains("--- END FILE ---");
              })
          .expectComplete()
          .verify(Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("should_handle_truncated_files_in_expansion")
    final void should_handle_truncated_files_in_expansion() {
      final ExpandedFileContext truncatedFile =
          ExpandedFileContext.truncated("src/LargeFile.java", "// First 500 lines...", 1500);
      final DiffExpansionResult expansionResult =
          new DiffExpansionResult(List.of(truncatedFile), 1, 1, 0, null);

      final ReviewPromptResult result =
          promptBuilder.buildStructuredReviewPrompt(
              createMinimalEnrichedBundle(),
              ReviewConfiguration.defaults(),
              TicketBusinessContext.empty(),
              expansionResult,
              PrMetadata.empty(),
              RepositoryPolicies.empty());
      final String userPrompt = result.userPrompt();

      assertThat(userPrompt).contains("[EXPANDED_FILES]");
      assertThat(userPrompt).contains("src/LargeFile.java");
      assertThat(userPrompt).contains("(truncated from 1500 lines)");
    }

    @Test
    @DisplayName("should_return_disabled_result_when_expansion_disabled")
    final void should_return_disabled_result_when_expansion_disabled() throws Exception {
      final ContextRetrievalConfig disabledConfig =
          new ContextRetrievalConfig(
              true,
              30,
              List.of("test-strategy"),
              new ContextRetrievalConfig.RolloutConfig(100, false, 1000),
              new ContextRetrievalConfig.DiffExpansionConfig(false, 100, 500, 10, Set.of()),
              null,
              null);

      final DiffExpansionService disabledService =
          new DiffExpansionService(testSCMPort, disabledConfig);
      final String diff = loadDiffFromClasspath("diff-samples/medium-pr.diff");
      final GitDiffDocument parsedDiff = diffParser.parse(diff);
      final DiffAnalysisBundle diffBundle =
          new DiffAnalysisBundle(testRepo, parsedDiff, diff, null);

      final Mono<DiffExpansionResult> expansionMono = disabledService.expandDiff(diffBundle);

      StepVerifier.create(expansionMono)
          .assertNext(
              result -> {
                assertThat(result.hasExpandedFiles()).isFalse();
                assertThat(result.skipReason()).isEqualTo("Feature disabled");
              })
          .expectComplete()
          .verify(Duration.ofSeconds(5));
    }
  }

  @Nested
  @DisplayName("Repository Policies Integration")
  final class RepositoryPoliciesIntegrationTest {

    @Test
    @DisplayName("should_fetch_and_include_repository_policies")
    final void should_fetch_and_include_repository_policies() {
      final Mono<RepositoryPolicies> policiesMono = repositoryPolicyProvider.getPolicies(testRepo);

      StepVerifier.create(policiesMono)
          .assertNext(
              policies -> {
                assertThat(policies.hasPolicies()).isTrue();
                assertThat(policies.contributingGuide()).isNotNull();
                assertThat(policies.contributingGuide().name()).isEqualTo("CONTRIBUTING.md");
                assertThat(policies.contributingGuide().hasContent()).isTrue();

                final ReviewPromptResult result =
                    promptBuilder.buildStructuredReviewPrompt(
                        createMinimalEnrichedBundle(),
                        ReviewConfiguration.defaults(),
                        TicketBusinessContext.empty(),
                        DiffExpansionResult.empty(),
                        PrMetadata.empty(),
                        policies);
                final String userPrompt = result.userPrompt();

                assertThat(userPrompt).contains("[POLICIES]");
                assertThat(userPrompt).contains("Repository guidelines to consider during review");
                assertThat(userPrompt).contains("CONTRIBUTING.md");
              })
          .expectComplete()
          .verify(Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("should_include_multiple_policy_types")
    final void should_include_multiple_policy_types() {
      final PolicyDocument contributing =
          new PolicyDocument(
              "CONTRIBUTING.md",
              "CONTRIBUTING.md",
              "# How to contribute",
              PolicyType.CONTRIBUTING,
              false);
      final PolicyDocument security =
          new PolicyDocument(
              "SECURITY.md", "SECURITY.md", "# Security Policy", PolicyType.SECURITY, false);
      final PolicyDocument prTemplate =
          new PolicyDocument(
              "PULL_REQUEST_TEMPLATE.md",
              ".github/PULL_REQUEST_TEMPLATE.md",
              "## Description",
              PolicyType.PR_TEMPLATE,
              false);

      final RepositoryPolicies policies =
          new RepositoryPolicies(contributing, null, prTemplate, security);

      final ReviewPromptResult result =
          promptBuilder.buildStructuredReviewPrompt(
              createMinimalEnrichedBundle(),
              ReviewConfiguration.defaults(),
              TicketBusinessContext.empty(),
              DiffExpansionResult.empty(),
              PrMetadata.empty(),
              policies);
      final String userPrompt = result.userPrompt();

      assertThat(userPrompt).contains("[POLICIES]");
      assertThat(userPrompt).contains("CONTRIBUTING.md");
      assertThat(userPrompt).contains("SECURITY.md");
      assertThat(userPrompt).contains("PULL_REQUEST_TEMPLATE.md");
      assertThat(userPrompt).contains("# How to contribute");
      assertThat(userPrompt).contains("# Security Policy");
      assertThat(userPrompt).contains("## Description");
    }

    @Test
    @DisplayName("should_return_empty_policies_when_disabled")
    final void should_return_empty_policies_when_disabled() {
      final ContextRetrievalConfig disabledConfig =
          new ContextRetrievalConfig(
              true,
              30,
              List.of("test-strategy"),
              new ContextRetrievalConfig.RolloutConfig(100, false, 1000),
              null,
              null,
              new ContextRetrievalConfig.RepositoryPoliciesConfig(
                  false, 5000, true, true, true, true));

      final RepositoryPolicyProvider disabledProvider =
          new RepositoryPolicyProvider(testSCMPort, disabledConfig);

      final Mono<RepositoryPolicies> policiesMono = disabledProvider.getPolicies(testRepo);

      StepVerifier.create(policiesMono)
          .assertNext(
              policies -> {
                assertThat(policies.hasPolicies()).isFalse();
              })
          .expectComplete()
          .verify(Duration.ofSeconds(5));
    }
  }

  @Nested
  @DisplayName("PR Metadata Integration")
  final class PrMetadataIntegrationTest {

    @Test
    @DisplayName("should_extract_and_include_pr_metadata")
    final void should_extract_and_include_pr_metadata() {
      final PullRequestId changeRequest = new PullRequestId(42);

      final Mono<PrMetadata> metadataMono =
          prMetadataExtractor.extractMetadata(testRepo, changeRequest);

      StepVerifier.create(metadataMono)
          .assertNext(
              metadata -> {
                assertThat(metadata.title()).isEqualTo("Feature: Add user authentication");
                assertThat(metadata.author()).isEqualTo("john.doe");
                assertThat(metadata.hasLabels()).isTrue();
                assertThat(metadata.hasCommits()).isTrue();

                final ReviewPromptResult result =
                    promptBuilder.buildStructuredReviewPrompt(
                        createMinimalEnrichedBundle(),
                        ReviewConfiguration.defaults(),
                        TicketBusinessContext.empty(),
                        DiffExpansionResult.empty(),
                        metadata,
                        RepositoryPolicies.empty());
                final String userPrompt = result.userPrompt();

                assertThat(userPrompt).contains("[PR_METADATA]");
                assertThat(userPrompt).contains("Pull Request: Feature: Add user authentication");
                assertThat(userPrompt).contains("Author: john.doe");
                assertThat(userPrompt).contains("Labels: enhancement, backend");
              })
          .expectComplete()
          .verify(Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("should_include_commit_history_in_metadata")
    final void should_include_commit_history_in_metadata() {
      final PrMetadata metadata = createTestPrMetadata();

      final ReviewPromptResult result =
          promptBuilder.buildStructuredReviewPrompt(
              createMinimalEnrichedBundle(),
              ReviewConfiguration.defaults(),
              TicketBusinessContext.empty(),
              DiffExpansionResult.empty(),
              metadata,
              RepositoryPolicies.empty());
      final String userPrompt = result.userPrompt();

      assertThat(userPrompt).contains("Recent Commits:");
      assertThat(userPrompt).contains("abc123: Add authentication service");
      assertThat(userPrompt).contains("def456: Add unit tests");
    }

    @Test
    @DisplayName("should_show_branch_information")
    final void should_show_branch_information() {
      final PrMetadata metadata = createTestPrMetadata();

      final ReviewPromptResult result =
          promptBuilder.buildStructuredReviewPrompt(
              createMinimalEnrichedBundle(),
              ReviewConfiguration.defaults(),
              TicketBusinessContext.empty(),
              DiffExpansionResult.empty(),
              metadata,
              RepositoryPolicies.empty());
      final String userPrompt = result.userPrompt();

      assertThat(userPrompt).contains("Branch: feature/auth → main");
    }
  }

  @Nested
  @DisplayName("Combined Context Integration")
  final class CombinedContextIntegrationTest {

    @Test
    @DisplayName("should_combine_all_context_sources_in_prompt")
    final void should_combine_all_context_sources_in_prompt() throws Exception {
      final String diff = loadDiffFromClasspath("diff-samples/medium-pr.diff");
      final GitDiffDocument parsedDiff = diffParser.parse(diff);
      final DiffAnalysisBundle diffBundle =
          new DiffAnalysisBundle(testRepo, parsedDiff, diff, null);

      final EnrichedDiffAnalysisBundle enrichedBundle =
          contextOrchestrator.retrieveEnrichedContext(diffBundle).block(Duration.ofSeconds(5));

      final DiffExpansionResult expansionResult =
          diffExpansionService.expandDiff(diffBundle).block(Duration.ofSeconds(5));

      final RepositoryPolicies policies =
          repositoryPolicyProvider.getPolicies(testRepo).block(Duration.ofSeconds(5));

      final PrMetadata prMetadata = createTestPrMetadata();

      final TicketBusinessContext ticketContext =
          new TicketBusinessContext(
              "JIRA-456", "User Auth Feature", "Implement secure user authentication");

      final ReviewPromptResult result =
          promptBuilder.buildStructuredReviewPrompt(
              enrichedBundle,
              ReviewConfiguration.defaults(),
              ticketContext,
              expansionResult,
              prMetadata,
              policies);
      final String userPrompt = result.userPrompt();

      assertThat(userPrompt).contains("BUSINESS CONTEXT FROM TICKET: JIRA-456");
      assertThat(userPrompt).contains("[PR_METADATA]");
      assertThat(userPrompt).contains("[DIFF]");
      assertThat(userPrompt).contains("[CONTEXT]");
      assertThat(userPrompt).contains("[EXPANDED_FILES]");
      assertThat(userPrompt).contains("[POLICIES]");
      assertThat(userPrompt).contains("[REVIEW_FOCUS]");

      final int ticketIndex = userPrompt.indexOf("BUSINESS CONTEXT FROM TICKET");
      final int prMetadataIndex = userPrompt.indexOf("[PR_METADATA]");
      final int diffIndex = userPrompt.indexOf("[DIFF]");

      assertThat(ticketIndex).isLessThan(prMetadataIndex);
      assertThat(prMetadataIndex).isLessThan(diffIndex);
    }

    @Test
    @DisplayName("should_handle_partial_context_availability")
    final void should_handle_partial_context_availability() {
      final ReviewPromptResult result =
          promptBuilder.buildStructuredReviewPrompt(
              createMinimalEnrichedBundle(),
              ReviewConfiguration.defaults(),
              TicketBusinessContext.empty(),
              DiffExpansionResult.empty(),
              createTestPrMetadata(),
              RepositoryPolicies.empty());
      final String userPrompt = result.userPrompt();

      assertThat(userPrompt).contains("[PR_METADATA]");
      assertThat(userPrompt).contains("[DIFF]");
      assertThat(userPrompt).doesNotContain("[CONTEXT]");
      assertThat(userPrompt).doesNotContain("[EXPANDED_FILES]");
      assertThat(userPrompt).doesNotContain("[POLICIES]");
    }

    @Test
    @DisplayName("should_handle_all_context_empty")
    final void should_handle_all_context_empty() {
      final ReviewPromptResult result =
          promptBuilder.buildStructuredReviewPrompt(
              createMinimalEnrichedBundle(),
              ReviewConfiguration.defaults(),
              TicketBusinessContext.empty(),
              DiffExpansionResult.empty(),
              PrMetadata.empty(),
              RepositoryPolicies.empty());
      final String userPrompt = result.userPrompt();

      assertThat(userPrompt).contains("[DIFF]");
      assertThat(userPrompt).doesNotContain("[PR_METADATA]");
      assertThat(userPrompt).doesNotContain("[CONTEXT]");
      assertThat(userPrompt).doesNotContain("[EXPANDED_FILES]");
      assertThat(userPrompt).doesNotContain("[POLICIES]");
      assertThat(userPrompt).doesNotContain("[REVIEW_FOCUS]");
    }
  }

  private ContextRetrievalConfig createFullConfig() {
    return new ContextRetrievalConfig(
        true,
        30,
        List.of("test-strategy"),
        new ContextRetrievalConfig.RolloutConfig(100, false, 1000),
        new ContextRetrievalConfig.DiffExpansionConfig(true, 100, 500, 10, Set.of(".lock", ".svg")),
        new ContextRetrievalConfig.PrMetadataConfig(true, true, true, true, 5),
        new ContextRetrievalConfig.RepositoryPoliciesConfig(true, 5000, true, false, true, true));
  }

  private PrMetadata createTestPrMetadata() {
    final List<CommitInfo> commits =
        List.of(
            new CommitInfo(
                "abc123", "Add authentication service", "john.doe", Instant.now(), List.of()),
            new CommitInfo("def456", "Add unit tests", "john.doe", Instant.now(), List.of()));

    return new PrMetadata(
        "Feature: Add user authentication",
        "This PR implements OAuth2 authentication",
        "john.doe",
        "main",
        "feature/auth",
        List.of("enhancement", "backend"),
        commits,
        5);
  }

  private EnrichedDiffAnalysisBundle createMinimalEnrichedBundle() {
    final GitDiffDocument minimalDiff = new GitDiffDocument(List.of());
    final DiffAnalysisBundle basicBundle =
        new DiffAnalysisBundle(
            testRepo,
            minimalDiff,
            "--- a/test.java\n+++ b/test.java\n@@ -1 +1 @@\n-old\n+new",
            null);
    return new EnrichedDiffAnalysisBundle(basicBundle);
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

  private static final class TestSCMPort implements SCMPort {

    @Override
    public Mono<String> getFileContent(final RepositoryIdentifier repo, final String filePath) {
      if (filePath.endsWith("CONTRIBUTING.md")) {
        return Mono.just(
            "# Contributing Guide\n\nPlease follow these guidelines when contributing.");
      }
      if (filePath.endsWith("SECURITY.md")) {
        return Mono.just("# Security Policy\n\nReport vulnerabilities to security@example.com");
      }
      if (filePath.endsWith("PULL_REQUEST_TEMPLATE.md")
          || filePath.endsWith("pull_request_template.md")) {
        return Mono.just("## Description\n\nPlease include a summary of the change.");
      }
      if (filePath.endsWith(".java")) {
        return Mono.just(
            """
            package com.example;

            public class TestClass {
                public void doSomething() {
                    // implementation
                }
            }
            """);
      }
      return Mono.error(new RuntimeException("File not found: " + filePath));
    }

    @Override
    public Mono<PrMetadata> getPullRequestMetadata(
        final RepositoryIdentifier repo, final ChangeRequestIdentifier changeRequest) {
      final List<CommitInfo> commits =
          List.of(
              new CommitInfo(
                  "abc123", "Add authentication service", "john.doe", Instant.now(), List.of()),
              new CommitInfo("def456", "Add unit tests", "john.doe", Instant.now(), List.of()));

      return Mono.just(
          new PrMetadata(
              "Feature: Add user authentication",
              "This PR implements OAuth2 authentication",
              "john.doe",
              "main",
              "feature/auth",
              List.of("enhancement", "backend"),
              commits,
              5));
    }

    @Override
    public Mono<DiffAnalysisBundle> getDiff(
        final RepositoryIdentifier repo, final ChangeRequestIdentifier changeRequest) {
      return Mono.empty();
    }

    @Override
    public Mono<Void> publishReview(
        final RepositoryIdentifier repo,
        final ChangeRequestIdentifier changeRequest,
        final ReviewResult reviewResult) {
      return Mono.empty();
    }

    @Override
    public Mono<Void> publishSummaryComment(
        final RepositoryIdentifier repo,
        final ChangeRequestIdentifier changeRequest,
        final String summaryComment) {
      return Mono.empty();
    }

    @Override
    public Mono<Boolean> isChangeRequestOpen(
        final RepositoryIdentifier repo, final ChangeRequestIdentifier changeRequest) {
      return Mono.just(true);
    }

    @Override
    public Mono<RepositoryInfo> getRepository(final RepositoryIdentifier repo) {
      return Mono.empty();
    }

    @Override
    public Flux<MergeRequestSummary> getOpenChangeRequests(final RepositoryIdentifier repo) {
      return Flux.empty();
    }

    @Override
    public Flux<RepositoryInfo> getAllRepositories() {
      return Flux.empty();
    }

    @Override
    public SourceProvider getProviderType() {
      return SourceProvider.GITHUB;
    }

    @Override
    public Mono<CommitResult> applyFix(
        final RepositoryIdentifier repo,
        final String branchName,
        final String filePath,
        final String fixDiff,
        final String commitMessage) {
      return Mono.empty();
    }

    @Override
    public Mono<Boolean> hasWriteAccess(final RepositoryIdentifier repo) {
      return Mono.just(true);
    }

    @Override
    public Mono<List<String>> listRepositoryFiles() {
      return Mono.just(List.of());
    }

    @Override
    public Flux<CommitInfo> getCommitsFor(
        final RepositoryIdentifier repo,
        final String filePath,
        final LocalDate since,
        final int maxResults) {
      return Flux.empty();
    }

    @Override
    public Flux<CommitInfo> getCommitsSince(
        final RepositoryIdentifier repo, final LocalDate since, final int maxResults) {
      return Flux.empty();
    }
  }
}
