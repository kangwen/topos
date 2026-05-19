import fs from 'node:fs';
import path from 'node:path';
import { mintLoadTestToken, triggerBroadcast, fetchActiveSessions } from './api.js';
import { clientIdForIndex } from './config.js';
import { LoadTestClient } from './client.js';
import {
  createMetrics,
  snapshotSecond,
  finalizeMetrics,
  evaluateThresholds,
} from './metrics.js';

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function mintTokens(loadTestApi, prefix, count) {
  const tokens = [];
  await Promise.all(
    Array.from({ length: count }, async (_, i) => {
      const clientId = clientIdForIndex(prefix, i + 1);
      const token = await mintLoadTestToken(loadTestApi, clientId);
      tokens[i] = { clientId, token };
    }),
  );
  return tokens;
}

export async function runScenario(config) {
  const startedAt = Date.now();
  const metrics = createMetrics(config.connections);
  const tokens = await mintTokens(config.loadTestApi, config.clientIdPrefix, config.connections);

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

  clients.forEach((c) => c.start());

  const connectDeadline = Date.now() + config.connectTimeoutMs + 5_000;
  while (Date.now() < connectDeadline) {
    if (metrics.connections_established >= config.connections) {
      break;
    }
    await sleep(200);
  }

  const holdMs = config.durationSec * 1000;
  const intervalMs = config.broadcastIntervalSec * 1000;
  let round = 0;
  const maxRounds =
    config.broadcastRounds > 0
      ? config.broadcastRounds
      : Math.max(1, Math.floor(holdMs / intervalMs));

  const statsTimer = setInterval(() => {
    snapshotSecond(metrics, Date.now());
    const online = metrics._online;
    process.stdout.write(
      `[${new Date().toISOString()}] online=${online}/${config.connections} ` +
        `recv=${metrics.messages_received} reconnect=${metrics.reconnect_attempts}\n`,
    );
  }, 1000);

  const broadcastTimer = setInterval(async () => {
    round += 1;
    if (round > maxRounds) {
      return;
    }
    try {
      const traceId = `${config.scenario}-r${round}-${Date.now()}`;
      await triggerBroadcast(config.broadcastApi ?? config.loadTestApi, traceId, round);
      metrics.messages_sent += 1;
      metrics.messages_expected += metrics._online;
    } catch (err) {
      process.stderr.write(`broadcast failed round=${round}: ${err.message}\n`);
    }
  }, intervalMs);

  await sleep(holdMs);
  clearInterval(statsTimer);
  clearInterval(broadcastTimer);

  let serverSessions = -1;
  try {
    serverSessions = await fetchActiveSessions(config.loadTestApi);
  } catch {
    // load-test API may be disabled
  }

  clients.forEach((c) => c.stop());
  await sleep(500);

  const endedAt = Date.now();
  const summary = finalizeMetrics(metrics);
  const thresholds = evaluateThresholds(summary);

  const report = {
    meta: {
      phase: config.phase,
      scenario: config.scenario,
      startedAt,
      endedAt,
      durationSec: Math.round((endedAt - startedAt) / 1000),
      baseUrl: config.baseUrl,
      wsUrl: config.wsUrl,
      serverActiveSessions: serverSessions,
    },
    summary,
    thresholds,
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
