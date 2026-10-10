# SmartAttend Project Plan

## Product architecture and delivery order

SmartAttend has two clients that will eventually share one Firebase backend:

1. **Student Android application** — native Kotlin/Jetpack Compose app. This is the completed current milestone.
2. **Teacher Admin Website** — the next major milestone, after student-app review. The existing `teacher-dashboard/` demo is preserved for possible reuse.
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
- Dashboard, subject attendance/detail, filterable history, shortage calculator, calendar/agenda schedule, notices, correction requests, device registration, settings, profile, and demo-boundary screens.
- Persistent offline mutations and explicit backend-unconfigured/pending states.
- Platform-independent BLE decision engine using simulated signals, allowlists, median/EMA filtering, dwell, hysteresis, and signal-loss timeout.

## Milestone 3 — Teacher Admin Website

Status: **Next, pending user review of the Student Android App.**

- Review the existing `teacher-dashboard/` prototype before deciding what can be reused.
- Build the professional teacher workflow for timetable/session control, live evidence monitoring, corrections, announcements, analytics, reports, profile, and settings.
- Keep the site in demo/local mode until the shared Firebase contract is implemented.

## Milestone 4 — Shared Firebase backend

Status: **Not started; credentials and product decisions required.**

- Configure Firebase Authentication and student/teacher authorization.
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
