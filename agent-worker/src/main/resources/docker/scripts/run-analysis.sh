#!/bin/bash
set -e

echo "=== AI Code Reviewer Analysis Container ==="
echo "Working directory: $(pwd)"
echo "User: $(whoami)"

# Execute the command passed to the container
if [ $# -gt 0 ]; then
    echo "Executing: $@"
    exec "$@"
else
    echo "No command specified, running default analysis..."

    # List workspace contents
    if [ -d "/workspace/repo" ]; then
        echo "Repository contents:"
        ls -la /workspace/repo/

        # Detect and run tests
        /opt/scripts/run-tests.sh /workspace/repo
    else
        echo "No repository found in /workspace/repo"
        exit 1
    fi
fi
