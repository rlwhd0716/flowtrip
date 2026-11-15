import { spawn } from 'node:child_process';
import { openSync, closeSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

// Explicitly detach every standard handle so Gradle does not wait for the server's lifetime.
const port = Number(process.argv[2] || 8787);
if (!Number.isInteger(port) || port < 1024 || port > 65535) throw new Error('Invalid proxy port');
const stdout = openSync(new URL('./proxy.stdout.log', import.meta.url), 'a');
const stderr = openSync(new URL('./proxy.stderr.log', import.meta.url), 'a');
try {
  const child = spawn(process.execPath, [fileURLToPath(new URL('./server.mjs', import.meta.url))], {
    cwd: fileURLToPath(new URL('../../', import.meta.url)),
    detached: true, windowsHide: true, stdio: ['ignore', stdout, stderr],
    env: { ...process.env, FLOWTRIP_PROXY_PORT: String(port) },
  });
  child.on('error', () => { process.stderr.write('Unable to launch place proxy.\n'); process.exitCode = 1; });
  child.unref();
  if (child.pid) process.stdout.write(String(child.pid));
} finally {
  closeSync(stdout); closeSync(stderr);
}
