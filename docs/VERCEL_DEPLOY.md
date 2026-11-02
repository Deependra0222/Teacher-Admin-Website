# Deploy the Teacher Website to Vercel

The teacher website and its Express backend run on Vercel as static pages plus one Vercel Function.
The Android app is not part of the deployment.

Live: <https://teacher-admin-website.vercel.app> (Vercel project `teacher-admin-website`, team
`harshraj333555s-projects`). Sign-in page: <https://teacher-admin-website.vercel.app/ERP.html>.

## What gets deployed

| Part | Where it comes from | How Vercel serves it |
| --- | --- | --- |
| Pages (`/`, `/ERP.html`, `/students.html`, …) | `teacher-website/` | Static files on Vercel's CDN (`outputDirectory`) |
| `/api/*` and `/firebase-config.js` | `backend/` through `api/index.mjs` | One Node.js 22 Vercel Function (rewrites in `vercel.json`) |

- `vercel.json`: install (`cd backend && npm ci`), build (`cd backend && npm run build`, TypeScript → `backend/dist`),
  static folder, rewrites, region `bom1` (Mumbai) and basic security headers.
- `api/index.mjs`: imports `createApp` from `backend/dist` and exports the Express app as the function. It adds the
  deployment's own `https://` Vercel URLs to the allowed CORS origins, and loads `firebase-admin` only when
  `FIREBASE_MODE` is not `disabled` (see the Admin SDK note below).
- `package.json` (root): only pins Node.js `22.x`, matching `backend/package.json` (`>=22 <24`).
- `.vercelignore`: keeps secrets, the Android/Gradle project, PDFs, docs and tests out of the upload. Everything left
  in `teacher-website/` is public, so `teacher-website/docs`, `tests`, `README.md` and `package.json` are excluded.
  Do not remove the secret lines (`.env*`, `.vercel`, key files).

The backend code is unchanged: local preview is still `cd backend && npm run dev` → `http://localhost:3000/`.

## Deploy from this PC

```powershell
npx vercel@latest deploy --prod --yes
```

The first run links the folder to the Vercel project `teacher-admin-website` (it writes `.vercel/`, which git
ignores). On the Vercel Hobby plan a CLI deploy is accepted only when the HEAD commit's author is a member of the
Vercel team, so commit with the git identity of the Vercel account before deploying.

The Vercel project is not connected to the GitHub repository: a `git push` does not deploy by itself. To deploy on
every push, connect the repository in Vercel → Project → Settings → Git.

## Check a deployment

| URL | Expected |
| --- | --- |
| `/` | Teacher dashboard (labelled demo data) |
| `/ERP.html` | Teacher sign-in page; "Continue in explicit DEMO Mode" works |
| `/api/health` | `200` `{"status":"ok","service":"smartattend-teacher-api",…}` |
| `/firebase-config.js` | `window.__SMARTATTEND_FIREBASE__ = {"enabled":false};` (no-store) |
| `/api/me` | `503` `FIREBASE_NOT_CONFIGURED` (fail-closed while Firebase is disabled) |

## Environment variables

Set them in Vercel → Project → Settings → Environment Variables (same names as `backend/.env.example`), then
redeploy. None are needed for the current deployment.

- `FIREBASE_MODE`: unset = `disabled`. Real teacher sign-in is off and only explicit Demo Mode works.
- `ALLOWED_ORIGINS`: extra browser origins, comma-separated. The deployment's own Vercel URLs are already allowed;
  add a custom domain here only if it is not the project's main production domain.
- `JSON_LIMIT`: request body limit, default `100kb`.

## Real teacher sign-in on Vercel (not enabled yet)

`FIREBASE_MODE=adc` uses Google Application Default Credentials. A Vercel Function has none (no `gcloud` login, no
Google metadata server), and service-account key files must never be committed. Do not set `FIREBASE_MODE=adc` on
Vercel yet: the site would load, but every teacher authorization would fail (fail-closed).

The Admin SDK also does not load on Vercel yet: `firebase-admin` 14 → `jwks-rsa` 4 `require()`s the ESM-only
`jose` 6, which works on Node.js 22.12+ locally but fails in Vercel's Node.js 22 runtime with `ERR_REQUIRE_ESM`
(first deployment, 3 Nov 2026). Reproduce locally with
`node --no-experimental-require-module`. That is why `api/index.mjs` imports it only when Firebase is enabled.

To enable it later:

1. Make the function load `firebase-admin` on Vercel (for example bundle `api/index.mjs` with esbuild, or try the
   Node.js 24 runtime) and check `/api/me` on a preview deployment.
2. Give the backend a credential that works on Vercel. Recommended: Vercel OIDC federation with a Google Cloud
   Workload Identity Pool (keyless, needs a small backend change to build the credential). Avoid key files.
3. Set `FIREBASE_MODE=adc`, `FIREBASE_PROJECT_ID=smartattend-2a5ce`, `FIREBASE_WEB_API_KEY`,
   `FIREBASE_WEB_AUTH_DOMAIN`, `FIREBASE_WEB_PROJECT_ID=smartattend-2a5ce`, `FIREBASE_WEB_APP_ID` and optionally
   `FIREBASE_WEB_STORAGE_BUCKET`, `FIREBASE_WEB_MESSAGING_SENDER_ID`.
4. Add the Vercel domain in Firebase Console → Authentication → Settings → Authorized domains (and to the Web API
   key's HTTP-referrer restrictions, if the key is restricted).
5. Redeploy and follow `docs/FIREBASE_TEACHER_LIVE_LOGIN_CHECKLIST.md`.
