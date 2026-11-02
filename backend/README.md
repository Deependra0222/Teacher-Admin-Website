# SmartAttend Teacher Backend — Milestone 2

This folder contains the local Node.js 22 / Express / TypeScript backend for the preserved website in `../teacher-website/`. Milestone 2 adds real Firebase Email/Password teacher sign-in, server-side ID-token verification, exact teacher-role/profile authorization, and assignment-scoped access.

It does **not** synchronize attendance, modify official records, deploy anything, or implement ESP32/BLE. Local Demo Mode remains separate and all of its records remain fictional.

## What works

- Website and login: `http://localhost:3000/ERP.html`
- Health: `GET /api/health` (public and non-sensitive)
- Current teacher: `GET /api/me` (valid Firebase teacher ID token required)
- Teacher assignments: `GET /api/teacher/assignments` (valid authorized teacher required)
- No assignments returns `{ "assignments": [] }`; it never grants broad access.
- Firebase runtime modes: `disabled` (default), `emulator`, and `adc` (existing live project with Application Default Credentials).
- Explicit Demo Mode uses only a tab-scoped marker. Real login clears it, failed login never enters it, and logout clears the current session.

## Security flow in plain language

1. The browser sends the email and password directly to Firebase Authentication.
2. Firebase returns a short-lived ID token after successful authentication.
3. The website sends that token only in `Authorization: Bearer <token>` to this backend. It does not put the password or token in application `localStorage`.
4. Firebase Admin verifies the ID token with revocation checking and obtains the UID. The backend does not trust any UID or role from the page.
5. The backend reads exactly `users/{verifiedUid}` and requires the exact lowercase role `teacher`, plus the required profile fields below.
6. Assignment queries are always filtered by that verified UID and `active == true`. A browser query parameter cannot choose another teacher.
7. Only after both Firebase identity and backend authorization succeed does the browser show the authenticated teacher shell. Demo records are hidden in that shell.

Firebase Admin credentials never go to the website. The public Firebase Web App configuration identifies the project but does not grant Admin access.

## Required teacher profile

Create the Authentication account first, copy its Firebase UID, then create this Cloud Firestore document manually:

```text
Document path: users/{Firebase Authentication UID}
```

| Field | Type | Required | Exact value/rule |
|---|---|---:|---|
| `role` | string | Yes | Exactly `teacher` (lowercase) |
| `name` | string | Yes | Non-empty teacher display name |
| `email` | string | Yes | Non-empty email (normally the Auth email) |
| `department` | string | Yes | Non-empty department name |
| `active` | boolean | No | Omit or set `true`; `false` denies access |

Example:

```json
{
  "role": "teacher",
  "name": "Dr Ananya Rao",
  "email": "teacher@college.edu",
  "department": "Computer Science",
  "active": true
}
```

Do not allow a teacher to create or edit their own role from the browser.

## Optional assignment documents

Teacher login succeeds without assignments, but the API returns an empty authorized list. To add an assignment, create a trusted administrator-managed document such as:

```text
Document path: teacherAssignments/CS301_BCA3A_TEACHERUID
```

```json
{
  "teacherUid": "THE_FIREBASE_AUTH_UID",
  "subjectId": "CS301",
  "batchId": "BCA-3A",
  "academicYear": "2026-27",
  "active": true
}
```

`teacherUid`, `subjectId`, `batchId`, and boolean `active: true` are required by the current endpoint. `academicYear` is optional. These documents define scope; they do not create attendance records.

## First-time setup

Use Node.js 22. This milestone was actually verified with Node.js 22.23.4 and npm 10.9.9.

```powershell
cd "D:\Smart Attend\backend"
npm ci
```

## Preview without live Firebase

The default is fail-closed for real login but keeps explicit Demo Mode available:

```powershell
cd "D:\Smart Attend\backend"
npm run dev
```

Open `http://localhost:3000/ERP.html`. The page states that Firebase is not configured; use the explicit Demo Mode button only for fictional data.

## Connect the existing live SmartAttend project locally

1. In Firebase Console, open the existing SmartAttend project (`smartattend-2a5ce`). The Android registration in this workspace confirms that exact project ID.
2. In **Project settings → General → Your apps**, register or select a Web App. Do not enable Hosting.
3. Copy `backend/.env.example` to the ignored `backend/.env`.
4. The verified project ID, Web project ID, storage bucket, and messaging sender ID are already documented. Copy the existing Web App's `apiKey`, `authDomain`, and `appId` exactly, then set `FIREBASE_MODE=adc`. Never substitute the Android App ID; the Web App ID contains `:web:`.
5. Configure Application Default Credentials. Prefer service-account impersonation when IAM permits; otherwise keep a deliberately generated local service-account key outside this workspace and set `GOOGLE_APPLICATION_CREDENTIALS` only for the backend process. Firebase CLI login is not Admin ADC, and plain gcloud end-user ADC may require a custom Desktop OAuth client for Firebase Authentication. See `docs/FIREBASE_TEACHER_LIVE_LOGIN_CHECKLIST.md`.
6. Run `npm run verify:firebase-project`. It must report `LIVE PROJECT CONFIG READY: YES` before the first live test. This checks project/Web configuration only; ADC must also be configured. The command prints only project/mode status, never Web keys, App IDs, tokens, or Admin credentials.
7. Run `npm run dev` and open `http://localhost:3000/ERP.html`.

The Admin identity needs permission to verify Firebase users and read the two authorization collections. If ADC is absent or lacks access, the backend fails closed with a safe authorization-service error.

On the currently inspected Windows machine, Application Default Credentials are absent and `gcloud` is not available on `PATH`. Leave `FIREBASE_MODE=disabled` until a supported ADC source is deliberately provided. Do not put a service-account key in `backend/`, the website, or Git.

No Firebase project, rule, record, or deployment is changed merely by starting the server. The browser performs Authentication sign-in; the backend performs read-only profile and assignment authorization.

## Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `PORT` | `3000` | Local HTTP port. |
| `ALLOWED_ORIGINS` | `http://localhost:3000` | Comma-separated allowed browser origins. |
| `JSON_LIMIT` | `100kb` | Maximum JSON request size. |
| `STATIC_DIR` | `../teacher-website` | Static website path. |
| `FIREBASE_MODE` | `disabled` | `disabled`, `emulator`, or `adc`. |
| `FIREBASE_PROJECT_ID` | none | Required for emulator/ADC modes. |
| `FIREBASE_AUTH_EMULATOR_HOST` | `127.0.0.1:9099` | Emulator mode only; no `http://`. |
| `FIRESTORE_EMULATOR_HOST` | `127.0.0.1:8080` | Emulator mode only; no `http://`. |
| `FIREBASE_WEB_*` | none | Public Firebase Web App options; required in ADC mode. |

The server publishes only the explicit public Web App options through no-cache `/firebase-config.js`. It never serializes Application Default Credentials or Admin secrets.

## Tests and build

```powershell
cd "D:\Smart Attend\backend"
npm test
npm run typecheck
npm run build
npm run verify:firebase-project
```

Firebase Emulator Suite verification uses the Android Studio bundled JDK on this machine:

```powershell
cd "D:\Smart Attend\backend"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
npm run test:emulators
```

The emulator command uses only the demo project ID `demo-smartattend`. Firebase CLI blocks non-emulated service calls for that ID. It loads the unchanged root `firestore.rules`; it does not deploy them.

## Folder responsibilities

```text
backend/
  src/app.ts                              API/static routes and safe errors
  src/auth/teacher-authorization.ts       token/profile/assignment policy
  src/config/environment.ts               validated runtime and Web config
  src/config/firebase-project-audit.ts    Android/Admin/Web ID consistency policy
  src/firebase/admin-runtime.ts            Admin SDK adapters and runtime
  src/firebase/config.ts                   Firebase configuration types
  src/middleware/require-teacher.ts         Bearer-token middleware
  src/tools/verify-firebase-project.ts      redacted local readiness command
  src/index.ts                             startup and shutdown
  tests/app.test.ts                        HTTP foundation tests
  tests/firebase-project-audit.test.ts      cross-client consistency tests
  tests/teacher-authorization.test.ts      authorization unit/API tests
  tests/firebase-runtime.test.ts            runtime/config/adapter tests
  tests/firebase-emulator.integration.test.ts   real emulator integration tests
```

## Dependency advisory note

`npm audit` currently reports 16 moderate, 0 high, and 0 critical advisories. `npm audit --omit=dev` still reports 6 moderate transitive findings under Firebase Admin's Google Cloud packages; the additional findings come from the development-only Firebase CLI tree. The registry's aggregate Firebase CLI suggestion is an inappropriate downgrade to `firebase-tools` 3.18.2 even though the installed current CLI is 15.34.2. No forced downgrade or unsafe `npm audit fix --force` was applied. Runtime tests and the emulator suite pass; reassess these upstream dependencies before deployment.

## Milestone boundary

Milestone 2 stops at authentication and authorized assignment scope. It does not connect students, timetable, sessions, corrections, notices, reports, official attendance, Cloud Functions, Android synchronization, or ESP32/BLE.
