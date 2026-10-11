# SmartAttend Student Android App and Firebase Authentication Test Report

Date: 2026-10-11 (Asia/Calcutta)

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
