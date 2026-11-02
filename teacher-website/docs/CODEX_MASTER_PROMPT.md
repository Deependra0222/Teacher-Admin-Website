# MASTER CODEX PROMPT — SmartAttend Teacher Website + Shared Firebase Backend

You are acting as lead software engineer, Android integration engineer, Firebase security architect, UX engineer and QA reviewer for a final-year college project called **SmartAttend**.

## EXISTING WORK AND NON-NEGOTIABLE CONTEXT

There are TWO different user interfaces:
- **Student:** existing native Android application in `D:\Smart Attend` using Kotlin, Jetpack Compose, Room and DataStore.
- **Teacher:** HTML/CSS/JavaScript portal provided in this `smartattend_teacher_upgrade` directory. The `original/` folder contains the user's previous original 9 files. Improved pages are local demo-only.

Both must use the **same existing Firebase project** called SmartAttend, already used by the Android student app. The project owner has verified email/password student login and read of a Firestore `users/{uid}` student document with string fields `role`, `name`, `email`, `rollNumber`, `department`, `batch`.

Current production Firestore rules were created to permit authenticated `get` of own user profile only; do NOT overwrite or weaken these without review and comprehensive emulated tests.

Student attendance, session times, correction requests, notices and Bluetooth shown in the Android app are still DEMO / SIMULATED unless actual backend and hardware integrations are verified. There is no proven physical ESP32 BLE scanning or trusted official attendance finalization at this point.

**Inspect the existing code and PROJECT_STATUS.md before making changes.** Do not delete or recreate the Android project. Do not mistakenly mix the teacher website with the student app package. Create Git checkpoint, work on an isolated branch and preserve the original website.

## GOAL

Deliver a real, role-secured **SmartAttend Teacher Administration Website**, shared Firestore schema and trusted backend. Keep the existing working student app and login intact. Implement each milestone independently with actual tests. Do not pretend mock data is live.

## START WITH AUDIT (MUST DO)

1. Inspect every uploaded original teacher website page (`index.html`, `ERP.html`, `live_class.html`, `report.html`, `Setting.html`, `style.css`, `form.css`, `script.js`, `script-1.js`).
2. Inspect the upgraded site and its `docs/BACKEND_ARCHITECTURE.md`, which identifies exact original issues and proposed schema.
3. Inspect Android app Auth/Firestore profile mapper, Room models, session calculators, navigation, BLE decision engine and current Firestore rules/collection names.
4. Produce a short gap analysis: implemented/verified, demo-only, missing, high-risk. Record necessary schema compatibility decisions. Do not rename the Android `users/{uid}` student profile fields without a migration.
5. Determine whether current local Firebase project config is for the same SmartAttend project; Android `google-services.json` is NOT the web SDK config.
6. Report verified commands/test results; do not invent evidence.

## MILESTONE A — UI AND WEB TOOLING

- Preserve the improved design language, navigation and accessibility. Consolidate duplicated scripts, remove null-reference errors, brittle inline events and unnecessary hardcoded attendance claims.
- Keep responsive dashboard with current/upcoming class, teacher profile, assigned batches, subjects, class session controls, student directory, timetable, reports, notices and correction requests.
- Provide states for loading, empty, online/offline, permission denied and missing teacher assignment; add confirmation dialogs for destructive actions.
- Keep a visibly labeled DEMO MODE for preview only. Never automatically fall back to demo when real login fails. Never show mock 'verified attendance' as official.
- Introduce Vite + TypeScript using Firebase modular SDK and reusable modules/services IF appropriate, preserving routes and easy setup for a beginner. No unnecessary framework rewrite.
- Create a separate `README.md` with exact run/test/build commands, deployment procedure and all key source paths.

## MILESTONE B — SECURE TEACHER LOGIN (BUILD AND VERIFY BEFORE OTHER PRODUCTION FEATURES)

1. **Register a Firebase WEB application** in the SAME Firebase project if one is not registered. Ask the user for Web SDK config or actionable Console steps; never request passwords, Firebase Admin private keys or service-account JSON.
2. Configure Firebase modular JS SDK and `signInWithEmailAndPassword` / `onAuthStateChanged` / `signOut` / password reset.
3. Do not accept faculty IDs as passwords or store login credentials in localStorage. If faculty-ID login is required later, use an authorized server-side resolver that maps it to a unique account without leaking existence.
4. Provision a test teacher through authorized admin action only. In `users/{uid}`, role MUST be assigned by admin: `teacher`. Student accounts must be explicitly denied teacher UI and teacher data.
5. Teacher must also have active assignments to subjects/batches. Verify role + assignment for every data request and sensitive operation in security rules/trusted server, not just the frontend.
6. Session persistence, logout, errors and unauthorized-role messages must be functional. Show separate DEMO PREVIEW only with explicit opt-in.
7. Verify teacher login on browser with a test teacher, negative tests with student UID, unauthenticated user and teacher with no assignment.
8. Never deploy teacher access if production Firestore rules have not passed negative tests.

## MILESTONE C — FIRESTORE DATA MODEL AND READS

Use a compatible, minimally invasive versioned data model, **after inspecting Android code**:
- `users/{uid}`: keep six student fields (`role`, `name`, `email`, `rollNumber`, `department`, `batch`) and teacher equivalent `name/email/role/department`.
- `subjects`, `batches`, `rooms`, `enrollments`, `teacherAssignments`, `timetable`, `sessions`, `attendance`, `presenceEvents`, `correctionRequests`, `notices`, `devices`, `auditLogs` (confirm naming/IDs with Android app first).
- Store canonical timestamps in Firestore Timestamp/UTC; render Asia/Kolkata times for college, with timezone identifier.
- Use stable `studentUid` (Firebase UID), server-issued session IDs and deterministic final attendance ID `sessionId_studentUid`, with version metadata and audit trail.
- Teacher UI: list only assigned students/subjects; query filters, date range, low-attendance warnings, paginated tables, exports; realtime listeners with unsubscribes.
- Student app: preserve working login; update only when shared schema and rules are verified. Student can read only own finalized attendance, enrolled sessions, timetable and relevant notices, submit allowed correction request.
- Do not grant broad `allow read, write: if request.auth != null` or general teacher write permissions.
- Create Firestore indexes if queries require them. Add seed scripts for **fictional test data** into emulator only, never silently insert sample data into production.

## MILESTONE D — TRUSTED SERVER OPERATIONS

Implement Cloud Functions for Firebase (TypeScript preferred) OR another appropriate authenticated server, with schema validation, Auth and App Check checks where supported, teacher assignment verification, transactions, idempotency keys, server timestamps and audit logs:
- `startSession({scheduleId, roomId, clientRequestId})`: verify teacher controls batch and subject; valid timetable/time; one active session; write `active` with server time.
- `endSession({sessionId, clientRequestId})`: validate teacher and session; idempotently close; finalize derived attendance ON SERVER.
- `submitPresenceEvidence({sessionId, deviceId, eventId, windowStart, windowEnd, nodes...})`: verify signed-in student, enrollment, device binding, active session, freshness and dedupe; allow only minimal authorized node IDs, RSSI aggregates, timestamps, no personal BLE broadcasts. Replayed/spoofed signals need risk controls, manual review capability.
- `reviewCorrection({requestId, decision, reason, clientRequestId})`: validate teacher assignment/pending state/version, transactional attendance revision, immutable audit.
- `publishNotice({subjectId,batchId,title,message,dueAt,clientRequestId})`: assigned teacher only, sanitize content, server author/timestamp; notify relevant students optionally.
- `requestDeviceChange(...)`: student authorized workflow; no self-activation without policy.
- All privileged functions must enforce permissions in server code. Admin SDK bypasses Firestore client rules; SECURITY MUST NOT depend only on client UI or client Firestore rules.
- Functions may require Blaze billing for deployment; ask explicit user permission before billing upgrade or deployment. All initial testing in Firebase Local Emulator Suite.

## MILESTONE E — CROSS-PLATFORM LIVE DATA

- Teacher starts an approved class in website -> server marks `sessions/{id}` active -> enrolled students' Android app sees it via realtime Firestore listener.
- Students do NOT become Present from opening app or entering a classroom within a single 10-second window.
- Android BLE scan implemented only in native Android: authorized 3 ESP32 broadcast node identifiers + RSSI and timestamps, filtering median/average, device-specific calibration, entry dwell 10–15 seconds, loss/exit hysteresis, monitoring intervals.
- Browser teacher website never claims to scan ESP32. It only displays backend-validated status.
- Physical ESP32 and real Android phone testing required. Emulator simulations remain clearly labeled.
- Server verifies evidence and computes attendance; example 75% policy must be confirmed by college before production. Define Present/Review/Absent/Late thresholds and session-length semantics explicitly and centrally, not hardcoded in both apps.
- Handle offline retries, idempotency and latency; never double count overlapping intervals.

## MILESTONE F — SECURITY, QUALITY AND RELEASE

- Security Rules: least privilege, teacher assignments, student own profile/attendance, no client edits to official attendance or role. No wildcard open rules.
- Use Firebase Local Emulator Suite with tests: unauthenticated denied, student cannot see teacher dashboard/other students, teacher cannot access other batches, students cannot change finalized attendance or their role, correction updates audited, duplicate requests idempotent.
- Frontend: semantic HTML, keyboard controls, mobile responsive layouts, loading/error/empty states, accessible labels, escaped user text and CSV formula injection safeguards.
- Backend: validation, input length caps, logging without secrets, optional App Check, rate limits, transactional updates, privacy/data minimization, backups and recovery plan.
- End-to-end: teacher logs in, starts class, Android sees active class, simulated presence clearly identified, physical BLE verified only on hardware, teacher ends class, server finalizes, student sees official attendance; corrections and notices work.
- Preserve Git history and working APK. Run tests and builds at each milestone. Provide a concise changelog, status, blockers and exact commands.
- **DO NOT deploy** website or rules to production, create real student data or enable paid billing until explicit user approval.

## DONE CRITERIA

Not 'finished' when a page looks attractive or compiles. The portal is done when teacher Firebase login is verified; teachers can view only assigned classes/students; trusted server operations and rules are tested; student app updates in realtime; official attendance cannot be forged from browser/Android client; corrections are audited; real BLE is separately verified on hardware; and end-to-end tests pass.

## FIRST ACTION NOW

Audit first, preserve the existing work, and implement only **MILESTONE B — secure teacher login** (plus required tooling). Stop with actual build/tests and any exact user-supplied Firebase Web App registration/configuration steps. Then proceed sequentially through other milestones on user review; do not ask approval for every minor coding decision. Never fabricate test results, secret values or real attendance data.
