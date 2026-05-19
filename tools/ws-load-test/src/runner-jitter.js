import { exec } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { promisify } from 'node:util';
import {
  mintTokensBatched,
  triggerBroadcast,
  fetchActiveSessions,
} from './api.js';
import { LoadTestClient } from './client.js';
import {
  createMetrics,
  snapshotSecond,
  finalizeMetrics,
  evaluateJitterThresholds,
} from './metrics.js';

const execAsync = promisify(exec);

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function scheduleToxicSpike(config) {
  if (!config.jitterSpike) {
    return () => {};
  }
  const delayMs = config.spikeAtSec * 1000;
  const script = path.resolve(process.cwd(), 'scripts/toxiproxy-spike.sh');
  const timer = setTimeout(() => {
    process.stdout.write(`\n>>> Toxic spike at t=${config.spikeAtSec}s\n`);
    execAsync(`bash "${script}"`, {
      env: {
        ...process.env,
        SPIKE_SEC: String(config.spikeSec),
        SPIKE_TOXICITY: String(config.spikeToxicity),
      },
    }).catch((err) => {
      process.stderr.write(`toxic spike failed: ${err.message}\n`);
    });
  }, delayMs);
  return () => clearTimeout(timer);
}

/**
 * 阶段 B：全链路经 Toxiproxy、阶梯建连、自动重连、中途 spike、抖动专项指标。
 */
export async function runJitterScenario(config) {
  const startedAt = Date.now();
  const target = config.connections;
  const metrics = createMetrics(target);
  metrics._minOnline = target;
  metrics._spikeAtMs = config.jitterSpike ? startedAt + config.spikeAtSec * 1000 : null;
  metrics._spikeEndMs = config.jitterSpike
    ? metrics._spikeAtMs + config.spikeSec * 1000
    : null;
  metrics._minOnlinePostSpike = target;
  metrics._postSpikeRecoveries = 0;

  const api = config.broadcastApi ?? config.loadTestApi;
  process.stdout.write(
    `\n>>> Phase B jitter: ${target} connections, ${config.durationSec}s, full_path=${config.fullPath}\n`,
  );

  const tokens = await mintTokensBatched(
    api,
    config.clientIdPrefix,
    target,
    10,
    config.mintRetries,
    true,
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
        onEstablished: () => {
          if (metrics._spikeAtMs != null && Date.now() > metrics._spikeAtMs + 5000) {
            if (metrics._lastDisconnectForClient?.get(clientId)) {
              const d = metrics._lastDisconnectForClient.get(clientId);
              if (Date.now() - d < 35_000) {
                metrics._postSpikeRecoveries += 1;
              }
            }
          }
        },
        onDisconnected: (client) => {
          if (!metrics._lastDisconnectForClient) {
            metrics._lastDisconnectForClient = new Map();
          }
          metrics._lastDisconnectForClient.set(client.clientId, Date.now());
        },
      }),
  );

  const cancelSpike = scheduleToxicSpike(config);
  const rampMs = config.connectRampSec * 1000;
  const staggerMs = rampMs / target;
  for (let i = 0; i < target; i += 1) {
    setTimeout(() => clients[i].start(), Math.floor(i * staggerMs));
  }

  const holdMs = config.durationSec * 1000;
  const intervalMs = config.broadcastIntervalSec * 1000;
  let round = 0;
  const maxRounds =
    config.broadcastRounds > 0
      ? config.broadcastRounds
      : Math.max(1, Math.floor(holdMs / intervalMs));

  const statsTimer = setInterval(() => {
    const online = metrics._online;
    metrics._minOnline = Math.min(metrics._minOnline, online);
    const now = Date.now();
    if (metrics._spikeEndMs == null || now >= metrics._spikeEndMs) {
      metrics._minOnlinePostSpike = Math.min(metrics._minOnlinePostSpike, online);
    }
    snapshotSecond(metrics, now);
    const elapsed = Math.round((Date.now() - startedAt) / 1000);
    process.stdout.write(
      `[${new Date().toISOString()}] t=${elapsed}s online=${online}/${target} ` +
        `est=${metrics.connections_established} disc=${metrics.abnormal_disconnects} ` +
        `reconn=${metrics.reconnect_attempts}/${metrics.reconnect_success}\n`,
    );
  }, 1000);

  const broadcastTimer = setInterval(async () => {
    round += 1;
    if (round > maxRounds) {
      return;
    }
    try {
      const traceId = `${config.scenario}-r${round}-${Date.now()}`;
      await triggerBroadcast(api, traceId, round);
      metrics.messages_sent += 1;
      metrics.messages_expected += metrics._online;
    } catch (err) {
      process.stderr.write(`broadcast failed round=${round}: ${err.message}\n`);
    }
  }, intervalMs);

  await sleep(holdMs);
  clearInterval(statsTimer);
  clearInterval(broadcastTimer);
  cancelSpike();

  const clientOnlineBeforeStop = metrics._online;

  let serverSessions = -1;
  try {
    serverSessions = await fetchActiveSessions(api);
  } catch {
    // ignore
  }

  clients.forEach((c) => c.stop());
  await sleep(500);

  const endedAt = Date.now();
  const recoveryOnline =
    serverSessions >= 0 ? serverSessions : clientOnlineBeforeStop;
  const summary = finalizeMetrics(metrics);
  const jitter = {
    min_online: metrics._minOnline,
    min_online_ratio: round4(target > 0 ? metrics._minOnline / target : 1),
    min_online_post_spike: metrics._minOnlinePostSpike,
    min_online_post_spike_ratio: round4(
      target > 0 ? metrics._minOnlinePostSpike / target : 1,
    ),
    end_online: recoveryOnline,
    recovery_online_ratio: round4(target > 0 ? recoveryOnline / target : 1),
    post_spike_recoveries: metrics._postSpikeRecoveries,
    spike_at_sec: config.jitterSpike ? config.spikeAtSec : null,
    connect_ramp_sec: config.connectRampSec,
    full_path: config.fullPath,
  };
  Object.assign(summary, jitter);

  const thresholds = evaluateJitterThresholds(summary);

  const report = {
    meta: {
      phase: config.phase,
      scenario: config.scenario,
      mode: 'jitter_enhanced',
      startedAt,
      endedAt,
      durationSec: Math.round((endedAt - startedAt) / 1000),
      baseUrl: config.baseUrl,
      wsUrl: config.wsUrl,
      serverActiveSessions: serverSessions,
      toxiproxyProfile: process.env.TOXIPROXY_PROFILE ?? 'enhanced',
    },
    summary,
    thresholds,
    per_second: metrics.per_second,
    client_events_tail: metrics.client_events.slice(-300),
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
    `${report.meta.phase}_${report.meta.scenario}_${config.connections}c_${report.meta.endedAt}.json`,
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
