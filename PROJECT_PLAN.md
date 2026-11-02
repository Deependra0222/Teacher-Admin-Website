# SmartAttend Project Plan

## Product architecture and delivery order

SmartAttend has two clients that will eventually share one Firebase backend:

1. **Student Android application** — native Kotlin/Jetpack Compose app. This is the completed current milestone.
2. **Teacher Admin Website** — the improved portal, Express foundation, Firebase teacher authentication, server-side role/profile authorization, and UID-scoped assignment endpoint are preserved and verified with local emulators. The older `teacher-dashboard/` demo remains untouched.
3. **Shared Firebase backend** — Authentication, secured Firestore records, Cloud Functions, audit logs, and synchronization for both clients.
4. **ESP32/BLE integration** — Android scanning, measured RSSI calibration, real-device duration/loss tests, and secure server-side attendance finalization.

A website is not required for students. Firebase and ESP32 work must not be represented as complete until credentials, deployment, and hardware tests exist.

## Milestone 1 — Launchable offline Android MVP

Status: **Completed.**

- Android Studio project `com.smartattend.app`, minimum SDK 26, Compose/Material 3.
- Demo login and the original Home, Attendance, History, Timetable, and Profile experience.
- Existing duration attendance engine, session validation, result persistence, and demo labels.

## Milestone 2 — Complete offline student application

Status: **Completed and verified on an API 36 emulator.**

- Incremental single-module `core` / `domain` / `data` / `ui` architecture.
- Room 2.8.5 for structured records and pending operations.
- Preferences DataStore 1.2.1 for installation ID, seed version, notifications, reminder lead time, and theme.
- Repository interfaces keep a future Firebase adapter separate from screens.
- Navigation Compose with five primary destinations and all approved nested workflows.
- Android launch splash plus the original offline demo entry; dashboard/live simulator; subject attendance/detail; filterable history; shortage calculator; calendar/agenda schedule; notices; correction requests; device registration; settings; profile; privacy/permission disclosure; and offline/BLE diagnostics.
- Persistent offline mutations and explicit backend-unconfigured/pending states.
- Platform-independent BLE decision engine using simulated signals, allowlists, median/EMA filtering, dwell, hysteresis, and signal-loss timeout.

## Milestone 2A — Firebase student authentication

Status: **Implemented and automated/emulator failure-path verified; real owner account success pending owner-entered credentials/profile.**

- Separate real Firebase and explicit local Demo Mode entry points; no automatic demo fallback.
- Firebase Email/Password sign-in and password reset behind an authentication repository.
- UID-bound, read-only `users/{uid}` authorization requiring `role == "student"` and a complete student profile.
- Auth-state restoration with Firebase precedence over stale demo state.
- DataStore persists only the explicit Demo Mode marker; passwords, roles, tokens, and Firebase profiles are never stored there.
- Missing/incomplete/wrong-role/unreadable profiles remain outside navigation and sign out locally.
- Firebase identities are displayed separately while every unsynchronized attendance record remains visibly DEMO / NOT VERIFIED BY BLE.
- Firestore rules are unchanged and the Android authentication adapter performs no Firestore write.

## Milestone 3 — Teacher Admin Website

Status: **Milestone 2 Firebase teacher authentication completed and emulator/browser verified; live-project configuration and owner test pending.**

- Preserved the existing `teacher-dashboard/` prototype unchanged.
- Imported the 25-file improved website into `teacher-website/`, retained its premium design and navigation, and kept the protected `original/` backup byte-for-byte unchanged.
- Added a Node.js 22-compatible, TypeScript/Express 5 foundation in `backend/` with static serving, health endpoint, strict CORS allowlist, bounded JSON parsing, safe API errors, tests, type checking, and production compilation.
- Added Firebase Web Email/Password login, password reset, session restoration, logout, explicit separate Demo Mode, and protected-page guards.
- Added Admin ID-token verification with revocation checking, exact `users/{uid}` teacher profile authorization, and active assignment reads bound to the verified UID.
- Added disabled/emulator/ADC runtime modes; the browser receives only public Web configuration while Admin credentials stay server-side.
- Verified positive and negative flows through Firebase Auth/Firestore emulators and a real browser. The unchanged production rules were not deployed.
- Next manual boundary: existing-project Web App config, local ADC, and an administrator-provisioned teacher Auth/profile record for live owner verification.
- Later: connect timetable/session control, live evidence monitoring, corrections, announcements, analytics, reports, profile, and settings only through secured assignment-scoped APIs.

## Firebase Android SDK foundation — Completed

- Google Services Gradle plugin `4.5.0` processes the local, Git-ignored `app/google-services.json` for `com.smartattend.app`.
- Firebase BoM `34.19.0` supplies the non-KTX Authentication and Cloud Firestore main modules.
- The default Firebase app plus Auth and Firestore client objects initialize on an API 36 emulator.
- The later authentication milestone now uses Auth and a single read-only `users/{uid}` lookup. No synchronized app-data write, Security Rules change, Cloud Function, user creation, or application data was added.

## Milestone 4 — Shared Firebase backend

Status: **Student and teacher authentication/profile authorization implemented; teacher flow emulator/rules verified; live teacher and student owner-account success plus synchronized app data remain pending.**

- Verify the existing student account with owner-entered credentials and an administrator-created `users/{uid}` profile.
- Owner-verify the teacher flow with the existing Firebase project, ignored Web config, ADC, and a trusted administrator-created teacher profile.
- Extend student/teacher authorization only through trusted backend/admin workflows.
- Implement secured Firestore collections and verified Security Rules.
- Implement Cloud Functions for idempotent evidence processing, teacher actions, device registration, and final attendance.
- Add offline synchronization adapters behind existing repositories.
- Test with Firebase Emulator Suite before any deployment.

## Milestone 5 — ESP32 and real BLE

Status: **Not started; hardware required.**

- Add Android runtime permissions and a scanner adapter that produces the existing `BleSample` model.
- Calibrate actual ESP32 nodes and phone models in classrooms, corridors, doors, and obstructed seating.
- Verify dwell, RSSI thresholds, multi-node rules, loss timeout, reconnects, battery/background limits, and adversarial conditions.
- Finalize attendance only on the server after enrollment, session, device, evidence, and teacher checks.

## Milestone 6 — Production readiness

Status: **Future.**

- Physical-device/accessibility matrix, production observability, privacy/legal review, release signing, backup/retention policy, performance, and store distribution.
