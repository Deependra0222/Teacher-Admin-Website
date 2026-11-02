# SmartAttend Teacher Backend Milestone 1 Design

## Intent

Import the user-supplied improved teacher portal without changing its visual design, preserve every previous SmartAttend artifact, and add the first runnable Node.js/Express/TypeScript backend foundation. This milestone is a local development foundation only; it does not authenticate teachers, read production Firestore data, or make attendance official.

## Goals

- Preserve the existing Android application and the older `teacher-dashboard/` prototype unchanged.
- Import the improved ZIP as `teacher-website/`, including the complete `original/` backup.
- Keep the uploaded HTML, CSS, JavaScript, navigation, demo labeling, and appearance byte-for-byte unchanged.
- Add a focused `backend/` using Node.js 22-compatible TypeScript and Express.
- Provide a tested `GET /api/health` endpoint, conservative CORS handling, bounded JSON parsing, predictable 404/error responses, and optional static serving of `teacher-website/`.
- Provide simple commands for development, type checking, tests, production build, and local preview.
- Document the Firebase Web App and trusted Admin SDK work required for Milestone 2 without performing it.

## Non-goals

- No Firebase Web SDK configuration or real teacher sign-in.
- No Firebase Admin initialization, token verification, role authorization, Firestore reads/writes, emulator configuration, rules deployment, or production deployment.
- No Android source changes, BLE work, Cloud Functions, attendance finalization, or production records.
- No React, Next.js, frontend redesign, or migration away from the supplied HTML/CSS/JavaScript.

## Constraints

- The ZIP's `original/` directory must remain unmodified and is verified by SHA-256 comparison against the uploaded archive.
- The older `teacher-dashboard/` remains in place so no prior work is overwritten.
- All demo data remains clearly local/demo and must not be described as verified attendance.
- No credentials, Web config, private keys, or service-account JSON are committed.
- The backend targets Node.js 22. The available development host is Node.js 23.11.0, so verification must report that exact environment rather than claim Node 22 runtime testing.

## Chosen approach

The improved package becomes a separate canonical `teacher-website/` directory and the backend lives at the requested root `backend/`. Express serves both `/api/*` and the static website, which gives a beginner one local command and avoids changing frontend markup. The older `teacher-dashboard/` and Android app remain untouched.

Alternatives rejected:

- Replacing `teacher-dashboard/`: risks deleting or obscuring earlier verified work.
- Converting the frontend to React/Next.js/Vite: directly conflicts with the supplied design-preservation requirement and adds no Milestone 1 value.
- Initializing Firebase Admin immediately: credentials, teacher provisioning, and authorization belong to Milestone 2 and would create a misleading partially secured integration.

## Milestone 1 interfaces

- `GET /api/health` returns HTTP 200 with a small non-sensitive readiness payload.
- Unknown `/api/*` routes return JSON 404 responses.
- Request bodies above the configured JSON limit return a safe client error.
- Browser origins are accepted only when listed in configuration; same-origin and non-browser requests remain supported.
- `/` serves the uploaded teacher dashboard when `teacher-website/` is available.

## Verification

- Compare imported site file hashes to the uploaded archive.
- Run JavaScript syntax checks for supplied scripts.
- Run automated backend endpoint/CORS/static-serving tests.
- Run TypeScript type checking and production compilation.
- Start the compiled server, request `/api/health` and `/`, and record the actual results.

