# Spring WebFlux Migration - Streaming LLM Usage Example

This document demonstrates how to use the new reactive streaming capabilities after the WebFlux migration.

## Key Changes

### 1. LLM Client with Streaming Support

The `LlmClient` now supports three types of operations:

```java
// Synchronous (backward compatible)
String response = llmClient.review(systemPrompt, userPrompt);

// Reactive (non-blocking)
Mono<String> responseMono = llmClient.reviewReactive(systemPrompt, userPrompt);

// Streaming (real-time chunks)
Flux<String> responseStream = llmClient.reviewStream(systemPrompt, userPrompt);
```

### 2. Orchestrator with Reactive Processing

The `CodeReviewOrchestrator` now processes chunks reactively:

```java
// Synchronous (backward compatible)
orchestrator.executeCodeReview(config);

// Reactive (non-blocking)
Mono<Void> execution = orchestrator.executeCodeReviewReactive(config);

// Streaming analysis of individual chunks
Mono<ReviewResult> result = orchestrator.analyzeChunkWithStreaming(
    config, chunk, staticReports, diffBundle
);
```

### 3. Service Layer Reactive Methods

All core services now have reactive variants:

```java
// DiffCollectionService
Mono<DiffAnalysisBundle> diffBundle = diffCollectionService
                .collectFromGitHubReactive(githubClient, prNumber);

// ReviewResultMerger
Mono<ReviewResult> merged = resultMerger.mergeReactive(reviewParts);
```

## Benefits of the Streaming Approach

1. **Non-blocking**: The application doesn't block while waiting for LLM responses
2. **Real-time feedback**: Stream responses as they arrive from the LLM
3. **Better resource utilization**: Process multiple chunks concurrently
4. **Improved user experience**: See results as they're generated

## Configuration for Streaming

To enable streaming in your LLM requests, the client automatically sets `stream: true` in the payload:

```json
{
  "model": "deepseek-coder-6.7b-instruct",
  "messages": [...],
  "temperature": 0.1,
  "stream": true
}
```

## Usage Pattern

Here's how to use the streaming functionality effectively:

```java
@Autowired
private CodeReviewOrchestrator orchestrator;

public void performReactiveCodeReview(ApplicationConfig config) {
    orchestrator.executeCodeReviewReactive(config)
        .doOnNext(result -> logger.info("Chunk completed"))
        .doOnError(error -> logger.error("Error during review", error))
        .doOnSuccess(unused -> logger.info("Review completed successfully"))
        .subscribe();
}
```

## Migration Notes

- All existing synchronous methods remain for backward compatibility
- New reactive methods use the `Reactive` suffix
- WebClient replaces Apache HttpClient for better reactive support
- Streaming responses are parsed chunk by chunk for real-time processing