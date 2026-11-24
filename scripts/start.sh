#!/usr/bin/env bash
# Launch CameraTV on the connected TV

set -euo pipefail

if ! command -v adb >/dev/null 2>&1; then
    echo "Error: 'adb' command not found in PATH." >&2
    exit 1
fi

echo "Launching CameraTV..."
adb shell am start -n app.hereisswapnil.cameratv/.MainActivity
