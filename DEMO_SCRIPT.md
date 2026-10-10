# SmartAttend Student App Review Script

## 0:00 — State the boundaries

“This is the offline Student Android App milestone. The data and BLE signals are simulated. Firebase, teacher synchronization, ESP32 scanning, and official attendance finalization are not active.”

Point out `DEMO - NOT VERIFIED BY BLE` on the login screen. Continue with the prefilled `CS-042` / `demo123` values.

## 0:40 — Dashboard and offline state

Show overall attendance, shortage count, current/next class, and the `Backend not configured` badge. Scroll to simulated classroom presence. Tap **Simulate signals**, explain median/EMA filtering plus dwell and allowlisting, then tap **Simulate loss**. Emphasize that this is local untrusted evidence.

## 1:30 — Attendance and calculator

Open **Attendance**, choose a subject, and show percentage, target, and recent records. Open the **Shortage calculator**, change attended/total/target values, and calculate. Explain that it projects classes to attend or miss without changing official records.

## 2:15 — History and corrections

Open **History**, use subject/status filters, and choose **Request correction**. Select a different requested status and enter at least ten meaningful characters. Submission is stored locally as pending; a duplicate open request is rejected. The student cannot approve or finalize it.

## 3:00 — Schedule and Inbox

Open **Schedule**, move between dates, and open a lecture/lab detail. Open **Inbox**, filter notices, open an assignment or announcement, and change read state. Mention that Room preserves structured records offline.

## 3:50 — More, device, and settings

Open **More**. Review Profile and seeded correction examples. In **Registered device**, save a label and show `PENDING SERVER VERIFICATION`; never describe it as trusted or verified. In **Settings**, change notification/reminder/theme preferences and show the confirmation required before resetting demo data.

## 4:40 — Next milestones

Open **About demo mode** and summarize:

1. Student Android milestone is ready for review.
2. Teacher Admin Website is next.
3. Both clients then connect to a verified Firebase backend.
4. Android BLE/ESP32 integration and hardware calibration come last.

Close by stating that real attendance will require server-side authorization/finalization and real classroom tests; BLE RSSI alone cannot prove identity or exact location.
