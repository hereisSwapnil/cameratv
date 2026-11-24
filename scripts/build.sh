#!/usr/bin/env bash
# Build debug APK

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$REPO_ROOT"

if [ ! -x "./gradlew" ]; then
    chmod +x ./gradlew
fi

echo "Building CameraTV debug APK..."
./gradlew assembleDebug

APK_PATH="$REPO_ROOT/app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    echo "✓ Build successful! APK: $APK_PATH"
else
    echo "✓ Build completed."
fi
