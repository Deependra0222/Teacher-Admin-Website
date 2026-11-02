// Runs api/index.mjs the way a Vercel Function is called (handler(req, res)), sends the requests
// given as JSON in argv[2] and prints one JSON line with the responses. Used by vercel-entry.test.mjs.
import http from 'node:http';
import { once } from 'node:events';

const requests = JSON.parse(process.argv[2] ?? '[]');

try {
  const { default: handler } = await import('../api/index.mjs');
  const server = http.createServer((req, res) => handler(req, res)).listen(0, '127.0.0.1');
  await once(server, 'listening');
  const base = `http://127.0.0.1:${server.address().port}`;
  const results = [];
  for (const request of requests) {
    const response = await fetch(base + request.path, { headers: request.headers ?? {} });
    results.push({ status: response.status, body: await response.text() });
  }
  server.close();
  console.log(JSON.stringify({
    ok: true,
    results,
    credentialsFile: process.env.GOOGLE_APPLICATION_CREDENTIALS ?? null,
  }));
} catch (error) {
  console.log(JSON.stringify({ ok: false, error: `${error.code ?? ''} ${error.message}`.trim() }));
}
