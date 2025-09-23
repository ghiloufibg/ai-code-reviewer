# 🤖 LLM Integration Testing Guide

This guide explains how to test LLM streaming responses in your AI Code Reviewer application using both **embedded LLM solutions** and **mock alternatives**.

## 🎯 Testing Approaches Overview

| Approach | Use Case | Pros | Cons | Resource Requirements |
|----------|----------|------|------|----------------------|
| **Ollama TestContainers** | True integration testing | Real LLM behavior, streaming validation | Slow, resource-intensive | High (requires Docker, 4GB+ RAM) |
| **Mock Streaming** | Unit testing, CI/CD | Fast, predictable, lightweight | Not real LLM behavior | Low |

## 🐳 Embedded LLM Testing with Ollama TestContainers

### Prerequisites
```bash
# Install Docker
# Ensure at least 4GB RAM available for containers
# Pull required dependencies
mvn dependency:resolve
```

### Running Integration Tests
```bash
# Run full LLM integration tests (requires Docker)
mvn test -Dtest=LlmStreamingIntegrationTest -Dllm.integration.test=true -Dspring.profiles.active=integration-test

# Run specific streaming test
mvn test -Dtest=LlmStreamingIntegrationTest#testStreamingLlmAnalysis -Dllm.integration.test=true
```

### What It Tests
- ✅ **Real LLM inference** with CodeLlama 7B model
- ✅ **Streaming response handling** with actual network chunks
- ✅ **Timeout and error scenarios** with real network conditions
- ✅ **Performance characteristics** under realistic load
- ✅ **Integration with Clean Architecture** ports and adapters

### Example Test Output
```
INFO  Pulling model codellama:7b-code in Ollama container...
INFO  Model codellama:7b-code successfully pulled
INFO  Received chunk #1: ## Code Review Analysis...
INFO  Received chunk #2: I've analyzed your diff...
INFO  Streaming test completed: 15 chunks, 1247 total characters
```

## 🎭 Mock Streaming Testing

### Running Mock Tests
```bash
# Run fast mock streaming tests
mvn test -Dtest=MockLlmStreamingTest -Dspring.profiles.active=mock-test

# Run all unit tests including streaming
mvn test -Dspring.profiles.active=mock-test
```

### What It Tests
- ✅ **Streaming chunk processing** with predictable data
- ✅ **Error handling scenarios** with simulated failures
- ✅ **Timeout behavior** with controlled delays
- ✅ **Response aggregation** logic
- ✅ **Performance testing** with consistent timing

### Example Test Output
```
INFO  Received mock chunk #1: Code Review Summary
INFO  Received mock chunk #2: I've analyzed your code diff
INFO  Mock streaming test completed: 9 chunks, 892 total characters
```

## 🔧 Configuration Profiles

### Integration Test Profile (`integration-test`)
```yaml
# application-integration-test.yml
spring:
  profiles:
    active: integration-test

# TestContainers will override these values
ollama:
  base-url: http://localhost:11434
  model: codellama:7b-code
  timeout: 60s
```

### Mock Test Profile (`mock-test`)
```yaml
# application-mock-test.yml
spring:
  profiles:
    active: mock-test

llm:
  mock:
    enabled: true
    response-delay: 100ms
    chunk-count: 9
```

## 🏗️ Architecture Integration

### Port Implementation
```java
// Real implementation for integration tests
@Component
@Profile("integration-test")
public class OllamaLlmStreamingAdapter implements LlmAnalysisPort {
    public Flux<String> streamAnalyzeDiff(DiffAnalysis diff, ReviewConfiguration config) {
        // Real streaming implementation
    }
}

// Mock implementation for unit tests
@TestComponent
@Profile("mock-test")
public class MockLlmStreamingAdapter implements LlmAnalysisPort {
    public Flux<String> simulateStreamingResponse() {
        // Predictable mock streaming
    }
}
```

### Use Case Testing
```java
@Test
void testStartReviewUseCaseWithStreaming() {
    // Test the full use case with either real or mock streaming
    StepVerifier.create(startReviewUseCase.execute(command))
        .assertNext(review -> {
            assertThat(review.getStatus()).isEqualTo(ReviewStatus.COMPLETED);
            assertThat(review.getAnalysisResults()).isNotEmpty();
        })
        .expectComplete()
        .verify(Duration.ofMinutes(2));
}
```

## 🚀 CI/CD Integration

### GitHub Actions Example
```yaml
name: LLM Testing

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run Mock Streaming Tests
        run: mvn test -Dspring.profiles.active=mock-test

  integration-tests:
    runs-on: ubuntu-latest
    if: github.event_name == 'pull_request'
    steps:
      - uses: actions/checkout@v3
      - name: Run LLM Integration Tests
        run: |
          mvn test -Dtest=LlmStreamingIntegrationTest \
                   -Dllm.integration.test=true \
                   -Dspring.profiles.active=integration-test
```

### Maven Profiles
```xml
<profiles>
    <profile>
        <id>llm-integration-test</id>
        <properties>
            <llm.integration.test>true</llm.integration.test>
            <spring.profiles.active>integration-test</spring.profiles.active>
        </properties>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-failsafe-plugin</artifactId>
                    <configuration>
                        <includes>
                            <include>**/*IntegrationTest.java</include>
                        </includes>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

## 📊 Performance Benchmarks

### Typical Performance Characteristics

| Test Type | Duration | Memory Usage | CPU Usage |
|-----------|----------|--------------|-----------|
| Mock Streaming | 1-2s | 50MB | Low |
| Ollama Integration | 30-60s | 2-4GB | High |

### Resource Requirements

#### For Ollama Integration Tests
- **RAM**: 4GB minimum, 8GB recommended
- **CPU**: 4 cores minimum for decent performance
- **Disk**: 2GB for model storage
- **Network**: Internet connection for initial model download

#### For Mock Tests
- **RAM**: Standard JVM requirements
- **CPU**: Minimal
- **Disk**: Standard
- **Network**: None required

## 🔍 Debugging and Troubleshooting

### Common Issues

#### Ollama Container Startup Issues
```bash
# Check Docker status
docker ps
docker logs <container-id>

# Verify model pull
docker exec <container-id> ollama list
```

#### Streaming Response Issues
```java
// Enable debug logging
logging.level.com.ghiloufi.aicode.infrastructure.adapter.output.external.llm=DEBUG

// Add detailed logging in tests
StepVerifier.create(streamingResponse)
    .thenConsumeWhile(chunk -> {
        log.info("Chunk received: {}", chunk);
        return true;
    })
```

### Test Data Validation
```java
@Test
void validateStreamingTestData() {
    // Ensure test diff is realistic
    assertThat(createTestDiffAnalysis().getRawDiff())
        .contains("--- a/")
        .contains("+++ b/")
        .contains("@@");

    // Verify streaming chunks are reasonable
    List<String> chunks = mockAdapter.simulateStreamingResponse()
        .collectList().block();
    assertThat(chunks).allMatch(chunk -> chunk.length() > 0);
}
```

## 🎯 Best Practices

### 1. Test Pyramid Approach
- **Unit Tests (90%)**: Use mock streaming for fast feedback
- **Integration Tests (10%)**: Use Ollama for critical path validation

### 2. Streaming Validation
```java
// Verify streaming characteristics
StepVerifier.create(streamingResponse)
    .expectSubscription()
    .recordWith(ArrayList::new)
    .thenConsumeWhile(chunk -> true)
    .expectRecordedMatches(chunks -> {
        // Validate chunk count, timing, content
        return chunks.size() > 1 &&
               chunks.stream().allMatch(c -> !c.isEmpty());
    })
    .expectComplete()
    .verify();
```

### 3. Error Scenario Testing
```java
// Test network interruption
Flux<String> interruptedStream = streamingFlux
    .doOnNext(chunk -> {
        if (chunk.contains("error-trigger")) {
            throw new RuntimeException("Network error");
        }
    });
```

### 4. Performance Testing
```java
@Test
void testStreamingPerformance() {
    StepVerifier.withVirtualTime(() -> streamingResponse)
        .expectSubscription()
        .expectNoEvent(Duration.ofMillis(50))
        .thenAwait(Duration.ofSeconds(5))
        .expectNextCount(expectedChunks)
        .expectComplete()
        .verify();
}
```

## 📈 Advanced Testing Scenarios

### Testing with Different Models
```java
@ParameterizedTest
@ValueSource(strings = {"codellama:7b-code", "codellama:13b-code"})
void testDifferentModels(String model) {
    ReviewConfiguration config = createConfigWithModel(model);
    // Test streaming with different model characteristics
}
```

### Load Testing Streaming
```java
@Test
void testConcurrentStreaming() {
    Flux.range(1, 10)
        .flatMap(i -> ollamaAdapter.streamAnalyzeDiff(diffAnalysis, config))
        .collectList()
        .as(StepVerifier::create)
        .assertNext(results -> assertThat(results).hasSize(10))
        .expectComplete()
        .verify(Duration.ofMinutes(5));
}
```

This comprehensive testing approach ensures your LLM streaming integration is robust, performant, and maintainable! 🚀