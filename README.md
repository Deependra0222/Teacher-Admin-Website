# SmartAttend

SmartAttend currently delivers a complete offline-capable **Student Android App milestone** built with Kotlin, Jetpack Compose, Material 3, Navigation Compose, Room, and Preferences DataStore. A teacher website is the next milestone after review; the existing `teacher-dashboard/` prototype has been preserved and was not expanded in this milestone.

This APK uses realistic demo data and a simulated BLE decision engine. It does **not** scan Bluetooth devices, connect to ESP32 hardware, authenticate with Firebase, synchronize with a teacher, or finalize official attendance. The UI labels these boundaries as `DEMO`, `SIMULATED`, and `NOT VERIFIED BY BLE`.

## Student features available now

- Dashboard with current/next class, overall attendance, shortage alerts, notices, offline/backend state, and simulated presence.
- Subject attendance, detail/history, progress, target warnings, and a validated shortage calculator.
- Date-selectable schedule/calendar with lecture and lab details.
- Announcements, assignments, attendance alerts, correction updates, filters, read/unread, and mark-all-read.
- Filterable history and local correction requests with required reasons and duplicate prevention.
- Device registration stored only as `PENDING SERVER VERIFICATION`.
- Profile, notifications/reminder settings, system/light/dark theme, demo information, logout, and confirmed demo reset.
- Offline Room data, DataStore preferences, deterministic demo seeding, pending-operation tracking, and backend-ready repositories.
- Simulated BLE filtering/dwell/hysteresis/loss diagnostics with no Bluetooth permission or scanner.

## Open and run in Android Studio on Windows

1. Start Android Studio.
2. Choose **Open**.
3. Select the whole project folder: `D:\Smart Attend`.
4. Wait for Gradle sync and indexing to finish. Do not open only the `app` folder.
5. If Android Studio asks for a Gradle JDK, open **File > Settings > Build, Execution, Deployment > Build Tools > Gradle** and choose JDK 17. The included path is `D:\Smart Attend\.toolchains\jdk17\jdk-17.0.20.1+1`.
6. Make sure the Windows `C:` drive has several GB free, then open **Tools > Device Manager** and start an emulator; alternatively connect an authorized Android phone with USB debugging enabled. At final handoff this computer had only about 0.04 GB free on `C:`, which blocks a new emulator boot until space is freed.
7. Select the `app` run configuration and your device in the top toolbar.
8. Press the green **Run** triangle.
9. On the login screen, keep the prefilled demo values `CS-042` and `demo123`, then tap **Continue in demo mode**.

Use the five bottom tabs and open their cards to review all nested workflows. On Dashboard, **Simulate signals** runs only the in-app decision engine. It is not real BLE evidence.

## Build from PowerShell

The easiest command uses the included JDK helper:

```powershell
cd "D:\Smart Attend"
.\build-smartattend.bat testDebugUnitTest assembleDebug
```

The verified APK is:

`D:\Smart Attend\app\build\outputs\apk\debug\app-debug.apk`

To install it with Android Studio, drag the APK onto a running emulator, or use **Run** so Android Studio builds and installs it. If `adb` is already on your PATH and you have authorized your phone, you may also run:

```powershell
adb install -r "D:\Smart Attend\app\build\outputs\apk\debug\app-debug.apk"
```

## Project structure

- `app/src/main/java/com/smartattend/app/core` — student/domain models.
- `app/src/main/java/com/smartattend/app/domain` — attendance, correction, shortage, sync, session, and simulated BLE rules.
- `app/src/main/java/com/smartattend/app/data` — repository contract, Room/DataStore implementation, and deterministic demo seed.
- `app/src/main/java/com/smartattend/app/ui` — Compose navigation, primary screens, and nested workflows.
- `app/schemas` — checked-in Room database schema snapshot.
- `teacher-dashboard/` — preserved local teacher prototype for review in the next milestone.
- `docs/` and `firestore.rules` — design and future backend readiness documents; not a deployed backend.

## Future milestones

1. Review this Student Android App.
2. Build the Teacher Admin Website.
3. Configure and verify the shared Firebase backend and server-side authorization/finalization.
4. Add Android BLE scanning and ESP32 firmware, calibrate with physical devices, and run real classroom tests.

Do not add `google-services.json`, service-account keys, signing keys, or other credentials to source control.
