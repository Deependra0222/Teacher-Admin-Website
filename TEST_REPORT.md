# SmartAttend Test Report

Date: 2026-10-10 (Asia/Calcutta)

## Automated checks

| Check | Result | Evidence |
|---|---|---|
| Gradle project configuration | PASS | `gradlew tasks --all` exited 0 under project-local Temurin JDK 17. |
| JVM unit tests | PASS | `gradlew testDebugUnitTest`: 18 tests, 0 failures, 0 errors. |
| Debug APK assembly | PASS | `gradlew assembleDebug` exited 0 and generated `app/build/outputs/apk/debug/app-debug.apk`. |
| Android lint | PASS WITH WARNINGS | `gradlew lintDebug`: 0 errors, 8 version/target recommendations pinned for the verified SDK 35 toolchain. |
| Teacher JavaScript parse | PASS | `node --check teacher-dashboard/app.js` exited 0. |

## Covered unit behavior

- 100%, 75%, 74.9%, 50%, and 49.9% policy boundaries.
- Confirmed time clamped to scheduled duration.
- Re-entry with no counting between intervals.
- Repeated Enter and repeated Exit rejection.
- Late arrival after the grace period.
- Early exit resulting below 50%.
- Wrong-batch and inactive-session rejection.
- Session end closes an open interval at the scheduled end.
- Pre-session evidence rejection.
- Blank identifier, short password, and valid demo login validation.
- No class, upcoming, active eligible, and active ineligible session display states.

## Manual/end-to-end checks

| Scenario | Result | Notes |
|---|---|---|
| Install APK on physical phone | NOT RUN | No physical Android device was attached. |
| Launch and tap through five screens | NOT RUN | The installed API 36 AVD remained `offline`; its emulator processes were stopped after diagnostics. |
| Firebase student login/profile | NOT RUN | No Firebase project or `google-services.json` exists. |
| Student-vs-teacher Firestore rules | NOT RUN | Requires Firebase Emulator Suite or a configured test project. |
| Android-to-dashboard synchronization | NOT RUN | Both sides are intentionally local-only until Firebase is configured. |
| Real BLE/ESP32 presence | NOT RUN | No hardware exists; no BLE implementation is included. |

## Known limitations

- Demo login is not security.
- Student presence controls are untrusted local evidence.
- Teacher dashboard state is local to its browser and not synchronized with Android.
- The APK is debug-signed and is not a Play Store/release artifact.
- Device-specific UI, background restrictions, and accessibility still need physical-device verification.
- BLE RSSI is variable, penetrates some walls, can be spoofed, and cannot prove identity or exact location.

