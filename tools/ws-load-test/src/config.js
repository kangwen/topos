const DEFAULTS = {
  baseUrl: 'http://127.0.0.1:8083',
  wsPath: '/ws/v1/strategy',
  loadTestPrefix: 'load',
  durationSec: 180,
  broadcastIntervalSec: 10,
  broadcastRounds: 0,
  connectTimeoutMs: 15_000,
  reconnectMaxMs: 30_000,
  clientIdPrefix: 'load',
  connections: 10,
  phase: 'A',
  scenario: 'baseline',
  ladder: false,
  ladderSteps: [10, 20, 50, 100],
  autoReconnect: true,
  reportDir: 'reports',
  burstRamp: false,
  rampSec: 60,
  mintBatchSize: 100,
  jitterMode: false,
  fullPath: false,
  connectRampSec: 15,
  mintRetries: 3,
  jitterSpike: false,
  spikeAtSec: 90,
  spikeSec: 20,
  spikeToxicity: 0.03,
};

function readEnv(name, fallback) {
  const v = process.env[name];
  return v === undefined || v === '' ? fallback : v;
}

function parseBool(v, fallback) {
  if (v === undefined) return fallback;
  return v === 'true' || v === '1' || v === 'yes';
}

function parseIntArg(v, fallback) {
  if (v === undefined || v === '') return fallback;
  const n = Number.parseInt(v, 10);
  return Number.isFinite(n) ? n : fallback;
}

export function parseArgs(argv) {
  const args = { ...DEFAULTS };
  for (let i = 2; i < argv.length; i += 1) {
    const key = argv[i];
    const next = argv[i + 1];
    switch (key) {
      case '--base-url':
        args.baseUrl = next;
        i += 1;
        break;
      case '--ws-path':
        args.wsPath = next;
        i += 1;
        break;
      case '--connections':
        args.connections = parseIntArg(next, args.connections);
        i += 1;
        break;
      case '--duration-sec':
        args.durationSec = parseIntArg(next, args.durationSec);
        i += 1;
        break;
      case '--broadcast-interval-sec':
        args.broadcastIntervalSec = parseIntArg(next, args.broadcastIntervalSec);
        i += 1;
        break;
      case '--broadcast-rounds':
        args.broadcastRounds = parseIntArg(next, args.broadcastRounds);
        i += 1;
        break;
      case '--phase':
        args.phase = next;
        i += 1;
        break;
      case '--scenario':
        args.scenario = next;
        i += 1;
        break;
      case '--client-id-prefix':
        args.clientIdPrefix = next;
        i += 1;
        break;
      case '--ladder':
        args.ladder = true;
        break;
      case '--no-reconnect':
        args.autoReconnect = false;
        break;
      case '--report-dir':
        args.reportDir = next;
        i += 1;
        break;
      case '--burst':
        args.burstRamp = true;
        break;
      case '--ramp-sec':
        args.rampSec = parseIntArg(next, args.rampSec);
        i += 1;
        break;
      case '--mint-batch-size':
        args.mintBatchSize = parseIntArg(next, args.mintBatchSize);
        i += 1;
        break;
      default:
        break;
    }
  }

  args.baseUrl = readEnv('BASE_URL', args.baseUrl).replace(/\/$/, '');
  args.wsPath = readEnv('WS_PATH', args.wsPath);
  args.durationSec = parseIntArg(readEnv('DURATION_SEC', ''), args.durationSec);
  args.broadcastIntervalSec = parseIntArg(readEnv('BROADCAST_INTERVAL_SEC', ''), args.broadcastIntervalSec);
  args.broadcastRounds = parseIntArg(readEnv('BROADCAST_ROUNDS', ''), args.broadcastRounds);
  args.connectTimeoutMs = parseIntArg(readEnv('CONNECT_TIMEOUT_MS', ''), args.connectTimeoutMs);
  args.reconnectMaxMs = parseIntArg(readEnv('RECONNECT_MAX_MS', ''), args.reconnectMaxMs);
  args.clientIdPrefix = readEnv('CLIENT_ID_PREFIX', args.clientIdPrefix);
  args.rampSec = parseIntArg(readEnv('RAMP_SEC', ''), args.rampSec);
  args.mintBatchSize = parseIntArg(readEnv('MINT_BATCH_SIZE', ''), args.mintBatchSize);

  if (args.burstRamp || args.phase === 'E' || args.scenario === 'burst_2000_60s') {
    args.burstRamp = true;
    if (args.connections === DEFAULTS.connections) {
      args.connections = 2000;
    }
    if (args.rampSec === DEFAULTS.rampSec && !process.env.RAMP_SEC) {
      args.rampSec = 60;
    }
    if (args.clientIdPrefix === DEFAULTS.clientIdPrefix) {
      args.clientIdPrefix = 'burst';
    }
    if (args.durationSec === DEFAULTS.durationSec) {
      args.durationSec = 30;
    }
    if (args.autoReconnect === DEFAULTS.autoReconnect) {
      args.autoReconnect = false;
    }
    if (args.broadcastRounds === DEFAULTS.broadcastRounds) {
      args.broadcastRounds = 0;
    }
  }

  if (args.phase === 'B' || args.scenario.startsWith('jitter')) {
    args.jitterMode = true;
    if (args.connections === DEFAULTS.connections) {
      args.connections = 100;
    }
    if (args.durationSec === DEFAULTS.durationSec) {
      args.durationSec = 240;
    }
    if (args.connectTimeoutMs === DEFAULTS.connectTimeoutMs) {
      args.connectTimeoutMs = 30_000;
    }
    if (args.autoReconnect === DEFAULTS.autoReconnect) {
      args.autoReconnect = true;
    }
    args.fullPath = !process.env.DIRECT_BASE_URL;
    args.jitterSpike = process.env.JITTER_SPIKE !== '0';
    args.spikeAtSec = parseIntArg(readEnv('SPIKE_AT_SEC', ''), args.spikeAtSec);
    if (args.clientIdPrefix === DEFAULTS.clientIdPrefix) {
      args.clientIdPrefix = 'load-b';
    }
    args.mintBatchSize = 10;
    args.mintRetries = parseIntArg(readEnv('MINT_RETRIES', ''), 8);
  }

  const directBase = readEnv('DIRECT_BASE_URL', '').replace(/\/$/, '');
  args.directBaseUrl = args.fullPath ? args.baseUrl : directBase || args.baseUrl;
  const wsBase = args.baseUrl.replace(/^http/, 'ws');
  args.wsUrl = `${wsBase}${args.wsPath}`;
  args.loadTestApi = `${args.directBaseUrl}/api/v1/wss/load-test`;
  args.broadcastApi = `${args.baseUrl}/api/v1/wss/load-test`;
  return args;
}

export function clientIdForIndex(prefix, index, width = 4) {
  return `${prefix}-${String(index).padStart(width, '0')}`;
}
