# 🤖 Projet : AI Code Reviewer

Ce projet est une application **Spring Boot** permettant de faire une **revue de code automatisée via un LLM** sur des
diffs GitHub ou locaux. Il utilise :

- Un **collecteur de diffs** (local ou GitHub).
- Des outils d’**analyse statique**.
- Un **LLM (Large Language Model)** pour analyser et commenter le code.
- Un système de **publication** des résultats (console ou GitHub PR).

L’objectif est de migrer la logique vers **Spring IoC**, améliorer la **maintenabilité**, la **testabilité** et
respecter les **bonnes pratiques modernes** (Clean Code, SOLID, Spring idioms).

---

# 📘 Instructions pour Claude – Refactorisation et Bonnes Pratiques avec Spring IoC

## 🎯 Objectif

Refactoriser ce projet Java afin de :

- Migrer la création manuelle des objets vers **Spring IoC (Inversion of Control)**.
- Améliorer la **maintenabilité** et la **testabilité** du code.
- Appliquer les **bonnes pratiques de développement (SOLID, Clean Code, Spring Boot idioms)**.
- Fournir un code **documenté** et **testé** (unitaires + intégration).

---

## 🔧 Directives générales

### 1. Spring IoC & Dependency Injection

- Supprimer les instanciations manuelles avec `new` dans `Application` et autres services.
- Annoter les services avec `@Service`, `@Component` ou `@Configuration`.
- Déclarer les dépendances via **injection constructeur**.
- Externaliser la configuration avec `@ConfigurationProperties` (ex. `ApplicationConfig`).

### 2. Architecture & Organisation

- Créer un **orchestrateur central** (`CodeReviewOrchestrator`) qui coordonne les étapes principales.
- Regrouper les classes dans des packages cohérents :
  ```
  config       → configuration Spring, properties
  service      → logique métier (diff, merge, analyse)
  client       → intégrations externes (GitHub, LLM)
  model        → objets métiers (ReviewResult, Diff, etc.)
  validation   → validation JSON, règles
  orchestrator → coordination du workflow
  ```
- Respecter **SRP** (Single Responsibility Principle) : une classe = une responsabilité claire.

### 3. Qualité & Robustesse

- Utiliser `try-with-resources` pour IO/HTTP.
- Créer des exceptions métiers claires (`DiffException`, `ValidationException`, etc.).
- Logger avec `slf4j` :
    - `info` = suivi utilisateur
    - `debug` = diagnostic développeur
    - `error` = erreurs critiques
- Préférer des objets immuables ou DTO immutables.

### 4. Testabilité

- Introduire des **interfaces** pour `GithubClient`, `LlmClient` afin de faciliter les mocks.
- Utiliser JUnit 5 + Mockito ou Spring `@MockBean`.
- Ajouter des **tests unitaires** pour :
    - `UnifiedDiffParser` (parser un diff réel)
    - `ReviewResultMerger` (fusion de résultats)
    - `LlmReviewValidator` (validation JSON)
- Ajouter des **tests d’intégration Spring Boot** pour vérifier l’orchestrateur complet.

### 5. Documentation

- Ajouter des **JavaDocs** sur toutes les classes et méthodes publiques.
- Générer un `README.md` avec :
    - Explication de l’architecture.
    - Instructions de build/test.
    - Exemples d’utilisation (mode local + GitHub).

---

## ✅ Tâches concrètes

### 1. Refactoriser `Application`

- Garder uniquement le bootstrap Spring Boot :
  ```java
  @SpringBootApplication
  public class Application {
      public static void main(String[] args) {
          SpringApplication.run(Application.class, args);
      }
  }
  ```
- Supprimer `initializeServices` → remplacé par injection Spring.

### 2. Créer `CodeReviewOrchestrator`

- Annoté `@Service`.
- Dépendances injectées : `DiffCollectionService`, `LlmClient`, `GithubClient`, `ReviewResultMerger`, etc.
- Contient la logique métier principale :
    1. Collecter le diff
    2. Lancer l’analyse (statique + LLM)
    3. Fusionner les résultats
    4. Publier (console ou GitHub)

### 3. Transformer les services en Beans Spring

- `GithubClient`, `LlmClient`, `DiffCollectionService`, `ReviewResultMerger`, `PromptBuilder` → `@Service` ou
  `@Component`.
- Créer des interfaces (`IGithubClient`, `ILlmClient`) pour faciliter les tests.

### 4. Externaliser la configuration

- Créer une classe properties :
  ```java
  @ConfigurationProperties(prefix = "reviewer")
  public class ReviewerProperties {
      private String mode;
      private String repository;
      private int timeoutSeconds;
      private int maxLinesPerChunk;
      // + getters/setters
  }
  ```
- Définir `application.yml` :
  ```yaml
  reviewer:
    mode: github
    repository: my-org/my-repo
    branch: main
    java-version: 17
    build-system: maven
    max-lines: 1500
    context: 5
    timeout: 45
  ```

### 5. Ajouter des tests

- Exemple de test unitaire :
  ```java
  @SpringBootTest
  class DiffCollectionServiceTest {
      @Autowired DiffCollectionService service;

      @Test void testParseSimpleDiff() {
          String diff = "--- a/Test.java\n+++ b/Test.java\n@@ -1,1 +1,1 @@\n- old\n+ new\n";
          DiffAnalysisBundle result = service.collectFromLocalGit("HEAD~1", "HEAD"); 
          assertThat(result.getTotalLineCount()).isGreaterThan(0);
      }
  }
  ```

---

## 📦 Livrables attendus

- Code refactorisé et organisé par packages, avec IoC complet.
- Documentation claire (`README.md` + JavaDocs).
- Tests unitaires et d'intégration avec ≥ 70% de couverture.
- Maintien de la compatibilité avec le workflow actuel (mode local et GitHub).

---

## 🎯 Code Quality Standards

### CRITICAL: Java 21 LTS - Project Standard

**This project uses Java 21 LTS exclusively.**

All generated code MUST use modern Java 21 features:
- Records for immutable data structures
- Pattern matching for instanceof and switch
- Text blocks for multi-line strings
- Sealed classes for restricted hierarchies
- Virtual threads for concurrency
- Enhanced switch expressions
- Local variable type inference (var) where it improves readability

**NO Java 22+ only features** - maintain Java 21 LTS compatibility

### 1. CLARITY
Code must be self-explanatory with clear intent:
- Use descriptive names for classes, methods, and variables
- Code should read like well-written prose
- Intent should be immediately obvious without documentation

### 2. CLEANLINESS
Follow clean code principles:
- Single Responsibility Principle for classes and methods
- No code duplication (DRY principle)
- Proper separation of concerns
- Consistent formatting and style

### 3. READABILITY
Code must be easy to read and understand:
- Use meaningful variable and method names
- Keep methods short and focused (ideally under 20 lines)
- Proper indentation and spacing
- Clear control flow without deeply nested structures

**CRITICAL: No Fully Qualified Names (FQN)**
- **NEVER use fully qualified class names in code** - this is ugly and reduces readability
- Always add proper import statements at the top of the file instead
- This applies to ALL code: production, tests, annotations, and configuration

**Prohibited Examples:**
```java
// ❌ WRONG - Ugly FQN usage
java.util.List<String> items = new java.util.ArrayList<>();
reactor.core.publisher.Flux<Data> flux = reactor.core.publisher.Flux.empty();
@SpringBootTest(classes = {com.example.config.AppConfig.class})
new com.example.service.MyService();

// ✅ CORRECT - Use imports
import java.util.List;
import java.util.ArrayList;
import reactor.core.publisher.Flux;
import com.example.config.AppConfig;
import com.example.service.MyService;

List<String> items = new ArrayList<>();
Flux<Data> flux = Flux.empty();
@SpringBootTest(classes = {AppConfig.class})
new MyService();
```

**Exception:** Only use FQN to resolve naming conflicts between different packages:
```java
// ✅ Acceptable - Resolving naming conflict
import java.util.Date;
java.sql.Date sqlDate = new java.sql.Date(System.currentTimeMillis());
```

**Enforcement:**
- Review all code for FQN before committing
- Check annotations (especially `@SpringBootTest`, `@Import`, etc.)
- Check object instantiations and static method calls
- Check generic type parameters

### 4. PRODUCTION-READY
Code must be robust and maintainable:
- Proper error handling and validation
- No hardcoded values or magic numbers
- Consider edge cases
- Thread-safe where applicable
- Performance-conscious but favor clarity over premature optimization

### 5. ENCAPSULATION & IMMUTABILITY
Enforce by default whenever possible:
- All class fields should be `private final` where possible
- All constructors should be marked `final` (implicit for non-abstract classes)
- All local variables should be marked `final`
- Prefer immutable data structures (records, List.of(), Set.of(), Map.of())
- Use defensive copying when returning mutable objects

**EXCEPTION: Spring Configuration Classes**
- Classes annotated with `@Configuration` MUST NOT be marked `final`
- Spring requires non-final classes to create CGLIB proxies for bean methods
- This applies to: `@Configuration`, `@ConfigurationProperties`
- Example: `public class DangerousPatternsConfig` (NOT `public final class`)

### 6. LOMBOK ANNOTATIONS

**CRITICAL: Use Lombok annotations to reduce boilerplate code.**

Lombok is included in the project dependencies and MUST be used whenever applicable:

**Required Annotations:**
- `@Getter` - Replace manual getter methods on classes with final fields
- `@RequiredArgsConstructor` - Replace manual constructors that only assign final fields
- `@Slf4j` - Replace manual `private static final Logger logger = LoggerFactory.getLogger(...)` declarations
- `@NonNull` - Add null checks to constructor parameters when null validation is required

**Usage Guidelines:**
- **Spring Components**: Use `@RequiredArgsConstructor` + `@Slf4j` for `@Service`, `@Component`, `@Repository` classes
- **Configuration Properties**: Use `@Getter` but keep explicit constructor for `@ConfigurationProperties` classes (Spring needs `@Name`/`@DefaultValue` annotations on constructor parameters)
- **Null Safety**: Add `@NonNull` on fields that must not be null (Lombok generates null-check in constructor)
- **Logger Naming**: `@Slf4j` generates `log` field (not `logger`), use `log.info()`, `log.debug()`, etc.

**Examples:**
```java
// ✅ CORRECT - Spring Service with Lombok
@Slf4j
@Service
@RequiredArgsConstructor
public class MyService {
  @NonNull private final DependencyA dependencyA;
  private final DependencyB dependencyB;

  public void doWork() {
    log.info("Working...");
  }
}

// ❌ WRONG - Manual boilerplate
@Service
public class MyService {
  private static final Logger logger = LoggerFactory.getLogger(MyService.class);
  private final DependencyA dependencyA;
  private final DependencyB dependencyB;

  public MyService(DependencyA dependencyA, DependencyB dependencyB) {
    this.dependencyA = Objects.requireNonNull(dependencyA);
    this.dependencyB = dependencyB;
  }
}

// ✅ CORRECT - ConfigurationProperties with Lombok @Getter
@Getter
@ConfigurationProperties(prefix = "my.config")
public final class MyConfigProperties {
  private final String value;

  public MyConfigProperties(@DefaultValue("default") String value) {
    this.value = value;
  }
}
```

**Test Considerations:**
- When testing null validation with `@NonNull`, expect message containing field name (e.g., `"factory"`)
- Lombok's null-check message format: `"fieldName is marked non-null but is null"`

### 7. NO DOCUMENTATION
Code should be self-explanatory and production-ready:
- NO Javadoc comments anywhere in codebase
- NO inline comments in production code
- NO comments in test code
- Code clarity replaces documentation need
- Names and structure convey intent

### 8. SIMPLICITY AND NO OVER-ENGINEERING

**CRITICAL: Keep code simple, clean, and clear.**

- **Simple First**: Always prefer the simplest solution that works
- **No Over-Engineering**: Avoid complex patterns, abstractions, or frameworks when simple code suffices
- **YAGNI Strictly**: You Aren't Gonna Need It - implement only what's required now
- **Avoid Premature Abstraction**: Don't create abstractions until you have 3+ concrete use cases
- **Readability Over Cleverness**: Clear, straightforward code beats clever, complex code
- **Minimal Dependencies**: Only add dependencies when absolutely necessary
- **No Speculative Features**: Build what's needed, not what might be needed

**Examples of Over-Engineering to AVOID**:
- ❌ Creating abstract factories when a simple constructor works
- ❌ Adding dependency injection framework for 3 classes
- ❌ Complex builder patterns for simple data objects (use records)
- ❌ Strategy pattern when a simple if/else or switch is clear
- ❌ Observer pattern when direct method calls are sufficient
- ❌ Reflection when compile-time type safety works

**Prefer**:
- ✅ Direct, straightforward code
- ✅ Standard library over external frameworks
- ✅ Records over complex POJOs with builders
- ✅ Simple conditionals over design patterns
- ✅ Composition over complex inheritance hierarchies

### 9. CODE FORMATTING

**CRITICAL: Google Java Style is mandatory.**

- **All code MUST be formatted with Google Java Style**
- **Format AFTER every code generation** - no exceptions
- Use Spotless Maven plugin (compatible with Java 21)
- Consistent formatting across entire codebase
- No manual formatting - let the tool handle it

**MANDATORY: After Every Code Generation**:
```bash
mvn spotless:apply
```

**This command MUST be executed:**
- After creating new files
- After editing existing files
- After any code generation or modification
- Before marking any task as complete
- Before committing code to git

**Pre-commit Requirement**:
- ALL code must be formatted before commit
- CI/CD will reject unformatted code
- Run `mvn spotless:apply` as the final step of every implementation task

---

## 🧪 Testing Standards

### CRITICAL: Test-Driven Development (TDD)

**ALL code generation MUST follow TDD:**
1. Write the test FIRST (red phase)
2. Write minimal code to make test pass (green phase)
3. Refactor for quality (refactor phase)
4. NEVER generate production code without tests first

### CRITICAL: Definition of Done

**A task is NOT complete until it is tested and working as expected:**
- Code must compile without errors
- All tests must pass successfully
- Code must be formatted with Google Java Style
- Code must meet all quality standards defined above
- NO task can be marked "done" if tests fail or code doesn't work
- "Working as expected" means: compiles + tests pass + meets requirements

### 1. TEST COVERAGE
Minimum 70% test coverage required:
- Cover main execution paths
- Cover edge cases and boundary conditions
- Cover error handling and exception scenarios
- Aim for both line and branch coverage

### 2. TESTING FRAMEWORK
Use JUnit 5 with Maven:
- Use `@Test`, `@BeforeEach`, `@AfterEach`, `@DisplayName` annotations
- Organize tests with `@Nested` classes for logical grouping
- Use `@ParameterizedTest` for testing multiple scenarios
- Tests must be runnable with `mvn test`
- Follow Maven standard directory structure (src/test/java)

### 3. ASSERTIONS
Use AssertJ and Hamcrest:
- Prefer AssertJ's fluent assertions for readability
- Use Hamcrest matchers where appropriate
- Write clear, descriptive assertion messages

### 4. NO MOCKING
Use real objects only:
- Create actual instances instead of mocks
- Build test fixtures with real object graphs
- Use test builders or factories for complex object creation
- NO Mockito, NO mocking frameworks

### 5. TEST QUALITY
Tests must be clear and maintainable:
- Follow Arrange-Act-Assert (AAA) pattern
- One logical assertion per test method
- Test method names in snake_case: `should_return_null_when_input_is_empty`
- Tests should be independent and repeatable
- NO documentation in tests (self-explanatory names and structure)

### 6. TEST ENCAPSULATION
Apply same immutability rules to tests:
- All test local variables marked `final`
- Use immutable test fixtures where possible

---

## 🛠️ Development Commands

**Maven-based project:**
```bash
mvn clean compile          # Compile the project
mvn test                   # Run all unit tests
mvn test -Dtest=ClassName  # Run specific test class
mvn verify                 # Run integration tests
mvn clean package          # Build JAR artifact
mvn com.coveo:fmt-maven-plugin:format  # Format code with Google Java Style
```

---

## 📝 Documentation and Tracking Rules

### Markdown File Management

**CRITICAL: All markdown files MUST follow these rules:**

1. **Location:** All `.md` files (except CLAUDE.md and README.md) MUST be placed in `trackings/` directory
   - Analysis reports → `trackings/`
   - Design documents → `trackings/`
   - Meeting notes → `trackings/`
   - Technical specifications → `trackings/`
   - Any other markdown documentation → `trackings/`

2. **Exceptions:** Only two markdown files allowed in project root:
   - `CLAUDE.md` - This file (guidance for Claude Code)
   - `README.md` - Project readme

3. **Git Management:**
   - `trackings/` directory and all its contents should NOT be committed to git
   - Add `trackings/` to `.gitignore`
   - `CLAUDE.md` and `README.md` in root should be committed normally

4. **Enforcement:**
   - Before creating any `.md` file, check if it's CLAUDE.md or README.md
   - If not, create it in `trackings/` directory
   - Create `trackings/` directory if it doesn't exist

---

## 🔐 Git Commit Standards

### CRITICAL: Commit Only Required Code

**When committing to git, ONLY include essential production code:**

1. **ALWAYS Commit:**
   - Production source code (`src/main/java/`)
   - Test code (`src/test/java/`)
   - Build configuration (`pom.xml`, `build.gradle`)
   - Project documentation (`CLAUDE.md`, `README.md`)
   - Configuration files necessary for build/deployment
   - `.gitignore` file

2. **NEVER Commit:**
   - IDE configuration files (`.idea/`, `*.iml`, `*.iws`, `*.ipr`)
   - Build outputs (`target/`, `build/`, `out/`, `*.class`)
   - Tracking documents (`trackings/` directory)
   - Temporary files (`*.tmp`, `*.bak`, `*.swp`, `*~`)
   - Log files (`*.log`)
   - OS-specific files (`.DS_Store`, `Thumbs.db`)
   - Maven/Gradle wrapper files unless explicitly needed
   - Local environment configurations
   - Generated reports (`reports/` directory)

3. **Verification Before Commit:**
   - Always run `git status` before staging files
   - Review `git diff` to verify only required code changes
   - Ensure `.gitignore` is properly configured
   - Check that no IDE, build, or temporary files are staged

4. **Clean Repository Principle:**
   - Keep repository minimal and focused on source code
   - Anyone cloning should only get what they need to build/run
   - No developer-specific or machine-specific files
   - No generated artifacts or build outputs

### CRITICAL: No AI Assistant References

**NEVER include any references to AI assistants in commits or codebase:**

1. **Prohibited in Commit Messages:**
   - NO mentions of "Claude Code", "Claude", "AI assistant", "ChatGPT", "Copilot", or similar
   - NO "Generated with [AI Tool]" footers or signatures
   - NO "Co-Authored-By" attributions to AI tools
   - Commit messages should reflect human authorship only

2. **Prohibited in Codebase:**
   - NO AI assistant references in code comments
   - NO "Generated by AI" annotations or headers
   - NO AI tool credits in documentation
   - Code should appear as human-authored

3. **Professional Standards:**
   - Commits represent team's work, not AI assistance
   - Maintain professional appearance in version control
   - Focus on technical content, not authorship attribution
   - AI tools are development aids, not co-authors

4. **Enforcement:**
   - Review all commit messages before pushing
   - Remove any AI tool references immediately
   - Amend commits if AI mentions are discovered
   - Keep version history clean and professional

---