// Vercel Function: the SmartAttend Express backend (backend/src/app.ts, compiled to backend/dist
// by the Vercel build). vercel.json serves the pages from teacher-website/ and rewrites /api/* and
// /firebase-config.js here. Settings are the environment variables listed in backend/.env.example,
// plus FIREBASE_SERVICE_ACCOUNT_KEY for real sign-in (docs/VERCEL_DEPLOY.md). Without FIREBASE_MODE
// the backend stays "disabled" (real sign-in off, explicit Demo Mode only).
import { writeFileSync } from 'node:fs';
import { Module, createRequire } from 'node:module';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

import { createApp } from '../backend/dist/app.js';
import { loadEnvironment } from '../backend/dist/config/environment.js';

// The deployment's own https origins (preview, branch and production URLs) are always allowed,
// in addition to ALLOWED_ORIGINS.
function vercelOrigins(env) {
  return [env.VERCEL_URL, env.VERCEL_BRANCH_URL, env.VERCEL_PROJECT_PRODUCTION_URL]
    .filter((host) => typeof host === 'string' && host.trim() !== '')
    .map((host) => `https://${host.trim()}`);
}

// A Vercel Function has no Google Application Default Credentials. In "adc" mode the service-account
// key comes from the encrypted Vercel environment variable FIREBASE_SERVICE_ACCOUNT_KEY (the key JSON,
// or that JSON in base64) and is written to a private temporary file named by
// GOOGLE_APPLICATION_CREDENTIALS, the standard ADC key-file setting. It is never part of the repository.
function provideServiceAccountKey(env) {
  const raw = env.FIREBASE_SERVICE_ACCOUNT_KEY?.trim();
  if (!raw || env.GOOGLE_APPLICATION_CREDENTIALS?.trim()) return;

  let key;
  try {
    key = JSON.parse(raw.startsWith('{') ? raw : Buffer.from(raw, 'base64').toString('utf8'));
  } catch {
    // JSON.parse messages quote the input, so the key text never goes into this error.
    throw new Error('FIREBASE_SERVICE_ACCOUNT_KEY is not a valid service-account JSON key.');
  }
  if (key?.type !== 'service_account' || key.project_id !== env.FIREBASE_PROJECT_ID?.trim()) {
    throw new Error('FIREBASE_SERVICE_ACCOUNT_KEY must be a service-account key of FIREBASE_PROJECT_ID.');
  }

  const file = path.join(tmpdir(), 'smartattend-service-account.json');
  writeFileSync(file, JSON.stringify(key), { mode: 0o600 });
  env.GOOGLE_APPLICATION_CREDENTIALS = file;
}

// Vercel starts Node.js with --no-experimental-require-module, so CommonJS code cannot require() an
// ES module there (ERR_REQUIRE_ESM). firebase-admin → jwks-rsa require()s the ESM-only jose package:
// jose is imported as ESM first and put in the CommonJS module cache, where require('jose') finds it.
// Where Node can require() ES modules itself (local runs), nothing is changed.
async function provideJoseToCommonJs() {
  if (process.features.require_module) return;

  const fromBackend = createRequire(new URL('../backend/package.json', import.meta.url));
  const josePath = createRequire(fromBackend.resolve('jwks-rsa')).resolve('jose');
  if (Module._cache[josePath] !== undefined) return;

  const jose = await import(pathToFileURL(josePath).href);
  const cached = new Module(josePath);
  cached.filename = josePath;
  cached.paths = Module._nodeModulePaths(path.dirname(josePath));
  cached.exports = jose;
  cached.loaded = true;
  Module._cache[josePath] = cached;
}

const config = loadEnvironment(process.env);

// firebase-admin is loaded only when Firebase is enabled.
let firebase;
if (config.firebase.mode !== 'disabled') {
  if (config.firebase.mode === 'adc') provideServiceAccountKey(process.env);
  await provideJoseToCommonJs();
  const { createFirebaseAdminRuntime } = await import('../backend/dist/firebase/admin-runtime.js');
  firebase = createFirebaseAdminRuntime(config.firebase);
}

export default createApp({
  ...config,
  allowedOrigins: [...new Set([...config.allowedOrigins, ...vercelOrigins(process.env)])],
  ...(firebase === undefined ? {} : { teacherAuthorization: firebase.teacherAuthorization }),
});
