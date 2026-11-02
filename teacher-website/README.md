# SmartAttend Teacher Portal — Improved Frontend Demo

This package is an **improved, functional local frontend prototype**, built from the submitted `smart_attendance.zip`. It is NOT the connected SmartAttend production backend or a live attendance system.

## What's included

- `index.html`: dashboard and demo KPIs, batch filters
- `students.html`: student directory and shortage filters
- `timetable.html`: day-selectable demo lecture/lab timetable
- `live_class.html`: persistent **local-only** demo sessions; no BLE scanning or real attendance
- `corrections.html`: explicitly simulated request approvals/rejections
- `notices.html`: local demo announcements (not sent to students)
- `report.html`: sample attendance graphs and demo CSV export
- `ERP.html`: **non-functional production login form**, with explicit warning; no credentials transmitted or stored
- `Setting.html`: local threshold settings and browser demo reset
- `assets/demo-data.js`: fictional sample datasets
- `assets/app.js`: shared demo logic, with error-safe page selectors, escaping and localStorage
- `assets/style.css`: responsive styling
- `original/`: all original HTML, CSS and JS files preserved for comparison
- `docs/CODEX_MASTER_PROMPT.md`: implementation handoff to Codex
- `docs/BACKEND_ARCHITECTURE.md`: end-to-end technical plan and critical security constraints

## To preview

1. Unzip the folder. On Windows, open it in VS Code.
2. Open a terminal in the folder and run:
   `python -m http.server 8000`
3. Visit `http://localhost:8000/index.html` in a browser.
4. Navigate between the pages. Demo actions are browser-local, stored in localStorage.
5. You can also open `index.html` directly, but a local web server is recommended and will be required when integrating ES modules and Firebase.

## Important safety and honesty

- **Do not enter real Firebase passwords into `ERP.html`**. The sign-in form is intentionally not connected.
- Student records are fictional, generated locally, and are not your actual Firestore collection.
- `live_class.html` does not receive ESP32 advertisements, verify presence, or update Android apps.
- `corrections.html` and `notices.html` only modify your own browser's demo state.
- Production attendance must be finalized by trusted backend code, not by browser JavaScript or Bluetooth RSSI alone.
- This ZIP does **not** contain `google-services.json`, Firebase service account keys, real students or private credentials.

## What to do for a real Firebase website

1. Keep your **existing** SmartAttend Firebase project, where your Android student login already works.
2. Register a **Web App** in Firebase Project Settings / Add App / Web and retrieve the public Web SDK config. This is **not** the Android `google-services.json` file.
3. The first teacher account must be created by an authorized administrator with `users/{uid}` role `teacher` and teacher–batch/subject assignment documents. Never let users choose their own roles.
4. Implement actual `signInWithEmailAndPassword` and `onAuthStateChanged`, with teacher role/assignment validation before loading sensitive data.
5. Maintain Firestore deny-by-default rules. Your current `allow get` for own `users/{uid}` does not authorize teacher dashboard queries.
6. Test new Rules using the **Firebase Local Emulator Suite**, including negative tests for students, strangers, and unauthorized teachers.
7. Implement trusted server-side operations (Cloud Functions or an equivalent secured server) for starting/end sessions, approving corrections and final attendance.
8. Obtain explicit approval before enabling billing, deploying security rules, or hosting the website publicly. Cloud Functions deployment may require Blaze billing.
9. Confirm backend field types and collection paths with the existing Kotlin Android project under `D:\Smart Attend` before changing shared schemas.

## Relationship to the student app

- Student app: Kotlin / Jetpack Compose; currently real Firebase email/password login and own profile reads work, but attendance/BLE remain demo.
- Teacher portal in this ZIP: HTML/CSS/JS only (browser demo); Firebase integration NOT IMPLEMENTED.
- Future: both use the **same** Firebase Auth tenant and Cloud Firestore database, with role-scoped access and trusted backend functions.

## Primary next action

Give Codex `docs/CODEX_MASTER_PROMPT.md` plus this site folder and access to `D:\Smart Attend`. Ask for a staged audit and changes, not for blind rewrites. Do not paste passwords or service-account JSON into Codex.
