# SmartAttend Teacher Firebase Authentication Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement real Firebase Web Email/Password identity with server-verified teacher role and UID-scoped assignment access while preserving explicit local Demo Mode.

**Architecture:** The browser obtains a Firebase ID token and sends it to Express. A dependency-injected authorization service verifies the token through Firebase Admin, maps the UID-bound Firestore teacher profile, and queries only that teacher's active assignments. Runtime modes keep offline demo, emulator, and production ADC configuration separate.

**Tech Stack:** Node.js 22, TypeScript, Express 5, Firebase Admin 14.5.0, Firebase modular Web SDK 13.1.0, Firebase Emulator Suite, plain HTML/CSS/JavaScript, Node test runner, Supertest.

**Spec:** `docs/superpowers/specs/2026-11-02-teacher-firebase-authentication-design.md`

**Completion:** All five tasks were completed on 2026-11-02. Actual file locations differ slightly from the pre-implementation estimate: Firebase runtime code is split between `config.ts` and `admin-runtime.ts`; shared page protection required only `assets/app.js` plus `ERP.html`; the root `firebase.json` can reference the unchanged root rules safely; and the integration test is `backend/tests/firebase-emulator.integration.test.ts`.

## Global Constraints

- Do not modify `teacher-website/original/`, the Android app, or `firestore.rules`.
- Do not deploy, change billing, write production data, or create production users.
- Do not commit Firebase Web config, Admin credentials, passwords, or tokens.
- Preserve the existing visual design and all explicit DEMO / NOT LIVE labeling.
- Real mode must never render local demo records as official/real data.
- Every authorization decision uses the verified Firebase UID, never browser-provided role or teacher UID.

## Review Focus

- A syntactically valid token for a student UID must still receive 403; Task 1 tests exact role authorization.
- Missing/partial profile fields must fail closed rather than produce a partial teacher; Task 1 tests every required field category.
- Empty assignments must return an empty array and never a broad fallback; Tasks 1 and 4 test this.
- Partial Firebase Web configuration must stop startup with a clear configuration error rather than emit broken JavaScript; Task 2 tests partial configuration.
- Real-mode page startup must not expose demo records while authorization is pending or unavailable; Task 3 tests session decisions and static page guards.

---

### Task 1: Teacher authorization service and protected routes

**Files:**
- Create: `backend/src/auth/teacher-authorization.ts`
- Create: `backend/src/middleware/require-teacher.ts`
- Modify: `backend/src/app.ts`
- Create: `backend/tests/teacher-authorization.test.ts`
- Modify: `backend/package.json`

**Interfaces:**
- Produces: `TeacherAuthorizationService`, `TokenVerifier`, `TeacherDataStore`, `AuthorizedTeacher`, `TeacherAssignment`, and `createRequireTeacher(service)`.
- Consumes later: Firebase Admin adapters in Task 2 implement `TokenVerifier` and `TeacherDataStore`.

- [x] Write failing Express tests for configured/unconfigured auth, missing/malformed headers, invalid/expired/revoked tokens, missing/incomplete/inactive/wrong-role profiles, authorized `/api/me`, empty assignments, UID-scoped assignments, and malformed assignment failure.
- [x] Run `npm test` and confirm RED because authorization modules/routes do not exist.
- [x] Implement the minimal service, middleware, safe errors, `GET /api/me`, and `GET /api/teacher/assignments`.
- [x] Run `npm test`, `npm run typecheck`, and `npm run build`; expect all green.
- [x] Commit as `feat: authorize teacher API requests`.

### Task 2: Firebase runtime configuration and Admin adapters

**Files:**
- Create: `backend/src/firebase/firebase-runtime.ts`
- Modify: `backend/src/config/environment.ts`
- Modify: `backend/src/index.ts`
- Modify: `backend/src/app.ts`
- Modify: `backend/.env.example`
- Modify: `backend/tests/app.test.ts`
- Create: `backend/tests/firebase-runtime.test.ts`

**Interfaces:**
- Consumes: Task 1 `TokenVerifier` and `TeacherDataStore`.
- Produces: `createFirebaseRuntime(config)`, `FirebaseRuntimeConfig`, `FirebaseWebConfig`, `ServerConfig.teacherAuthorization`, and `/firebase-config.js`.

- [x] Write failing tests for disabled/emulator/ADC configuration, required project ID, complete-versus-partial Web config, JavaScript escaping/no-store headers, and adapter mapping of verified UID/profile/assignments.
- [x] Run `npm test` and confirm RED for missing runtime interfaces.
- [x] Implement disabled, emulator, and ADC Admin initialization; server-only Auth/Firestore adapters; runtime public Web config; and fail-closed startup validation.
- [x] Run unit tests, type checking, and build; expect all green.
- [x] Commit as `feat: configure Firebase teacher runtime`.

### Task 3: Website login, session restoration, Demo separation, and logout

**Files:**
- Create: `teacher-website/assets/teacher-auth-core.js`
- Create: `teacher-website/assets/teacher-auth.js`
- Create: `teacher-website/tests/teacher-auth.test.mjs`
- Modify: `teacher-website/assets/app.js`
- Modify: `teacher-website/ERP.html`
- Modify: the eight non-login top-level HTML pages under `teacher-website/`
- Preserve: `teacher-website/original/**`
- Modify: `backend/package.json`

**Interfaces:**
- Consumes: `/firebase-config.js`, `/api/me`, `/api/teacher/assignments`, and Firebase modular Web SDK 13.1.0.
- Produces: explicit `demo`/`firebase` session mode, real login controller, protected-page guard, logout, and safe authorized shell.

- [x] Write failing frontend tests for correct login/authorization, wrong password, student/profile denial, network/backend restart behavior, explicit Demo Mode, no automatic demo fallback, logout, and restoration.
- [x] Run the frontend test command and confirm RED because the controller does not exist.
- [x] Implement the pure controller, Firebase Web adapter, login form integration, session-only persistence, page guard, demo-data suppression in real mode, and logout through the existing navigation styling.
- [x] Add runtime config/auth scripts to improved pages without editing CSS layout or `original/`.
- [x] Run frontend/backend tests, JavaScript syntax checks, browser checks, and original-folder hash verification.
- [x] Commit as `feat: connect secure teacher login`.

### Task 4: Firebase Emulator integration verification

**Files:**
- Create: `backend/firebase.json`
- Create: `backend/tests/firebase-emulator.test.ts`
- Modify: `backend/package.json`
- Modify: `backend/package-lock.json`

**Interfaces:**
- Consumes: Task 2 Admin runtime and Task 1 protected Express endpoints.
- Produces: `npm run test:emulator` using disposable `demo-smartattend` Auth/Firestore data only.

- [x] Add local Firebase CLI tooling and a deny-by-default reference to the existing unchanged rules file.
- [x] Write the emulator test before wiring its script; cover correct teacher credentials/token, incorrect password, student role, missing profile, invalid token, empty assignments, assigned results, and backend reinitialization.
- [x] Run the emulator test and confirm the intended RED condition before completing any missing adapter behavior.
- [x] Make only the minimal integration corrections, then rerun until green.
- [x] Confirm no production project ID, credential, or record is used.
- [x] Commit as `test: verify Firebase teacher authentication`.

### Task 5: Documentation, regression verification, and milestone status

**Files:**
- Modify: `backend/README.md`
- Modify: `README.md`
- Modify: `PROJECT_PLAN.md`
- Modify: `PROJECT_STATUS.md`
- Modify: `TEST_REPORT.md`
- Add/modify only milestone-specific documents under `docs/superpowers/`

**Interfaces:**
- Consumes: all completed runtime commands, endpoints, schemas, and observed test results.
- Produces: beginner setup, first-teacher provisioning, Demo/real explanation, exact results, and unresolved advisories.

- [x] Document Firebase Console Web App registration, `.env` values, emulator/ADC startup, exact teacher profile and assignment documents, local commands, logout/session behavior, and safe limitations.
- [x] Run dependency installation, unit tests, emulator tests, typecheck, build, localhost browser checks, Android/older-teacher regressions, and protected-file/rules diffs.
- [x] Update status/test documents with only observed results, including the live-login blocker and dependency advisories.
- [x] Commit as `docs: record teacher Firebase authentication milestone`.

