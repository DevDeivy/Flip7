import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';
import { setTimeout as delay } from 'node:timers/promises';
import process from 'node:process';

const rootDir = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const serverDir = resolve(rootDir, '..', 'server');
const frontendPort = 4173;
const backendPort = 8080;

function spawnProcess(command, args, options = {}) {
  const child = spawn(command, args, {
    cwd: options.cwd,
    stdio: 'inherit',
    shell: false,
    env: { ...process.env, ...options.env },
  });

  return child;
}

async function waitForUrl(url, timeoutMs = 120000, acceptAnyStatus = false) {
  const startedAt = Date.now();

  while (Date.now() - startedAt < timeoutMs) {
    try {
      const response = await fetch(url, { method: 'GET' });
      if (acceptAnyStatus || response.ok) {
        return;
      }
    } catch {
      // retry
    }

    await delay(1000);
  }

  throw new Error(`Timed out waiting for ${url}`);
}

async function main() {
  const backend = spawnProcess('./gradlew', ['bootRun'], { cwd: serverDir });
  let frontend;

  const cleanup = () => {
    if (frontend && !frontend.killed) {
      frontend.kill('SIGTERM');
    }

    if (!backend.killed) {
      backend.kill('SIGTERM');
    }
  };

  process.on('SIGINT', () => {
    cleanup();
    process.exit(130);
  });

  process.on('SIGTERM', () => {
    cleanup();
    process.exit(143);
  });

  try {
    await waitForUrl(`http://127.0.0.1:${backendPort}/api/flip/rooms`, 120000, true);
    frontend = spawnProcess('npm', ['run', 'dev', '--', '--host', '127.0.0.1', '--port', String(frontendPort)], {
      cwd: rootDir,
    });

    await waitForUrl(`http://127.0.0.1:${frontendPort}`, 120000, false);

    const cucumber = spawnProcess('npm', ['run', 'test:functional:run'], { cwd: rootDir });

    const exitCode = await new Promise((resolve) => {
      cucumber.on('exit', (code) => resolve(code ?? 1));
    });

    cleanup();
    process.exit(exitCode);
  } catch (error) {
    cleanup();
    console.error(error instanceof Error ? error.message : error);
    process.exit(1);
  }
}

void main();
