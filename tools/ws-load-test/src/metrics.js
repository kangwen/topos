function percentile(sorted, p) {
  if (sorted.length === 0) return 0;
  const idx = Math.ceil((p / 100) * sorted.length) - 1;
  return sorted[Math.max(0, Math.min(sorted.length - 1, idx))];
}

export function createMetrics(targetConnections) {
  return {
    connections_target: targetConnections,
    connections_established: 0,
    abnormal_disconnects: 0,
    messages_expected: 0,
    messages_received: 0,
    messages_sent: 0,
    reconnect_attempts: 0,
    reconnect_success: 0,
    recover_ms_samples: [],
    e2e_latency_ms_samples: [],
    per_second: [],
    client_events: [],
    _online: 0,
  };
}

export function snapshotSecond(metrics, ts) {
  metrics.per_second.push({
    ts,
    online: metrics._online,
    reconnect_attempts: metrics.reconnect_attempts,
    messages_received: metrics.messages_received,
  });
}

export function recordClientEvent(metrics, event) {
  metrics.client_events.push(event);
  if (metrics.client_events.length > 20_000) {
    metrics.client_events.shift();
  }
}

export function finalizeMetrics(metrics) {
  const recoverSorted = [...metrics.recover_ms_samples].sort((a, b) => a - b);
  const latencySorted = [...metrics.e2e_latency_ms_samples].sort((a, b) => a - b);
  const expected = metrics.messages_expected;
  const received = metrics.messages_received;
  const delivery_rate = expected > 0 ? received / expected : 1;
  const connect_rate =
    metrics.connections_target > 0
      ? metrics.connections_established / metrics.connections_target
      : 1;
  const reconnect_rate =
    metrics.reconnect_attempts > 0
      ? metrics.reconnect_success / metrics.reconnect_attempts
      : 1;

  return {
    connections_target: metrics.connections_target,
    connections_established: metrics.connections_established,
    connect_success_rate: round4(connect_rate),
    abnormal_disconnects: metrics.abnormal_disconnects,
    messages_sent: metrics.messages_sent,
    messages_expected: expected,
    messages_received: received,
    delivery_rate: round4(delivery_rate),
    reconnect_attempts: metrics.reconnect_attempts,
    reconnect_success: metrics.reconnect_success,
    reconnect_success_rate: round4(reconnect_rate),
    avg_recover_ms: avg(recoverSorted),
    p95_recover_ms: percentile(recoverSorted, 95),
    avg_e2e_latency_ms: avg(latencySorted),
    p95_e2e_latency_ms: percentile(latencySorted, 95),
    per_second_samples: metrics.per_second.length,
  };
}

function avg(sorted) {
  if (sorted.length === 0) return 0;
  return Math.round(sorted.reduce((a, b) => a + b, 0) / sorted.length);
}

function round4(n) {
  return Math.round(n * 10_000) / 10_000;
}

/** 阶段 B：稳定性 + 必须观察到网络压力下的断开/重连 + 恢复后在线率 */
export function evaluateJitterThresholds(summary) {
  const checks = [
    {
      name: 'connect_success_rate',
      pass: summary.connect_success_rate >= 0.99,
      actual: summary.connect_success_rate,
      threshold: 0.99,
    },
    {
      name: 'delivery_rate',
      pass: summary.delivery_rate >= 0.98,
      actual: summary.delivery_rate,
      threshold: 0.98,
    },
    {
      name: 'reconnect_success_rate',
      pass:
        summary.reconnect_attempts === 0 ||
        summary.reconnect_success_rate >= 0.93,
      actual: summary.reconnect_success_rate,
      threshold: 0.93,
    },
    {
      name: 'recovery_online_ratio',
      pass: (summary.recovery_online_ratio ?? 0) >= 0.95,
      actual: summary.recovery_online_ratio ?? 0,
      threshold: 0.95,
    },
    {
      name: 'min_online_post_spike_ratio',
      pass: (summary.min_online_post_spike_ratio ?? 0) >= 0.5,
      actual: summary.min_online_post_spike_ratio ?? 0,
      threshold: 0.5,
    },
    {
      name: 'observed_network_stress',
      pass:
        summary.abnormal_disconnects >= 5 || summary.reconnect_attempts >= 5,
      actual: Math.max(summary.abnormal_disconnects, summary.reconnect_attempts),
      threshold: 5,
    },
  ];
  return {
    passed: checks.every((c) => c.pass),
    checks,
  };
}

/** 突发建连：窗口内建连成功率 >= 99%，且（若全部连上）耗时不超过 ramp 窗口 */
export function evaluateBurstThresholds(summary, rampSec) {
  const rampRate = summary.ramp_connect_success_rate ?? summary.connect_success_rate;
  const checks = [
    {
      name: 'ramp_connect_success_rate',
      pass: rampRate >= 0.99,
      actual: rampRate,
      threshold: 0.99,
    },
  ];
  if (summary.time_to_full_connect_ms != null) {
    checks.push({
      name: 'time_to_full_connect_within_ramp',
      pass: summary.time_to_full_connect_ms <= rampSec * 1000,
      actual: summary.time_to_full_connect_ms,
      threshold: rampSec * 1000,
    });
  }
  return {
    passed: checks.every((c) => c.pass),
    checks,
  };
}

export function evaluateThresholds(summary) {
  const checks = [
    {
      name: 'connect_success_rate',
      pass: summary.connect_success_rate >= 0.99,
      actual: summary.connect_success_rate,
      threshold: 0.99,
    },
    {
      name: 'delivery_rate',
      pass: summary.delivery_rate >= 0.99,
      actual: summary.delivery_rate,
      threshold: 0.99,
    },
    {
      name: 'reconnect_success_rate',
      pass: summary.reconnect_attempts === 0 || summary.reconnect_success_rate >= 0.98,
      actual: summary.reconnect_success_rate,
      threshold: 0.98,
    },
  ];
  return {
    passed: checks.every((c) => c.pass),
    checks,
  };
}
