# SmartAttend Offline-First Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build and verify the launchable offline SmartAttend Android app, local teacher demo, and integration documentation described by the kit.

**Architecture:** A single Android app module separates pure domain logic, demo/local data, and Compose UI behind one activity-scoped view model. Replaceable presence and cloud boundaries keep simulation, future BLE, and future Firebase responsibilities distinct.

**Tech Stack:** Kotlin, Android SDK 35, Jetpack Compose, Material 3, Gradle Kotlin DSL, JUnit 4, SharedPreferences, HTML/CSS/JavaScript teacher demo.

**Spec:** `docs/superpowers/specs/2026-10-10-smartattend-offline-design.md`

## Global Constraints

- Application ID is `com.smartattend.app`; minimum SDK is API 26.
- Primary color is `#1769E0`, navy is `#123563`, and all demo evidence says `DEMO - NOT VERIFIED BY BLE`.
- No Firebase credential, password, private signing key, or service-account key may be committed.
- No real BLE scanning or claim of secure physical presence is in this phase.
- Student-reported evidence and teacher-authorized final attendance remain separate.

## Review Focus

- Boundary percentages 75/74.9 and 50/49.9 must map to the intended status.
- Repeated or out-of-order Enter/Exit must not double-count duration.
- Time outside session bounds must never be counted.
- Wrong-batch and inactive-session students must not create evidence.
- A missing Firebase configuration must not prevent the offline app from launching.

---

### Task 1: Android project foundation

**Files:** Create Gradle wrapper/configuration, app manifest/resources, and the initial Compose activity/theme.

**Interfaces:** Produces a buildable `:app` module and package `com.smartattend.app` consumed by all later tasks.

- [x] Create project configuration for SDK 35, min SDK 26, Kotlin, Compose, Material 3, lifecycle, and JUnit.
- [x] Generate the Gradle wrapper from the verified local Gradle distribution.
- [x] Run `gradlew.bat tasks` and resolve configuration errors.

### Task 2: Attendance domain and presence boundary

**Files:** Create `core/model`, `domain/attendance`, `domain/presence`, and matching JVM tests.

**Interfaces:** Produces `AttendanceCalculator`, `AttendanceTracker`, `AppClock`, `PresenceSource`, `SimulatedPresenceSource`, and immutable model types for UI/data.

- [x] Write calculator boundary tests and verify they fail because implementation is absent.
- [x] Implement minimum calculator behavior and verify the tests pass.
- [x] Write tracker tests for re-entry, duplicates, late arrival, early exit, enrollment, inactive sessions, and open-interval finalization; verify expected failures.
- [x] Implement tracker and presence source behavior; run the full unit suite.

### Task 3: Demo repository, persistence, and view model

**Files:** Create demo fixtures, local preference store, UI state, and `SmartAttendViewModel` with validation tests for pure helpers.

**Interfaces:** Consumes domain types and produces immutable screen state plus login/logout/enter/exit/finalize actions.

- [x] Write failing login-validation and active-session-state tests.
- [x] Implement validators and demo repository.
- [x] Add persistence that stores no password and restores sign-in/simulation state.
- [x] Implement the Android view model and run unit tests.

### Task 4: Compose student experience

**Files:** Create app shell, reusable components, login, Home, Attendance, History, Timetable, Profile, and theme files.

**Interfaces:** Consumes `SmartAttendUiState`; emits only view-model actions.

- [x] Implement login and visible demo disclosures.
- [x] Implement the five destinations and functional bottom navigation.
- [x] Add loading, empty, warning, and error presentation.
- [x] Compile the debug Kotlin sources and fix all compiler/resource errors.

### Task 5: Firebase readiness and local teacher dashboard

**Files:** Create `firestore.rules`, Firebase data/setup documentation, and `teacher-dashboard` assets.

**Interfaces:** Defines shared collection names and a local-only dashboard workflow; no Android/Firebase runtime dependency is introduced without configuration.

- [x] Draft deny-by-default Firestore rules and the collection contract.
- [x] Implement local dashboard sign-in disclosure, session controls, evidence/final tables, correction audit, and CSV export.
- [x] Add example Firebase web configuration with placeholders only.
- [x] Verify JavaScript syntax and document what remains manual.

### Task 6: Submission documentation and verification

**Files:** Create `README.md`, `TEST_REPORT.md`, `DEMO_SCRIPT.md`, `VIVA_NOTES.md`; update `PROJECT_PLAN.md` and `PROJECT_STATUS.md`.

**Interfaces:** Provides beginner run/setup instructions and a truthful handoff record.

- [x] Run the full JVM unit suite and record exact results.
- [x] Run `assembleDebug`; record the APK path only on success.
- [x] Check for an attached Android target and report installation/UI testing accurately.
- [x] Audit requirements and limitations, then update all handoff documents.

