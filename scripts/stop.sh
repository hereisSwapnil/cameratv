#!/usr/bin/env bash
# Force-stop CameraTV on the connected TV

set -euo pipefail

if ! command -v adb >/dev/null 2>&1; then
    echo "Error: 'adb' command not found in PATH." >&2
    exit 1
fi

echo "Stopping CameraTV..."
adb shell am force-stop app.hereisswapnil.cameratv
echo "✓ CameraTV stopped."
