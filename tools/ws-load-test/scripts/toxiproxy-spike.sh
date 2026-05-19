#!/usr/bin/env bash
# 压测中途注入 5% RST  spike，持续 SPIKE_SEC 秒后移除（用于触发重连）
set -euo pipefail

PROXY_API="${TOXIPROXY_URL:-http://127.0.0.1:8474}"
PROXY_NAME="${TOXIPROXY_PROXY_NAME:-strategy_ws}"
SPIKE_SEC="${SPIKE_SEC:-20}"
TOXICITY="${SPIKE_TOXICITY:-0.03}"

json() {
  curl -sS -H 'Content-Type: application/json' "$@"
}

echo "[toxiproxy-spike] adding ${TOXICITY} reset_peer upstream+downstream for ${SPIKE_SEC}s..."

for stream in upstream downstream; do
  json -X POST "${PROXY_API}/proxies/${PROXY_NAME}/toxics" -d "{
    \"name\": \"spike_reset_${stream}\",
    \"type\": \"reset_peer\",
    \"stream\": \"${stream}\",
    \"toxicity\": ${TOXICITY},
    \"attributes\": {}
  }" >/dev/null
done

sleep "${SPIKE_SEC}"

for stream in upstream downstream; do
  json -X DELETE "${PROXY_API}/proxies/${PROXY_NAME}/toxics/spike_reset_${stream}" >/dev/null 2>&1 || true
done

echo "[toxiproxy-spike] spike removed"
