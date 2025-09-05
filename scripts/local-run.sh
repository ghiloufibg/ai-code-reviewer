
#!/usr/bin/env bash
set -euo pipefail
MODEL=${1:-qwen2.5-coder:7b-instruct}
export OLLAMA_HOST=${OLLAMA_HOST:-http://127.0.0.1:11434}

mvn -q -DskipTests=false -e -B clean verify
bash scripts/semgrep-run.sh

java -jar target/ai-code-reviewer-0.1.0.jar   --repo local/local   --pr 0   --model "$MODEL"   --ollama "$OLLAMA_HOST"   --mode local   --base main --head HEAD
