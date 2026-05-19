#!/usr/bin/env bash
set -euo pipefail

PROXY_API="${TOXIPROXY_URL:-http://127.0.0.1:8474}"
PROXY_NAME="${TOXIPROXY_PROXY_NAME:-strategy_ws}"

curl -sS -X DELETE "${PROXY_API}/proxies/${PROXY_NAME}" >/dev/null 2>&1 || true

if [[ -f /tmp/toxiproxy-server.pid ]]; then
  pid=$(cat /tmp/toxiproxy-server.pid)
  kill "${pid}" 2>/dev/null || true
  rm -f /tmp/toxiproxy-server.pid
fi

echo "toxiproxy proxy ${PROXY_NAME} removed"
