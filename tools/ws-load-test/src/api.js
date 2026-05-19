async function parseRsp(res) {
  const body = await res.json();
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}: ${JSON.stringify(body)}`);
  }
  if (body.code !== undefined && body.code !== 0 && body.code !== 200) {
    throw new Error(`API error code=${body.code} msg=${body.message ?? body.msg}`);
  }
  return body.data ?? body;
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export async function mintLoadTestToken(loadTestApi, clientId, retries = 1) {
  let lastErr;
  for (let attempt = 0; attempt < retries; attempt += 1) {
    try {
      const res = await fetch(`${loadTestApi}/token`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ clientId }),
      });
      const data = await parseRsp(res);
      return data.accessToken;
    } catch (err) {
      lastErr = err;
      if (attempt < retries - 1) {
        await sleep(300 * (attempt + 1));
      }
    }
  }
  throw lastErr;
}

export async function mintTokensBatched(
  loadTestApi,
  prefix,
  count,
  batchSize,
  retries = 3,
  sequential = false,
) {
  const tokens = new Array(count);
  if (sequential) {
    for (let idx = 1; idx <= count; idx += 1) {
      const clientId = `${prefix}-${String(idx).padStart(4, '0')}`;
      const token = await mintLoadTestToken(loadTestApi, clientId, retries);
      tokens[idx - 1] = { clientId, token };
      if (idx % 20 === 0 || idx === count) {
        process.stdout.write(`minted tokens ${idx}/${count}\n`);
      }
    }
    return tokens;
  }
  for (let offset = 0; offset < count; offset += batchSize) {
    const end = Math.min(offset + batchSize, count);
    await Promise.all(
      Array.from({ length: end - offset }, async (_, j) => {
        const idx = offset + j + 1;
        const clientId = `${prefix}-${String(idx).padStart(4, '0')}`;
        const token = await mintLoadTestToken(loadTestApi, clientId, retries);
        tokens[idx - 1] = { clientId, token };
      }),
    );
    process.stdout.write(`minted tokens ${end}/${count}\n`);
  }
  return tokens;
}

export async function triggerBroadcast(loadTestApi, traceId, round) {
  const res = await fetch(`${loadTestApi}/broadcast`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ traceId, round }),
  });
  return parseRsp(res);
}

export async function fetchActiveSessions(loadTestApi) {
  const res = await fetch(`${loadTestApi}/sessions`);
  const data = await parseRsp(res);
  return data.activeSessions ?? 0;
}

export async function login(baseUrl, username, password) {
  const res = await fetch(`${baseUrl}/api/v1/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });
  const data = await parseRsp(res);
  return data.accessToken;
}
