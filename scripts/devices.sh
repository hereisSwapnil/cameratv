#!/usr/bin/env bash
# List connected ADB devices

set -euo pipefail

if ! command -v adb >/dev/null 2>&1; then
    echo "Error: 'adb' command not found in PATH." >&2
    exit 1
fi

echo "Connected ADB devices:"
adb devices -l
