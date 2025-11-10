# 🚀 AI Code Reviewer MVP/POC - Prioritized Parallel Execution Plan

**Generated**: 2025-11-02
**Project**: Spring WebFlux AI Code Reviewer for GitHub
**Timeline**: 6 working days (1.5 weeks)
**Team Size**: 3 developers

---

## 📋 Executive Summary

**Goal**: Complete Spring WebFlux AI code reviewer for GitHub with unified AI interaction point
**Timeline**: **6 working days** (1.5 weeks)
**Team**: 3 developers working in parallel
**Outcome**: Production-ready MVP demonstrating automated PR code review

### Key Metrics
- **Architecture**: Hexagonal + Reactive (Spring WebFlux)
- **Primary SCM**: GitHub (kohsuke/github-api)
- **AI Provider**: OpenAI (gpt-4o-mini)
- **Streaming**: Server-Sent Events (SSE)
- **Deployment**: Docker Compose
- **Testing**: Unit + Integration + E2E

---

## 🎯 MVP Scope Definition

### ✅ MUST HAVE (In Scope)

1. **GitHub Integration**
   - Fetch PR diffs via GitHub API
   - Post review comments on PRs
   - Handle authentication via GITHUB_TOKEN

2. **Unified AI Service** ⭐ CORE FEATURE
   - Single entry point for all AI interactions
   - `UnifiedAIService` as application service
   - Abstract provider implementation details
   - Streaming support with backpressure

3. **OpenAI Provider**
   - Streaming code review with gpt-4o-mini
   - Circuit breaker for fault tolerance
   - Retry logic with exponential backoff
   - Rate limit handling

4. **SSE Streaming**
   - Real-time review progress updates
   - Server-Sent Events implementation
   - Client subscription management
   - Error propagation

5. **REST API**
   - Trigger PR reviews
   - Check review status
   - Stream results
   - Health checks

6. **Production Quality**
   - Proper logging (SLF4J + Logback)
   - Configuration externalization
   - Health check endpoints
   - Metrics exposure

7. **Testing**
   - Unit tests (>70% coverage)
   - Integration tests
   - E2E workflow validation
   - Performance testing

### ❌ OUT OF SCOPE (Explicitly Deferred)

- ❌ Multiple LLM providers (Ollama, Claude, Gemini)
- ❌ Other SCM systems (GitLab, Bitbucket, Azure DevOps)
- ❌ Advanced orchestration patterns
- ❌ Static analysis integration (PMD, Checkstyle, SpotBugs)
- ❌ Persistent storage (use in-memory for MVP)
- ❌ Advanced caching (Redis)
- ❌ Distributed tracing
- ❌ Legacy module migration (will DELETE instead)

---

## 🏗️ Architecture: Unified AI Interaction Point

### Core Design Principle
**One Service to Rule Them All**: `UnifiedAIService` is THE single entry point for all AI interactions

### Location
```
core/src/main/java/com/ghiloufi/aicode/core/application/service/UnifiedAIService.java
```

### Interface Design

```java
package com.ghiloufi.aicode.core.application.service;

import com.ghiloufi.aicode.core.domain.model.*;
import com.ghiloufi.aicode.core.domain.port.output.AIInteractionPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Duration;

/**
 * Unified AI Service - THE single entry point for all AI interactions.
 *
 * This service abstracts all AI provider details and provides a clean,
 * consistent interface for code review analysis.
 *
 * Design Pattern: Application Service in Hexagonal Architecture
 * Uses: AIInteractionPort (output port) to communicate with adapters
 */
@Service
@Slf4j
public class UnifiedAIService {

    private final AIInteractionPort aiPort;
    private final PromptBuilder promptBuilder;
    private final ReviewResultParser resultParser;

    public UnifiedAIService(
        AIInteractionPort aiPort,
        PromptBuilder promptBuilder,
        ReviewResultParser resultParser
    ) {
        this.aiPort = aiPort;
        this.promptBuilder = promptBuilder;
        this.resultParser = resultParser;
    }

    /**
     * Primary method for streaming code reviews.
     * This is THE unified entry point for all AI-powered code analysis.
     *
     * @param diff The code diff to review
     * @param config Review configuration and preferences
     * @return Stream of review chunks as they are generated
     */
    public Flux<ReviewChunk> reviewCodeStreaming(
        DiffAnalysisBundle diff,
        ReviewConfiguration config
    ) {
        log.info("Starting AI code review for {} files", diff.getFileCount());

        return Mono.fromCallable(() -> promptBuilder.buildReviewPrompt(diff, config))
            .doOnNext(prompt -> log.debug("Built review prompt: {} chars", prompt.length()))
            .flatMapMany(prompt -> aiPort.streamCompletion(prompt))
            .map(chunk -> resultParser.parseChunk(chunk))
            .doOnNext(chunk -> log.debug("Received review chunk: {}", chunk.getType()))
            .doOnComplete(() -> log.info("AI code review completed successfully"))
            .doOnError(e -> log.error("AI code review failed", e))
            .timeout(Duration.ofSeconds(60))
            .retry(3);
    }

    /**
     * Non-streaming version for simple completions.
     * Use this for non-review AI interactions.
     *
     * @param prompt The prompt to send to AI
     * @param options AI options (temperature, max tokens, etc.)
     * @return Complete AI response
     */
    public Mono<String> complete(String prompt, AIOptions options) {
        log.debug("AI completion request: {} chars", prompt.length());

        return aiPort.complete(prompt, options)
            .doOnSuccess(response -> log.debug("AI completion received: {} chars", response.length()))
            .doOnError(e -> log.error("AI completion failed", e))
            .timeout(Duration.ofSeconds(30))
            .retry(2);
    }

    /**
     * Health check for AI service availability.
     *
     * @return Health status including provider details
     */
    public Mono<AIHealthStatus> checkHealth() {
        return aiPort.healthCheck()
            .map(healthy -> new AIHealthStatus(healthy, "OpenAI", "gpt-4o-mini"))
            .doOnNext(status -> log.debug("AI health check: {}", status.isHealthy()))
            .onErrorReturn(new AIHealthStatus(false, "OpenAI", "error"));
    }
}
```

### Output Port Definition

```java
package com.ghiloufi.aicode.core.domain.port.output;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * AIInteractionPort - Output port for AI provider adapters.
 *
 * Implementations: OpenAIAdapter, OllamaAdapter, ClaudeAdapter, etc.
 * Pattern: Hexagonal Architecture - Output Port
 */
public interface AIInteractionPort {

    /**
     * Stream completion tokens from AI provider.
     *
     * @param prompt The prompt to send
     * @return Stream of response chunks
     */
    Flux<String> streamCompletion(String prompt);

    /**
     * Get complete response from AI provider.
     *
     * @param prompt The prompt to send
     * @param options AI configuration options
     * @return Complete response
     */
    Mono<String> complete(String prompt, AIOptions options);

    /**
     * Check if AI provider is available and healthy.
     *
     * @return true if healthy, false otherwise
     */
    Mono<Boolean> healthCheck();
}
```

### Adapter Implementation Example

```java
package com.ghiloufi.aicode.worker.adapter;

import com.ghiloufi.aicode.core.domain.port.output.AIInteractionPort;
import com.ghiloufi.aicode.worker.provider.OpenAIProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * OpenAI implementation of AIInteractionPort.
 *
 * This adapter translates generic AI requests into OpenAI-specific calls.
 */
@Component
@Slf4j
public class OpenAIAdapter implements AIInteractionPort {

    private final OpenAIProvider provider;

    public OpenAIAdapter(OpenAIProvider provider) {
        this.provider = provider;
    }

    @Override
    public Flux<String> streamCompletion(String prompt) {
        return provider.streamChat(prompt)
            .map(chunk -> chunk.getContent())
            .onErrorResume(e -> {
                log.error("OpenAI streaming failed", e);
                return Flux.error(new AIServiceException("OpenAI failed", e));
            });
    }

    @Override
    public Mono<String> complete(String prompt, AIOptions options) {
        return provider.complete(prompt, options)
            .map(response -> response.getContent())
            .onErrorResume(e -> {
                log.error("OpenAI completion failed", e);
                return Mono.error(new AIServiceException("OpenAI failed", e));
            });
    }

    @Override
    public Mono<Boolean> healthCheck() {
        return provider.isHealthy()
            .onErrorReturn(false);
    }
}
```

### Why This Design?

✅ **Single Responsibility**: All AI logic centralized
✅ **Testable**: Mock `AIInteractionPort` for testing
✅ **Extensible**: Easy to add new providers without changing callers
✅ **Observable**: Centralized logging and metrics
✅ **Hexagonal**: Application service using output ports
✅ **Reactive**: Non-blocking with backpressure
✅ **Fault Tolerant**: Timeout, retry, circuit breaker built-in

---

## 📅 Detailed Execution Plan

### **🧹 PHASE 0: CLEANUP (Day 0 - Morning)**

**Duration**: 4 hours
**Team**: ALL DEVELOPERS TOGETHER
**Priority**: 🔴 P0 - CRITICAL & BLOCKING

| Task | Owner | Time | Description |
|------|-------|------|-------------|
| **T0.1** | All | 1h | Archive legacy module → rename to `legacy-archive/` or delete completely |
| **T0.2** | All | 1h | Update root `pom.xml` - remove `<module>legacy</module>` |
| **T0.3** | All | 1h | Fix all import errors, search and remove legacy references |
| **T0.4** | All | 0.5h | Verify compilation: `mvn clean compile` |
| **T0.5** | All | 0.5h | Commit changes: `git commit -m "chore: remove legacy module for MVP focus"` |

**Commands**:
```bash
# Search for legacy imports
grep -r "import.*legacy" core/ api-gateway/ worker-llm/ common/

# Remove legacy from build
sed -i '/<module>legacy<\/module>/d' pom.xml

# Verify build
mvn clean compile

# Commit cleanup
git add -A
git commit -m "chore: archive legacy module for clean MVP workspace"
```

**Checkpoint**: ✅ Green build, zero legacy dependencies, clean workspace

---

### **🏗️ PHASE 1: FOUNDATION (Day 0.5-2)**

**Duration**: 1.5 days
**Team**: 3 developers working in PARALLEL
**Priority**: 🔴 P0 - CRITICAL PATH

#### **Stream 1A: Core Domain** (Developer A)

**Priority**: 🔴 P0 - CRITICAL PATH
**Duration**: 12 hours (1.5 days)

| Task | Time | Description | Deliverable |
|------|------|-------------|-------------|
| **T1.1** | 2h | Move `UnifiedDiffParser` from legacy to `core/service/diff/` | Diff parser in core |
| **T1.2** | 3h | Clean `GitHubAdapter` - remove `System.out`, add `logger` | Clean adapter |
| **T1.3** | 1h | Verify `GitHubPort` interface completeness | Complete interface |
| **T1.4** | 4h | Unit tests for diff parsing (5 test cases minimum) | Test coverage |
| **T1.5** | 2h | Integration test for `GitHubAdapter` with mocks | Integration test |

**Acceptance Criteria**:
- ✅ `UnifiedDiffParser` in `core/service/diff/`
- ✅ No `System.out.println` in production code
- ✅ `GitHubPort` interface documented
- ✅ 5+ unit tests passing
- ✅ 1 integration test passing

**Files to Create/Modify**:
```
core/src/main/java/com/ghiloufi/aicode/core/
├── service/diff/UnifiedDiffParser.java
├── domain/port/output/GitHubPort.java
└── infrastructure/adapter/GitHubAdapter.java

core/src/test/java/com/ghiloufi/aicode/core/
├── service/diff/UnifiedDiffParserTest.java
└── infrastructure/adapter/GitHubAdapterTest.java
```

---

#### **Stream 1B: Unified AI Service** (Developer B)

**Priority**: 🔴 P0 - CRITICAL PATH
**Duration**: 12 hours (1.5 days)

| Task | Time | Description | Deliverable |
|------|------|-------------|-------------|
| **T1.6** | 1h | Create `AIInteractionPort` interface in domain ports | Interface definition |
| **T1.7** | 4h | Create `UnifiedAIService` in `core/application/service/` | Core service |
| **T1.8** | 2h | Move `PromptBuilder` to `core/service/prompt/` | Prompt builder |
| **T1.9** | 3h | Create `OpenAIAdapter` implementing `AIInteractionPort` | Adapter impl |
| **T1.10** | 2h | Unit tests for `UnifiedAIService` (mock port) | Test coverage |

**Acceptance Criteria**:
- ✅ `AIInteractionPort` interface defined
- ✅ `UnifiedAIService` with streaming support
- ✅ `reviewCodeStreaming()` method implemented
- ✅ Error handling, timeout, retry configured
- ✅ Unit tests with mocked port

**Files to Create**:
```
core/src/main/java/com/ghiloufi/aicode/core/
├── domain/port/output/AIInteractionPort.java
├── application/service/UnifiedAIService.java
└── service/prompt/PromptBuilder.java

worker-llm/src/main/java/com/ghiloufi/aicode/worker/
└── adapter/OpenAIAdapter.java

core/src/test/java/com/ghiloufi/aicode/core/
└── application/service/UnifiedAIServiceTest.java
```

---

#### **Stream 1C: Infrastructure** (Developer C)

**Priority**: 🟡 P1 - HIGH PRIORITY
**Duration**: 10 hours (1.25 days)

| Task | Time | Description | Deliverable |
|------|------|-------------|-------------|
| **T1.11** | 2h | Replace all `System.out.println` with `logger.info/debug` | Clean logging |
| **T1.12** | 1h | Configure logging (`logback-spring.xml`) | Log config |
| **T1.13** | 2h | Create `application-mvp.yml` with minimal config | MVP config |
| **T1.14** | 2h | Set up health check endpoints (`/actuator/health`) | Health checks |
| **T1.15** | 3h | Configure Resilience4j circuit breaker | Fault tolerance |

**Acceptance Criteria**:
- ✅ Zero `System.out.println` in `src/main/`
- ✅ Proper logging configuration
- ✅ `application-mvp.yml` complete
- ✅ Health endpoint returns 200
- ✅ Circuit breaker configured

**Commands**:
```bash
# Find all System.out usage
grep -r "System\.out" src/main/java/

# Replace with logger (manual review each)
# Example: System.out.println("msg") → logger.info("msg")

# Verify health endpoint
curl http://localhost:8080/actuator/health
```

**Files to Create/Modify**:
```
api-gateway/src/main/resources/
├── application-mvp.yml
└── logback-spring.xml

api-gateway/src/main/java/com/ghiloufi/aicode/gateway/config/
└── Resilience4jConfig.java
```

---

**SYNC POINT - Wednesday EOD**

**Meeting**: All developers (30 minutes)
**Agenda**:
1. Demo current progress
2. Verify interfaces connect properly
3. Review shared DTOs
4. Identify blockers
5. Plan Day 3-4 integration

**Validation**:
- ✅ All Phase 1 tasks completed
- ✅ Compilation succeeds
- ✅ Unit tests pass
- ✅ No merge conflicts
- ✅ Interfaces agreed upon

---

### **🔗 PHASE 2: INTEGRATION (Day 3-4)**

**Duration**: 1.5 days
**Team**: 3 developers working in PARALLEL
**Priority**: 🔴 P0 - DEPENDS ON PHASE 1

#### **Stream 2A: API Gateway** (Developer A)

**Priority**: 🔴 P0 - CRITICAL PATH
**Duration**: 12 hours (1.5 days)

| Task | Time | Description | Deliverable |
|------|------|-------------|-------------|
| **T2.1** | 3h | Clean `GitHubController` - remove legacy, use `UnifiedAIService` | Clean controller |
| **T2.2** | 4h | Implement `ReviewManagementUseCase` properly | Use case impl |
| **T2.3** | 3h | Ensure SSE streaming works end-to-end | SSE streaming |
| **T2.4** | 2h | Add OpenAPI annotations for Swagger docs | API docs |

**Acceptance Criteria**:
- ✅ `GitHubController` uses `UnifiedAIService`
- ✅ `@Validated` annotations on all endpoints
- ✅ SSE streaming tested manually
- ✅ Swagger UI accessible

**Files to Modify**:
```
api-gateway/src/main/java/com/ghiloufi/aicode/gateway/
├── controller/GitHubController.java
└── service/ReviewManagementService.java
```

---

#### **Stream 2B: LLM Worker** (Developer B)

**Priority**: 🔴 P0 - CRITICAL PATH
**Duration**: 12 hours (1.5 days)

| Task | Time | Description | Deliverable |
|------|------|-------------|-------------|
| **T2.5** | 3h | Refine `OpenAIProvider` for streaming only | Streaming provider |
| **T2.6** | 2h | Implement retry logic with exponential backoff | Retry logic |
| **T2.7** | 2h | Add timeout handling (30s default) | Timeout config |
| **T2.8** | 2h | Request/response logging (sanitize API keys) | Secure logging |
| **T2.9** | 3h | Integration test with real OpenAI (optional) | Integration test |

**Acceptance Criteria**:
- ✅ `OpenAIProvider` streams tokens
- ✅ Retry on transient failures (3 attempts)
- ✅ Timeout after 30 seconds
- ✅ No API keys in logs
- ✅ Integration test passes

**Files to Modify**:
```
worker-llm/src/main/java/com/ghiloufi/aicode/worker/
├── provider/OpenAIProvider.java
└── adapter/OpenAIAdapter.java

worker-llm/src/test/java/com/ghiloufi/aicode/worker/
└── provider/OpenAIProviderIntegrationTest.java
```

---

#### **Stream 2C: Orchestration** (Developer C)

**Priority**: 🟡 P1 - HIGH PRIORITY
**Duration**: 12 hours (1.5 days)

| Task | Time | Description | Deliverable |
|------|------|-------------|-------------|
| **T2.10** | 4h | Create `ReviewOrchestrationService` workflow | Orchestration |
| **T2.11** | 3h | Implement `ReviewRepository` (in-memory) | Repository |
| **T2.12** | 2h | Add state tracking (pending/in_progress/completed) | State machine |
| **T2.13** | 3h | Implement PR comment publishing | GitHub publishing |

**Acceptance Criteria**:
- ✅ Workflow: fetch diff → analyze → publish
- ✅ In-memory repository functional
- ✅ State transitions tracked
- ✅ Comments posted to GitHub

**Files to Create**:
```
core/src/main/java/com/ghiloufi/aicode/core/
├── application/service/ReviewOrchestrationService.java
├── infrastructure/repository/InMemoryReviewRepository.java
└── domain/model/ReviewState.java
```

---

**SYNC POINT - Friday EOD**

**Meeting**: All developers (30 minutes)
**Agenda**:
1. Full integration demonstration
2. Test E2E workflow manually
3. Identify integration issues
4. Plan testing strategy

**Validation**:
- ✅ All Phase 2 tasks completed
- ✅ Integration tests pass
- ✅ E2E workflow functional
- ✅ No critical bugs

---

### **✨ PHASE 3: VALIDATION (Day 5-6)**

**Duration**: 1.5 days
**Team**: Developers collaborate on testing
**Priority**: 🔴 P0 - FINAL VALIDATION

#### **Stream 3A: Testing** (Developer A + B)

**Priority**: 🔴 P0 - CRITICAL
**Duration**: 12 hours (1.5 days)

| Task | Time | Description | Deliverable |
|------|------|-------------|-------------|
| **T3.1** | 4h | E2E test: Real GitHub PR → AI review → Comment | E2E test |
| **T3.2** | 3h | Error scenario tests (invalid PR, API failures) | Error tests |
| **T3.3** | 2h | Performance test: Concurrent reviews | Performance test |
| **T3.4** | 3h | Load test: SSE streaming with multiple clients | Load test |

**Acceptance Criteria**:
- ✅ E2E test with real GitHub PR passes
- ✅ Error scenarios handled gracefully
- ✅ 3 concurrent reviews complete successfully
- ✅ SSE handles 10+ concurrent clients

**Test Files to Create**:
```
api-gateway/src/test/java/com/ghiloufi/aicode/gateway/
├── integration/E2EReviewWorkflowTest.java
├── integration/ErrorScenarioTest.java
├── performance/ConcurrentReviewTest.java
└── performance/SSELoadTest.java
```

---

#### **Stream 3B: Documentation & DevOps** (Developer C)

**Priority**: 🟡 P1 - IMPORTANT
**Duration**: 12 hours (1.5 days)

| Task | Time | Description | Deliverable |
|------|------|-------------|-------------|
| **T3.5** | 2h | Update README with architecture diagram | README |
| **T3.6** | 2h | Create `docker-compose.yml` for deployment | Docker Compose |
| **T3.7** | 2h | Write deployment guide with examples | Deployment docs |
| **T3.8** | 1h | Add configuration examples (`.env.example`) | Config examples |
| **T3.9** | 2h | Document `UnifiedAIService` usage with examples | API docs |
| **T3.10** | 3h | Set up CI/CD pipeline (GitHub Actions) | CI/CD |

**Acceptance Criteria**:
- ✅ README updated and comprehensive
- ✅ Docker Compose starts successfully
- ✅ Deployment guide complete
- ✅ Configuration examples provided
- ✅ CI/CD pipeline passing

**Files to Create**:
```
deployment/
├── docker-compose.yml
├── Dockerfile
├── .env.example
└── scripts/
    ├── start.sh
    ├── stop.sh
    └── logs.sh

.github/workflows/
└── mvp-ci.yml

README.md
DEPLOYMENT.md
```

---

**FINAL CHECKPOINT - Tuesday EOD**

**Meeting**: All developers + stakeholders (1 hour)
**Agenda**:
1. Full system demonstration
2. Review all acceptance criteria
3. Collect feedback
4. Plan production deployment

**Validation**:
- ✅ All tests passing (unit + integration + E2E)
- ✅ Documentation complete
- ✅ Docker deployment works
- ✅ Stakeholders approve

---

## 📊 Dependency Graph & Critical Path

```
┌─────────────────────────────────────────────────┐
│  Day 0: CLEANUP (BLOCKING ALL)                  │
│  - Archive legacy module                        │
│  - Update pom.xml                               │
│  - Verify compilation                           │
└──────────────────┬──────────────────────────────┘
                   ↓
┌─────────────────────────────────────────────────┐
│  Day 0.5-2: FOUNDATION (Parallel)               │
├──────────────┬──────────────┬───────────────────┤
│ Stream 1A    │ Stream 1B    │ Stream 1C         │
│ Core Domain  │ AI Service   │ Infrastructure    │
│ (Dev A)      │ (Dev B)      │ (Dev C)           │
│ - Diff parse │ - Unified AI │ - Logging         │
│ - GitHub     │ - Ports      │ - Config          │
│ - Tests      │ - Adapter    │ - Health checks   │
└──────┬───────┴──────┬───────┴─────┬─────────────┘
       │              │             │
       └──────────────┼─────────────┘
                      ↓
         ┌────────────────────────┐
         │ SYNC: Wednesday EOD    │
         │ - Integration review   │
         │ - Interface validation │
         └────────────┬───────────┘
                      ↓
┌─────────────────────────────────────────────────┐
│  Day 3-4: INTEGRATION (Depends on Phase 1)      │
├──────────────┬──────────────┬───────────────────┤
│ Stream 2A    │ Stream 2B    │ Stream 2C         │
│ API Gateway  │ LLM Worker   │ Orchestration     │
│ (Dev A)      │ (Dev B)      │ (Dev C)           │
│ - Controller │ - Provider   │ - Workflow        │
│ - Use case   │ - Retry      │ - Repository      │
│ - SSE        │ - Timeout    │ - Publishing      │
└──────┬───────┴──────┬───────┴─────┬─────────────┘
       │              │             │
       └──────────────┼─────────────┘
                      ↓
         ┌────────────────────────┐
         │ SYNC: Friday EOD       │
         │ - Integration demo     │
         │ - Bug fixes            │
         └────────────┬───────────┘
                      ↓
┌─────────────────────────────────────────────────┐
│  Day 5-6: VALIDATION (Depends on Phase 2)       │
├──────────────┬──────────────────────────────────┤
│ Stream 3A    │ Stream 3B                        │
│ Testing      │ Docs & DevOps                    │
│ (Dev A+B)    │ (Dev C)                          │
│ - E2E tests  │ - README                         │
│ - Error tests│ - Docker Compose                 │
│ - Perf tests │ - Deployment guide               │
│ - Load tests │ - CI/CD pipeline                 │
└──────┬───────┴──────┬───────────────────────────┘
       │              │
       └──────────────┘
                ↓
    ┌───────────────────────┐
    │ FINAL: Tuesday EOD    │
    │ - Full demo           │
    │ - Stakeholder review  │
    │ - Production ready    │
    └───────────────────────┘
```

**Critical Path** (Longest dependency chain):
```
Cleanup → Stream 1A (Core) → Stream 2A (API) → Testing → Demo
Total: 0.5 + 1.5 + 1.5 + 1.5 + 1 = 6 days
```

**Parallel Efficiency**:
- 3 developers working in parallel
- ~60% time savings vs sequential
- 6 days instead of ~15 days sequential

---

## 🎯 Success Criteria

### Technical Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| **Compilation** | ✅ Zero errors | `mvn clean compile` |
| **Unit Tests** | ✅ >70% coverage | JaCoCo report |
| **Integration Tests** | ✅ All passing | `mvn verify` |
| **E2E Test** | ✅ Passes | Manual execution |
| **Response Time** | ✅ <60s for typical PR | Performance test |
| **SSE First Chunk** | ✅ <5s | Load test |
| **Console Output** | ✅ Zero System.out in src/main | `grep -r "System\.out" src/main` |
| **Secrets** | ✅ All externalized | Code review |
| **Health Check** | ✅ Returns 200 | `curl /actuator/health` |

### Functional Metrics

| Feature | Status | Validation |
|---------|--------|------------|
| **Fetch GitHub PR** | ✅ | API call succeeds |
| **Parse Diff** | ✅ | Unit tests pass |
| **AI Analysis** | ✅ | Integration test |
| **Stream Results** | ✅ | SSE client receives chunks |
| **Post Comment** | ✅ | Comment appears on PR |
| **Unified AI Service** | ✅ | Single entry point used |
| **Error Handling** | ✅ | Graceful degradation |
| **Health Endpoint** | ✅ | Returns accurate status |

### Business Metrics

| Criterion | Target | Validation |
|-----------|--------|------------|
| **Demo-able** | ✅ | Stakeholder demo completed |
| **Deployable** | ✅ | Docker Compose works |
| **Documented** | ✅ | README complete |
| **Testable** | ✅ | All test suites pass |
| **Production Ready** | ✅ | Security review passed |

---

## ⚠️ Risk Management

### 🔴 HIGH RISK: OpenAI Rate Limits

**Impact**: Could block development, testing, and demo
**Probability**: High (60%)

**Mitigation Strategies**:
1. **Circuit Breaker**: Implement Resilience4j circuit breaker
   - Fail fast after threshold
   - Automatic recovery

2. **Cost Optimization**: Use `gpt-4o-mini`
   - 60% cheaper than `gpt-4`
   - Faster response times
   - Sufficient quality for MVP

3. **Development Mode**: Add mock AI provider
   ```java
   @Profile("dev")
   public class MockAIAdapter implements AIInteractionPort {
       // Return canned responses for testing
   }
   ```

4. **Response Caching**: Cache common diff patterns
   - Reduces API calls
   - Improves test reliability

5. **Rate Monitoring**: Log API usage
   - Track daily quota
   - Alert on threshold (80%)

**Fallback Plan**: If OpenAI unavailable during demo, use recorded responses

---

### 🟡 MEDIUM RISK: Legacy Dependencies

**Impact**: Hidden dependencies could break build
**Probability**: Medium (40%)

**Mitigation Strategies**:
1. **Comprehensive Search**:
   ```bash
   grep -r "import.*legacy" . --include="*.java"
   grep -r "com.ghiloufi.aicode.legacy" . --include="*.java"
   grep -r "legacy" pom.xml
   ```

2. **Immediate Verification**:
   - Run `mvn clean compile` after each import fix
   - Don't batch changes

3. **Team Cleanup**: All developers together
   - Pair review of changes
   - Shared responsibility

4. **Rollback Plan**: Git branch for safety
   ```bash
   git checkout -b cleanup/remove-legacy
   # ... make changes ...
   # If issues: git checkout main
   ```

**Fallback Plan**: If cleanup takes >4 hours, defer and work around legacy

---

### 🟡 MEDIUM RISK: SSE Streaming Complexity

**Impact**: Streaming might not work correctly
**Probability**: Medium (30%)

**Mitigation Strategies**:
1. **Use Existing Pattern**: `GitHubController` already has SSE
   - Copy working implementation
   - Minimal modifications

2. **Early Testing**: Test streaming on Day 3
   - Don't wait until Phase 3
   - Allows time for fixes

3. **Incremental Implementation**:
   - First: Non-streaming endpoint
   - Then: Add SSE on top
   - Validate each step

4. **Fallback to Polling**: If SSE fails
   ```java
   // Plan B: Polling endpoint
   @GetMapping("/reviews/{id}/status")
   public Mono<ReviewStatus> getStatus(@PathVariable String id)
   ```

**Fallback Plan**: Use polling instead of SSE for MVP

---

### 🟢 LOW RISK: Integration Points

**Impact**: Components might not connect smoothly
**Probability**: Low (20%)

**Mitigation Strategies**:
1. **Interface-First Design**: Define interfaces before implementation
   - `AIInteractionPort` agreed Day 1
   - `GitHubPort` already exists
   - DTOs documented

2. **Daily Sync**: 15-minute standup
   - Share progress
   - Surface blockers early
   - Adjust plans

3. **Checkpoint Reviews**: Wednesday and Friday
   - Integration testing
   - Interface validation
   - Quick adjustments

4. **Pair Programming**: For complex integrations
   - Developer A + B for AI integration
   - Real-time problem solving

**Fallback Plan**: Add adapter layer if interfaces don't match

---

## 📚 Configuration Strategy

### Environment Variables

**Required**:
```bash
# OpenAI Configuration
export OPENAI_API_KEY="sk-proj-..."

# GitHub Configuration
export GITHUB_TOKEN="ghp_..."
```

**Optional**:
```bash
# Override defaults
export AI_MODEL="gpt-4o"              # Default: gpt-4o-mini
export AI_TEMPERATURE="0.2"            # Default: 0.1
export AI_MAX_TOKENS="8000"            # Default: 4000
export AI_TIMEOUT_SECONDS="90"         # Default: 60

export GITHUB_TIMEOUT_SECONDS="45"     # Default: 30
export GITHUB_MAX_REPOSITORIES="100"   # Default: 50

export LOG_LEVEL="INFO"                # Default: DEBUG (mvp profile)
export SERVER_PORT="9090"              # Default: 8080
```

### Application Configuration

**File**: `api-gateway/src/main/resources/application-mvp.yml`

```yaml
# ============================================
# AI Code Reviewer MVP Configuration
# ============================================

spring:
  application:
    name: ai-code-reviewer-mvp
  profiles:
    active: mvp
  webflux:
    base-path: /
  jackson:
    serialization:
      write-dates-as-timestamps: false

# ============================================
# Unified AI Configuration
# ============================================
ai:
  provider: openai  # Single provider for MVP

  unified-service:
    timeout-seconds: ${AI_TIMEOUT_SECONDS:60}
    max-retries: 3
    circuit-breaker:
      failure-threshold: 50.0
      wait-duration: 30s
      slow-call-threshold: 10s

  openai:
    api-key: ${OPENAI_API_KEY}
    base-url: https://api.openai.com/v1
    model: ${AI_MODEL:gpt-4o-mini}
    temperature: ${AI_TEMPERATURE:0.1}
    max-tokens: ${AI_MAX_TOKENS:4000}
    stream-enabled: true
    timeout: ${AI_TIMEOUT_SECONDS:60}s

# ============================================
# GitHub SCM Configuration
# ============================================
scm:
  github:
    token: ${GITHUB_TOKEN}
    base-url: https://api.github.com
    timeout-seconds: ${GITHUB_TIMEOUT_SECONDS:30}
    max-repositories: ${GITHUB_MAX_REPOSITORIES:50}
    rate-limit-protection: true

# ============================================
# Review Configuration
# ============================================
review:
  max-concurrent: 3
  result-retention-hours: 24
  enable-publishing: true
  publish-mode: comment  # comment | check-run

  orchestration:
    workflow:
      - fetch-diff
      - analyze-code
      - publish-results
    timeout-minutes: 10

# ============================================
# Server Configuration
# ============================================
server:
  port: ${SERVER_PORT:8080}
  shutdown: graceful
  http2:
    enabled: true

# ============================================
# Reactor Configuration
# ============================================
reactor:
  netty:
    pool:
      max-connections: 500
      acquire-timeout: 30s
    ioWorkerCount: 4

# ============================================
# Logging Configuration
# ============================================
logging:
  level:
    root: INFO
    com.ghiloufi.aicode: ${LOG_LEVEL:DEBUG}
    org.springframework.web.reactive: INFO
    reactor.netty: INFO
    org.kohsuke.github: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/ai-code-reviewer.log
    max-size: 10MB
    max-history: 7

# ============================================
# Management & Metrics
# ============================================
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
  health:
    circuit-breakers:
      enabled: true
    livenessstate:
      enabled: true
    readinessstate:
      enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
      environment: mvp
    export:
      prometheus:
        enabled: true

# ============================================
# Resilience4j Circuit Breaker
# ============================================
resilience4j:
  circuitbreaker:
    instances:
      openai:
        register-health-indicator: true
        sliding-window-size: 10
        minimum-number-of-calls: 5
        permitted-number-of-calls-in-half-open-state: 3
        wait-duration-in-open-state: 30s
        failure-rate-threshold: 50
        slow-call-rate-threshold: 50
        slow-call-duration-threshold: 10s

      github:
        register-health-indicator: true
        sliding-window-size: 10
        minimum-number-of-calls: 5
        wait-duration-in-open-state: 10s
        failure-rate-threshold: 50

  retry:
    instances:
      openai:
        max-attempts: 3
        wait-duration: 1s
        exponential-backoff-multiplier: 2
        retry-exceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException

      github:
        max-attempts: 3
        wait-duration: 500ms
        exponential-backoff-multiplier: 1.5

# ============================================
# OpenAPI Documentation
# ============================================
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
  info:
    title: AI Code Reviewer API
    version: 1.0.0-MVP
    description: Automated code review powered by AI
```

---

## 🐳 Docker Deployment

### Dockerfile

**File**: `deployment/Dockerfile`

```dockerfile
# ============================================
# Stage 1: Build Application
# ============================================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy dependency files first (layer caching)
COPY ../pom.xml .
COPY common/pom.xml common/
COPY ../core/pom.xml core/
COPY ../api-gateway/pom.xml api-gateway/
COPY worker-llm/pom.xml worker-llm/
COPY backplane/pom.xml backplane/
COPY open-api/pom.xml open-api/

# Download dependencies (cached if pom.xml unchanged)
RUN mvn dependency:go-offline -B

# Copy source code
COPY common/ common/
COPY ../core core/
COPY ../api-gateway api-gateway/
COPY worker-llm/ worker-llm/
COPY backplane/ backplane/
COPY open-api/ open-api/

# Build application (skip tests for faster builds)
RUN mvn clean package -DskipTests -pl api-gateway -am

# ============================================
# Stage 2: Runtime Image
# ============================================
FROM eclipse-temurin:21-jre-alpine

# Install curl for health checks
RUN apk add --no-cache curl

WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/api-gateway/target/*.jar app.jar

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=40s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM options
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### Docker Compose

**File**: `deployment/docker-compose.yml`

```yaml
version: '3.8'

services:
  # ============================================
  # AI Code Reviewer Service
  # ============================================
  ai-code-reviewer:
    build:
      context: ..
      dockerfile: deployment/Dockerfile
    container_name: ai-code-reviewer-mvp
    ports:
      - "8080:8080"
    environment:
      # Spring Profile
      - SPRING_PROFILES_ACTIVE=mvp

      # API Keys (from .env file)
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - GITHUB_TOKEN=${GITHUB_TOKEN}

      # Optional Overrides
      - LOG_LEVEL=${LOG_LEVEL:-DEBUG}
      - AI_MODEL=${AI_MODEL:-gpt-4o-mini}
      - SERVER_PORT=8080

      # JVM Options
      - JAVA_OPTS=-Xmx512m -Xms256m -XX:+UseG1GC

    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

    restart: unless-stopped

    networks:
      - ai-reviewer-network

    volumes:
      - app-logs:/app/logs

  # ============================================
  # Prometheus (Optional - Metrics)
  # ============================================
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
    depends_on:
      - ai-code-reviewer
    networks:
      - ai-reviewer-network
    restart: unless-stopped

  # ============================================
  # Grafana (Optional - Dashboards)
  # ============================================
  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
      - GF_USERS_ALLOW_SIGN_UP=false
    volumes:
      - grafana-data:/var/lib/grafana
    depends_on:
      - prometheus
    networks:
      - ai-reviewer-network
    restart: unless-stopped

networks:
  ai-reviewer-network:
    driver: bridge

volumes:
  app-logs:
  prometheus-data:
  grafana-data:
```

### Environment File

**File**: `deployment/.env.example`

```bash
# ============================================
# AI Code Reviewer Environment Configuration
# ============================================

# ============================================
# Required: API Keys
# ============================================
OPENAI_API_KEY=sk-proj-your-key-here
GITHUB_TOKEN=ghp_your-token-here

# ============================================
# Optional: Configuration Overrides
# ============================================

# Logging
LOG_LEVEL=DEBUG

# AI Configuration
AI_MODEL=gpt-4o-mini
AI_TEMPERATURE=0.1
AI_MAX_TOKENS=4000
AI_TIMEOUT_SECONDS=60

# GitHub Configuration
GITHUB_TIMEOUT_SECONDS=30
GITHUB_MAX_REPOSITORIES=50

# Server Configuration
SERVER_PORT=8080

# ============================================
# Usage Instructions
# ============================================
# 1. Copy this file: cp .env.example .env
# 2. Edit .env and add your real API keys
# 3. Run: docker-compose up -d
```

### Startup Script

**File**: `deployment/scripts/start.sh`

```bash
#!/bin/bash

# ============================================
# AI Code Reviewer - Startup Script
# ============================================

set -e  # Exit on error

echo "🚀 Starting AI Code Reviewer MVP..."

# Change to deployment directory
cd "$(dirname "$0")/.."

# ============================================
# 1. Check Prerequisites
# ============================================
echo "📋 Checking prerequisites..."

# Check Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker not found. Please install Docker."
    exit 1
fi

# Check Docker Compose
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose not found. Please install Docker Compose."
    exit 1
fi

echo "✅ Docker and Docker Compose found"

# ============================================
# 2. Check Environment File
# ============================================
echo "📋 Checking environment configuration..."

if [ ! -f .env ]; then
    echo "⚠️  .env file not found"
    echo "📝 Copying from .env.example..."
    cp .env.example .env
    echo ""
    echo "⚠️  IMPORTANT: Please edit .env and add your API keys:"
    echo "   - OPENAI_API_KEY"
    echo "   - GITHUB_TOKEN"
    echo ""
    echo "Then run this script again."
    exit 1
fi

# Validate required variables
source .env

if [ -z "$OPENAI_API_KEY" ] || [ "$OPENAI_API_KEY" = "sk-proj-your-key-here" ]; then
    echo "❌ OPENAI_API_KEY not set in .env"
    echo "Please edit .env and add your OpenAI API key"
    exit 1
fi

if [ -z "$GITHUB_TOKEN" ] || [ "$GITHUB_TOKEN" = "ghp_your-token-here" ]; then
    echo "❌ GITHUB_TOKEN not set in .env"
    echo "Please edit .env and add your GitHub token"
    exit 1
fi

echo "✅ Environment configuration valid"

# ============================================
# 3. Stop Existing Containers
# ============================================
echo "🛑 Stopping existing containers..."
docker-compose down 2>/dev/null || true

# ============================================
# 4. Build and Start
# ============================================
echo "🔨 Building Docker image..."
docker-compose build --no-cache

echo "🚀 Starting containers..."
docker-compose up -d

# ============================================
# 5. Wait for Health Check
# ============================================
echo "⏳ Waiting for application to be healthy..."

max_attempts=30
attempt=0

while [ $attempt -lt $max_attempts ]; do
    if curl -f http://localhost:8080/actuator/health &>/dev/null; then
        echo "✅ Application is healthy!"
        break
    fi

    attempt=$((attempt + 1))
    echo "   Attempt $attempt/$max_attempts..."
    sleep 2
done

if [ $attempt -eq $max_attempts ]; then
    echo "❌ Application failed to start"
    echo "📋 Checking logs..."
    docker-compose logs --tail=50 ai-code-reviewer
    exit 1
fi

# ============================================
# 6. Display Information
# ============================================
echo ""
echo "=========================================="
echo "✅ AI Code Reviewer MVP Started!"
echo "=========================================="
echo ""
echo "📊 Dashboard:     http://localhost:8080"
echo "🏥 Health Check:  http://localhost:8080/actuator/health"
echo "📖 API Docs:      http://localhost:8080/swagger-ui.html"
echo "📈 Prometheus:    http://localhost:9090"
echo "📊 Grafana:       http://localhost:3000 (admin/admin)"
echo ""
echo "🔧 Useful Commands:"
echo "   View logs:     docker-compose logs -f ai-code-reviewer"
echo "   Stop:          docker-compose down"
echo "   Restart:       docker-compose restart"
echo ""
echo "🧪 Test API:"
echo "   curl http://localhost:8080/actuator/health"
echo ""
echo "=========================================="
```

### Stop Script

**File**: `deployment/scripts/stop.sh`

```bash
#!/bin/bash

# ============================================
# AI Code Reviewer - Stop Script
# ============================================

set -e

echo "🛑 Stopping AI Code Reviewer..."

cd "$(dirname "$0")/.."

docker-compose down

echo "✅ AI Code Reviewer stopped"
```

### Logs Script

**File**: `deployment/scripts/logs.sh`

```bash
#!/bin/bash

# ============================================
# AI Code Reviewer - Logs Script
# ============================================

cd "$(dirname "$0")/.."

# Follow logs by default
docker-compose logs -f ai-code-reviewer
```

---

## 🧪 Testing Strategy

### Testing Pyramid

```
         /\
        /E2E\        5% - Full workflow (3-5 tests)
       /------\
      / Integ \      15% - Module integration (10-15 tests)
     /----------\
    /   Unit     \   80% - Unit tests (100+ tests)
   /--------------\
```

### Unit Tests (80% of tests)

**Priority 1 - Core Domain**:

```java
// UnifiedDiffParserTest.java
@Test
void shouldParseSimpleDiff() { }

@Test
void shouldHandleMultipleFiles() { }

@Test
void shouldParseHunkHeaders() { }

@Test
void shouldHandleRenames() { }

@Test
void shouldHandleBinaryFiles() { }
```

**Priority 2 - AI Service**:

```java
// UnifiedAIServiceTest.java
@ExtendWith(MockitoExtension.class)
class UnifiedAIServiceTest {

    @Mock
    private AIInteractionPort aiPort;

    @Mock
    private PromptBuilder promptBuilder;

    @InjectMocks
    private UnifiedAIService service;

    @Test
    void shouldStreamReviewChunks() {
        // Given
        when(promptBuilder.buildReviewPrompt(any(), any()))
            .thenReturn("prompt");
        when(aiPort.streamCompletion("prompt"))
            .thenReturn(Flux.just("chunk1", "chunk2"));

        // When
        StepVerifier.create(service.reviewCodeStreaming(diff, config))
            // Then
            .expectNextCount(2)
            .verifyComplete();
    }

    @Test
    void shouldRetryOnFailure() { }

    @Test
    void shouldTimeoutAfter60Seconds() { }

    @Test
    void shouldHandleCircuitBreakerOpen() { }
}
```

**Priority 3 - API Layer**:

```java
// GitHubControllerTest.java
@WebFluxTest(GitHubController.class)
class GitHubControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ReviewManagementUseCase useCase;

    @Test
    void shouldStartReview() {
        // Given
        when(useCase.startReview(any(), anyInt()))
            .thenReturn(Mono.just(reviewResult));

        // When
        webTestClient.post()
            .uri("/api/v1/scm/owner/repo/pull-requests/123/analyze")
            .exchange()
            // Then
            .expectStatus().isAccepted()
            .expectBody()
            .jsonPath("$.reviewId").exists()
            .jsonPath("$.status").isEqualTo("in_progress");
    }

    @Test
    void shouldValidateInput() { }

    @Test
    void shouldHandleInvalidPR() { }
}
```

### Integration Tests (15% of tests)

```java
// ReviewWorkflowIntegrationTest.java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "ai.openai.api-key=test-key",
    "scm.github.token=test-token"
})
class ReviewWorkflowIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private GitHub gitHub;

    @MockBean
    private AIInteractionPort aiPort;

    @Test
    void shouldCompleteFullReviewWorkflow() {
        // Given: Mock GitHub PR
        GHRepository mockRepo = mock(GHRepository.class);
        GHPullRequest mockPR = mock(GHPullRequest.class);
        when(gitHub.getRepository("owner/repo")).thenReturn(mockRepo);
        when(mockRepo.getPullRequest(123)).thenReturn(mockPR);

        // And: Mock AI responses
        when(aiPort.streamCompletion(anyString()))
            .thenReturn(Flux.just("{\"issues\": []}"));

        // When: Initiate review
        webTestClient.post()
            .uri("/api/v1/scm/owner/repo/pull-requests/123/analyze")
            .exchange()
            // Then: Verify accepted
            .expectStatus().isAccepted()
            .expectBody()
            .jsonPath("$.reviewId").exists()
            .jsonPath("$.status").isEqualTo("in_progress");

        // And: Verify workflow executed
        verify(gitHub).getRepository("owner/repo");
        verify(aiPort).streamCompletion(anyString());
    }

    @Test
    void shouldHandleGitHubAPIFailure() { }

    @Test
    void shouldHandleAIServiceFailure() { }
}
```

### E2E Tests (5% of tests)

```java
// E2EReviewWorkflowTest.java
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = {
    "ai.openai.api-key=${OPENAI_API_KEY}",  // Real key
    "scm.github.token=${GITHUB_TOKEN}"       // Real token
})
class E2EReviewWorkflowTest {

    private static final String TEST_REPO = "your-org/test-repo";
    private static final int TEST_PR = 1;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @Disabled("Requires real API keys - run manually")
    void shouldReviewRealGitHubPR() {
        // When: Trigger review of real PR
        String reviewId = webTestClient.post()
            .uri("/api/v1/scm/{owner}/{repo}/pull-requests/{number}/analyze",
                 "your-org", "test-repo", TEST_PR)
            .exchange()
            .expectStatus().isAccepted()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Then: Wait for completion (up to 2 minutes)
        await().atMost(2, MINUTES)
            .pollInterval(5, SECONDS)
            .until(() -> {
                String status = webTestClient.get()
                    .uri("/api/v1/reviews/{id}/status", reviewId)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();
                return "completed".equals(status);
            });

        // And: Verify comment posted on GitHub
        // ... check GitHub PR comments ...
    }
}
```

### Performance Tests

```java
// ConcurrentReviewTest.java
@SpringBootTest
class ConcurrentReviewTest {

    @Test
    void shouldHandle3ConcurrentReviews() {
        // Given: 3 different PRs
        List<Mono<String>> reviews = Arrays.asList(
            startReview("repo1", 1),
            startReview("repo2", 2),
            startReview("repo3", 3)
        );

        // When: Execute in parallel
        List<String> results = Flux.merge(reviews)
            .collectList()
            .block(Duration.ofSeconds(120));

        // Then: All complete successfully
        assertThat(results).hasSize(3);
        assertThat(results).allMatch(status -> status.equals("completed"));
    }
}
```

### Test Data Fixtures

**Structure**:
```
src/test/resources/
├── fixtures/
│   ├── github/
│   │   ├── pr-diff-simple.txt
│   │   ├── pr-diff-multifile.txt
│   │   ├── pr-diff-rename.txt
│   │   └── pr-response.json
│   ├── openai/
│   │   ├── stream-chunk-001.json
│   │   ├── stream-chunk-002.json
│   │   └── completion-response.json
│   └── reviews/
│       ├── review-result-valid.json
│       └── review-result-invalid.json
```

---

## 📖 Documentation Requirements

### README.md

**Sections**:
1. **Project Overview** with architecture diagram
2. **Features** list
3. **Quick Start** (5-minute setup)
4. **Configuration** guide
5. **API Documentation** link
6. **Development** setup
7. **Testing** instructions
8. **Deployment** guide
9. **Troubleshooting** common issues
10. **Contributing** guidelines

### DEPLOYMENT.md

**Sections**:
1. **Prerequisites**
2. **Environment Setup**
3. **Docker Deployment**
4. **Kubernetes Deployment** (future)
5. **Monitoring** setup
6. **Backup & Recovery**
7. **Scaling** guidelines

### API Documentation

**Tools**:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI Spec: `http://localhost:8080/api-docs`

**Key Endpoints**:
```
POST   /api/v1/scm/{owner}/{repo}/pull-requests/{number}/analyze
GET    /api/v1/scm/{owner}/{repo}/pull-requests/{number}/status
GET    /api/v1/scm/{owner}/{repo}/pull-requests/{number}/stream  (SSE)
GET    /actuator/health
GET    /actuator/metrics
```

---

## 🎓 MVP Launch Checklist

### Pre-Launch (Day 5)

**Environment**:
- [ ] `.env` file created with real API keys
- [ ] Environment variables validated
- [ ] Secrets not in source control

**Build & Test**:
- [ ] `mvn clean compile` succeeds
- [ ] All unit tests pass (`mvn test`)
- [ ] Integration tests pass (`mvn verify`)
- [ ] E2E test executed manually
- [ ] No `System.out.println` in `src/main/`

**Docker**:
- [ ] Docker image builds successfully
- [ ] `docker-compose up` works
- [ ] Health check returns 200
- [ ] Logs visible via `docker-compose logs`

**Documentation**:
- [ ] README updated
- [ ] DEPLOYMENT guide complete
- [ ] API documentation accessible
- [ ] Configuration examples provided

### Demo Preparation (Day 6 Morning)

**Demo Environment**:
- [ ] Test GitHub repository ready
- [ ] Sample PR created for demo
- [ ] OpenAI API key with sufficient credits
- [ ] Fallback plan if API fails

**Demo Script**:
- [ ] Introduction prepared (2 minutes)
- [ ] Live demo flow planned (10 minutes)
- [ ] Backup slides ready
- [ ] Q&A preparation

**Stakeholders**:
- [ ] Meeting invite sent
- [ ] Demo URL shared
- [ ] Access credentials provided (if needed)

### Launch (Day 6 Afternoon)

**Final Validation**:
- [ ] All acceptance criteria met
- [ ] No critical bugs
- [ ] Performance metrics within targets
- [ ] Security review passed

**Demo Execution**:
- [ ] Full system demonstration
- [ ] Stakeholder feedback collected
- [ ] Critical issues logged
- [ ] Next iteration planned

**Post-Demo**:
- [ ] Feedback documented
- [ ] Production deployment plan created
- [ ] Team retrospective scheduled

---

## 🚧 Known Limitations & Future Enhancements

### MVP Limitations

1. **Single LLM Provider**: OpenAI only
   - Future: Add Ollama, Claude, Gemini

2. **In-Memory Storage**: Reviews not persisted
   - Future: Add PostgreSQL

3. **GitHub Only**: No other SCM systems
   - Future: GitLab, Bitbucket, Azure DevOps

4. **Basic Error Handling**: Limited retry logic
   - Future: Advanced circuit breaking, fallbacks

5. **No Caching**: Every review calls AI
   - Future: Redis caching layer

6. **Manual Deployment**: Docker Compose only
   - Future: Kubernetes, Helm charts

### Phase 4+ Roadmap

**Week 3-4: Multi-Provider Support**
- [ ] Add Ollama provider
- [ ] Add Anthropic Claude provider
- [ ] Provider selection configuration
- [ ] Cost optimization strategies

**Month 2: Advanced Features**
- [ ] Static analysis integration (PMD, Checkstyle)
- [ ] Multiple SCM support (GitLab, Bitbucket)
- [ ] Persistent storage (PostgreSQL)
- [ ] Advanced caching (Redis)
- [ ] Webhook support

**Month 3: Production Hardening**
- [ ] Kubernetes deployment
- [ ] Prometheus metrics
- [ ] Distributed tracing (Jaeger)
- [ ] Rate limiting and quotas
- [ ] Multi-tenancy support

---

## 📞 Support & Feedback

### Getting Help

**Issues**: https://github.com/your-org/ai-code-reviewer/issues
**Discussions**: https://github.com/your-org/ai-code-reviewer/discussions
**Email**: team@example.com

### Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for:
- Code style guidelines
- Pull request process
- Testing requirements
- Documentation standards

---

## 📝 Change Log

**Version 1.0.0-MVP** (2025-11-02)
- Initial MVP release
- GitHub integration
- OpenAI code review
- SSE streaming
- Docker deployment
- Basic documentation

---

**Generated**: 2025-11-02
**Last Updated**: 2025-11-02
**Version**: 1.0.0-MVP
**Status**: Ready for Implementation

---

**Next Steps**:
1. Review and approve this plan
2. Assign developers to streams
3. Set up development environment
4. Execute Phase 0 cleanup
5. Begin parallel development!
