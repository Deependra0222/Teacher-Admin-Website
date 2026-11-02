# SmartAttend Teacher Portal — Firebase Authentication Milestone

This is the preserved improved teacher website, now connected to Firebase Email/Password Authentication and the local SmartAttend Express authorization backend.

## Two intentionally separate modes

- **Firebase teacher session:** Firebase authenticates the account; the Express backend verifies the ID token, reads `users/{uid}`, requires exact role `teacher`, and loads only active assignments for that verified UID. Local demo student/attendance records are hidden.
- **Explicit Demo Mode:** starts only when the teacher clicks **Continue in explicit DEMO Mode**. It uses fictional browser-local records and remains labeled DEMO / NOT LIVE / NOT VERIFIED BY BLE.

A failed Firebase sign-in never opens Demo Mode. A student, missing profile, wrong role, incomplete profile, invalid token, or backend failure is denied with a clear message.

## Preview

Run the backend; do not use `python -m http.server`, because the same-origin authentication APIs and runtime Firebase configuration come from Express.

```powershell
cd "D:\Smart Attend\backend"
npm ci
npm run dev
```

Open:

```text
http://localhost:3000/ERP.html
```

With the default `FIREBASE_MODE=disabled`, real sign-in is disabled safely and explicit Demo Mode remains available. See `../backend/README.md` for emulator and existing-live-project setup.

## Browser security behavior

- Uses the Firebase modular Web SDK from the official Google CDN.
- Uses session persistence; the website does not manually save ID tokens.
- Sends the current short-lived Firebase ID token only in the backend Authorization header.
- Never stores passwords.
- Clears any old Demo Mode marker before a real sign-in attempt.
- Signs Firebase out when backend teacher authorization fails.
- Restores an authorized teacher session after navigation/reopen within the browser session.
- Provides Firebase logout and password-reset request actions.
- Protects every non-login page with the same session/role check before enabling its mode.

## Preserved design and files

The existing blue-and-white layout, responsive navigation, page routes, and local demo interactions remain. `original/` was not modified; its nine Milestone 1 hashes still match. The older root `teacher-dashboard/` and the Student Android project are untouched by this website milestone.

## Milestone boundary

Real authentication is implemented, but timetable, student directory, live sessions, corrections, notices, reports, and official attendance are not connected to live records yet. In a real Firebase session those local demo datasets are hidden and controls remain inactive. No deployment or Firebase rules change was performed.
