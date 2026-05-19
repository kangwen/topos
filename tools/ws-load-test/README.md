# WebSocket 策略端点压测（`/ws/v1/strategy`）

验证 **10 → 20 → 50 → 100** 并发下的建连成功率、广播送达率、重连恢复；以及网络抖动 / 真实断开后的恢复能力。

## 前置条件

1. **启动 strategy-server（loadtest profile）**

```bash
cd topos-server/topos-strategy-server
mvn -q spring-boot:run -Dspring-boot.run.profiles=loadtest
```

`loadtest` profile 默认使用 **H2 内存库**（无需本地 MySQL），并关闭 Flyway/Quartz。若要用真实 MySQL，设置环境变量覆盖即可：

```bash
export SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3306/topos?...'
export SPRING_DATASOURCE_USERNAME=你的用户
export SPRING_DATASOURCE_PASSWORD=你的密码
```

`application-loadtest.yml` 还会：

- `topos.kafka.trade-signal.enabled=false`（排除 Kafka 干扰）
- `topos.wss.load-test.enabled=true`（压测专用 token + 广播 API）
- 放行 `/api/v1/wss/load-test/**`

2. **安装压测客户端依赖**

```bash
cd tools/ws-load-test
npm install
```

## 压测 API（仅 loadtest profile）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/wss/load-test/token` | body: `{ "clientId": "load-0001" }` → JWT |
| POST | `/api/v1/wss/load-test/broadcast` | body: `{ "traceId": "...", "round": 1 }` |
| GET | `/api/v1/wss/load-test/sessions` | 服务端当前在线数 |

> 同一登录用户的 JWT `clientId` 相同，且 `close-previous-session-on-duplicate=true` 会踢掉旧连接；压测必须为**每个连接**签发独立 `clientId`（本工具默认 `load-0001` …）。

## 执行顺序

### 阶段 A：纯连接稳定性（无抖动）

```bash
# 阶梯 10 → 20 → 50 → 100，每档默认 180s
npm run phase:a

# 或单档
node src/index.js --phase A --connections 50 --duration-sec 300 --scenario baseline
```

### 阶段 B：网络抖动（增强版）

**目标**：在 100~300ms 延迟 + 约 2% 超时 + 1% RST 下，验证**不误杀、能重连、送达可接受**。

| 项 | 增强后配置 |
|----|------------|
| 并发 | 100（15s 内阶梯建连） |
| 时长 | 240s |
| 全链路 | Token + WS + 广播均经 `18083` |
| 重连 | 开启（指数退避） |
| 中途 spike | t=90s 注入 3% RST，持续 20s |

```bash
brew install toxiproxy   # 一次性

npm run phase:b:full     # enhanced 毒性 + 压测 + 清理

# 旧版轻量对照（无 spike、50 连接逻辑见 phase:b:light）
npm run phase:b:light
```

**验收（阶段 B）**

| 指标 | 阈值 |
|------|------|
| 建连成功率 | ≥ 99% |
| 广播送达率 | ≥ 98% |
| 重连成功率（有重连时） | ≥ 93%（增强场景允许少量失败） |
| 结束时恢复在线 `recovery_online_ratio` | ≥ 95% |
| spike 后最低在线 `min_online_post_spike_ratio` | ≥ 50% |
| `observed_network_stress` | 断开或重连 ≥ 5 次（证明确实压到网络） |

环境变量：`JITTER_SPIKE=0` 关闭中途 spike；`DIRECT_BASE_URL=http://127.0.0.1:8083` 仅 token 直连（不推荐）。

### 阶段 C：强抖动 / 真实断开

1. 注入 5–15s 断流或高丢包（`--scenario jitter_heavy`）。
2. **真实断开**：重启 strategy-server 或经代理 `reset`；观察客户端指数退避重连（1s→2s→4s→8s，上限 30s + 抖动）。

```bash
npm run phase:c
```

### 阶段 D：回归稳态

去除抖动后：

```bash
npm run phase:d
```

### 阶段 E：突发建连（1 分钟内 2000 连接）

在 **60s** 内均匀发起 **2000** 条 WebSocket 建连（约每 30ms 1 条），统计 ramp 窗口结束时的建连成功率与达满耗时；默认再保活 30s（不广播、不重连）。

```bash
npm run phase:e

# 自定义规模
node src/index.js --burst --connections 2000 --ramp-sec 60 --duration-sec 30 \
  --client-id-prefix burst --scenario burst_2000_60s --no-reconnect
```

**注意**

- 单进程 Node 维持 2000 条长连接，建议 `ulimit -n 4096`，必要时 `NODE_OPTIONS=--max-old-space-size=4096`。
- 服务端 `topos.wss.max-sessions` / Tomcat `max-connections` 默认 20000，本地一般够用。
- Token 按批签发（默认每批 100），避免压垮 HTTP。

**验收（阶段 E）**

| 指标 | 阈值 |
|------|------|
| `ramp_connect_success_rate`（60s 到时已建连数 / 目标） | ≥ 99% |
| `time_to_full_connect_ms`（全部连上时） | ≤ 60000 |

## 结果输出

每档运行结束会在 `tools/ws-load-test/reports/` 生成 JSON，字段包括：

- `connections_target`, `connections_established`, `connect_success_rate`, `abnormal_disconnects`
- `messages_sent`, `messages_expected`, `messages_received`, `delivery_rate`
- `reconnect_attempts`, `reconnect_success`, `reconnect_success_rate`
- `avg_recover_ms`, `p95_recover_ms`, `avg_e2e_latency_ms`, `p95_e2e_latency_ms`
- `meta.scenario`: `baseline` | `jitter_light` | `jitter_heavy` | `hard_disconnect`

### 建议验收阈值（本地可先宽松）

| 指标 | 阈值 |
|------|------|
| 建连成功率 | ≥ 99% |
| 广播送达率 | ≥ 99% |
| 抖动窗口内重连成功率 | ≥ 98%（有重连时） |
| P95 端到端延迟 | 先记录基线，后续对比 |

控制台会打印 `PASS/FAIL` 与上述阈值对比。

## 环境变量

见 `.env.example`：`BASE_URL`, `DURATION_SEC`, `BROADCAST_INTERVAL_SEC` 等。

## 常见误判

- 测试时关闭 IDE 索引等重 CPU 任务，避免放大延迟。
- 本地 loopback 极限 ≠ 线上；本地重点验证**稳定性与恢复逻辑**。
- 每档建议重复 2–3 次取中位数。
- 不要用同一 `clientId` 开多条连接（会被 4401 替换断开）。

## Toxiproxy（可选）

```bash
docker run -d --name toxiproxy -p 8474:8474 -p 18083:18083 ghcr.io/shopify/toxiproxy
# 将客户端 BASE_URL / WS 指向 toxiproxy 上游为 127.0.0.1:8083
```

通过 Toxiproxy HTTP API 添加 `latency`、`timeout`、`limit_data` 等 toxic，便于阶段 B/C 自动化。
