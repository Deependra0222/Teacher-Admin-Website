# SmartAttend Student Application Milestone Design

## Purpose and success criteria

This milestone turns the existing SmartAttend offline demonstration into a complete, navigable student application foundation while preserving its verified attendance engine and explicit demo boundaries. The student can use every requested workflow without Firebase credentials or classroom hardware, but simulated evidence must never appear as teacher-finalized or BLE-verified attendance.

The milestone succeeds when the Android project builds as `com.smartattend.app` for API 26+, all student destinations and actions work with persistent demo data, offline state is visible, the BLE decision engine is exercised through simulated samples, automated tests pass, and the debug APK completes an emulator smoke test. Firebase writes, real Android BLE scanning, ESP32 firmware, and hardware verification remain later milestones.

## Delivery order

1. Complete and review the student Android application milestone described here.
2. Build the teacher administration website after the user reviews the student app.
3. Connect both clients through Firebase Authentication, Firestore, Cloud Functions, and the shared authorization contract.
4. Integrate ESP32 BLE scanning, calibrate with real devices, and run hardware tests before enabling verified attendance.

The existing `teacher-dashboard/` prototype is preserved but not expanded during this milestone.

## Architecture

The project remains a single Android `app` module, organized into focused `core`, `domain`, `data`, and `ui` packages. Navigation Compose replaces the manual destination switch. A `StudentRepository` contract supplies student-facing data, while repository implementations isolate demo data, Room persistence, and the future Firebase adapter from composables.

Room stores structured offline records, notification-read state, device-registration records, and pending operations. DataStore stores small preferences such as demo-session state, selected settings, the local installation identifier, and authentication-mode flags. Existing SharedPreferences data is migrated or safely ignored without crashing. ViewModels expose immutable UI state and call use-case/domain classes; composables do not calculate attendance, shortage, correction validation, synchronization policy, or BLE presence.

The first implementation keeps a demo-backed repository as the authoritative source and mirrors mutable records locally. The future Firebase repository will implement the same contracts. A synchronization coordinator classifies local operations as pending, syncing, synced, or failed without pretending that an unavailable backend accepted them.

## Navigation and screens

The authenticated app uses five primary destinations and nested routes:

- **Dashboard:** greeting, current/next lecture, overall attendance, shortage alerts, unread notices, offline/sync state, and a clearly labeled simulated-presence card.
- **Attendance:** subject-wise cards, percentage/progress, attended/total sessions, shortage warnings, subject detail/history, and access to the shortage calculator.
- **Schedule:** agenda and calendar modes, date selection, lecture/lab details, active/upcoming state, room, subject, and instructor demo data.
- **Inbox:** announcements, assignments, deadlines, notification type/filter, read/unread actions, and empty state.
- **More:** profile, correction requests, registered-device status, settings, demo information, and logout.

Nested routes cover subject detail, shortage calculator, attendance history, correction request list/form/detail, device registration, notification detail, schedule detail, and settings. Back navigation returns to the originating screen and bottom-navigation state remains stable.

## Student workflows

### Subject attendance and shortage calculator

Each subject record includes attended classes, total classes, required percentage, and teacher-finalized/demo source state. The calculator answers both questions without mutating official records:

- How many consecutive future classes may be missed while remaining at or above the target percentage?
- How many consecutive future classes must be attended to reach the target percentage?

Inputs reject negative totals, attended counts greater than totals, and targets outside 1-100. Results explain the calculation and remain labeled as projections.

### Attendance history

History supports subject and status filters, chronological grouping, late indication, source/verification labeling, and correction-request entry. Demo history is visibly marked. Student clients cannot directly edit finalized attendance.

### Correction requests

A student selects an attendance record, chooses a requested status, supplies a reason of at least ten non-whitespace characters, and submits a local pending request. Duplicate open requests for the same attendance record are rejected. Requests show pending, approved, rejected, or sync-failed status with timestamps and an optional teacher response. Demo approval/rejection is not simulated in the student client; only seeded examples may demonstrate those states.

### Notifications and announcements

Notices cover announcements, assignments, attendance alerts, and correction updates. Students can filter, open, mark read/unread, and mark all read. Assignment notices include a deadline and overdue/due-soon presentation. Local changes persist offline.

### Device registration

The screen shows the current app-installation identifier, device label, registration state, last check, and an explanation of future server verification. Demo registration stores a local `PENDING_SERVER_VERIFICATION` record only. It must not display “trusted”, “secure”, or “verified”. Production registration later exchanges an installation-scoped public key and platform-attestation token with a Cloud Function; no private key or service credential leaves protected device storage.

### Settings and offline handling

Settings include notification preferences, reminder lead time, theme choice, demo-data reset, and data-source information. A persistent connectivity/synchronization indicator distinguishes demo-only, offline with pending changes, online but backend unconfigured, syncing, synchronized, and failed states. Pending corrections and read-state operations survive process death and retry only when a real sync adapter is configured.

## BLE presence decision engine

This milestone implements only the platform-independent decision engine and a simulator. It does not request Bluetooth permissions, scan nearby hardware, or emit verified attendance.

The engine consumes timestamped samples containing session ID, beacon ID, RSSI, and whether the sample is simulated. Configuration supplies an allowlisted beacon set, rolling-window size, entry threshold, exit threshold, minimum verification dwell, and signal-loss timeout. Demo defaults exist solely for repeatable tests and are labeled as uncalibrated.

For each allowlisted beacon, the engine applies a median window followed by an exponential moving average. Entry requires the configured number of distinct beacon signals above the entry threshold for the full dwell duration. Exit requires all qualifying signals to remain below the weaker exit threshold or absent for the signal-loss timeout. Hysteresis prevents threshold oscillation. Samples with unknown beacons, backward timestamps, wrong sessions, or implausible RSSI values are rejected. The engine emits candidate, entered, staying, exited, rejected, and signal-lost decisions with diagnostic reasons.

Simulated BLE decisions remain untrusted evidence. Later, a real `BlePresenceSource` will convert Android scan results into the same sample type after runtime-permission checks. Classroom calibration will replace demo thresholds, and secure server-side finalization will combine authorized session state, enrollment, device registration, evidence timing, and teacher controls.

## Data model and persistence

Room entities cover student profile cache, subjects, attendance summaries, attendance events/history, timetable entries, notices, correction requests, registered device metadata, and pending sync operations. Database records carry stable IDs, update timestamps, source mode, and verification state. DAOs expose observable flows and transactional mutations.

DataStore owns small scalar preferences and session flags; it does not duplicate Room collections. Demo seeding is idempotent and versioned so app upgrades do not duplicate rows. Resetting demo data requires confirmation, recreates the seed transactionally, and never deletes files outside the application sandbox.

Repository interfaces use domain models rather than Room/Firebase classes. The demo repository can seed and mutate local state. The future Firebase adapter maps remote documents to the same domain models and enqueues writes through the synchronization boundary.

## Firebase and server readiness

No Firebase dependency is enabled until `app/google-services.json` and project configuration exist. Interfaces reserve these responsibilities:

- Firebase Authentication establishes the student UID; demo login cannot produce a production identity.
- Firestore reads only the authenticated student's profile, enrollment, timetable, notices, correction requests, and finalized attendance.
- Student clients submit presence evidence and correction requests but cannot finalize or edit attendance.
- Cloud Functions validate device registration and finalize attendance server-side using idempotent operations and audit logs.
- App Check and platform attestation supplement authentication but do not make BLE location proof tamper-proof.

The existing deny-by-default rules remain documentation/readiness artifacts until verified with the Firebase Emulator Suite. The application must report backend-unconfigured state rather than a false sync success.

## Visual design and accessibility

The app keeps the existing blue/navy identity while moving to a modern Material 3 visual system: tonal surfaces, rounded cards, clear section hierarchy, meaningful status colors, and responsive phone/tablet spacing. Navigation uses Material icons rather than letter placeholders. Content supports system font scaling, screen-reader labels, touch targets of at least 48 dp, and sufficient contrast. Important states use text and icons in addition to color.

Every demo-data screen carries a compact `DEMO DATA` or `SIMULATED — NOT VERIFIED BY BLE` indicator appropriate to its content. Offline and pending states are visible without alarmist wording.

## Error handling

- Database seeding and migrations fail safely with a recoverable error screen; destructive fallback is never automatic.
- Invalid calculator/correction inputs display field-level errors and do not mutate storage.
- Repository operations expose loading, empty, content, and retryable error states.
- Duplicate submissions are prevented using stable operation IDs and repository checks.
- Navigation handles missing/deleted record IDs by returning an explanatory not-found state.
- BLE engine rejection is diagnostic and never silently becomes positive presence.

## Testing and verification

Pure JVM tests cover shortage calculations, correction validation and duplicate detection, notification read state, sync-state reduction, demo seeding rules, navigation-independent formatting, and all BLE filtering/dwell/hysteresis/signal-loss boundaries. Room DAO/repository behavior uses Android instrumentation tests when an emulator is available. Compose smoke tests cover essential navigation and form actions where practical.

Completion requires fresh successful runs of unit tests, debug APK assembly, and Android lint. Emulator installation and navigation are reported only if actually executed. Firebase, real BLE, server finalization, and physical-device behavior remain `NOT RUN` until their required environments exist.

## Milestone acceptance checklist

- All existing verified attendance behavior and tests remain green.
- All primary and nested student routes are reachable and return correctly.
- Dashboard, attendance, calendar/schedule, inbox, profile/settings, calculator, corrections, and device registration have functional interactions and persistent demo state.
- Offline/pending/backend-unconfigured states are truthful and recover after restart.
- BLE decisions are produced only by the simulator and carry simulated provenance.
- No student action directly edits a finalized attendance record.
- Project plan/status, README, demo script, and test report reflect actual outcomes.
- The debug APK builds and is ready for beginner-guided Android Studio testing.

## Explicit non-goals for this milestone

- Teacher administration website changes.
- Firebase project creation, deployment, or credential handling.
- Android BLE scanning, foreground/background scan services, or Bluetooth permissions.
- ESP32 firmware or classroom RSSI calibration.
- Server-side attendance finalization.
- Production security, proxy-proof claims, Play Store signing, or release deployment.
