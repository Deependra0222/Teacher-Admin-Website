# SmartAttend Teacher Backend — Milestone 1

This folder contains the local Node.js/Express foundation for the SmartAttend Teacher Website. It serves the unchanged website in `../teacher-website/` and exposes a small health endpoint.

This milestone does **not** implement Firebase teacher login, teacher-role authorization, Firestore data, live attendance, or ESP32/BLE verification. The website still shows fictional browser-local demo data and labels it as DEMO / NOT LIVE.

## What runs now

- Teacher website: `http://localhost:3000/`
- Teacher sign-in preview: `http://localhost:3000/ERP.html`
- Backend health check: `http://localhost:3000/api/health`
- Unknown `/api/*` paths return JSON 404 errors instead of website HTML.
- CORS accepts only configured browser origins. Requests without an `Origin` header, such as local health checks, remain supported.
- JSON request bodies are limited to 100 KB by default and receive safe parsing/size errors.

The installed `firebase-admin` package is reserved for the next milestone. No Firebase Admin app is initialized and no Firebase request is made here.

## Prerequisite

Install Node.js 22. The project declares support for Node.js 22 and uses npm's lockfile for reproducible dependencies.

The initial verification machine had Node.js 23.11.0 and npm 11.4.2 installed. Builds and tests passed there, but that is not a claim that Node.js 22 was executed on this machine.

## First-time setup

Open PowerShell and run:

```powershell
cd "D:\Smart Attend\backend"
npm ci
```

`npm ci` installs exactly the versions recorded in `package-lock.json`.

## Start in development mode

```powershell
cd "D:\Smart Attend\backend"
npm run dev
```

Leave that PowerShell window open. Then open:

```text
http://localhost:3000/
```

Stop the server with `Ctrl+C`.

## Test and build

```powershell
cd "D:\Smart Attend\backend"
npm test
npm run typecheck
npm run build
npm start
```

- `npm test` runs the real Express app through Supertest without opening a network port.
- `npm run typecheck` validates strict TypeScript types.
- `npm run build` creates `dist/`.
- `npm start` runs the compiled `dist/index.js` server.

## Environment settings

The defaults work without an `.env` file. To customize local settings:

```powershell
cd "D:\Smart Attend\backend"
Copy-Item .env.example .env
```

Available values:

| Variable | Default | Purpose |
|---|---|---|
| `PORT` | `3000` | Local HTTP port, from 1 to 65535. |
| `ALLOWED_ORIGINS` | `http://localhost:3000` | Comma-separated browser origins allowed to call the API. |
| `JSON_LIMIT` | `100kb` | Maximum JSON request-body size. |
| `STATIC_DIR` | `../teacher-website` | Optional path to the static teacher website. |

The local `.env` file is ignored by Git. Never place service-account JSON, private keys, passwords, or Android `google-services.json` in this backend.

## Folder responsibilities

```text
backend/
  src/app.ts                  Express routes, CORS, static files, safe errors
  src/config/environment.ts   Validated local server settings
  src/index.ts                Process startup and HTTP listener
  tests/app.test.ts           Health, CORS, errors, static site and config tests
```

There are no empty controller/service/repository folders in Milestone 1. They will be added only when an implemented feature needs them.

## Firebase Console work for Milestone 2

No Firebase Console change is required to demonstrate Milestone 1. Before implementing real teacher authentication:

1. Open Firebase Console and select the existing **SmartAttend** project used by the Android app.
2. Open **Project settings → General → Your apps**.
3. Click the Web (`</>`) app button.
4. Register a Web app named, for example, **SmartAttend Teacher Portal**.
5. Do not enable Firebase Hosting yet.
6. Keep the displayed Firebase Web SDK configuration available for the next milestone. It contains public project identifiers and is different from Android `google-services.json`.
7. Do not download or commit a service-account private key. Local trusted-server access will first be developed with the Firebase Emulator Suite or Application Default Credentials.
8. Do not add `role: "teacher"` from browser code. A teacher account and `users/{uid}` profile must be provisioned manually by an authorized administrator after the teacher-profile and assignment schema is confirmed.

Milestone 2 will add Firebase Web Email/Password sign-in, ID-token forwarding, Firebase Admin token verification, exact teacher-role checks, assignment authorization, logout/session errors, and negative tests for student/unauthorized users. Production Firestore rules will remain restrictive.

## College demonstration

1. Run `npm ci`, `npm test`, and `npm run build` in `backend/`.
2. Run `npm start`.
3. Open `http://localhost:3000/` and navigate through the existing pages.
4. Open `http://localhost:3000/api/health` to show that the Express backend is running.
5. Explain that all current page records and actions remain local DEMO data; real Firebase teacher authentication starts in Milestone 2.
