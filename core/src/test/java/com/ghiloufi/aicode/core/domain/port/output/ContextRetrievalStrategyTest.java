package com.ghiloufi.aicode.core.domain.port.output;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.ContextRetrievalMetadata;
import com.ghiloufi.aicode.core.domain.model.ContextRetrievalResult;
import com.ghiloufi.aicode.core.domain.model.DiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.GitDiffDocument;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ContextRetrievalStrategyTest {

  private static final class TestStrategy implements ContextRetrievalStrategy {
    private final String strategyName;
    private final int priority;

    TestStrategy(final String strategyName, final int priority) {
      this.strategyName = strategyName;
      this.priority = priority;
    }

    @Override
    public Mono<ContextRetrievalResult> retrieveContext(final DiffAnalysisBundle diffBundle) {
      final ContextRetrievalMetadata metadata =
          new ContextRetrievalMetadata(strategyName, Duration.ofMillis(100), 0, 0, Map.of());
      return Mono.just(new ContextRetrievalResult(List.of(), metadata));
    }

    @Override
    public String getStrategyName() {
      return strategyName;
    }

    @Override
    public int getPriority() {
      return priority;
    }
  }

  @Test
  void should_return_strategy_name() {
    final ContextRetrievalStrategy strategy = new TestStrategy("test-strategy", 10);

    assertThat(strategy.getStrategyName()).isEqualTo("test-strategy");
  }

  @Test
  void should_return_priority() {
    final ContextRetrievalStrategy strategy = new TestStrategy("test-strategy", 5);

    assertThat(strategy.getPriority()).isEqualTo(5);
  }

  @Test
  void should_retrieve_context_reactively() {
    final ContextRetrievalStrategy strategy = new TestStrategy("test-strategy", 10);
    final RepositoryIdentifier repo =
        RepositoryIdentifier.create(SourceProvider.GITLAB, "test/repo");
    final DiffAnalysisBundle bundle =
        new DiffAnalysisBundle(repo, new GitDiffDocument(List.of()), "diff content");

    final Mono<ContextRetrievalResult> result = strategy.retrieveContext(bundle);

    StepVerifier.create(result)
        .assertNext(
            contextResult -> {
              assertThat(contextResult.matches()).isEmpty();
              assertThat(contextResult.metadata().strategyName()).isEqualTo("test-strategy");
            })
        .verifyComplete();
  }

  @Test
  void should_support_different_priorities() {
    final ContextRetrievalStrategy lowPriority = new TestStrategy("low", 1);
    final ContextRetrievalStrategy highPriority = new TestStrategy("high", 10);

    assertThat(highPriority.getPriority()).isGreaterThan(lowPriority.getPriority());
  }
}
