#!/usr/bin/env bash
# Stream live Android TV logcat filtered to CameraTV

set -euo pipefail

if ! command -v adb >/dev/null 2>&1; then
    echo "Error: 'adb' command not found in PATH." >&2
    exit 1
fi

echo "Streaming CameraTV logs (Ctrl+C to stop)..."
adb logcat -v time -s CameraStreamController:V MainActivity:V ExoPlayer:W ConfigManager:V
