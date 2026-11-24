#!/usr/bin/env bash
# Clean build artifacts and Gradle cache

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$REPO_ROOT"

if [ ! -x "./gradlew" ]; then
    chmod +x ./gradlew
fi

echo "Cleaning build artifacts..."
./gradlew clean
echo "✓ Clean complete."
