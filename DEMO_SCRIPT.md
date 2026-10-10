# Five-Minute SmartAttend Demo Script

## 0:00 - Problem and limits

“A one-time check-in does not show whether a student stayed in class. SmartAttend demonstrates duration-based attendance. Today the presence source is deliberately simulated; no BLE hardware is connected.”

## 0:40 - Student login and navigation

Open the Android app, point out the yellow demo banner, use the prefilled local login, and tap Home, Attendance, History, Timetable, and Profile. Explain that sample records are local and visibly labeled.

## 1:40 - Active class and presence

Return to Home. Show Operating Systems, Lab 204, the active eligible state, and campus-local time. Tap Enter. Explain that repeated Enter is rejected and only an explicit action creates evidence. Tap Exit and show accumulated minutes.

## 2:40 - Attendance calculation

Tap Calculate demo result. Explain the example policy: 75% Present, 50% Review, below 50% Absent, with Late stored separately. Mention re-entry, session bounds, wrong-batch rejection, and fake-clock unit tests.

## 3:30 - Teacher workflow

Open `teacher-dashboard/index.html`, sign in with the prefilled demo values, start the session, inspect evidence and final attendance, correct one student with a written reason, show the audit entry, and export CSV. State that the dashboard is not synchronized until Firebase is configured.

## 4:30 - Security and future hardware

Show `firestore.rules` conceptually: students cannot assign roles or edit final attendance; teachers control sessions/finalization; corrections are audited. Close with the planned three-ESP32 `BlePresenceSource`, measured RSSI calibration, 2-of-3 consistency, dwell/exit timing, and the limitation that RSSI cannot prove identity or exact location.

