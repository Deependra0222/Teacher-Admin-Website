# SmartAttend Real Teacher Login Readiness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminate the Firebase project-ID drift and provide a tested, secret-safe readiness check for the first live teacher login.

**Architecture:** Keep Android configuration read-only and use it as the local audit reference. Existing backend startup continues to validate Admin versus Web IDs; a separate CLI compares both against Android without coupling deployed server runtime to the Android file.

**Tech Stack:** Node.js 22, TypeScript, Firebase Admin, Firebase modular Web SDK, Node test runner, Firebase Emulator Suite.

**Spec:** `docs/superpowers/specs/2026-11-02-real-teacher-login-readiness-design.md`

## Global Constraints

- Do not edit Android configuration/source, `teacher-website/original/`, `teacher-dashboard/`, or `firestore.rules`.
- Do not create accounts/data, deploy, change billing, or run a production login.
- Do not print or commit API keys, App IDs, tokens, passwords, or Admin credentials.
- Do not guess Web App values.

## Review Focus

- A backend/Web project different from Android must fail the audit with no secret output.
- Disabled mode must remain usable before live values exist.
- ADC absence must be reported as a setup blocker, not replaced with embedded credentials.
- Profile lookup must remain direct `users/{verifiedUid}` rather than an email query.
- Existing Demo Mode and logout behavior must remain unchanged.

---

### Task 1: Cross-client Firebase project audit

**Files:**
- Create: `backend/src/config/firebase-project-audit.ts`
- Create: `backend/src/tools/verify-firebase-project.ts`
- Create: `backend/tests/firebase-project-audit.test.ts`
- Modify: `backend/package.json`

**Interfaces:**
- Produces: `auditFirebaseProject(input)` and `npm run verify:firebase-project`.
- Consumes: Android `google-services.json` project metadata and `loadEnvironment(process.env)`.

- [x] Write failing tests for matching, disabled, isolated emulator mode, Admin mismatch, Web mismatch, missing Android project, and secret-free report output.
- [x] Run the focused test and confirm RED because the audit module does not exist.
- [x] Implement the pure audit and CLI with redacted status-only output.
- [x] Run the focused and complete backend suites.

### Task 2: Requested authentication coverage audit

**Files:**
- Modify: `backend/tests/teacher-authorization.test.ts`
- Modify: `teacher-website/tests/teacher-auth.test.mjs`

**Interfaces:**
- Consumes existing `TeacherAuthorizationService` and session controller.

- [x] Add characterization tests for token-verifier connection failure and logout failure handling where coverage is missing.
- [x] Run backend, website, and emulator suites and record exact counts.

### Task 3: Correct configuration examples and live-test checklist

**Files:**
- Modify: `backend/.env.example`
- Modify: `backend/README.md`
- Modify: stale Milestone 2 tests/docs that name `smartattend-e9a35`
- Modify: `PROJECT_STATUS.md`
- Modify: `TEST_REPORT.md`

- [x] Replace only stale teacher-side active examples/test fixtures with verified `smartattend-2a5ce`; retain explicit historical correction notes.
- [x] Document exact supplied-versus-required values and current ADC/gcloud blocker.
- [x] Document the first live login and diagnostic checklist without running production operations.

### Task 4: Final regression and boundary verification

**Files:**
- Preserve all excluded paths.

- [x] Run project audit in disabled mode, backend tests/typecheck/build, website tests/syntax, emulator suite, Android JVM regression, and old teacher tests.
- [x] Verify no diff in Android, original backup, old teacher dashboard, or Firestore rules; run secret-pattern and whitespace checks.
- [x] Leave the reviewed changes uncommitted for owner review and stop for owner confirmation.
