#!/usr/bin/env bash
# Build and install APK to Android TV with runtime permissions granted

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

if ! command -v adb >/dev/null 2>&1; then
    echo "Error: 'adb' command not found in PATH." >&2
    exit 1
fi

APK_PATH="$REPO_ROOT/app/build/outputs/apk/debug/app-debug.apk"

# If APK doesn't exist, build it
if [ ! -f "$APK_PATH" ]; then
    echo "APK not found. Building first..."
    "$SCRIPT_DIR/build.sh"
fi

echo "Installing $APK_PATH to connected device..."
adb install -r -g "$APK_PATH"
echo "✓ Installation successful!"
