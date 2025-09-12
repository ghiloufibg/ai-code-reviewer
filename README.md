# AI Code Reviewer (Java 17 + Maven + Spring Boot + Ollama + GitHub Actions)

Un **outil d’auto‑review de code 100 % gratuit** pour Pull Requests GitHub, qui :

- récupère le **diff unifié** de la PR (avec contexte),
- exécute des **linters/SAST** : Checkstyle, PMD, SpotBugs, **Semgrep (p/java, p/owasp-top-ten)**,
- interroge un **LLM local** via **Ollama** (aucun service payant/externe),
- **valide** la sortie contre un **schéma JSON** strict,
- **publie** un **commentaire résumé** + des **commentaires inline** sur la PR.

## 🧩 Fonctionnement (vue d’ensemble)

```
PR Diff  →  Chunking (par fichier/diffHunkBlock)  →  Prompt LLM  →  Réponse JSON (validée)  →  Publication GitHub
                  ↑                              ↑
          Linters/SAST (Maven + Semgrep)   +   Contexte projet/tests
```

- **Chunking** : `MAX_DIFF_LINES_PER_CALL = 1500` lignes/chunk.
- **Contexte** : ±5 lignes.
- **Résilience** : 1 retry si JSON invalide.
- **Inline mapping** : `(file, start_line)` → `position` patch ; fallback summary si patch obsolète.

## ✅ Prérequis / Requirements

**Local** : Java 17, Maven 3.9+, Git, Ollama (serveur), modèle `qwen2.5-coder:7b-instruct` (ou `deepseek-coder-v2:lite`,
`starcoder2:7b`), Semgrep (`pip install semgrep`).
**CI** : `ubuntu-latest` (CPU only), permissions `pull-requests: write`, `issues: write`, `GITHUB_TOKEN` auto.

## 📦 Installation locale

```bash
ollama pull qwen2.5-coder:7b-instruct
export OLLAMA_HOST=http://127.0.0.1:11434
mvn -B clean verify
bash scripts/semgrep-run.sh
java -jar target/ai-code-reviewer-0.1.0.jar   --repo local/local --pr 0   --model qwen2.5-coder:7b-instruct   --ollama $OLLAMA_HOST   --mode local --base main --head HEAD
```

Artifacts : `target/artifacts/` (JSON, patch, prompt).  
Astuce : `scripts/local-run.sh` automatise build + run.

## 🧪 Tests unitaires

```bash
mvn -q -DskipTests=false test
```

## ⚙️ Paramètres

**Env** : `MODEL`, `OLLAMA_HOST`, `GITHUB_TOKEN`.  
**CLI** : `--repo`, `--pr`, `--model`, `--ollama`, `--mode`, `--max-lines`, `--context`, `--timeout`.

## 🔧 SAST/Linters

Checkstyle (`config/checkstyle.xml`), PMD (`config/pmd-ruleset.xml`), SpotBugs (XML sous `target/`), Semgrep (
`scripts/semgrep-run.sh`).

## 🤖 GitHub Actions

Workflow : `.github/workflows/ai-code-review.yml` — checkout → Java → Ollama → pull model → `mvn verify` → Semgrep →
bot → artifacts.

## 🛠 Dépannage

- Ollama non joignable → vérifier `OLLAMA_HOST` / `ollama serve`
- Modèle manquant → `ollama pull <model>`
- Réponse non‑JSON → retry auto ; sinon réduire le diff / augmenter le chunking
- Semgrep absent → `pip install semgrep`
- Inline manquant → patch obsolète → fallback summary

## 📝 Licence

Apache‑2.0 (voir `LICENSE`).
