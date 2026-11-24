#!/usr/bin/env bash
# Connect to Android TV via ADB over Wi-Fi

set -euo pipefail

if ! command -v adb >/dev/null 2>&1; then
    echo "Error: 'adb' command not found in PATH." >&2
    exit 1
fi

TARGET_IP="${1:-}"

if [ -z "$TARGET_IP" ]; then
    echo "Usage: ./scripts/connect.sh <TV_IP_ADDRESS>[:PORT]"
    echo "Example: ./scripts/connect.sh 192.168.1.50"
    echo ""
    echo "Currently connected devices:"
    adb devices
    exit 1
fi

# Default port 5555 if not specified
if [[ "$TARGET_IP" != *:* ]]; then
    TARGET_IP="${TARGET_IP}:5555"
fi

echo "Connecting to $TARGET_IP..."
adb connect "$TARGET_IP"
adb devices
