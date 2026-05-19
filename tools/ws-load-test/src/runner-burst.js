import fs from 'node:fs';
import path from 'node:path';
import { mintLoadTestToken, fetchActiveSessions } from './api.js';
import { clientIdForIndex } from './config.js';
import { LoadTestClient } from './client.js';
import {
  createMetrics,
  snapshotSecond,
  finalizeMetrics,
  evaluateBurstThresholds,
} from './metrics.js';

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function mintTokensBatched(loadTestApi, prefix, count, batchSize) {
  const tokens = new Array(count);
  for (let offset = 0; offset < count; offset += batchSize) {
    const end = Math.min(offset + batchSize, count);
    await Promise.all(
      Array.from({ length: end - offset }, async (_, j) => {
        const idx = offset + j + 1;
        const clientId = clientIdForIndex(prefix, idx, 4);
        const token = await mintLoadTestToken(loadTestApi, clientId);
        tokens[idx - 1] = { clientId, token };
      }),
    );
    process.stdout.write(`minted tokens ${end}/${count}\n`);
  }
  return tokens;
}

/**
 * 在 rampSec 内均匀发起连接，统计窗口内建连成功率与达满时间。
 */
export async function runBurstRampScenario(config) {
  const startedAt = Date.now();
  const target = config.connections;
  const rampMs = config.rampSec * 1000;
  const metrics = createMetrics(target);
  metrics._rampStartedAt = startedAt;

  process.stdout.write(
    `\n>>> Burst ramp: ${target} connections within ${config.rampSec}s\n`,
  );

  const tokens = await mintTokensBatched(
    config.loadTestApi,
    config.clientIdPrefix,
    target,
    config.mintBatchSize,
  );

  const clients = tokens.map(
    ({ clientId, token }) =>
      new LoadTestClient({
        clientId,
        token,
        wsUrl: config.wsUrl,
        connectTimeoutMs: config.connectTimeoutMs,
        reconnectMaxMs: config.reconnectMaxMs,
        autoReconnect: config.autoReconnect,
        metrics,
      }),
  );

  const rampStart = Date.now();
  const staggerMs = rampMs / target;
  for (let i = 0; i < target; i += 1) {
    const delay = Math.floor(i * staggerMs);
    setTimeout(() => clients[i].start(), delay);
  }

  const statsTimer = setInterval(() => {
    const elapsed = Math.round((Date.now() - rampStart) / 1000);
    snapshotSecond(metrics, Date.now());
    process.stdout.write(
      `[${new Date().toISOString()}] t=${elapsed}s online=${metrics._online}/${target} ` +
        `established=${metrics.connections_established} reconnect=${metrics.reconnect_attempts}\n`,
    );
  }, 1000);

  await sleep(rampMs);
  const establishedAtRampEnd = metrics.connections_established;
  const onlineAtRampEnd = metrics._online;
  const rampEndAt = Date.now();

  let timeToFullConnectMs = null;
  const fullDeadline = rampStart + rampMs + config.connectTimeoutMs;
  while (Date.now() < fullDeadline) {
    if (metrics.connections_established >= target) {
      timeToFullConnectMs = Date.now() - rampStart;
      break;
    }
    await sleep(200);
  }
  if (timeToFullConnectMs == null && metrics.connections_established >= target) {
    timeToFullConnectMs = Date.now() - rampStart;
  }

  const holdMs = config.durationSec * 1000;
  if (holdMs > 0) {
    process.stdout.write(`holding ${config.durationSec}s after ramp...\n`);
    await sleep(holdMs);
  }

  clearInterval(statsTimer);

  let serverSessions = -1;
  try {
    serverSessions = await fetchActiveSessions(config.loadTestApi);
  } catch {
    // ignore
  }

  clients.forEach((c) => c.stop());
  await sleep(500);

  const endedAt = Date.now();
  const summary = finalizeMetrics(metrics);
  const burst = {
    ramp_sec: config.rampSec,
    established_at_ramp_end: establishedAtRampEnd,
    online_at_ramp_end: onlineAtRampEnd,
    ramp_connect_success_rate: round4(
      target > 0 ? establishedAtRampEnd / target : 1,
    ),
    time_to_full_connect_ms: timeToFullConnectMs,
    ramp_end_at: rampEndAt,
  };
  Object.assign(summary, burst);

  const thresholds = evaluateBurstThresholds(summary, config.rampSec);

  const report = {
    meta: {
      phase: config.phase,
      scenario: config.scenario,
      mode: 'burst_ramp',
      startedAt,
      endedAt,
      durationSec: Math.round((endedAt - startedAt) / 1000),
      baseUrl: config.baseUrl,
      wsUrl: config.wsUrl,
      serverActiveSessions: serverSessions,
      rampSec: config.rampSec,
      connections: target,
    },
    summary,
    thresholds,
    per_second: metrics.per_second,
    client_events_tail: metrics.client_events.slice(-200),
  };

  await writeReport(config, report);
  printReport(report);
  return report;
}

async function writeReport(config, report) {
  const dir = path.resolve(process.cwd(), config.reportDir);
  fs.mkdirSync(dir, { recursive: true });
  const file = path.join(
    dir,
    `${report.meta.phase}_${report.meta.scenario}_${config.connections}c_ramp${config.rampSec}s_${report.meta.endedAt}.json`,
  );
  fs.writeFileSync(file, JSON.stringify(report, null, 2));
  process.stdout.write(`report written: ${file}\n`);
}

function printReport(report) {
  const s = report.summary;
  const t = report.thresholds;
  process.stdout.write('\n=== Summary ===\n');
  process.stdout.write(JSON.stringify(s, null, 2));
  process.stdout.write('\n\n=== Thresholds ===\n');
  for (const c of t.checks) {
    const mark = c.pass ? 'PASS' : 'FAIL';
    process.stdout.write(`${mark} ${c.name}: actual=${c.actual} threshold>=${c.threshold}\n`);
  }
  process.stdout.write(`\nOverall: ${t.passed ? 'PASS' : 'FAIL'}\n\n`);
}

function round4(n) {
  return Math.round(n * 10_000) / 10_000;
}
