// Tests for the Vercel entry api/index.mjs. Needs the compiled backend: `cd backend && npm run build`.
// Every case runs in its own Node.js process with --no-experimental-require-module, the flag Vercel's
// Node.js runtime is started with (seen on Vercel with Node 22.23.4 and 24.22.2, 3 Nov 2026).
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdirSync, mkdtempSync, readFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { after, describe, test } from 'node:test';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('../', import.meta.url));
const driver = fileURLToPath(new URL('./vercel-entry-driver.mjs', import.meta.url));
const tempRoot = mkdtempSync(path.join(tmpdir(), 'smartattend-vercel-test-'));
after(() => rmSync(tempRoot, { recursive: true, force: true }));

const VERCEL_FLAG = '--no-experimental-require-module';
const PROJECT = 'demo-smartattend';
const WEB_CONFIG = {
  FIREBASE_MODE: 'adc',
  FIREBASE_PROJECT_ID: PROJECT,
  FIREBASE_WEB_API_KEY: 'test-api-key',
  FIREBASE_WEB_AUTH_DOMAIN: `${PROJECT}.firebaseapp.com`,
  FIREBASE_WEB_PROJECT_ID: PROJECT,
  FIREBASE_WEB_APP_ID: '1:000000000000:web:test',
};
const FAKE_KEY = {
  type: 'service_account',
  project_id: PROJECT,
  private_key_id: 'test',
  private_key: '-----BEGIN PRIVATE KEY-----\nTEST\n-----END PRIVATE KEY-----\n',
  client_email: `test@${PROJECT}.iam.gserviceaccount.com`,
  client_id: '1',
};

let caseNumber = 0;
function runEntry(env, requests = []) {
  const temp = path.join(tempRoot, String(++caseNumber));
  mkdirSync(temp);
  const result = spawnSync(process.execPath, [VERCEL_FLAG, driver, JSON.stringify(requests)], {
    cwd: root,
    encoding: 'utf8',
    env: {
      PATH: process.env.PATH,
      SystemRoot: process.env.SystemRoot,
      TEMP: temp,
      TMP: temp,
      TMPDIR: temp,
      VERCEL_URL: 'preview-test.vercel.app',
      ...env,
    },
  });
  const line = result.stdout.trim().split('\n').pop() ?? '';
  assert.ok(line.startsWith('{'), `driver printed no result. stderr: ${result.stderr}`);
  return { ...JSON.parse(line), temp };
}

describe('Vercel runtime limitation', () => {
  test('firebase-admin cannot be required directly with the Vercel flag (ERR_REQUIRE_ESM)', () => {
    const result = spawnSync(process.execPath, [
      VERCEL_FLAG,
      '-e',
      "require('./backend/node_modules/firebase-admin/lib/auth/index.js')",
    ], { cwd: root, encoding: 'utf8' });
    assert.notEqual(result.status, 0);
    assert.match(result.stderr, /ERR_REQUIRE_ESM/);
  });
});

describe('api/index.mjs with Firebase disabled', () => {
  test('serves health, a disabled config and fail-closed teacher routes', () => {
    const out = runEntry({}, [
      { path: '/api/health' },
      { path: '/firebase-config.js' },
      { path: '/api/me' },
      { path: '/api/nope' },
      { path: '/api/health', headers: { Origin: 'https://preview-test.vercel.app' } },
      { path: '/api/health', headers: { Origin: 'https://evil.example' } },
    ]);
    assert.equal(out.ok, true, out.error);
    assert.deepEqual(out.results.map((r) => r.status), [200, 200, 503, 404, 200, 403]);
    assert.match(out.results[1].body, /"enabled":false/);
    assert.equal(out.credentialsFile, null);
  });
});

describe('api/index.mjs with Firebase enabled (adc)', () => {
  test('loads firebase-admin despite the Vercel flag and checks tokens', () => {
    const out = runEntry(WEB_CONFIG, [
      { path: '/firebase-config.js' },
      { path: '/api/me' },
      { path: '/api/me', headers: { Authorization: 'Bearer not-a-firebase-token' } },
    ]);
    assert.equal(out.ok, true, out.error);
    assert.match(out.results[0].body, /"enabled":true/);
    assert.match(out.results[0].body, /"apiKey":"test-api-key"/);
    assert.equal(out.results[1].status, 401);
    assert.match(out.results[1].body, /AUTHENTICATION_REQUIRED/);
    assert.equal(out.results[2].status, 401);
    assert.match(out.results[2].body, /INVALID_TOKEN/);
    assert.equal(out.credentialsFile, null);
  });

  for (const [name, encode] of [
    ['JSON', (key) => JSON.stringify(key, null, 2)],
    ['base64', (key) => Buffer.from(JSON.stringify(key)).toString('base64')],
  ]) {
    test(`writes a ${name} FIREBASE_SERVICE_ACCOUNT_KEY to a temp file for GOOGLE_APPLICATION_CREDENTIALS`, () => {
      const out = runEntry({ ...WEB_CONFIG, FIREBASE_SERVICE_ACCOUNT_KEY: encode(FAKE_KEY) });
      assert.equal(out.ok, true, out.error);
      assert.equal(path.dirname(out.credentialsFile), out.temp);
      assert.deepEqual(JSON.parse(readFileSync(out.credentialsFile, 'utf8')), FAKE_KEY);
    });
  }

  test('rejects a broken key without echoing it', () => {
    const out = runEntry({
      ...WEB_CONFIG,
      FIREBASE_SERVICE_ACCOUNT_KEY: '{"type":"service_account","private_key":"SECRET-MARKER"',
    });
    assert.equal(out.ok, false);
    assert.match(out.error, /not a valid service-account JSON key/);
    assert.doesNotMatch(out.error, /SECRET-MARKER/);
  });

  test('rejects a key of another project', () => {
    const out = runEntry({
      ...WEB_CONFIG,
      FIREBASE_SERVICE_ACCOUNT_KEY: JSON.stringify({ ...FAKE_KEY, project_id: 'some-other-project' }),
    });
    assert.equal(out.ok, false);
    assert.match(out.error, /service-account key of FIREBASE_PROJECT_ID/);
  });

  test('leaves an existing GOOGLE_APPLICATION_CREDENTIALS alone', () => {
    const out = runEntry({
      ...WEB_CONFIG,
      GOOGLE_APPLICATION_CREDENTIALS: '/existing/key.json',
      FIREBASE_SERVICE_ACCOUNT_KEY: JSON.stringify(FAKE_KEY),
    });
    assert.equal(out.ok, true, out.error);
    assert.equal(out.credentialsFile, '/existing/key.json');
  });
});
