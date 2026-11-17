#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# Requires python dbus-next, gdbus and dbus-run-session; runs on an isolated bus.
export VISION_TEST_PYTHON="${VISION_TEST_PYTHON:-python3}"
dbus-run-session -- bash -c '
set -euo pipefail
ready=$(mktemp)
rm "$ready"
"$VISION_TEST_PYTHON" tests/mpris-fixture.py "$ready" &
fixture=$!
trap '\''kill "$fixture" 2>/dev/null || true; rm -f "$ready"'\'' EXIT
for i in {1..50}; do [ -f "$ready" ] && break; sleep 0.1; done
[ -f "$ready" ]
timeout 30s bash tests/run-core.sh --mpris
'
