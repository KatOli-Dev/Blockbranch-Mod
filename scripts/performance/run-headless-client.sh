#!/usr/bin/env bash
set -euo pipefail

if (( $# == 0 )); then
    printf 'Usage: %s COMMAND [ARG ...]\n' "$0" >&2
    exit 64
fi

# SDL3/D-Bus probes can disconnect the final X client during startup.
# Keep the virtual display alive across that disconnect instead of resetting it.
export SDL_VIDEODRIVER="${SDL_VIDEODRIVER:-x11}"
if [[ -n "${BLOCKBRANCH_CAPTURE_MANIFEST:-}" ]]; then
    capture_repo="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
    capture_java="${JAVA_HOME:+$JAVA_HOME/bin/}java"
    "$capture_java" "$capture_repo/scripts/performance/CaptureManifest.java" \
        "$BLOCKBRANCH_CAPTURE_MANIFEST" "$capture_repo"
fi
exec xvfb-run -a -s '-screen 0 1024x576x24 -noreset' "$@"
