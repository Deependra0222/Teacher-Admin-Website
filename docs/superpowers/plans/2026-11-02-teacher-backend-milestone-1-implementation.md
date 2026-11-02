# SmartAttend Teacher Backend Milestone 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Preserve and locally serve the uploaded teacher portal from a tested Node.js/Express/TypeScript backend foundation without connecting Firebase or changing the site design.

**Architecture:** Keep the existing Android app and `teacher-dashboard/` untouched, import the ZIP as `teacher-website/`, and add a standalone `backend/`. `createApp` owns HTTP behavior and accepts configuration so tests exercise the real Express app without opening ports; `index.ts` only loads environment settings and starts the listener.

**Tech Stack:** Node.js 22 target, TypeScript, Express 5, Firebase Admin dependency reserved for Milestone 2, CORS, dotenv, Node's built-in test runner, Supertest, tsx.

**Spec:** `docs/superpowers/specs/2026-11-02-teacher-backend-milestone-1-design.md`

## Global Constraints

- Do not modify the Android application, `teacher-dashboard/`, production Firestore rules, or Firebase data.
- Preserve the imported website and its `original/` directory byte-for-byte.
- Do not add Firebase credentials or claim Firebase authentication is implemented.
- Keep all sample attendance labeled DEMO / NOT LIVE / not verified.
- Report Node.js 23.11.0 as the actual host runtime while targeting Node.js 22 compatibility.

## Review Focus

- A disallowed browser origin must not receive teacher API data; Task 2 tests a rejected origin.
- An unset optional origin list must still permit same-origin/non-browser requests; Task 2 tests a request without `Origin`.
- Unknown API routes must never fall through to HTML; Task 2 tests JSON 404 behavior.
- Static files must resolve from an explicit directory, not the process working directory; Task 2 tests an isolated fixture directory.
- Oversized/malformed JSON must return safe errors without stack traces; Task 2 tests both cases.

---

### Task 1: Preserve and import the improved teacher website

**Files:**
- Create: `teacher-website/**` from the uploaded ZIP's `smartattend_teacher_upgrade/`
- Preserve: `teacher-dashboard/**`
- Preserve: `app/**`

**Interfaces:**
- Consumes: uploaded ZIP at `C:\Users\mindb\Downloads\SmartAttend_Teacher_Website_Improved_Demo (1).zip`
- Produces: unchanged static site rooted at `teacher-website/index.html`

- [ ] **Step 1: Record source SHA-256 hashes for all 25 ZIP files**

Use PowerShell to hash the extracted inspection copy and retain the result in the execution ledger.

- [ ] **Step 2: Import the package into `teacher-website/`**

Copy the package as supplied. Do not merge it into `teacher-dashboard/` and do not edit any imported file.

- [ ] **Step 3: Verify the import**

Run a source-versus-destination SHA-256 comparison, `node --check` on all four JavaScript files, and a static internal-link check.

Expected: 25 matching file hashes, four successful syntax checks, and no missing local HTML/CSS/JS targets.

- [ ] **Step 4: Commit**

```text
git add teacher-website
git commit -m "feat: preserve improved teacher website"
```

### Task 2: Build the tested Express foundation

**Files:**
- Create: `backend/package.json`
- Create: `backend/package-lock.json`
- Create: `backend/tsconfig.json`
- Create: `backend/.env.example`
- Create: `backend/src/app.ts`
- Create: `backend/src/config/environment.ts`
- Create: `backend/src/index.ts`
- Create: `backend/tests/app.test.ts`
- Modify: `.gitignore`

**Interfaces:**
- Consumes: `teacher-website/` as an explicit static directory
- Produces: `createApp(config: AppConfig): Express`, `loadEnvironment(env: NodeJS.ProcessEnv): ServerConfig`, and compiled `dist/index.js`

- [ ] **Step 1: Add package metadata and test tooling only**

Define `dev`, `build`, `start`, `typecheck`, and `test` scripts; install compatible locked dependencies. Add backend environment/secrets and `node_modules` outputs to `.gitignore`.

- [ ] **Step 2: Write failing HTTP behavior tests**

In `backend/tests/app.test.ts`, assert:

- `/api/health` returns 200 with `{ status: "ok", service: "smartattend-teacher-api" }` plus a string timestamp.
- requests without `Origin` and requests from an allowed origin succeed.
- a disallowed origin receives a safe 403 JSON error.
- unknown `/api/*` routes receive a JSON 404.
- malformed and oversized JSON receive safe 400/413 JSON errors without stack traces.
- `/` serves an explicit static fixture's `index.html`.

- [ ] **Step 3: Run tests and verify RED**

Run: `npm test`

Expected: FAIL because `src/app.ts` and its interfaces do not exist.

- [ ] **Step 4: Implement the minimal application and environment loader**

Implement the interfaces above with Express, `cors`, a 100 KB JSON limit, API-first routing, static serving, and a final safe error handler. `src/index.ts` starts the configured port without initializing Firebase.

- [ ] **Step 5: Run focused tests and verify GREEN**

Run: `npm test`

Expected: all endpoint tests pass.

- [ ] **Step 6: Verify types and production build**

Run: `npm run typecheck` and `npm run build`.

Expected: both exit 0 and `backend/dist/index.js` exists.

- [ ] **Step 7: Commit**

```text
git add .gitignore backend
git commit -m "feat: add teacher API foundation"
```

### Task 3: Document, verify, and update project status

**Files:**
- Create: `backend/README.md`
- Modify: `PROJECT_PLAN.md`
- Modify: `PROJECT_STATUS.md`
- Modify: `TEST_REPORT.md`
- Modify: root `README.md` only if a teacher-backend entry point is otherwise undiscoverable

**Interfaces:**
- Consumes: Task 1 static site and Task 2 npm scripts/server
- Produces: beginner-readable setup/preview instructions and an evidence-backed milestone record

- [ ] **Step 1: Write operating documentation**

Document prerequisites, `npm install`, development/build/test/start commands, URLs, environment variables, architecture, demo limitations, and exact Firebase Console steps deferred to Milestone 2.

- [ ] **Step 2: Run the complete verification set**

Run website integrity checks, `npm test`, `npm run typecheck`, `npm run build`, then start `node dist/index.js` and request `/api/health` plus `/`.

Expected: every automated command exits 0, health is JSON 200, and `/` returns the uploaded teacher dashboard HTML.

- [ ] **Step 3: Update project tracking**

Record the files added, unchanged design guarantee, exact commands and results, Node/Firebase CLI environment findings, deferred Firebase work, and demonstration steps. Do not claim unrun browser, Firebase, or production tests.

- [ ] **Step 4: Commit**

```text
git add backend/README.md README.md PROJECT_PLAN.md PROJECT_STATUS.md TEST_REPORT.md docs/superpowers
git commit -m "docs: record teacher backend milestone one"
```

