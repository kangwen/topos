#!/usr/bin/env bash
# 阶段 B 网络抖动：TOXIPROXY_PROFILE=light|enhanced（默认 enhanced）
set -euo pipefail

PROXY_API="${TOXIPROXY_URL:-http://127.0.0.1:8474}"
PROXY_NAME="${TOXIPROXY_PROXY_NAME:-strategy_ws}"
LISTEN="${TOXIPROXY_LISTEN:-127.0.0.1:18083}"
UPSTREAM="${TOXIPROXY_UPSTREAM:-127.0.0.1:8083}"
SERVER_BIN="${TOXIPROXY_SERVER:-toxiproxy-server}"
PROFILE="${TOXIPROXY_PROFILE:-enhanced}"

json() {
  curl -sS -H 'Content-Type: application/json' "$@"
}

wait_api() {
  for _ in $(seq 1 30); do
    if json "${PROXY_API}/version" >/dev/null 2>&1; then
      return 0
    fi
    sleep 0.5
  done
  echo "toxiproxy API not reachable at ${PROXY_API}" >&2
  return 1
}

add_latency() {
  local stream=$1
  json -X POST "${PROXY_API}/proxies/${PROXY_NAME}/toxics" -d "{
    \"name\": \"latency_${stream}\",
    \"type\": \"latency\",
    \"stream\": \"${stream}\",
    \"toxicity\": 1.0,
    \"attributes\": { \"latency\": 200, \"jitter\": 100 }
  }"
}

add_timeout_loss() {
  local stream=$1
  local toxicity=$2
  local name=$3
  json -X POST "${PROXY_API}/proxies/${PROXY_NAME}/toxics" -d "{
    \"name\": \"${name}\",
    \"type\": \"timeout\",
    \"stream\": \"${stream}\",
    \"toxicity\": ${toxicity},
    \"attributes\": { \"timeout\": 0 }
  }"
}

add_reset_loss() {
  local stream=$1
  local toxicity=$2
  local name=$3
  json -X POST "${PROXY_API}/proxies/${PROXY_NAME}/toxics" -d "{
    \"name\": \"${name}\",
    \"type\": \"reset_peer\",
    \"stream\": \"${stream}\",
    \"toxicity\": ${toxicity},
    \"attributes\": {}
  }"
}

if ! json "${PROXY_API}/version" >/dev/null 2>&1; then
  echo "starting ${SERVER_BIN}..."
  "${SERVER_BIN}" >/tmp/toxiproxy-server.log 2>&1 &
  echo $! >/tmp/toxiproxy-server.pid
  wait_api
fi

echo "toxiproxy version: $(json "${PROXY_API}/version") profile=${PROFILE}"

json -X DELETE "${PROXY_API}/proxies/${PROXY_NAME}" >/dev/null 2>&1 || true

json -X POST "${PROXY_API}/proxies" -d "{
  \"name\": \"${PROXY_NAME}\",
  \"listen\": \"${LISTEN}\",
  \"upstream\": \"${UPSTREAM}\",
  \"enabled\": true
}"

for stream in upstream downstream; do
  add_latency "${stream}"
done

case "${PROFILE}" in
  light)
    add_timeout_loss upstream 0.01 timeout_loss_upstream
    ;;
  enhanced)
    # ~1% timeout + ~1% RST，双向；更接近「延迟 + 偶发丢包/断流」
    add_timeout_loss upstream 0.02 timeout_loss_upstream
    add_timeout_loss downstream 0.02 timeout_loss_downstream
    add_reset_loss upstream 0.01 reset_loss_upstream
    add_reset_loss downstream 0.01 reset_loss_downstream
    ;;
  *)
    echo "unknown profile: ${PROFILE}" >&2
    exit 1
    ;;
esac

echo ""
echo "Toxiproxy ready (profile=${PROFILE})."
echo "  Listen:  ${LISTEN} -> ${UPSTREAM}"
echo "  Phase B: BASE_URL=http://127.0.0.1:18083 npm run phase:b"
