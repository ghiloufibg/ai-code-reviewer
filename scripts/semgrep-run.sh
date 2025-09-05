
#!/usr/bin/env bash
set -euo pipefail
mkdir -p target
semgrep --config p/java --config p/owasp-top-ten --json -o target/semgrep.json || true
