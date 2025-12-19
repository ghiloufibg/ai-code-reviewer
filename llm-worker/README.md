# LLM Worker Module

The **llm-worker** module provides LLM-based code review analysis. It implements the `ReviewAnalysisPort` from the core module using streaming chat completions.

## Architecture

```
llm-worker/
├── adapter/
│   └── ChatStreamingAnalysisAdapter   # ReviewAnalysisPort implementation
├── config/
│   ├── ContextStrategyConfiguration   # Context strategy beans
│   ├── LangChain4jConfig              # LLM provider configuration
│   ├── TicketContextConfiguration     # Ticket service configuration
│   └── WorkerProperties               # Worker settings
├── consumer/
│   └── ReviewRequestConsumer          # Redis queue consumer
├── processor/
│   ├── ReviewProcessor                # Request processing
│   └── ReviewService                  # Review orchestration
├── publisher/
│   └── ReviewResultPublisher          # Result publishing
├── schema/
│   └── ReviewResultSchema             # Structured output schemas
├── service/
│   ├── context/                       # Context retrieval helpers
│   ├── prompt/                        # Prompt building services
│   └── DefaultTicketContextService    # Ticket context extraction
└── strategy/
    ├── HistoryBasedContextStrategy    # Git history context
    └── MetadataBasedContextStrategy   # File metadata context
```

## Key Components

### ChatStreamingAnalysisAdapter

Implements `ReviewAnalysisPort` for LLM-based streaming code review:

```java
@Service
public class ChatStreamingAnalysisAdapter implements ReviewAnalysisPort {

    @Override
    public Flux<ReviewChunk> analyzeCode(
        EnrichedDiffAnalysisBundle enrichedBundle,
        ReviewConfiguration configuration) {
        // Streaming LLM analysis implementation
    }

    @Override
    public String getAnalysisMethod() {
        return "chat-streaming";
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }
}
```

### Context Strategies

| Strategy | Purpose | Priority |
|----------|---------|----------|
| `HistoryBasedContextStrategy` | Analyzes git history for co-change patterns | 100 |
| `MetadataBasedContextStrategy` | Extracts file metadata and structure | 50 |

### Prompt Building

- `PromptBuilder` - Constructs review prompts with context
- `PromptTemplateService` - Template management
- `TokenCounter` - Token counting for context limits

## Configuration

```yaml
llm-worker:
  enabled: true
  provider: openai           # or: anthropic, azure
  model: gpt-4o-mini
  streaming: true
  max-tokens: 4096

langchain4j:
  open-ai:
    chat-model:
      api-key: ${OPENAI_API_KEY}
      model-name: gpt-4o-mini
      temperature: 0.2

context-strategy:
  history:
    enabled: true
    max-commits: 100
    lookback-days: 90
  metadata:
    enabled: true
```

## Message Queue Integration

The worker consumes review requests from Redis:

```
Queue: review-requests
├── ReviewRequestConsumer (listens)
├── ReviewProcessor (processes)
└── ReviewResultPublisher (publishes results)
```

## Dependencies

```xml
<dependency>
    <groupId>com.ghiloufi.aicode</groupId>
    <artifactId>core</artifactId>
</dependency>

<!-- LangChain4j for LLM integration -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
</dependency>

<!-- Redis for queue -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

## Extending

To add a new context strategy:

1. Implement `ContextRetrievalStrategy` from core
2. Register as a Spring bean in `ContextStrategyConfiguration`
3. Set appropriate priority (higher = runs first)

```java
@Component
public class CustomContextStrategy implements ContextRetrievalStrategy {

    @Override
    public Mono<ContextRetrievalResult> retrieveContext(DiffAnalysisBundle diffBundle) {
        // Custom context retrieval logic
    }

    @Override
    public String getStrategyName() {
        return "custom";
    }

    @Override
    public int getPriority() {
        return 75; // Between history (100) and metadata (50)
    }
}
```

## Testing

```bash
mvn test -pl llm-worker
```

All tests: 230 passing

## Related Modules

- **core** - Shared kernel with ports and domain models
- **api-gateway** - REST API and SSE streaming
- **agent-worker** (future) - Multi-agent analysis implementation
