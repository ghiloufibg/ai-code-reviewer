# Prompt pour **Claude** — Expert API‑First / Architecte (Spring Boot + Angular)

> **Objectif** : Analyser deux prototypes (backend Spring Boot + frontend Angular), extraire/aligner les modèles de
> données, puis produire un contrat **API‑first** complet (OpenAPI v3) incluant **endpoints**, **DTOs** (Java &
> TypeScript), mappings, exemples et instructions pour générer automatiquement les clients. Livrables prêts à mettre dans
> un PR.

---

## Contexte fourni

- Tu recevras (ou tu auras accès à) deux répertoires :
    - `backend/` — projet **Spring Boot** (Java/Kotlin), contenant `src/main/java` (controllers, services, entities,
      dto), `pom.xml`/`build.gradle`.
    - `frontend/` — projet **Angular**, contenant `src/app` (services, models, components), `package.json`.
- Si des fichiers manquent, indique précisément lesquels et propose une stratégie d'extraction alternative (ex. parsing
  des commits, README, swagger existant, etc.).

---

## Rôle demandé

Tu es **Architecte API-First** et **expert en conception de contrats OpenAPI**. Tu dois appliquer un raisonnement
approfondi (deep reasoning, "ultrathink") pour proposer un modèle de données partagé, une spécification OpenAPI complète
et un plan d'intégration CI pour la génération automatique des clients.

> **Note** : explique les décisions architecturales (choix de modélisation, compatibilité ascendante, sécurité,
> pagination, etc.) de façon concise et actionnable.

---

## Tâches détaillées (ordre recommandé)

1. **Analyse statique complète**
    - Parcoure tous les fichiers Java/Kotlin (`@RestController`, `@Controller`, `@RequestMapping`, `@GetMapping`,
      classes DTO, Entities, Repositories, Services) afin d'extraire :
        - endpoints (path, méthodes HTTP, paramètres path/query/header/body),
        - types de request/response (classes utilisées),
        - validations (`@Valid`, `@NotNull`, `@Size`, patterns),
        - codes d'erreurs personnalisés et exceptions.
    - Parcoure le front Angular pour extraire :
        - services HTTP (méthodes, endpoints consommés),
        - interfaces/models TypeScript,
        - usages (components qui consomment données) pour comprendre les shapes attendues et les champs optionnels.
    - Produis un inventaire `endpoints_discovered.csv` listant **chaque** endpoint découvert avec : `source_file`,
      `http_method`, `path`, `java_request_type`, `java_response_type`, `ts_interface_if_found`, `notes`.

2. **Inférence du modèle de données partagé (Canonical DTOs)**
    - À partir de l'inventaire, identifie les entités métier (ex : User, Project, Review, Comment, File, Violation).
    - Propose **DTOs canoniques** (schémas unifiés) qui couvrent les besoins du backend et du frontend. Pour chaque
      DTO :
        - nom, description, champs (nom, type canonique, requis/optionnel), validations, exemples.
    - Résous les conflits (même champ avec types divergents) en proposant règles de transformation (ex : Java `Long` ↔
      TypeScript `number`; Java `Instant` ↔ ISO8601 `string`).
    - Explique les décisions sur : nullable vs optional, flattening vs nested objects, usage d'IDs vs embeddings (ex :
      `authorId` vs `author` object).

3. **Conception OpenAPI v3 (API‑First)**
    - Produis un `openapi.yaml` conforme OpenAPI v3.1 (ou v3.0 si contrainte), contenant :
        - `info` (title, version, description),
        - `servers` (dev/staging/prod placeholders),
        - `paths` pour **tous** les endpoints (méthode, sécurité, paramètres, requestBody, responses),
        - `components/schemas` pour **tous** les DTOs canoniques,
        - `components/responses` réutilisables (400/401/403/404/429/500) et `components/parameters` partagés,
        - exemples (`example` ou `examples`) illustrant cas success & erreurs.
    - Pour chaque endpoint, fournis **au moins** un exemple request/response JSON réeliste.
    - Inclue des descriptions claires pour chaque chemin et champ (utile pour docs générées).

4. **Mapping & transformation**
    - Fournis un tableau `mapping.md` indiquant :
        - pour chaque endpoint, le mapping `java_type ↔ openapi_schema ↔ ts_interface`.
        - règles de sérialisation (dates, enums, nullable), considérations Jackson (ex: `@JsonProperty`), et annotations
          utiles côté Spring (`@Schema` si besoin).
    - Donne des snippets de code pour :
        - Java DTOs (avec annotations Jackson/Swagger `@Schema` / validation annotations),
        - TypeScript interfaces/types (avec champs optionnels `?`).

5. **Compatibilité & versioning**
    - Propose une stratégie de versioning d'API (path versioning `/v1/` vs header `Accept`), gestion des changements
      breaking, et contrat de dépréciation.

6. **Sécurité & auth**
    - Déclare le schéma de sécurité recommandé dans `openapi.yaml` (ex : `bearerAuth` JWT), endpoints protégés, scopes
      si OAuth2, et exemples d'erreurs 401/403.

7. **Qualité & tests contractuels**
    - Génère des tests contractuels recommandes (ex : Postman collection ou tests contractuels d'API avec
      `schemathesis`/`pact`/`rest-assured`) pour valider que le backend respecte le contrat.

8. **Génération clients & CI**
    - Fournis commandes & configuration pour générer clients : `openapi-generator` (Java (Feign/RestTemplate/OkHttp),
      TypeScript (fetch/axios/angular)), avec options recommandées.
    - Ajoute job CI (GitHub Actions snippets) qui :
        - valide `openapi.yaml` (lint),
        - génère clients et commit dans un répertoire `generated/` ou crée PR automatique,
        - exécute tests contractuels.

9. **Plan d'action & PR ready artifacts**
    - Crée une checklist pour un PR : `openapi.yaml`, `components/schemas`, `mapping.md`, `dtos/java/`, `dtos/ts/`,
      `samples/`, `ci/`.
    - Priorise endpoints critiques pour un premier release (ex : authentication, core entities CRUD, review submission).

---

## Contraintes et conventions à appliquer

- **Types & conversions** :
    - Dates → ISO‑8601 strings. Java `Instant`/`OffsetDateTime` → OpenAPI `string` format `date-time`.
    - IDs → `string` si UUID, `integer`/`number` si numeric DB ids. Si mix, privilégier `string` to avoid JS precision
      problems.
    - Enums → OpenAPI `schema` enum + example strings.
- **Validation** : traduire `@NotNull/@Size/@Pattern` vers `required`, `minLength`/`maxLength`, `pattern`.
- **Field naming** : use `camelCase` across API payloads (TypeScript + Java Jackson `@JsonProperty` if needed).
- **Pagination** : use standard `limit`/`offset` or `cursor` approach (recommendation : cursor for large datasets).
  Document in `components/schemas`.
- **Errors** : standard error envelope `{ code, message, details? }`.
- **Backward compatibility** : avoid breaking changes; quand nécessaire, bump major version and document deprecation.

---

## Output attendu (fichiers / artefacts)

- `openapi.yaml` (complete) — **PR ready**.
- `endpoints_discovered.csv` — inventaire brut.
- `dtos/canonical/` — Markdown + Java class snippets + TypeScript interfaces.
- `mapping.md` — tableau de correspondance complet.
- `examples/` — exemples JSON request/response.
- `ci/openapi-generate-action.yml` — GitHub Actions snippet.
- `contract-tests/` — échantillon de tests (ou collection Postman).
- `readme_api_design.md` — résumé des décisions + guide d'intégration pour devs.

---

## Critères d'acceptation

- Le `openapi.yaml` couvre **100%** des endpoints découverts ou liste clairement ceux non couverts et pourquoi.
- Les DTOs canoniques sont suffisamment précis pour générer des clients typés (TypeScript + Java) sans modifications
  manuelles majeures.
- Le mapping documenté permet de transformer automatiquement les types entre Java/TS.
- CI snippet valide le contrat et génère clients automatiquement.

---

## Exemples & templates (à inclure dans la livraison)

- **Template endpoint (OpenAPI)**

```yaml
paths:
  /api/v1/reviews/{reviewId}:
    get:
      summary: "Récupère une review par son ID"
      parameters:
        - name: reviewId
          in: path
          required: true
          schema:
            type: string
      responses:
        '200':
          description: "Review récupérée"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ReviewDTO'
        '404':
          $ref: '#/components/responses/NotFound'
```

- **Template DTO (OpenAPI schema)**

```yaml
components:
  schemas:
    ReviewDTO:
      type: object
      properties:
        id:
          type: string
          description: "UUID de la review"
        authorId:
          type: string
        title:
          type: string
        createdAt:
          type: string
          format: date-time
      required: [ id, authorId, title ]
```

---

## Instructions d'exécution (pour toi — Claude)

1. Commence par produire l'`endpoints_discovered.csv` en listant chaque mapping entre code source (chemin du fichier +
   ligne approximative) et l'endpoint.
2. Construis ensuite les DTOs canoniques et l'`openapi.yaml` minimal couvrant tous les endpoints. Pour chaque endpoint
   manquant de type ou ambigu, note la source d'ambiguïté et propose 2 options puis choisis la meilleure.
3. Génère les snippets Java/TS, `mapping.md`, et `ci/`.
4. Termine par un court rapport (`readme_api_design.md`) avec risques, décisions clés, et points ouverts.

---

## Format de livraison attendu

- Archive `.zip` ou dossier structuré :
    - `openapi.yaml`
    - `endpoints_discovered.csv`
    - `dtos/canonical/` (`*.md`, `*.java.snippets`, `*.ts`)
    - `mapping.md`
    - `examples/`
    - `ci/` (github action yaml)
    - `readme_api_design.md`

---

## Questions à poser (si besoin)

- Y a‑t‑il une contrainte sur la version OpenAPI (3.0 vs 3.1) ?
- Stratégie d'auth choisie (JWT Bearer vs OAuth2) ?
- Préférence sur `id` numeric vs string (UUID) ?

> Si tu ne reçois pas de réponse aux questions ci‑dessus, prends des décisions conservatrices et documente-les (ex :
> choisir OpenAPI 3.1, JWT Bearer, IDs en `string`/UUID).

---

**Fin du prompt — agir maintenant**

Merci de produire les artefacts listés ci‑dessus et de documenter clairement toute hypothèse ou décision prise pendant
l'analyse.

