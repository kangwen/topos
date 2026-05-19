#!/usr/bin/env node
import { parseArgs } from './config.js';
import { runScenario } from './runner.js';
import { runBurstRampScenario } from './runner-burst.js';
import { runJitterScenario } from './runner-jitter.js';

async function main() {
  const config = parseArgs(process.argv);

  if (config.jitterMode) {
    await runJitterScenario(config);
    return;
  }

  if (config.burstRamp) {
    await runBurstRampScenario(config);
    return;
  }

  if (config.ladder) {
    for (const n of config.ladderSteps) {
      process.stdout.write(`\n>>> Ladder step: ${n} connections\n`);
      await runScenario({ ...config, connections: n });
    }
    return;
  }

  await runScenario(config);
}

main().catch((err) => {
  process.stderr.write(`${err.stack ?? err.message}\n`);
  process.exit(1);
});
