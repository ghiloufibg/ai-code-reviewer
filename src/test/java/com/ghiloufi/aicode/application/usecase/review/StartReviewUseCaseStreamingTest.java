package com.ghiloufi.aicode.application.usecase.review;

import com.ghiloufi.aicode.application.command.StartReviewCommand;
import com.ghiloufi.aicode.application.port.output.DiffCollectionPort;
import com.ghiloufi.aicode.application.port.output.LlmAnalysisPort;
import com.ghiloufi.aicode.application.port.output.AIAnalysisPort;
import com.ghiloufi.aicode.application.port.output.StaticAnalysisPort;
import com.ghiloufi.aicode.application.port.output.ReviewPublishingPort;
import com.ghiloufi.aicode.domain.repository.ReviewRepository;
import com.ghiloufi.aicode.domain.service.ReviewPolicyService;
import com.ghiloufi.aicode.domain.entity.*;
import com.ghiloufi.aicode.domain.value.RepositoryInfo;
import com.ghiloufi.aicode.domain.value.ReviewConfiguration;
import com.ghiloufi.aicode.infrastructure.mock.MockLlmStreamingAdapter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration test validating the StartReviewUseCase works with streaming LLM responses.
 *
 * <p>This test ensures that:
 * 1. Clean Architecture ports work correctly with streaming
 * 2. Use case orchestration handles streaming responses properly
 * 3. Domain entities are created and updated correctly
 * 4. The full workflow from command to result works end-to-end
 */
@ExtendWith(MockitoExtension.class)
@Slf4j
class StartReviewUseCaseStreamingTest {

    @Mock
    private DiffCollectionPort diffCollectionPort;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private StaticAnalysisPort staticAnalysisPort;

    @Mock
    private ReviewPublishingPort reviewPublishingPort;

    @Mock
    private ReviewPolicyService reviewPolicyService;

    private AIAnalysisPort aiAnalysisPort;
    private StartReviewUseCase startReviewUseCase;

    @BeforeEach
    void setUp() {
        // Use mock streaming adapter for predictable testing - implements AIAnalysisPort
        aiAnalysisPort = new MockLlmStreamingAdapter();

        startReviewUseCase = new StartReviewUseCase(
            reviewRepository,
            diffCollectionPort,
            staticAnalysisPort,
            aiAnalysisPort,
            reviewPublishingPort,
            reviewPolicyService
        );
    }

    @Test
    void shouldExecuteFullReviewWorkflowWithStreamingLlm() {
        // Given: A review command and mock data
        StartReviewCommand command = createTestCommand();
        DiffAnalysis mockDiffAnalysis = createMockDiffAnalysis();
        CodeReview savedReview = createMockCodeReview();

        // Mock the dependencies
        when(diffCollectionPort.collectDiff(any())).thenReturn(Mono.just(mockDiffAnalysis));
        when(reviewRepository.save(any(CodeReview.class))).thenReturn(Mono.just(savedReview));
        when(staticAnalysisPort.isAvailable()).thenReturn(false); // Disable static analysis for this test
        when(reviewPublishingPort.supports(any())).thenReturn(false); // Disable publishing for this test

        // When: We execute the use case
        Mono<CodeReview> result = startReviewUseCase.execute(command);

        // Then: The full workflow should complete successfully with streaming LLM analysis
        StepVerifier.create(result)
            .assertNext(review -> {
                log.info("✅ Review completed with ID: {}", review.getId());

                // Verify basic review properties
                assertThat(review).isNotNull();
                assertThat(review.getId()).isNotNull();
                assertThat(review.getRepositoryInfo()).isEqualTo(command.repositoryInfo());
                assertThat(review.getConfiguration()).isEqualTo(command.configuration());

                // Verify review status progression (simplified check)
                assertThat(review.getStatus()).isNotNull();

                // Verify analysis results from streaming LLM
                assertThat(review.getAnalysisResults()).isNotEmpty();

                AnalysisResult llmResult = review.getAnalysisResults().stream()
                    .filter(result1 -> result1.getType() == AnalysisResult.AnalysisType.LLM_ANALYSIS)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("LLM analysis result should be present"));

                // Verify streaming LLM analysis content
                assertThat(llmResult.getSummary())
                    .contains("Code Review Summary")
                    .contains("Missing Error Handling")
                    .contains("Recommendations");

                assertThat(llmResult.getToolVersion()).isEqualTo("mock-llm-v1.0");
                assertThat(llmResult.getNotes())
                    .contains("Mock streaming analysis")
                    .hasSize(3);

                log.info("📝 LLM Analysis Summary: {}",
                    llmResult.getSummary().substring(0, Math.min(100, llmResult.getSummary().length())));
                log.info("🔧 Tool Version: {}", llmResult.getToolVersion());
                log.info("📋 Notes Count: {}", llmResult.getNotes().size());
            })
            .expectComplete()
            .verify(Duration.ofSeconds(10)); // Allow time for streaming to complete
    }

    @Test
    void shouldHandleStreamingAnalysisCorrectly() {
        // Given: Direct streaming analysis test
        DiffAnalysis diffAnalysis = createMockDiffAnalysis();
        ReviewConfiguration config = createTestConfiguration();

        // When: We perform streaming analysis
        Mono<AnalysisResult> analysisResult = aiAnalysisPort.analyze(diffAnalysis, config);

        // Then: We should get properly aggregated streaming results
        StepVerifier.create(analysisResult)
            .assertNext(result -> {
                log.info("🔍 Streaming Analysis Validation:");

                // Verify analysis type and metadata
                assertThat(result.getType()).isEqualTo(AnalysisResult.AnalysisType.LLM_ANALYSIS);
                assertThat(result.getAnalyzedAt()).isNotNull();
                assertThat(result.getToolVersion()).isEqualTo("mock-llm-v1.0");

                // Verify streaming content was properly aggregated
                String summary = result.getSummary();
                assertThat(summary)
                    .isNotEmpty()
                    .contains("## Code Review Summary")
                    .contains("### Changes Detected:")
                    .contains("### Issues Found:")
                    .contains("### Recommendations:")
                    .contains("Overall, the changes look good");

                // Verify streaming metadata in notes
                assertThat(result.getNotes())
                    .contains("Mock streaming analysis")
                    .anyMatch(note -> note.contains("chunks"));

                log.info("   ✅ Analysis type: {}", result.getType());
                log.info("   ✅ Summary length: {} characters", summary.length());
                log.info("   ✅ Notes count: {}", result.getNotes().size());
                log.info("   ✅ Streaming aggregation: SUCCESSFUL");
            })
            .expectComplete()
            .verify(Duration.ofSeconds(5));
    }

    @Test
    void shouldValidateStreamingPerformanceCharacteristics() {
        // Given: Performance test setup
        DiffAnalysis diffAnalysis = createMockDiffAnalysis();
        ReviewConfiguration config = createTestConfiguration();

        // When: We measure streaming performance
        long startTime = System.currentTimeMillis();

        StepVerifier.create(aiAnalysisPort.analyze(diffAnalysis, config))
            .assertNext(result -> {
                long duration = System.currentTimeMillis() - startTime;

                log.info("⚡ Streaming Performance Metrics:");
                log.info("   • Duration: {}ms", duration);
                log.info("   • Response size: {} characters", result.getSummary().length());

                // Verify performance characteristics
                assertThat(duration)
                    .as("Streaming should take reasonable time to simulate realistic delays")
                    .isGreaterThan(500)  // Should take at least 500ms for realistic streaming
                    .isLessThan(5000);   // But not too long for unit tests

                assertThat(result.getSummary().length())
                    .as("Streaming should produce substantial content")
                    .isGreaterThan(400); // Should have meaningful content

                log.info("   ✅ Performance characteristics: VALID");
            })
            .expectComplete()
            .verify(Duration.ofSeconds(10));
    }

    @Test
    void shouldHandleErrorsInStreamingGracefully() {
        // Given: Mock adapter that can simulate errors
        MockLlmStreamingAdapter errorAdapter = new MockLlmStreamingAdapter();
        DiffAnalysis diffAnalysis = createMockDiffAnalysis();
        ReviewConfiguration config = createTestConfiguration();

        // When: We test error scenarios
        StepVerifier.create(errorAdapter.simulateStreamingWithError())
            .expectSubscription()
            .expectNextCount(3) // Should receive some chunks before error
            .expectErrorMatches(throwable -> {
                log.info("🚨 Expected error caught: {}", throwable.getMessage());
                return throwable instanceof RuntimeException &&
                       throwable.getMessage().contains("Simulated LLM connection error");
            })
            .verify(Duration.ofSeconds(2));

        log.info("✅ Error handling validation: PASSED");
    }

    @Test
    void shouldValidateCompleteIntegrationFlow() {
        // Given: Complete integration test setup
        StartReviewCommand command = createTestCommand();
        DiffAnalysis mockDiffAnalysis = createMockDiffAnalysis();

        // Mock repository operations
        when(diffCollectionPort.collectDiff(any())).thenReturn(Mono.just(mockDiffAnalysis));
        when(reviewRepository.save(any(CodeReview.class)))
            .thenAnswer(invocation -> {
                CodeReview review = invocation.getArgument(0);
                return Mono.just(review);
            });
        when(staticAnalysisPort.isAvailable()).thenReturn(false);
        when(reviewPublishingPort.supports(any())).thenReturn(false);

        // When: We execute the complete flow
        long startTime = System.currentTimeMillis();

        StepVerifier.create(startReviewUseCase.execute(command))
            .assertNext(review -> {
                long totalDuration = System.currentTimeMillis() - startTime;

                log.info("🎯 Complete Integration Flow Validation:");
                log.info("   • Total duration: {}ms", totalDuration);
                log.info("   • Review ID: {}", review.getId());
                log.info("   • Analysis results count: {}", review.getAnalysisResults().size());

                // Verify complete workflow
                assertThat(review.getId()).isNotNull();
                assertThat(review.getStatus()).isNotNull();
                assertThat(review.getAnalysisResults()).isNotEmpty();

                // Verify streaming LLM analysis is present and valid
                boolean hasLlmAnalysis = review.getAnalysisResults().stream()
                    .anyMatch(result -> result.getType() == AnalysisResult.AnalysisType.LLM_ANALYSIS &&
                                       result.getSummary().contains("Code Review Summary"));

                assertThat(hasLlmAnalysis)
                    .as("LLM streaming analysis should be properly integrated")
                    .isTrue();

                log.info("   ✅ LLM streaming integration: SUCCESSFUL");
                log.info("   ✅ Domain entity creation: SUCCESSFUL");
                log.info("   ✅ Use case orchestration: SUCCESSFUL");
                log.info("   ✅ Clean Architecture flow: VERIFIED");
            })
            .expectComplete()
            .verify(Duration.ofSeconds(15));
    }

    // Helper methods for test data creation
    private StartReviewCommand createTestCommand() {
        RepositoryInfo repositoryInfo = RepositoryInfo.forLocalCommits(
            "test-repo", "HEAD~1", "HEAD", "main"
        );

        ReviewConfiguration configuration = createTestConfiguration();

        return new StartReviewCommand(repositoryInfo, configuration);
    }

    private ReviewConfiguration createTestConfiguration() {
        return new ReviewConfiguration(
            "mock-model", "http://localhost:11434", 60, 1500, 5, "17", "maven",
            false, true, false
        );
    }

    private DiffAnalysis createMockDiffAnalysis() {
        String testDiff = """
            --- a/src/main/java/Example.java
            +++ b/src/main/java/Example.java
            @@ -1,5 +1,7 @@
             public class Example {
                 public void method() {
            -        System.out.println("Hello");
            +        System.out.println("Hello World");
            +        // TODO: Add error handling
                 }
            +    public void newMethod() {}
             }
            """;

        List<FileModification> fileModifications = List.of(
            new FileModification(
                "src/main/java/Example.java",
                "src/main/java/Example.java",
                List.of(new DiffHunk(1, 5, 1, 7, List.of(
                    "-        System.out.println(\"Hello\");",
                    "+        System.out.println(\"Hello World\");",
                    "+        // TODO: Add error handling"
                ))),
                FileModification.ModificationType.MODIFIED
            )
        );

        return new DiffAnalysis(testDiff, fileModifications);
    }

    private CodeReview createMockCodeReview() {
        RepositoryInfo repositoryInfo = RepositoryInfo.forLocalCommits(
            "test-repo", "HEAD~1", "HEAD", "main"
        );
        ReviewConfiguration configuration = createTestConfiguration();

        CodeReview review = new CodeReview(repositoryInfo, configuration);
        review.start();

        // Add mock LLM analysis result
        AnalysisResult llmResult = new AnalysisResult(
            AnalysisResult.AnalysisType.LLM_ANALYSIS,
            "Mock streaming analysis completed",
            List.of(),
            List.of("Mock streaming analysis"),
            "mock-llm-v1.0"
        );

        review.addAnalysisResult(llmResult);
        return review;
    }
}