# SmartAttend

SmartAttend is an offline-first Android demonstration of continuous classroom attendance. It includes a Kotlin/Jetpack Compose student app, a tested simulated-presence engine, local persistence, Firebase-ready security documentation, and a separate teacher dashboard demo.

This build does **not** use real Bluetooth or ESP32 data. Every simulated screen and record is labeled `DEMO - NOT VERIFIED BY BLE`. It is not proxy-proof or production-ready attendance software.

## What works now

- Local demo login (prefilled `CS-042` / `demo123`); no password is stored.
- Home/current class with active eligibility, room, time, and Enter/Exit simulation.
- Attendance totals and shortage warnings.
- History with Present, Review, Absent, and Late information.
- Today's timetable and active class indicator.
- Profile, future BLE placeholder, logout, and restored demo state.
- Attendance boundaries: >=75% Present, 50% to <75% Review, below 50% Absent; Late is separate.
- Re-entry accumulation, duplicate event rejection, batch/session validation, and open-interval finalization.
- Offline teacher dashboard demo with correction reasons, audit history, and CSV export.

## Project structure

- `app/` - Android application.
- `app/src/main/java/com/smartattend/app/core` - shared records.
- `app/src/main/java/com/smartattend/app/domain` - pure attendance/session/presence behavior.
- `app/src/main/java/com/smartattend/app/data` - demo data and local persistence.
- `app/src/main/java/com/smartattend/app/ui` - Material 3 Compose screens.
- `teacher-dashboard/` - local HTML/CSS/JavaScript teacher demo.
- `firestore.rules` and `docs/FIREBASE_SETUP.md` - future Firebase boundary.
- `PROJECT_PLAN.md`, `PROJECT_STATUS.md`, and `TEST_REPORT.md` - handoff and evidence.

## Open and run in Android Studio (Windows)

1. Open Android Studio and choose **Open**.
2. Select `D:\Smart Attend` (the folder containing `settings.gradle.kts`).
3. Allow Gradle sync to finish. The project is configured for Android SDK 35, minimum SDK 26, Gradle 8.12, and JDK 17.
4. If Android Studio asks for a Gradle JDK, choose `D:\Smart Attend\.toolchains\jdk17\jdk-17.0.20.1+1` or another installed JDK 17.
5. Connect a phone with USB debugging or start an emulator, select it, and press **Run**.
6. Use the prefilled demo login. Tap the five bottom-navigation items. On Home, use **Enter**, **Exit**, then **Calculate demo result**.

From PowerShell, the beginner-safe build command is:

```powershell
.\build-smartattend.bat testDebugUnitTest assembleDebug
```

The generated debug APK is at `app\build\outputs\apk\debug\app-debug.apk`. To install it on an authorized connected phone:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Teacher dashboard demo

Open `teacher-dashboard\index.html`, or run this inside `teacher-dashboard`:

```powershell
python -m http.server 8080
```

Then visit `http://localhost:8080`. The prefilled account is local demo input only. The dashboard and Android app do not synchronize until Firebase is configured.

## Firebase setup

Follow `docs/FIREBASE_SETUP.md`. You must create the Firebase project, register `com.smartattend.app`, add `app/google-services.json`, enable Email/Password Authentication, create Firestore, and bootstrap teacher permissions from a trusted admin environment. Do not add service-account keys to either client.

## Future ESP32 integration

Keep `PresenceSource` unchanged and add a real `BlePresenceSource` only after the exact ESP32 boards and Android phone are known. Start with one beacon, then three. Measure RSSI at seats, walls, doors, and corridors; add filtering, dwell time, an initial 2-of-3 rule, and exit timeout based on measurements. BLE RSSI does not provide exact location and cannot by itself prevent spoofing or proxy attendance.

## Backup checklist

Back up all source and documentation, especially `app/src`, Gradle files, `teacher-dashboard`, `firestore.rules`, `docs`, project plan/status, test report, and the APK. Do not back up/share `local.properties`, `app/google-services.json`, signing keys, or `teacher-dashboard/firebase-config.js`. The ignored `.toolchains` JDK can be downloaded again; install/configure JDK 17 when restoring on another computer.

