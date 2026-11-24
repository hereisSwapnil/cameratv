#!/usr/bin/env bash
# Full deploy: build APK, install to TV, push camera config, and start app

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=== Step 1/4: Building APK ==="
"$SCRIPT_DIR/build.sh"

echo "=== Step 2/4: Installing APK to TV ==="
"$SCRIPT_DIR/install.sh"

echo "=== Step 3/4: Pushing Camera Config ==="
if [ -f "$SCRIPT_DIR/../private/cameras.json" ] || [ -f "$SCRIPT_DIR/../cameras.json" ]; then
    "$SCRIPT_DIR/push_cameras.sh"
else
    echo "Notice: Neither private/cameras.json nor cameras.json found. Skipping config push."
    echo "You can create one from cameras.sample.json and run ./scripts/push_cameras.sh anytime."
fi

echo "=== Step 4/4: Launching CameraTV ==="
"$SCRIPT_DIR/start.sh"

echo "✓ Full deployment completed!"
