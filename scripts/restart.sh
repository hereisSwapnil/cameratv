#!/usr/bin/env bash
# Restart CameraTV on the connected TV

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Restarting CameraTV..."
"$SCRIPT_DIR/stop.sh"
sleep 1
"$SCRIPT_DIR/start.sh"
echo "✓ CameraTV restarted."
