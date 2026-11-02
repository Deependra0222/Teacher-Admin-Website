// Vercel Function: the SmartAttend Express backend (backend/src/app.ts, compiled to backend/dist
// by the Vercel build). vercel.json serves the pages from teacher-website/ and rewrites /api/* and
// /firebase-config.js here. Settings are the environment variables listed in backend/.env.example;
// without FIREBASE_MODE the backend stays "disabled" (real sign-in off, explicit Demo Mode only).
import { createApp } from '../backend/dist/app.js';
import { loadEnvironment } from '../backend/dist/config/environment.js';

// The deployment's own https origins (preview, branch and production URLs) are always allowed,
// in addition to ALLOWED_ORIGINS.
function vercelOrigins(env) {
  return [env.VERCEL_URL, env.VERCEL_BRANCH_URL, env.VERCEL_PROJECT_PRODUCTION_URL]
    .filter((host) => typeof host === 'string' && host.trim() !== '')
    .map((host) => `https://${host.trim()}`);
}

const config = loadEnvironment(process.env);

// firebase-admin is loaded only when Firebase is enabled. Its jwks-rsa dependency require()s the
// ESM-only jose package, which Vercel's Node.js 22 runtime refuses (ERR_REQUIRE_ESM), so loading
// it in "disabled" mode would crash the function. See docs/VERCEL_DEPLOY.md.
const firebase = config.firebase.mode === 'disabled'
  ? undefined
  : (await import('../backend/dist/firebase/admin-runtime.js')).createFirebaseAdminRuntime(config.firebase);

export default createApp({
  ...config,
  allowedOrigins: [...new Set([...config.allowedOrigins, ...vercelOrigins(process.env)])],
  ...(firebase === undefined ? {} : { teacherAuthorization: firebase.teacherAuthorization }),
});
