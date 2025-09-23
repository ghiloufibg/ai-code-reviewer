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
- Tests unitaires et d’intégration avec ≥ 70% de couverture.
- Maintien de la compatibilité avec le workflow actuel (mode local et GitHub).

---