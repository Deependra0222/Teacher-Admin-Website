# SmartAttend Offline-First Design

## Purpose and success criteria

SmartAttend is a beginner-operable final-year demonstration app for continuous classroom attendance. The first deliverable must open in Android Studio, build as package `com.smartattend.app`, run from Android 8.0 (API 26), and demonstrate the student workflow without cloud credentials or ESP32 hardware. Every simulated value must be visibly identified as demo evidence, not verified physical presence.

Success for the offline milestone means a student can use a local demo login, view Home, Attendance, History, Timetable, and Profile, enter and exit a simulated active session, and obtain a deterministic attendance result. Core attendance rules must be covered by unit tests. State that matters during a demo must survive process restarts. A separate teacher dashboard may demonstrate the teacher workflow locally, but it must not claim to be synchronized with the Android app until Firebase is configured.

## Scope

The build includes:

- Kotlin Android application using Jetpack Compose and Material 3.
- Demo authentication with explicit warnings that it is not secure authentication.
- Five student destinations and bottom navigation.
- Local sample students, subjects, timetable, session, attendance, and history data.
- Eligibility and active-session validation.
- A `PresenceSource` boundary, `SimulatedPresenceSource`, clock abstraction, interval accumulation, late flag, and attendance policy.
- Local persistence for demo sign-in and in-progress simulation state.
- Unit tests for all attendance policy boundaries and event rejection cases required by the kit.
- Firebase data model, restrictive Firestore rules, and manual setup documentation without credentials.
- A local teacher dashboard demo with session controls, evidence/final records, justified correction, audit history, and CSV export.
- Submission documentation and honest test/limitation reporting.

The build excludes real BLE scanning, ESP32 firmware, exact indoor location, face recognition, production registration, production deployment, and any claim of tamper-proof attendance.

## Architecture

The Android app is a single `app` module organized by responsibility. `core/model` owns shared records. `domain/attendance` owns pure calculation and tracking logic. `domain/presence` defines the replaceable presence-source contract. `data/demo` owns sample records, while `data/local` stores only local demo session state. `ui` contains Compose screens and one activity-scoped view model.

The UI talks to `SmartAttendViewModel`, which exposes immutable `SmartAttendUiState`. The view model uses a demo repository and an `AttendanceTracker`; it never calculates attendance in composables. The tracker consumes a `Clock`, validates student/session relationships, rejects repeated or invalid events, accumulates non-overlapping time intervals, and finalizes through `AttendanceCalculator`. `BlePresenceSource` is intentionally only documented as a future adapter.

## Data flow and persistence

Demo login accepts a non-blank student identifier and a password of at least four characters, stores no password, and persists only the signed-in flag and selected demo student identifier. The active simulation snapshot persists whether the student is inside, the open interval start, accumulated duration, and first entry. Opening the app does not create evidence. Explicit Enter/Exit actions create student-reported demo evidence.

The future Firestore model uses `users`, `subjects`, `enrollments`, `timetable`, `sessions`, `presenceEvents`, `attendance`, and `auditLogs`. Student clients may read only their authorized data and submit narrowly scoped untrusted evidence. Teachers control sessions and finalized attendance. Teacher corrections require a reason and an audit record.

## Attendance rules

- Confirmed percentage is `confirmed duration / scheduled session duration * 100`, clamped to 0-100.
- At least 75% is Present; 50% to below 75% is Review; below 50% is Absent.
- Late is a separate flag when the first accepted entry is after the configurable grace period.
- Re-entry continues accumulation without overlap.
- Repeated Enter, repeated Exit, wrong enrollment, and inactive sessions are rejected.
- Finishing a session closes any open interval at the session end and never counts time outside session bounds.

## UI design

The visual system uses primary blue `#1769E0`, navy `#123563`, white/light-gray surfaces, rounded cards, accessible contrast, and restrained motion. A persistent demo banner states `DEMO - NOT VERIFIED BY BLE`. Home shows the current class and simulation controls. Attendance shows subject totals and shortage warnings. History labels Present, Late, Review, or Absent. Timetable identifies the active lecture. Profile includes student details, logout, and a future-device placeholder.

## Error handling

Validation failures are user-readable and do not mutate state. Empty/loading/error states are represented in UI state. Local storage failures fall back to a fresh demo state and surface a non-fatal message. Firebase configuration absence is documented rather than handled as a fake success. The dashboard validates correction reasons and CSV export data.

## Testing and verification

Pure JVM tests cover 100%, 75%, 74.9%, 50%, 49.9%, re-entry, repeated Enter/Exit, late arrival, early exit, wrong enrollment, inactive session, and finalization with an open interval. Gradle `testDebugUnitTest` and `assembleDebug` are required before any build-success claim. With no attached device or emulator, installation and tap-through remain explicitly unverified.

## Assumptions and constraints

- Campus display time uses the device locale; persisted event instants use UTC epoch milliseconds.
- One classroom, one teacher, and a small demo cohort are sufficient.
- Firebase remains unconfigured until the user supplies `app/google-services.json` and creates the console resources.
- Real BLE integration starts only after the exact ESP32 model and target Android phone version are known.

