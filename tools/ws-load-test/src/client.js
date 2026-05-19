import WebSocket from 'ws';
import { recordClientEvent } from './metrics.js';

function jitterMs(maxJitter) {
  return Math.floor(Math.random() * maxJitter);
}

function backoffMs(attempt, maxMs) {
  const base = Math.min(maxMs, 1000 * 2 ** Math.max(0, attempt - 1));
  return base + jitterMs(Math.min(1000, base));
}

export class LoadTestClient {
  constructor({
    clientId,
    token,
    wsUrl,
    connectTimeoutMs,
    reconnectMaxMs,
    autoReconnect,
    metrics,
    onEstablished,
    onDisconnected,
  }) {
    this.clientId = clientId;
    this.token = token;
    this.wsUrl = wsUrl;
    this.connectTimeoutMs = connectTimeoutMs;
    this.reconnectMaxMs = reconnectMaxMs;
    this.autoReconnect = autoReconnect;
    this.metrics = metrics;
    this.onEstablished = onEstablished;
    this.onDisconnected = onDisconnected;
    this.ws = null;
    this.intentionalClose = false;
    this.reconnectAttempt = 0;
    this.disconnectAt = 0;
    this.connected = false;
    this.countedEstablishment = false;
    this.pendingReconnect = false;
  }

  start() {
    this.connect();
  }

  stop() {
    this.intentionalClose = true;
    if (this.ws) {
      this.ws.close(1000, 'test_complete');
    }
  }

  connect() {
    const url = `${this.wsUrl}?access_token=${encodeURIComponent(this.token)}`;
    const ws = new WebSocket(url);
    this.ws = ws;
    let opened = false;

    const connectTimer = setTimeout(() => {
      if (!opened) {
        ws.terminate();
        this.scheduleReconnect('connect_timeout');
      }
    }, this.connectTimeoutMs);

    ws.on('open', () => {
      opened = true;
      clearTimeout(connectTimer);
      const now = Date.now();
      if (!this.connected) {
        this.metrics._online += 1;
        this.connected = true;
        if (!this.countedEstablishment) {
          this.countedEstablishment = true;
          this.metrics.connections_established += 1;
          recordClientEvent(this.metrics, {
            clientId: this.clientId,
            connectTs: now,
            reconnectAttempt: this.reconnectAttempt,
          });
        }
      }
      if (this.disconnectAt > 0) {
        const recoverMs = now - this.disconnectAt;
        this.metrics.recover_ms_samples.push(recoverMs);
        this.metrics.reconnect_success += 1;
        recordClientEvent(this.metrics, {
          clientId: this.clientId,
          reconnectAttempt: this.reconnectAttempt,
          recoverTs: now,
          recoverMs,
        });
      }
      this.reconnectAttempt = 0;
      this.onEstablished?.(this);
    });

    ws.on('message', (data) => {
      this.handleMessage(data.toString());
    });

    ws.on('ping', () => {
      // `ws` replies with pong automatically; also send app-level pong for parity with production clients.
      if (ws.readyState === WebSocket.OPEN) {
        ws.send('PONG');
      }
    });

    ws.on('close', (code, reason) => {
      clearTimeout(connectTimer);
      this.handleClose(code, reason?.toString() ?? '');
    });

    ws.on('error', (err) => {
      clearTimeout(connectTimer);
      if (!opened) {
        this.scheduleReconnect(err.message);
      } else if (!this.intentionalClose) {
        try {
          ws.terminate();
        } catch {
          // ignore
        }
        this.handleClose(1006, err.message);
      }
    });
  }

  handleMessage(raw) {
    try {
      const msg = JSON.parse(raw);
      if (msg?.type === 'load_test_signal' && msg.traceId) {
        const receivedAtMs = Date.now();
        const sentAtMs = msg.sentAtMs ?? 0;
        this.metrics.messages_received += 1;
        if (sentAtMs > 0 && receivedAtMs >= sentAtMs) {
          this.metrics.e2e_latency_ms_samples.push(receivedAtMs - sentAtMs);
        }
      }
    } catch {
      // ignore non-json frames
    }
  }

  handleClose(code, reason) {
    if (this.connected) {
      this.metrics._online = Math.max(0, this.metrics._online - 1);
      this.connected = false;
    }
    if (!this.intentionalClose) {
      this.metrics.abnormal_disconnects += 1;
      recordClientEvent(this.metrics, {
        clientId: this.clientId,
        disconnectReason: `${code}:${reason}`,
        disconnectTs: Date.now(),
      });
      this.onDisconnected?.(this, code, reason);
      this.scheduleReconnect(`close_${code}`);
    }
  }

  scheduleReconnect(reason) {
    if (!this.autoReconnect || this.intentionalClose || this.pendingReconnect) {
      return;
    }
    this.pendingReconnect = true;
    this.reconnectAttempt += 1;
    this.metrics.reconnect_attempts += 1;
    this.disconnectAt = Date.now();
    const delay = backoffMs(this.reconnectAttempt, this.reconnectMaxMs);
    const stagger = jitterMs(3000);
    recordClientEvent(this.metrics, {
      clientId: this.clientId,
      reconnectAttempt: this.reconnectAttempt,
      reconnectDelayMs: delay + stagger,
      reconnectReason: reason,
    });
    setTimeout(() => {
      this.pendingReconnect = false;
      this.connect();
    }, delay + stagger);
  }
}
