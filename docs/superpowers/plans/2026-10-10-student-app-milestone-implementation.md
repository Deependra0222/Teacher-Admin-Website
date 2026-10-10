# SmartAttend Student Application Milestone Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver the complete offline-capable SmartAttend student application, including persistent student workflows and a simulated BLE presence decision engine, without claiming Firebase or hardware verification.

**Architecture:** Keep the single Android module and introduce Room-backed repositories plus Preferences DataStore, with domain policies isolated from Android APIs. Navigation Compose owns primary and nested routes; the existing attendance tracker remains intact and consumes only explicitly simulated presence until a future scanner adapter is hardware-tested.

**Tech Stack:** Kotlin 2.1.10, Android Gradle Plugin 8.9.1, Jetpack Compose/Material 3, Navigation Compose, Room 2.8.5, Preferences DataStore 1.2.1, Kotlin coroutines/Flow, JUnit 4, AndroidX instrumentation tests.

**Spec:** `docs/superpowers/specs/2026-10-10-student-app-milestone-design.md`

## Global Constraints

- Package remains `com.smartattend.app`; minimum SDK remains 26; Java/Kotlin target remains 17.
- Existing Android source, teacher demo, build-kit documents, attendance rules, and passing tests must be preserved.
- All seeded attendance, device registration, presence, and correction data must display `DEMO` or `NOT VERIFIED BY BLE` provenance.
- Student code may submit evidence and correction requests but must never directly finalize attendance.
- Do not add Firebase configuration, Bluetooth permissions/scanning, ESP32 firmware, deployment, secrets, or production-security claims.
- Work in the user-designated `D:\Smart Attend` checkout and keep it buildable after every task.

## Review Focus

- Invalid shortage inputs and 100% targets must return explicit, finite outcomes rather than divide-by-zero or misleading projections; Task 1 tests these cases.
- Backward, unknown-beacon, wrong-session, implausible-RSSI, and stale BLE samples must never produce entry; Task 2 tests each class.
- A duplicate pending correction for the same attendance record must be rejected without inserting another row; Tasks 1 and 4 test policy and persistence.
- Demo/backend-unconfigured states must never render as synchronized or verified; Tasks 1, 5, and 7 test state mapping and copy.
- Process restart and idempotent reseeding must preserve user changes without duplicate seed records; Tasks 3, 4, and 8 test database/DataStore behavior.

---

### Task 1: Student domain policies and models

**Files:**
- Modify: `app/src/main/java/com/smartattend/app/core/model/Models.kt`
- Create: `app/src/main/java/com/smartattend/app/domain/shortage/ShortageCalculator.kt`
- Create: `app/src/main/java/com/smartattend/app/domain/correction/CorrectionRequestPolicy.kt`
- Create: `app/src/main/java/com/smartattend/app/domain/sync/SyncStateResolver.kt`
- Test: `app/src/test/java/com/smartattend/app/domain/shortage/ShortageCalculatorTest.kt`
- Test: `app/src/test/java/com/smartattend/app/domain/correction/CorrectionRequestPolicyTest.kt`
- Test: `app/src/test/java/com/smartattend/app/domain/sync/SyncStateResolverTest.kt`

**Interfaces:**
- Produces: `ShortageCalculator.calculate(attended, total, targetPercentage): ShortageProjection`; `CorrectionRequestPolicy.validate(...)`; `SyncStateResolver.resolve(...)`; domain models for notices, corrections, devices, verification, and sync.
- Consumes: Existing `AttendanceStatus`, `SubjectAttendance`, and student/session models.

- [ ] Write table-driven tests for 18/20 at 75% (4 misses), 10/20 at 75% (20 required), already-at-target, invalid counts/targets, and unattainable 100% recovery.
- [ ] Run the new shortage tests and confirm missing-type failures.
- [ ] Implement the shortage result types and calculator with integer-safe ceiling/floor behavior.
- [ ] Write and run failing correction tests for short reasons, same-status requests, duplicate open requests, and valid submission.
- [ ] Implement correction validation and expanded domain models.
- [ ] Write and run failing sync-state tests for demo-only, backend-unconfigured, offline-pending, syncing, synchronized, and failed states.
- [ ] Implement sync-state resolution and run all Task 1 tests plus the existing JVM suite.
- [ ] Commit as `feat: add student workflow domain policies`.

### Task 2: Simulated BLE presence decision engine

**Files:**
- Create: `app/src/main/java/com/smartattend/app/domain/presence/BlePresenceDecisionEngine.kt`
- Test: `app/src/test/java/com/smartattend/app/domain/presence/BlePresenceDecisionEngineTest.kt`

**Interfaces:**
- Produces: `BleSample`, `BleDecisionConfig`, `BleDecision`, and stateful `BlePresenceDecisionEngine.process(sample)` / `advanceTime(at)`.
- Consumes: `Instant`, allowed session/beacon IDs, demo configuration, and no Android Bluetooth APIs.

- [ ] Write failing tests for allowed-beacon dwell entry, insufficient beacon count, median/EMA noise rejection, entry/exit hysteresis, signal-loss exit, and staying state.
- [ ] Add failing tests for unknown beacon, wrong session, RSSI outside -127..0, and backward timestamps.
- [ ] Implement per-beacon median windows, EMA smoothing, candidate dwell tracking, and loss timeout with simulated provenance.
- [ ] Run the focused tests and full JVM suite; commit as `feat: add simulated BLE decision engine`.

### Task 3: Room and DataStore persistence foundation

**Files:**
- Modify: `build.gradle.kts`
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/smartattend/app/data/local/entity/StudentEntities.kt`
- Create: `app/src/main/java/com/smartattend/app/data/local/StudentDao.kt`
- Create: `app/src/main/java/com/smartattend/app/data/local/SmartAttendDatabase.kt`
- Create: `app/src/main/java/com/smartattend/app/data/local/AppPreferencesRepository.kt`
- Create: `app/src/androidTest/java/com/smartattend/app/data/local/SmartAttendDatabaseTest.kt`

**Interfaces:**
- Produces: Room entities/DAOs/database singleton and DataStore flows/update methods for installation ID, notification settings, reminder lead time, theme, and seed version.
- Consumes: Domain IDs/enums via explicit mapping in Task 4.

- [ ] Add KSP, Room 2.8.5, Room testing, DataStore 1.2.1, lifecycle Compose, coroutines test, and Material icons dependencies; sync with `gradlew tasks`.
- [ ] Write the in-memory Room instrumentation test for inserts, flows, notification updates, duplicate-correction query, and transactional reset; confirm compilation initially fails on missing database types.
- [ ] Implement normalized entities, indexes, DAO operations, and database version 1 without destructive migration fallback.
- [ ] Implement Preferences DataStore with generated installation ID and typed settings updates.
- [ ] Run JVM tests and assemble the APK; defer the instrumentation execution only if no emulator is available.
- [ ] Commit as `feat: add offline Room and DataStore storage`.

### Task 4: Offline student repository and idempotent demo seed

**Files:**
- Create: `app/src/main/java/com/smartattend/app/data/StudentRepository.kt`
- Create: `app/src/main/java/com/smartattend/app/data/local/OfflineStudentRepository.kt`
- Create: `app/src/main/java/com/smartattend/app/data/local/EntityMappers.kt`
- Create: `app/src/main/java/com/smartattend/app/data/demo/StudentDemoSeed.kt`
- Test: `app/src/test/java/com/smartattend/app/data/demo/StudentDemoSeedTest.kt`
- Extend: `app/src/androidTest/java/com/smartattend/app/data/local/SmartAttendDatabaseTest.kt`

**Interfaces:**
- Produces: observable repository flows for profile, subjects, history, schedule, notices, corrections, device, pending operations; mutations for read state, correction submission, demo device registration, settings/reset.
- Consumes: Task 1 domain models and policies plus Task 3 DAOs/DataStore.

- [ ] Write failing seed tests proving stable IDs, complete demo provenance, multiple dates/types, and no duplicate IDs.
- [ ] Implement deterministic seed records and entity/domain mappers.
- [ ] Write the instrumentation behavior for idempotent initialization, persisted notice read state, duplicate correction rejection, pending-operation creation, and demo reset.
- [ ] Implement repository initialization and transactional mutations; preserve existing `DemoPreferences` attendance-session state.
- [ ] Run JVM tests, assemble, and available instrumentation tests; commit as `feat: add offline student repository`.

### Task 5: Navigation and application state

**Files:**
- Create: `app/src/main/java/com/smartattend/app/ui/navigation/StudentRoutes.kt`
- Modify: `app/src/main/java/com/smartattend/app/ui/SmartAttendViewModel.kt`
- Modify: `app/src/main/java/com/smartattend/app/ui/SmartAttendApp.kt`
- Create: `app/src/main/java/com/smartattend/app/ui/components/StudentComponents.kt`
- Test: `app/src/test/java/com/smartattend/app/ui/navigation/StudentRoutesTest.kt`

**Interfaces:**
- Produces: five primary destinations, nested route builders/parsers, repository-backed `SmartAttendUiState`, and view-model actions.
- Consumes: Task 4 repository flows and existing attendance simulation methods.

- [ ] Write failing route tests for subject/history/notice/schedule/correction parameter round trips and malformed IDs.
- [ ] Implement route constants/builders and replace manual destination switching with `NavHost` and Material icons.
- [ ] Refactor the ViewModel to initialize/collect the offline repository while retaining login, tracker, and persisted demo session behavior.
- [ ] Add actions for notices, corrections, device registration, settings, calculator inputs, filtering, and reset with explicit user messages.
- [ ] Run JVM tests and assemble; commit as `feat: add repository-backed student navigation`.

### Task 6: Primary student screens

**Files:**
- Create: `app/src/main/java/com/smartattend/app/ui/dashboard/DashboardScreen.kt`
- Modify: `app/src/main/java/com/smartattend/app/ui/attendance/AttendanceScreen.kt`
- Create: `app/src/main/java/com/smartattend/app/ui/schedule/ScheduleScreen.kt`
- Create: `app/src/main/java/com/smartattend/app/ui/inbox/InboxScreen.kt`
- Create: `app/src/main/java/com/smartattend/app/ui/more/MoreScreen.kt`
- Modify: `app/src/main/java/com/smartattend/app/ui/theme/Theme.kt`

**Interfaces:**
- Produces: functional dashboard, subject list, date-selectable agenda/calendar, notice inbox/filter/read actions, and More hub.
- Consumes: Task 5 UI state, route callbacks, and view-model actions.

- [ ] Implement premium Material 3 cards, semantic icons, loading/empty/error states, truthful demo/sync badges, and accessible actions.
- [ ] Wire dashboard simulation Enter/Exit/Calculate, shortage/notice/correction shortcuts, schedule date selection, notice filters/read actions, and More routes.
- [ ] Preserve existing Home behavior through the redesigned dashboard and retain existing screen files when no longer routed.
- [ ] Assemble and run lint; fix compile/lint errors before committing as `feat: build primary student experience`.

### Task 7: Nested student workflows

**Files:**
- Create: `app/src/main/java/com/smartattend/app/ui/attendance/SubjectDetailScreen.kt`
- Create: `app/src/main/java/com/smartattend/app/ui/attendance/ShortageCalculatorScreen.kt`
- Modify: `app/src/main/java/com/smartattend/app/ui/history/HistoryScreen.kt`
- Create: `app/src/main/java/com/smartattend/app/ui/schedule/ScheduleDetailScreen.kt`
- Create: `app/src/main/java/com/smartattend/app/ui/inbox/NoticeDetailScreen.kt`
- Create: `app/src/main/java/com/smartattend/app/ui/more/CorrectionScreens.kt`
- Create: `app/src/main/java/com/smartattend/app/ui/more/DeviceRegistrationScreen.kt`
- Create: `app/src/main/java/com/smartattend/app/ui/more/SettingsScreen.kt`
- Modify: `app/src/main/java/com/smartattend/app/ui/profile/ProfileScreen.kt`

**Interfaces:**
- Produces: every nested route and working form/action in the approved specification.
- Consumes: Task 1 validation/calculation results and Task 5 state/actions.

- [ ] Build subject detail/history filters and calculator form with field-level validation and explanatory projections.
- [ ] Build schedule/notice details with not-found handling.
- [ ] Build correction list/form with mandatory reason, requested status selection, duplicate feedback, and persisted pending state.
- [ ] Build device screen that can create only `PENDING_SERVER_VERIFICATION`; include simulated BLE diagnostics without a scan action.
- [ ] Build settings/profile with persistent controls, reset confirmation, backend-unconfigured explanation, and logout.
- [ ] Verify all routes/actions on an emulator if available, run tests/assemble/lint, and commit as `feat: complete student workflows`.

### Task 8: Persistence, compatibility, and regression verification

**Files:**
- Modify: `app/src/main/AndroidManifest.xml` only if required by storage/application initialization (no Bluetooth or network permissions).
- Modify: persistence/navigation files found defective through tests.
- Extend: relevant JVM and instrumentation tests.

**Interfaces:**
- Produces: restart-safe demo state and regression evidence across legacy attendance plus new student workflows.
- Consumes: all previous task interfaces.

- [ ] Run existing and new JVM tests, then instrumentation tests on a booted API 36 emulator.
- [ ] Exercise login, all five primary destinations, every nested workflow, notice read state, correction submission/duplicate rejection, device pending registration, settings persistence, and demo attendance.
- [ ] Force-stop/relaunch and verify persistent state; write regression tests before fixing any discovered defect.
- [ ] Run `testDebugUnitTest assembleDebug lintDebug` and record exact counts/results.
- [ ] Commit verified fixes as `test: verify complete student application`.

### Task 9: Documentation and milestone handoff

**Files:**
- Modify: `PROJECT_PLAN.md`
- Modify: `PROJECT_STATUS.md`
- Modify: `README.md`
- Modify: `TEST_REPORT.md`
- Modify: `DEMO_SCRIPT.md`

**Interfaces:**
- Produces: beginner Android Studio instructions, exact APK path, measured verification results, limitations, and next Teacher Admin Website milestone.
- Consumes: Task 8 evidence only; no unverified claim is permitted.

- [ ] Update architecture and feature documentation, clearly separating completed demo/offline features from Firebase and hardware work.
- [ ] Record exact commands, test counts, lint findings, emulator/device results, APK size/hash, and unresolved blockers.
- [ ] Run final verification once more on the documented tree and commit as `docs: complete student app milestone handoff`.
