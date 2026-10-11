# SmartAttend

SmartAttend currently delivers a complete offline-capable **Student Android App milestone** built with Kotlin, Jetpack Compose, Material 3, Navigation Compose, Room, and Preferences DataStore. It now has a separate Firebase Email/Password sign-in path with UID-bound, read-only Firestore student-profile authorization. A teacher website is the next major product milestone after review; the existing `teacher-dashboard/` prototype has been preserved and was not expanded in this milestone.

This APK uses realistic demo data and a simulated BLE decision engine. Firebase login calls `signInWithEmailAndPassword` and authorizes only `users/{authenticatedUid}` profiles whose role is exactly `student`. The app does **not** write Firestore, synchronize attendance with a teacher, scan Bluetooth devices, connect to ESP32 hardware, or finalize official attendance. A Firebase identity never makes the still-local attendance records official; the UI continues to label them `DEMO`, `SIMULATED`, and `NOT VERIFIED BY BLE`.

## Student features available now

- Dashboard with current/next class, overall attendance, shortage alerts, notices, offline/backend state, and simulated presence.
- Subject attendance, detail/history, progress, target warnings, and a validated shortage calculator.
- Date-selectable schedule/calendar with lecture and lab details.
- Announcements, assignments, attendance alerts, correction updates, filters, read/unread, and mark-all-read.
- Filterable history and local correction requests with required reasons and duplicate prevention.
- Device registration stored only as `PENDING SERVER VERIFICATION`.
- Separate Firebase and Demo Mode login, password reset, session restoration, profile-aware sign-out/exit, and explicit authentication errors.
- Profile, notifications/reminder settings, system/light/dark theme, demo information, and confirmed demo reset.
- Offline Room data, DataStore preferences, deterministic demo seeding, pending-operation tracking, and backend-ready repositories.
- Firebase Authentication plus a read-only Firestore profile adapter; production attendance/data synchronization remains intentionally disconnected.
- Simulated BLE filtering/dwell/hysteresis/loss diagnostics with no Bluetooth permission or scanner.
- Android launch splash plus dedicated Privacy & Permissions and Offline & BLE Diagnostics screens.

## Open and run in Android Studio on Windows

1. Start Android Studio.
2. Choose **Open**.
3. Select the whole project folder: `D:\Smart Attend`.
4. Wait for Gradle sync and indexing to finish. Do not open only the `app` folder.
5. If Android Studio asks for a Gradle JDK, open **File > Settings > Build, Execution, Deployment > Build Tools > Gradle** and choose JDK 17. The included path is `D:\Smart Attend\.toolchains\jdk17\jdk-17.0.20.1+1`.
6. Make sure the Windows `C:` drive has several GB free, then open **Tools > Device Manager** and start an emulator; alternatively connect an authorized Android phone with USB debugging enabled. The earlier low-disk blocker was cleared before final verification.
7. Select the `app` run configuration and your device in the top toolbar.
8. Press the green **Run** triangle.
9. On the login screen choose one explicit path:
   - For your Firebase student account, enter its email and password and tap **Sign in with Firebase**. The account also needs the administrator-created `users/{uid}` document described in `docs/FIREBASE_SETUP.md`.
   - To review local sample data without Firebase, tap **Continue in Demo Mode**. No demo credentials are required.

Use the five bottom tabs and open their cards to review all nested workflows. Under **More**, review **Privacy & permissions** and **Offline & BLE status**. On Dashboard, **Simulate signals** runs only the in-app decision engine. It is not real BLE evidence.

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
- `app/src/main/java/com/smartattend/app/data/firebase` — Firebase Auth, read-only student-profile authorization, and safe error mapping.
- `app/src/main/java/com/smartattend/app/ui` — Compose navigation, primary screens, and nested workflows.
- `app/schemas` — checked-in Room database schema snapshot.
- `teacher-dashboard/` — preserved local teacher prototype for review in the next milestone.
- `docs/` and `firestore.rules` — design and future backend readiness documents; not a deployed backend.

## Future milestones

1. Review this Student Android App.
2. Build the Teacher Admin Website.
3. Verify the real Firebase student account/profile flow with owner-entered credentials, then implement secured Firestore synchronization and server-side authorization/finalization.
4. Add Android BLE scanning and ESP32 firmware, calibrate with physical devices, and run real classroom tests.

Do not add `google-services.json`, service-account keys, signing keys, or other credentials to source control.
