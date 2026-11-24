#!/usr/bin/env bash
# Push cameras config to Android TV and reload live without restarting the app

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Check if adb is installed
if ! command -v adb >/dev/null 2>&1; then
    echo "Error: 'adb' command not found in PATH. Please install Android Platform Tools." >&2
    exit 1
fi

# Determine config file
CONFIG_FILE=""
if [ -n "${1:-}" ]; then
    CONFIG_FILE="$1"
elif [ -f "$REPO_ROOT/private/cameras.json" ]; then
    CONFIG_FILE="$REPO_ROOT/private/cameras.json"
elif [ -f "$REPO_ROOT/cameras.json" ]; then
    CONFIG_FILE="$REPO_ROOT/cameras.json"
fi

if [ -z "$CONFIG_FILE" ] || [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Neither private/cameras.json nor cameras.json found!" >&2
    echo "Usage: ./scripts/push_cameras.sh [path/to/cameras.json]" >&2
    echo "Create private/cameras.json or cameras.json using cameras.sample.json as a template." >&2
    exit 1
fi

echo "--> Pushing $(basename "$CONFIG_FILE") to TV (/sdcard/cameras.json)..."
adb push "$CONFIG_FILE" /sdcard/cameras.json

echo "--> Pushing to app-specific external storage..."
adb shell mkdir -p /sdcard/Android/data/app.hereisswapnil.cameratv/files > /dev/null 2>&1 || true
adb push "$CONFIG_FILE" /sdcard/Android/data/app.hereisswapnil.cameratv/files/cameras.json > /dev/null 2>&1 || true

echo "--> Sending RELOAD_CONFIG broadcast..."
adb shell am broadcast -a app.hereisswapnil.cameratv.RELOAD_CONFIG > /dev/null 2>&1 || true

echo "✓ Successfully pushed cameras configuration! Cameras updated instantly."
