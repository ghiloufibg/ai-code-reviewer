package com.ghiloufi.aicode.core.service.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.application.service.DefaultTicketContextService;
import com.ghiloufi.aicode.core.application.service.TicketContextService;
import com.ghiloufi.aicode.core.config.FeaturesConfiguration;
import com.ghiloufi.aicode.core.config.PromptPropertiesFactory;
import com.ghiloufi.aicode.core.domain.model.DiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.DiffHunkBlock;
import com.ghiloufi.aicode.core.domain.model.EnrichedDiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.GitDiffDocument;
import com.ghiloufi.aicode.core.domain.model.GitFileModification;
import com.ghiloufi.aicode.core.domain.model.PrMetadata;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.model.ReviewConfiguration;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import com.ghiloufi.aicode.core.domain.model.TicketBusinessContext;
import com.ghiloufi.aicode.core.domain.model.TicketContext;
import com.ghiloufi.aicode.core.domain.port.output.TicketSystemPort;
import com.ghiloufi.aicode.core.domain.service.DiffFormatter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;

@DisplayName("PromptBuilder Ticket Integration Tests")
@SpringBootTest(
    classes = {
      FeaturesConfiguration.class,
      PromptTemplateService.class,
      PromptPropertiesFactory.class
    })
final class PromptBuilderTicketIntegrationTest {

  private PromptBuilder promptBuilder;
  private TicketContextService ticketContextService;
  @Autowired private PromptTemplateService promptTemplateService;
  private RepositoryIdentifier testRepo;

  @BeforeEach
  final void setUp() {
    final DiffFormatter diffFormatter = new DiffFormatter();
    final TicketSystemPort ticketSystem = createMockTicketSystem();
    ticketContextService = new DefaultTicketContextService(ticketSystem);
    promptBuilder = new PromptBuilder(diffFormatter, promptTemplateService);
    testRepo = RepositoryIdentifier.create(SourceProvider.GITLAB, "test/repo");
  }

  @Test
  @DisplayName("should_include_ticket_context_when_bracketed_ticket_id_in_title")
  final void should_include_ticket_context_when_bracketed_ticket_id_in_title() {
    final EnrichedDiffAnalysisBundle enrichedBundle =
        createEnrichedBundle("[TM-123] : Add user authentication", null);
    final ReviewConfiguration config = ReviewConfiguration.defaults();

    final TicketContext ticketContext =
        ticketContextService
            .extractFromMergeRequest(
                enrichedBundle.prMetadata().title(), enrichedBundle.prMetadata().description())
            .block();

    final ReviewPromptResult result =
        promptBuilder.buildStructuredReviewPrompt(enrichedBundle, config, ticketContext);
    final String userPrompt = result.userPrompt();

    assertThat(userPrompt).contains("BUSINESS CONTEXT FROM TICKET: TM-123");
    assertThat(userPrompt).contains("Feature/Fix: Test Feature");
    assertThat(userPrompt).contains("TICKET DESCRIPTION:");
    assertThat(userPrompt).contains("Test description");
    assertThat(userPrompt).contains("[REVIEW_FOCUS]");
    assertThat(userPrompt).contains("Verify code implements business requirements from ticket");
  }

  @Test
  @DisplayName("should_include_ticket_context_when_bracketed_ticket_id_in_description")
  final void should_include_ticket_context_when_bracketed_ticket_id_in_description() {
    final EnrichedDiffAnalysisBundle enrichedBundle =
        createEnrichedBundle("Add user authentication", "This implements [TM-123]");
    final ReviewConfiguration config = ReviewConfiguration.defaults();

    final TicketContext ticketContext =
        ticketContextService
            .extractFromMergeRequest(
                enrichedBundle.prMetadata().title(), enrichedBundle.prMetadata().description())
            .block();

    final ReviewPromptResult result =
        promptBuilder.buildStructuredReviewPrompt(enrichedBundle, config, ticketContext);
    final String userPrompt = result.userPrompt();

    assertThat(userPrompt).contains("BUSINESS CONTEXT FROM TICKET: TM-123");
    assertThat(userPrompt).contains("Feature/Fix: Test Feature");
  }

  @Test
  @DisplayName("should_not_include_ticket_context_when_no_ticket_id_found")
  final void should_not_include_ticket_context_when_no_ticket_id_found() {
    final EnrichedDiffAnalysisBundle enrichedBundle =
        createEnrichedBundle("Add user authentication", "Implementation details");
    final ReviewConfiguration config = ReviewConfiguration.defaults();

    final TicketContext ticketContext =
        ticketContextService
            .extractFromMergeRequest(
                enrichedBundle.prMetadata().title(), enrichedBundle.prMetadata().description())
            .block();

    final ReviewPromptResult result =
        promptBuilder.buildStructuredReviewPrompt(enrichedBundle, config, ticketContext);
    final String userPrompt = result.userPrompt();

    assertThat(userPrompt).doesNotContain("BUSINESS CONTEXT FROM TICKET");
    assertThat(userPrompt).doesNotContain("[REVIEW_FOCUS]");
    assertThat(userPrompt).contains("[REPO]");
    assertThat(userPrompt).contains("[DIFF]");
  }

  @Test
  @DisplayName("should_handle_null_merge_request_title_and_description")
  final void should_handle_null_merge_request_title_and_description() {
    final EnrichedDiffAnalysisBundle enrichedBundle = createEnrichedBundle(null, null);
    final ReviewConfiguration config = ReviewConfiguration.defaults();

    final TicketContext ticketContext =
        ticketContextService
            .extractFromMergeRequest(
                enrichedBundle.prMetadata().title(), enrichedBundle.prMetadata().description())
            .block();

    final ReviewPromptResult result =
        promptBuilder.buildStructuredReviewPrompt(enrichedBundle, config, ticketContext);
    final String userPrompt = result.userPrompt();

    assertThat(userPrompt).doesNotContain("BUSINESS CONTEXT FROM TICKET");
    assertThat(userPrompt).contains("[REPO]");
    assertThat(userPrompt).contains("[DIFF]");
  }

  @Test
  @DisplayName("should_have_separate_system_and_user_prompts")
  final void should_have_separate_system_and_user_prompts() {
    final EnrichedDiffAnalysisBundle enrichedBundle =
        createEnrichedBundle("[TM-123] : Feature", null);
    final ReviewConfiguration config = ReviewConfiguration.defaults();

    final TicketContext ticketContext =
        ticketContextService
            .extractFromMergeRequest(
                enrichedBundle.prMetadata().title(), enrichedBundle.prMetadata().description())
            .block();

    final ReviewPromptResult result =
        promptBuilder.buildStructuredReviewPrompt(enrichedBundle, config, ticketContext);

    assertThat(result.systemPrompt()).containsAnyOf("code review assistant", "Senior software engineer");
    assertThat(result.userPrompt()).contains("BUSINESS CONTEXT FROM TICKET");
    assertThat(result.userPrompt())
        .doesNotContain("code review assistant")
        .doesNotContain("Senior software engineer");
  }

  @Test
  @DisplayName("should_not_include_ticket_context_when_ticket_has_no_description")
  final void should_not_include_ticket_context_when_ticket_has_no_description() {
    final TicketSystemPort ticketSystemWithoutDescription =
        ticketId -> Mono.just(new TicketBusinessContext(ticketId, "Title only", null));

    final TicketContextService service =
        new DefaultTicketContextService(ticketSystemWithoutDescription);
    final PromptBuilder builderWithNoDesc =
        new PromptBuilder(new DiffFormatter(), promptTemplateService);

    final EnrichedDiffAnalysisBundle enrichedBundle =
        createEnrichedBundle("[TM-999] : Feature", null);
    final ReviewConfiguration config = ReviewConfiguration.defaults();

    final TicketContext ticketContext =
        service
            .extractFromMergeRequest(
                enrichedBundle.prMetadata().title(), enrichedBundle.prMetadata().description())
            .block();

    final ReviewPromptResult result =
        builderWithNoDesc.buildStructuredReviewPrompt(enrichedBundle, config, ticketContext);
    final String userPrompt = result.userPrompt();

    assertThat(userPrompt).doesNotContain("BUSINESS CONTEXT FROM TICKET");
    assertThat(userPrompt).doesNotContain("[REVIEW_FOCUS]");
  }

  private EnrichedDiffAnalysisBundle createEnrichedBundle(
      final String mergeRequestTitle, final String mergeRequestDescription) {
    final GitFileModification file = new GitFileModification("Test.java", "Test.java");
    final DiffHunkBlock hunk = new DiffHunkBlock(1, 1, 1, 1);
    hunk.lines = List.of(" test");
    file.diffHunkBlocks = List.of(hunk);

    final GitDiffDocument diff = new GitDiffDocument(List.of(file));
    final PrMetadata prMetadata =
        new PrMetadata(
            mergeRequestTitle, mergeRequestDescription, null, null, null, List.of(), List.of(), 1);
    final DiffAnalysisBundle bundle = new DiffAnalysisBundle(testRepo, diff, "raw", prMetadata);

    return new EnrichedDiffAnalysisBundle(bundle);
  }

  private TicketSystemPort createMockTicketSystem() {
    return ticketId -> {
      if (ticketId.matches("[A-Z]+-\\d+")) {
        return Mono.just(new TicketBusinessContext(ticketId, "Test Feature", "Test description"));
      }
      return Mono.empty();
    };
  }
}
