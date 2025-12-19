# Core Module

The **core** module is the shared kernel of the AI Code Reviewer system. It defines domain models, ports (interfaces), and common services that can be used by any reviewer implementation.

## Architecture

This module follows **Hexagonal Architecture** (Ports & Adapters):

```
core/
├── domain/
│   ├── model/          # Domain entities and value objects
│   └── port/
│       ├── input/      # Use cases (driven ports)
│       └── output/     # Infrastructure ports (driving ports)
├── application/
│   └── service/        # Application services and interfaces
├── infrastructure/
│   ├── adapter/        # Port implementations
│   └── persistence/    # Database entities and repositories
└── service/            # Shared services (parsing, validation, filtering)
```

## Key Ports

### ReviewAnalysisPort

The primary abstraction for code analysis implementations:

```java
public interface ReviewAnalysisPort {
    Flux<ReviewChunk> analyzeCode(
        EnrichedDiffAnalysisBundle enrichedBundle,
        ReviewConfiguration configuration);

    String getAnalysisMethod();
    String getProviderName();
    String getModelName();
    boolean supportsStreaming();
}
```

**Implementations:**
- `ChatStreamingAnalysisAdapter` (llm-worker) - LLM-based streaming analysis
- Future: `AgenticAnalysisAdapter` (agent-worker) - Multi-agent analysis

### ContextRetrievalStrategy

Strategy interface for retrieving additional context for code review:

```java
public interface ContextRetrievalStrategy {
    Mono<ContextRetrievalResult> retrieveContext(DiffAnalysisBundle diffBundle);
    String getStrategyName();
    int getPriority();
}
```

**Implementations in llm-worker:**
- `HistoryBasedContextStrategy` - Git history analysis
- `MetadataBasedContextStrategy` - File metadata context

### Other Ports

| Port | Purpose |
|------|---------|
| `SCMPort` | Source Control Management integration (GitLab, GitHub) |
| `AIInteractionPort` | Raw AI/LLM interaction |
| `TicketSystemPort` | Ticket system integration (Jira) |
| `TicketAnalysisPort` | LLM-based ticket analysis |

## Domain Models

### Core Entities

- `ReviewResult` - Complete review with issues and notes
- `ReviewChunk` - Streaming chunk of review content
- `DiffAnalysisBundle` - Parsed diff with file changes
- `EnrichedDiffAnalysisBundle` - Diff with additional context
- `ReviewConfiguration` - Review settings and policies

### Value Objects

- `RepositoryIdentifier` - Repository identification (GitLab, GitHub)
- `ChangeRequestIdentifier` - MR/PR identification
- `TicketContext` - Ticket business context

## Shared Services

| Service | Purpose |
|---------|---------|
| `JsonReviewResultParser` | Parse LLM JSON responses |
| `ReviewResultValidator` | JSON schema validation |
| `ConfidenceFilter` | Filter low-confidence issues |
| `DuplicateIssueFilter` | Deduplicate review issues |

## Module Dependencies

```
core (this module)
  └── No internal module dependencies

llm-worker
  └── depends on: core

api-gateway
  └── depends on: core

agent-worker (future)
  └── depends on: core
```

## Design Principles

1. **Pure Abstractions**: Core contains only interfaces and domain models
2. **No LLM Implementation**: All LLM-specific code lives in worker modules
3. **Provider Agnostic**: Supports multiple SCM providers and LLM backends
4. **Streaming First**: All analysis ports support reactive streaming
5. **Extensible**: New analyzers implement `ReviewAnalysisPort`

## Usage

Add as a dependency in your module's `pom.xml`:

```xml
<dependency>
    <groupId>com.ghiloufi.aicode</groupId>
    <artifactId>core</artifactId>
    <version>${project.version}</version>
</dependency>
```

## Testing

```bash
mvn test -pl core
```

All tests: 586 passing
