# SmartAttend Project Plan

## Milestone 1 - Launchable offline Android MVP

Status: **Completed and debug APK verified.** Physical-device tap-through remains manual.

- Create the Android Studio project with package `com.smartattend.app`, min SDK 26, Kotlin, Compose, and Material 3.
- Add demo login, Home, Attendance, History, Timetable, and Profile.
- Add visible `DEMO - NOT VERIFIED BY BLE` labels and local sample data.
- Build and verify a debug APK.

## Milestone 2 - Offline attendance engine

Status: **Completed with 25 passing JVM tests across attendance, session, persistence, login, and formatting behavior.**

- Add `PresenceSource` and `SimulatedPresenceSource`.
- Validate active session and enrollment before accepting an event.
- Accumulate non-overlapping presence intervals across re-entry.
- Calculate Present/Review/Absent and a separate late flag.
- Persist in-progress demo state locally and cover required boundary cases with tests.

## Milestone 3 - Firebase readiness

Status: **Readiness completed; live integration blocked by the intentionally absent Firebase project/configuration.**

- Document the `users`, `subjects`, `enrollments`, `timetable`, `sessions`, `presenceEvents`, `attendance`, and `auditLogs` schema.
- Add restrictive Firestore rules and manual console setup steps.
- Integrate Authentication/Firestore only after a matching `google-services.json` exists.

## Milestone 4 - Teacher dashboard

Status: **Completed as a clearly labeled local-only demo.** Firebase synchronization is not claimed.

- Provide a responsive local demo for session start/stop, evidence, final attendance, justified correction, audit log, and CSV export.
- Keep local demo data visibly separate from a future Firebase-connected mode.

## Milestone 5 - QA and submission pack

Status: **Build, 25 Android JVM tests, 3 teacher-dashboard tests, lint, JavaScript parsing, and documentation completed.** Emulator/phone launch is not verified because the available AVD remained offline and no phone was attached.

- Run unit tests and debug assembly.
- Record actual PASS/FAIL/NOT RUN outcomes.
- Add README, demo script, viva notes, backup list, and precise limitations.

## Future hardware phase

- After three ESP32 boards and the target phone are available, implement `BlePresenceSource` without changing attendance calculation.
- Calibrate multi-node RSSI/dwell/exit behavior with measured classroom data.
- Preserve simulation mode for diagnostics and never claim exact location or proxy prevention.

