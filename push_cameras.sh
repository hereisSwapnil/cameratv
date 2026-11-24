#!/usr/bin/env bash
# Push cameras config to Android TV and reload
# Convenience wrapper pointing to scripts/push_cameras.sh
exec "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/scripts/push_cameras.sh" "$@"
