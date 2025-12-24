#!/bin/bash
set -e

REPO_PATH="${1:-/workspace/repo}"
cd "$REPO_PATH"

echo "=== Test Runner ==="
echo "Repository path: $REPO_PATH"

# Detect test framework and run tests
detect_and_run_tests() {
    # Maven/Java
    if [ -f "pom.xml" ]; then
        echo "Detected: Maven project"
        mvn test -B -q --fail-at-end 2>&1 || true
        return 0
    fi

    # Gradle
    if [ -f "build.gradle" ] || [ -f "build.gradle.kts" ]; then
        echo "Detected: Gradle project"
        if [ -f "gradlew" ]; then
            chmod +x gradlew
            ./gradlew test --no-daemon -q 2>&1 || true
        else
            gradle test --no-daemon -q 2>&1 || true
        fi
        return 0
    fi

    # Node.js/NPM
    if [ -f "package.json" ]; then
        echo "Detected: Node.js project"
        npm install --silent 2>&1 || true
        npm test -- --ci 2>&1 || true
        return 0
    fi

    # Python/pytest
    if [ -f "pytest.ini" ] || [ -f "pyproject.toml" ] || [ -f "setup.py" ]; then
        echo "Detected: Python project"
        if [ -f "requirements.txt" ]; then
            pip3 install -r requirements.txt -q 2>&1 || true
        fi
        pytest -v --tb=short 2>&1 || true
        return 0
    fi

    # Go
    if [ -f "go.mod" ]; then
        echo "Detected: Go project"
        go test ./... -v 2>&1 || true
        return 0
    fi

    # Rust/Cargo
    if [ -f "Cargo.toml" ]; then
        echo "Detected: Rust project"
        cargo test 2>&1 || true
        return 0
    fi

    echo "No test framework detected"
    return 1
}

detect_and_run_tests

echo "=== Test execution complete ==="
