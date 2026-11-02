# SmartAttend Verification Report

Updated: 2026-11-02 (Asia/Calcutta)

## Teacher Website / Firebase Authentication Milestone 2

### Outcome

Milestone 2 is implemented and verified against Firebase Authentication and Cloud Firestore emulators. The website now performs Firebase Email/Password sign-in and the Express backend independently verifies the ID token, exact teacher role/profile, and UID-scoped active assignments. No production deployment, production rule change, official attendance write, Android change, or ESP32/BLE work occurred.

Live login to the existing `smartattend-e9a35` project was **not run** because this workspace does not contain the Firebase Web App configuration, local Application Default Credentials, or an administrator-provisioned teacher test account/profile. This is the remaining external configuration boundary, not an emulator/build failure.

### Actual automated results

| Command/check | Result | Evidence |
|---|---|---|
| `backend: npm ci` | PASS WITH ADVISORIES | Clean lockfile install added 704 packages; 16 moderate, 0 high, 0 critical advisories. |
| `backend: npm test` | PASS | 44 assertions across 6 suites; 0 failures. |
| `backend: npm run typecheck` | PASS | Strict TypeScript check exited 0. |
| `backend: npm run build` | PASS | TypeScript compilation exited 0 and refreshed `backend/dist/`. |
| `backend: npm run test:emulators` | PASS | 12 assertions; 0 failures; Auth and Firestore emulators shut down cleanly. |
| `teacher-website: npm test` | PASS | 10 assertions across 3 suites; 0 failures. |
| Website syntax | PASS | `node --check` passed for `assets/app.js`, `assets/teacher-auth.js`, and `assets/teacher-auth-core.js`. |
| Compiled disabled-mode server | PASS | `/api/health`, `/firebase-config.js`, and `/ERP.html` returned 200; config returned `enabled:false` with `Cache-Control: no-store`; protected `/api/me` failed closed with 503 `FIREBASE_NOT_CONFIGURED`. |
| Protected original backup | PASS | All 9 `teacher-website/original/` SHA-256 values still match the Milestone 1 baseline. |
| Protected project boundaries | PASS | Git diff against `codex/teacher-backend-m1` is empty for `app/`, `teacher-dashboard/`, `teacher-website/original/`, and `firestore.rules`. |
| Website local links | PASS | Static scan found 0 missing top-level local HTML/CSS/JavaScript targets. |
| Secret-pattern scan | PASS | No private-key/service-account/test-password pattern was found outside ignored/generated dependencies and Android's ignored config. |
| Older `teacher-dashboard` regression | PASS | 3 tests; 0 failures. |
| Android JVM regression | PASS | `build-smartattend.bat testDebugUnitTest --no-daemon` completed successfully; 27 tasks were up-to-date. |
| Whitespace check | PASS | `git diff --check` reported no whitespace errors before documentation updates. |

Backend unit tests cover safe Firebase runtime configuration, public Web config serialization, Admin adapters, missing/malformed Bearer headers, invalid/expired/revoked tokens, missing/incomplete/inactive/wrong-role profiles, authorized `/api/me`, empty assignments, verified-UID assignment scoping, malformed authorization data, and upstream service failure.

Emulator tests use only project ID `demo-smartattend` and cover:

- correct Firebase teacher account plus complete `users/{uid}` profile;
- incorrect password at Firebase Authentication;
- student denial;
- missing Firestore profile;
- incorrectly capitalized role denial;
- missing and invalid ID tokens;
- authorized empty assignment result;
- only active assignments for the verified teacher UID, ignoring a query-supplied UID;
- current `firestore.rules` denying unauthenticated and cross-student profile reads while permitting the student's own profile read;
- successful authorization after the backend Firebase runtime is closed and recreated.

### Actual interactive browser results

The website was served at `http://localhost:3000` and connected to local Auth/Firestore emulators. The browser test verified:

- disabled mode clearly disables real sign-in while preserving the explicit Demo Mode action;
- explicit Demo Mode opens the existing labeled local dashboard;
- Exit Demo Mode clears that tab session and returns to login;
- correct emulator teacher credentials reach the authenticated dashboard;
- the authenticated shell shows the authorized teacher name/department and one UID-scoped assignment while hiding demo student rows;
- a page reload restores the Firebase teacher session;
- logout returns to login, and opening a protected page afterward redirects back with an authentication-required message;
- incorrect password stays on login and shows `The email or password is incorrect.`;
- a student Auth account is denied with `This account is not authorized for the teacher portal.`;
- an Auth account without `users/{uid}` is denied with `No authorized teacher profile exists for this account.`;
- the password-reset action succeeds against the Auth emulator;
- no automatic Demo Mode fallback occurred in any failed real-login path.

The first restoration run exposed a real ordering defect: `setPersistence()` initialized an existing Auth session before `connectAuthEmulator()`. Moving the emulator connection immediately after `getAuth()` fixed the issue; the full browser restoration and emulator suites then passed.

### Authorization contract verified

Teacher profile path: `users/{Firebase Authentication UID}`.

Required non-empty strings: `role` (exactly `teacher`), `name`, `email`, and `department`. Optional `active` must be a boolean when present, and `false` denies access.

Assignment path: `teacherAssignments/{assignmentId}`. Active results require string `teacherUid` matching the verified UID, non-empty string `subjectId`, non-empty string `batchId`, and boolean `active: true`; optional `academicYear` must be a non-empty string when present.

### Files changed in Milestone 2

Modified:

- `.gitignore`
- `PROJECT_PLAN.md`
- `PROJECT_STATUS.md`
- `TEST_REPORT.md`
- `backend/.env.example`
- `backend/README.md`
- `backend/package-lock.json`
- `backend/package.json`
- `backend/src/app.ts`
- `backend/src/config/environment.ts`
- `backend/src/index.ts`
- `teacher-website/ERP.html`
- `teacher-website/README.md`
- `teacher-website/assets/app.js`
- `teacher-website/docs/BACKEND_ARCHITECTURE.md`
- `teacher-website/docs/WEBSITE_AUDIT.md`

New:

- `backend/src/auth/teacher-authorization.ts`
- `backend/src/firebase/admin-runtime.ts`
- `backend/src/firebase/config.ts`
- `backend/src/middleware/require-teacher.ts`
- `backend/tests/firebase-emulator.integration.test.ts`
- `backend/tests/firebase-runtime.test.ts`
- `backend/tests/teacher-authorization.test.ts`
- `firebase.json`
- `teacher-website/assets/teacher-auth-core.js`
- `teacher-website/assets/teacher-auth.js`
- `teacher-website/package.json`
- `teacher-website/tests/teacher-auth.test.mjs`
- `docs/superpowers/specs/2026-11-02-teacher-firebase-authentication-design.md`
- `docs/superpowers/plans/2026-11-02-teacher-firebase-authentication-implementation.md`

No file in `teacher-website/original/`, `teacher-dashboard/`, the Android application, or `firestore.rules` was changed.

### Dependency audit

Final `npm audit --json`: 16 moderate, 0 high, and 0 critical advisories. `npm audit --omit=dev --json` reports 6 moderate transitive findings under Firebase Admin's Google Cloud dependencies; the additional findings come from Firebase CLI and its emulator/deployment utilities. npm's aggregate CLI suggestion is an inappropriate downgrade to Firebase CLI 3.18.2 from the installed current 15.34.2. No forced downgrade, override, or `npm audit fix --force` was applied.

### Not run / intentionally stopped

- Live teacher sign-in to the existing Firebase project.
- Production Firestore rules modification or deployment.
- Production Hosting/backend deployment or billing upgrade.
- Real student/timetable/session/correction/notice/report synchronization.
- Official attendance record creation or modification.
- Android changes, Android-to-teacher synchronization, ESP32, or BLE hardware testing.

## Teacher Website / Node.js Backend Milestone 1

| Check | Result | Evidence |
|---|---|---|
| Uploaded website preservation | PASS | 25/25 working-tree SHA-256 hashes and 25/25 committed Git-blob hashes match the extracted ZIP. |
| Protected `original/` backup | PASS | 9/9 files remain byte-for-byte; Git text conversion is disabled only for this backup path. |
| Website JavaScript syntax | PASS | `node --check` passed for `assets/app.js`, `assets/demo-data.js`, `original/script.js`, and `original/script-1.js`. |
| Website local links | PASS | Static scan found 0 missing local HTML/CSS/JS link targets. |
| Clean dependency installation | PASS WITH ADVISORIES | `npm ci` installed 319 packages; audit reports 6 moderate, 0 high, 0 critical transitive advisories under Firebase Admin dependencies. |
| Backend automated tests | PASS | `npm test`: 10 tests across 2 suites, 0 failures. |
| TypeScript type check | PASS | `npm run typecheck` exited 0. |
| Production compile | PASS | `npm run build` exited 0 and produced `backend/dist/index.js`. |
| Compiled server health | PASS | `GET http://localhost:3000/api/health` returned HTTP 200 and `service: smartattend-teacher-api`. |
| Compiled dashboard serving | PASS | `/` returned HTTP 200 with the uploaded Teacher Dashboard title. |
| Compiled login-page serving | PASS | `/ERP.html` returned HTTP 200 with the existing `teacherLogin` form. |
| Older teacher prototype regression | PASS | `node --test dashboard-model.test.js`: 3 tests, 0 failures. |
| Android JVM regression | PASS | `build-smartattend.bat testDebugUnitTest --no-daemon`: build successful using bundled JDK 17. |

Backend tests cover health response, no-Origin requests, allowed and denied browser origins, API JSON 404 behavior, malformed JSON, oversized JSON, explicit static-directory serving, environment defaults/overrides, and invalid port rejection.

Actual host runtime was Node.js 23.11.0 with npm 11.4.2. The project targets Node.js 22, but Node.js 22 runtime execution was not performed. Firebase CLI, Firebase Emulator Suite, Firebase Web Auth, Firebase Admin initialization, Firestore, real teacher credentials, interactive browser testing, and deployment were not run. All teacher attendance content remains local DEMO / NOT LIVE data.

## Student Android and Firebase authentication verification (2026-10-11)

## Automated verification

| Check | Result | Evidence |
|---|---|---|
| Focused BLE unit suite after regression fix | PASS | 7 tests; the signal-loss deadline now ignores cached timer-only observations. |
| Complete JVM suite | PASS | 79 tests across 17 suites; 0 failures. |
| Requested build/lint command | PASS | `build-smartattend.bat testDebugUnitTest assembleDebug lintDebug --no-daemon` invoked `gradlew.bat` with project JDK 17 and completed successfully. |
| Google Services resource processing | PASS | `processDebugGoogleServices` completed during the build for package `com.smartattend.app`. |
| Android lint | PASS WITH RECOMMENDATIONS | `lintDebug`: 0 errors, 9 dependency/target-version recommendations. |
| Instrumentation suite | PASS | `connectedDebugAndroidTest`: 9 tests, 0 failures on Pixel 7 API 36, including Firebase client initialization and 6 login UI tests. |
| Debug APK install | PASS | `adb install -r` installed the generated APK on Pixel 7 API 36. |
| Cold app launch | PASS | `com.smartattend.app/.MainActivity` launched into the separate Firebase/Demo choice screen. |
| Controlled Firebase Auth failure | PASS | A deliberately nonexistent non-secret account produced `Incorrect email or password.` and did not enter Demo Mode. |
| APK output | PASS | `app/build/outputs/apk/debug/app-debug.apk`, 26,930,890 bytes. |
| APK integrity | RECORDED | SHA-256 `61BB09E4CA5F97BEE77C3CBCA310897F3779F5E982C121AF519E91F10412337C`. |

The Android project uses Gradle 8.12, Android Gradle Plugin 8.9.1, Kotlin/Compose plugin 2.2.0, Room 2.8.5, DataStore 1.2.1, Google Services plugin 4.5.0, Firebase BoM 34.19.0, Authentication 24.2.0, Firestore 26.6.0, compile/target SDK 35, minimum SDK 26, and JDK 17.

## JVM behavior covered

- Existing Present/Review/Absent boundaries, late handling, clamping, re-entry, duplicate events, batch/session validation, expired evidence, and persisted-session policy.
- Login validation, session display states, and history formatting.
- Shortage projections including valid recovery/miss counts, invalid counts/targets, exact targets, and unreachable 100% cases.
- Correction validation: minimum reason, changed status, duplicate pending request, and valid submission.
- Sync-state reduction for demo, backend-unconfigured, offline/pending, syncing, synchronized, and failure modes.
- BLE allowlist, distinct-beacon threshold, dwell, median/EMA outlier rejection, hysteresis, staying, signal loss, unknown beacon, wrong session, invalid RSSI, and backward timestamp rejection.
- Deterministic seed IDs, demo provenance, distinct dates/types, and seeded correction examples.
- Nested route encoding/decoding and malformed-route rejection.
- Distinct, valid top-level routes for Privacy & Permissions and Offline & BLE Diagnostics.
- Firebase credential validation and stable user-facing Auth/Firestore error categories.
- UID-bound profile mapping, required fields, exact student role, missing/incomplete profiles, and rejection of client-controlled document IDs.
- Firebase-versus-demo session precedence, explicit demo entry, no failure fallback, logout, reset validation, and profile removal after logout.
- Server-only profile authorization policy plus password-reset responses that do not reveal whether an account exists.

## Instrumentation behavior covered

- In-memory Room inserts and observable flows.
- Persisted notice read mutation.
- Open-correction lookup.
- Transactional database clearing.
- Idempotent repository initialization and non-duplicated seed.
- Duplicate open correction rejection and pending-operation creation.
- Transactional demo reset.
- Packaged Firebase configuration initializes the default Firebase app and constructs Authentication and Firestore client instances without performing a service operation.
- Login Compose UI starts with empty credential fields; Firebase, Demo Mode, and password-reset actions invoke only their own callbacks; busy state disables both entry actions; errors stay on login.

## Emulator checks actually performed

The `Pixel_7` AVD booted on Android API 36. The fresh APK installed and launched. UI hierarchy checks confirmed:

- Empty Firebase credential fields plus distinct **Sign in with Firebase** and **Continue in Demo Mode** buttons.
- Explicit Demo Mode entry and Dashboard with `DEMO - NOT VERIFIED BY BLE`.
- Demo Mode cold-start restoration only after explicit entry.
- Profile showed `DEMO LOCAL IDENTITY`; **Exit Demo Mode** returned to login.
- Local invalid email rendered `Enter a valid email address.` without navigation.
- A controlled Firebase Auth request for an intentionally nonexistent account rendered `Incorrect email or password.` and never entered Demo Mode.
- A force-stop/relaunch after failed Firebase login returned to `Student sign in`, proving no old demo session override.
- Attendance, Schedule, Inbox, and More primary destinations.
- Shortage calculator, subject detail, filtered history, correction form, schedule detail, and notice detail.
- Profile, correction list, device registration, settings, and About Demo routes.
- Privacy & Permissions and Offline & BLE Diagnostics routes with the expected offline-demo and simulated-BLE boundaries. Firebase contributes network permissions, but app-level Auth/Firestore workflows remain disconnected.
- Device registration action rendered `PENDING SERVER VERIFICATION`.
- Demo labels and the disconnected-sync state were visible; a Firebase profile would still label attendance as demo/not synchronized.

The `Medium_Phone_API_36` AVD remained `offline` in ADB and was not used. An earlier Pixel 7 restart failed while `C:` had about 0.04 GB free; that condition was later resolved. With about 27.56 GB free, Pixel 7 API 36 booted again, the final APK installed, both instrumentation tests passed, and the new routes passed a UI-hierarchy smoke check. Physical process-death/OEM behavior remains for phone testing.

## Not run / not verified

| Scenario | Result | Reason |
|---|---|---|
| Physical Android phone | NOT RUN | No phone was attached. |
| Successful registered Firebase login | NOT RUN | The user's real password was not supplied or recorded. Success also requires a complete administrator-created `users/{uid}` profile. |
| Incorrect password for the registered account | NOT RUN | Requires the project owner to enter controlled credentials directly in the emulator; the safe nonexistent-account failure path was verified instead. |
| Live missing/wrong-role profile | NOT RUN | Requires controlled valid Auth users and Firestore profile documents; pure authorization cases pass in JVM tests. |
| Password-reset email delivery | NOT RUN | Callback, local validation, and error handling are tested; no real account email action was triggered. |
| Firebase sync/rules/functions | NOT RUN | No synchronized app-data flow, Rules emulator run/deployment, rules change, or Cloud Function was added. |
| Real Android notifications | NOT RUN | This milestone stores preferences and inbox records only. |
| Real BLE scan/service | NOT RUN | No Bluetooth permission or scanner was added by design. |
| ESP32 RSSI/dwell/loss calibration | NOT RUN | No hardware was available. |
| Server-side attendance finalization | NOT RUN | Cloud Functions/backend are not configured. |
| Production security/release signing | NOT RUN | APK is a debug artifact. |

## Known lint recommendations

Lint reports only target/dependency update recommendations: target SDK 35 and several intentionally pinned AndroidX test/runtime versions. There are no lint errors. These warnings should be reconsidered with the next verified toolchain upgrade rather than changed without regression testing.
