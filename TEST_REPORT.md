# SmartAttend Student Android App and Firebase SDK Foundation Test Report

Date: 2026-10-11 (Asia/Calcutta)

## Automated verification

| Check | Result | Evidence |
|---|---|---|
| Focused BLE unit suite after regression fix | PASS | 7 tests; the signal-loss deadline now ignores cached timer-only observations. |
| Complete JVM suite | PASS | 49 tests across 12 suites; 0 failures, 0 errors. |
| Requested build command | PASS | `build-smartattend.bat testDebugUnitTest assembleDebug --no-daemon` invoked `gradlew.bat` with project JDK 17 and completed successfully. |
| Google Services resource processing | PASS | `processDebugGoogleServices` completed during the build for package `com.smartattend.app`. |
| Android lint | PASS WITH RECOMMENDATIONS | `lintDebug`: 0 errors, 11 dependency/target-version recommendations. |
| Instrumentation suite | PASS | `connectedDebugAndroidTest`: 3 tests, 0 failures on Pixel 7 API 36, including Firebase client initialization. |
| Debug APK install | PASS | `adb install -r` installed the generated APK on Pixel 7 API 36. |
| Cold app launch | PASS | `com.smartattend.app/.MainActivity` launched and rendered the demo login; logcat recorded `FirebaseApp initialization successful`. |
| APK output | PASS | `app/build/outputs/apk/debug/app-debug.apk`, 24,066,020 bytes. |
| APK integrity | RECORDED | SHA-256 `5BB93771BEB85B860E1567FE8ADAB1EB48E9C2DB5D58F1A38C1C357EF27692FB`. |

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

## Instrumentation behavior covered

- In-memory Room inserts and observable flows.
- Persisted notice read mutation.
- Open-correction lookup.
- Transactional database clearing.
- Idempotent repository initialization and non-duplicated seed.
- Duplicate open correction rejection and pending-operation creation.
- Transactional demo reset.
- Packaged Firebase configuration initializes the default Firebase app and constructs Authentication and Firestore client instances without performing a service operation.

## Emulator checks actually performed

The `Pixel_7` AVD booted on Android API 36. The APK installed and launched. UI hierarchy/screenshot checks confirmed:

- Demo login and Dashboard.
- Attendance, Schedule, Inbox, and More primary destinations.
- Shortage calculator, subject detail, filtered history, correction form, schedule detail, and notice detail.
- Profile, correction list, device registration, settings, and About Demo routes.
- Privacy & Permissions and Offline & BLE Diagnostics routes with the expected offline-demo and simulated-BLE boundaries. Firebase contributes network permissions, but app-level Auth/Firestore workflows remain disconnected.
- Device registration action rendered `PENDING SERVER VERIFICATION`.
- Demo labels and the disconnected-sync state were visible; no screen presented SDK initialization as a completed backend connection.

The `Medium_Phone_API_36` AVD remained `offline` in ADB and was not used. An earlier Pixel 7 restart failed while `C:` had about 0.04 GB free; that condition was later resolved. With about 27.56 GB free, Pixel 7 API 36 booted again, the final APK installed, both instrumentation tests passed, and the new routes passed a UI-hierarchy smoke check. Physical process-death/OEM behavior remains for phone testing.

## Not run / not verified

| Scenario | Result | Reason |
|---|---|---|
| Physical Android phone | NOT RUN | No phone was attached. |
| Real Firebase login/sync/rules/functions | NOT RUN | SDK initialization was tested, but no test account, Auth request, Firestore operation, rules-emulator run, rules change, or Cloud Function was authorized for this milestone. |
| Real Android notifications | NOT RUN | This milestone stores preferences and inbox records only. |
| Real BLE scan/service | NOT RUN | No Bluetooth permission or scanner was added by design. |
| ESP32 RSSI/dwell/loss calibration | NOT RUN | No hardware was available. |
| Server-side attendance finalization | NOT RUN | Cloud Functions/backend are not configured. |
| Production security/release signing | NOT RUN | APK is a debug artifact. |

## Known lint recommendations

Lint reports only target/dependency update recommendations: target SDK 35 and several intentionally pinned AndroidX/Firebase test/runtime versions. There are no lint errors. These warnings should be reconsidered with the next verified toolchain upgrade rather than changed without regression testing.
