# Deploy the Teacher Website to Vercel

The teacher website and its Express backend run on Vercel as static pages plus one Vercel Function.
The Android app is not part of the deployment.

Live: <https://teacher-admin-website.vercel.app> (Vercel project `teacher-admin-website` in deependra0222's Vercel
team `deependra2`). Sign-in page: <https://teacher-admin-website.vercel.app/ERP.html>.

## What gets deployed

| Part | Where it comes from | How Vercel serves it |
| --- | --- | --- |
| Pages (`/`, `/ERP.html`, `/students.html`, …) | `teacher-website/` | Static files on Vercel's CDN (`outputDirectory`) |
| `/api/*` and `/firebase-config.js` | `backend/` through `api/index.mjs` | One Node.js 22 Vercel Function (rewrites in `vercel.json`) |

- `vercel.json`: install (`cd backend && npm ci`), build (`cd backend && npm run build`, TypeScript → `backend/dist`),
  static folder, rewrites, region `bom1` (Mumbai) and basic security headers.
- `api/index.mjs`: imports `createApp` from `backend/dist` and exports the Express app as the function. It adds the
  deployment's own `https://` Vercel URLs to the allowed CORS origins and, only when `FIREBASE_MODE` is not
  `disabled`, prepares and loads `firebase-admin` (see "Real teacher sign-in on Vercel").
- `package.json` (root): pins Node.js `22.x`, matching `backend/package.json` (`>=22 <24`), and runs the entry's
  tests (`npm test` → `tests/vercel-entry.test.mjs`; build the backend first: `cd backend && npm ci && npm run build`).
- `.vercelignore`: keeps secrets, the Android/Gradle project, PDFs, docs and tests out of the upload. Everything left
  in `teacher-website/` is public, so `teacher-website/docs`, `tests`, `README.md` and `package.json` are excluded.
  Do not remove the secret lines (`.env*`, `.vercel`, key files).

The backend code is unchanged: local preview is still `cd backend && npm run dev` → `http://localhost:3000/`.

## Deploy from this PC

This repository deploys from **deependra0222's** Vercel account only. Commits must be authored by
`deependra0222 <deependrabandoliya8@gmail.com>` (set in the clone's local git config): on the Hobby plan Vercel
refuses a deployment whose commit author is not on the Vercel team.

The Vercel CLI on this PC is logged in to another account by default, so this repository keeps its own CLI login in
`%USERPROFILE%\.vercel-deependra` (`-Q` is short for `--global-config`):

```powershell
npx vercel@latest login -Q "$env:USERPROFILE\.vercel-deependra"     # once
npx vercel@latest whoami -Q "$env:USERPROFILE\.vercel-deependra"    # must print the deependra0222 account
npx vercel@latest deploy --prod --yes -Q "$env:USERPROFILE\.vercel-deependra"
```

The CLI writes `.vercel/` (project link) and `.env.local` (a short-lived `VERCEL_OIDC_TOKEN`); git ignores both.

### Deploy on every push

The Vercel project is connected to the GitHub repository `Deependra0222/Teacher-Admin-Website` (production branch
`main`): every push to `main` deploys to production and other branches get preview deployments. This needs the
Vercel GitHub App on the `Deependra0222` GitHub account to keep access to this repository (GitHub → Settings →
Applications → Installed GitHub Apps → Vercel → Configure → Repository access).

## Check a deployment

| URL | Expected |
| --- | --- |
| `/` | Teacher dashboard (labelled demo data) |
| `/ERP.html` | Teacher sign-in page; "Continue in explicit DEMO Mode" works |
| `/api/health` | `200` `{"status":"ok","service":"smartattend-teacher-api",…}` |
| `/firebase-config.js` | Firebase off: `{"enabled":false}`; on: `{"enabled":true,"firebaseOptions":{…}}` (no-store) |
| `/api/me` | Firebase off: `503` `FIREBASE_NOT_CONFIGURED`; on: `401` `AUTHENTICATION_REQUIRED` without a token |

## Environment variables

Set them in Vercel → Project → Settings → Environment Variables (same names as `backend/.env.example`), then
redeploy. With none set, Firebase is off and only explicit Demo Mode works.

- `FIREBASE_MODE`: unset = `disabled`; `adc` = real teacher sign-in (next section).
- `ALLOWED_ORIGINS`: extra browser origins, comma-separated. The deployment's own Vercel URLs are already allowed;
  add a custom domain here only if it is not the project's main production domain.
- `JSON_LIMIT`: request body limit, default `100kb`.

## Real teacher sign-in on Vercel

How `api/index.mjs` makes the backend's `adc` mode work in a Vercel Function:

- **Credential.** A Vercel Function has no Google Application Default Credentials (no `gcloud` login, no metadata
  server). The service-account key is stored as the encrypted, sensitive Vercel variable
  `FIREBASE_SERVICE_ACCOUNT_KEY` (the key JSON, or that JSON in base64). At start-up it is checked (`type` must be
  `service_account`, `project_id` must equal `FIREBASE_PROJECT_ID`), written to a private temp file and named by
  `GOOGLE_APPLICATION_CREDENTIALS`, the standard ADC key-file setting. The key is never committed, and errors never
  quote it.
- **Loading `firebase-admin`.** Vercel starts Node.js with `--no-experimental-require-module` (seen on Node 22.23.4 and
  24.22.2), so CommonJS cannot `require()` ES modules there. `firebase-admin` 14 → `jwks-rsa` 4 `require()`s the
  ESM-only `jose` 6, which fails with `ERR_REQUIRE_ESM`. The entry imports `jose` as ESM first and puts it in the
  CommonJS module cache, where `require('jose')` finds it; where Node can do this itself (local runs) nothing changes.
  `tests/vercel-entry.test.mjs` runs the entry with the same flag; a preview deployment with Firebase on answered
  `/api/me` with a fake token as `401 INVALID_TOKEN` (3 Nov 2026).

To turn it on (or after rotating the key):

1. Firebase Console → project `smartattend-2a5ce` → Project settings → General → Your apps → Web app: copy
   `apiKey`, `authDomain`, `appId` (and `storageBucket`, `messagingSenderId`). These are public values.
2. Project settings → Service accounts → Generate new private key. Upload the downloaded JSON file (outside this
   repository) as a sensitive variable, then delete the file:
   `Get-Content <key.json> -Raw | npx vercel@latest env add FIREBASE_SERVICE_ACCOUNT_KEY production --sensitive -Q "$env:USERPROFILE\.vercel-deependra"`
3. Set `FIREBASE_MODE=adc`, `FIREBASE_PROJECT_ID=smartattend-2a5ce`, `FIREBASE_WEB_API_KEY`,
   `FIREBASE_WEB_AUTH_DOMAIN`, `FIREBASE_WEB_PROJECT_ID=smartattend-2a5ce`, `FIREBASE_WEB_APP_ID`,
   `FIREBASE_WEB_STORAGE_BUCKET`, `FIREBASE_WEB_MESSAGING_SENDER_ID` for Production.
4. Firebase Console → Authentication → Settings → Authorized domains: add `teacher-admin-website.vercel.app` (and the
   domain in the Web API key's HTTP-referrer restrictions, if the key is restricted).
5. Redeploy, then sign in on `/ERP.html` with a teacher account that has a complete `users/{uid}` profile
   (`docs/FIREBASE_TEACHER_LIVE_LOGIN_CHECKLIST.md`).

To turn it off again, remove `FIREBASE_MODE` (or set it to `disabled`) and redeploy. Revoke a leaked key in Google
Cloud Console → IAM → Service accounts → Keys.
