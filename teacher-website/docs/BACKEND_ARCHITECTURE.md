# SmartAttend — Website, Android App, Firebase and BLE Integration Logic

> Milestone 2 update (2026-11-02): Firebase teacher login, server-side ID-token verification, exact teacher profile authorization, and verified-UID assignment reads are now implemented and emulator/browser tested. The rest of this document remains the forward architecture; live-project teacher login and all attendance/business synchronization remain pending.

## 1. Present vs future states (very important)

**Verified/observed:** Android student demo runs, its real Firebase email/password login and own Firestore `users/{uid}` profile authorization were successfully tested by the project owner. The improved teacher website supports separate explicit Demo Mode and real Firebase teacher authentication through the local secured backend; the real-data pages remain unconnected.

**Not yet verified/implemented as production:** live-project teacher login with owner configuration/credentials, live sessions, BLE scanning on actual Android hardware, ESP32 beacons, authenticated attendance evidence ingestion, official attendance finalization, realtime data sync between Android and website, correction approval in Firestore, notifications, audit logging and production deployment.

## 2. Why Firebase is the backend

A browser page or Android app cannot establish a trustworthy source of official attendance by itself. A shared backend provides:

1. Identity: Firebase Authentication ties each account to a stable UID, not a mutable email/roll number.
2. Authorization: roles and teacher assignments determine who may view or operate on particular data.
3. Persistence: Firestore stores long-lived student, schedule, session and finalized attendance records.
4. Realtime: listeners deliver session changes to both web and Android clients.
5. Server validation: Cloud Functions (or another trusted server) confirm permissions, times, enrollment, evidence and idempotency before changing official records.
6. Auditability: original record, change reason, approver, timestamp and revision can be retained.
7. Operational resilience: caching/retry states allow clients to cope with network interruptions.

**Firebase is NOT just a database.** It is a group of services: Authentication (login), Firestore (data), Cloud Functions (trusted logic), Security Rules (client read/write authorization), App Check (abuse mitigation, not proof of classroom presence), optional FCM (notifications), Hosting (serves website).

## 3. Overall data path

Teacher web → Firebase Auth → role and assignment checks → callable startSession → Firestore sessions/{sessionId} [active]

Student Android → Firebase Auth → enrolled session listener → Android BLE scanner (future) → local RSSI samples / filtering → signed-in evidence submission → trusted backend validates → presenceEvents / derived intervals.

Teacher web → live onSnapshot view for assigned session → endSession callable → server merges validated intervals and applies college policy → finalized attendance documents and audit trail → student app sees updated attendance.

**ESP32 units are BLE broadcasters, not trusted databases or official attendance adjudicators.** Teacher website does not need to scan Bluetooth; scanning belongs on compatible physical student phones.

## 4. Proposed Firestore schema: clarify against existing Android code before migrating

Use existing `users/{uid}` documents with EXACT case-sensitive fields:

```text
users/{uid}
  role: "student" | "teacher" | "admin"    // assigned only by trusted admin
  name: string
  email: string
  rollNumber: string // students, preserve leading zeros
  department: string
  batch: string
  active: boolean (optional proposed field)

subjects/{subjectId}
  code: "DBMS"
  title: "Database Management Systems"
  department, semester

batches/{batchId}
  name: "TE-A"
  department, semester

teacherAssignments/{assignmentId}
  teacherUid, batchId, subjectId, active, academicYear

enrollments/{enrollmentId}
  studentUid, batchId, subjectId, active

rooms/{roomId}
  label: "302", beaconNodeIds: [...]

timetable/{scheduleId}
  subjectId, batchId, teacherUid, roomId, weekday, startLocal, endLocal, timezone

sessions/{sessionId}
  scheduleId, subjectId, teacherUid, batchId, roomId
  plannedStartAt, plannedEndAt, startedAt, endedAt: Firestore Timestamp
  state: scheduled | active | ended | finalized | cancelled
  policyVersion, createdAt, updatedAt

presenceEvents/{eventId}  // restricted; high-volume summary only
  studentUid, sessionId, deviceId, nodeId, rssiSummary, observedStartAt,
  observedEndAt, clientEventId, submittedAt, decision, decisionReason

attendance/{sessionId_studentUid} // predictable unique key to avoid duplicates
  sessionId, studentUid, status, presenceSeconds, sessionSeconds,
  percentage, late: boolean, final: boolean, finalizedAt, policyVersion,
  version, updatedBy

correctionRequests/{requestId}
  studentUid, sessionId, requestedStatus, reason, details, state,
  createdAt, reviewedBy, reviewedAt, reviewerReason

notices/{noticeId}
  authorUid, subjectId, batchId, title, message, dueAt, publishedAt

auditLogs/{logId} // immutable via client APIs
  actorUid, action, targetPath, before, after, reason, timestamp, requestId

devices/{deviceId}
  studentUid, fingerprintHashOrOpaqueIdentifier, enrolledAt,
  state, lastSeenAt, changeRequestState
```

**Privacy:** Keep personal names/roll numbers out of BLE broadcasts. The phone needs an authorized node identifier plus RSSI and timestamps to associate readings with a room; do not imply RSSI alone identifies a beacon. Limit retention; avoid collecting Wi-Fi, contacts, gallery or other irrelevant data.

## 5. Website: exact feature logic

### Teacher sign-in

- Register a Firebase *Web* app in the existing project and initialize modular Firebase SDK.
- `signInWithEmailAndPassword(auth,email,password)`; never store or log plaintext password.
- `onAuthStateChanged()` routes to teacher profile lookup `users/{uid}`.
- Check `role == 'teacher' && active !== false` and load `teacherAssignments` scoped to UID. Merely showing UI is not security; rules and functions must enforce the same role and assignment.
- Student login must never unlock teacher dashboard. Missing profile, network failure and denied permission show separate messages.
- Implement signOut and loading/error states. No auto-fallback to demo; demo must be explicitly selected.

### Dashboard / students / reports

- Read assigned subjects + batches, active/upcoming sessions, counts and relevant records via queries.
- Use `onSnapshot` listeners for active sessions and correction queues, unsubscribe on logout/page change.
- Calculate attendance from *finalized* `attendance` records only; do not show locally generated evidence as official.
- Filter by batch/subject/date, paginate larger tables; CSV export includes only authorized rows, no hidden private fields.

### Start a lecture

Teacher selects an assigned `scheduleId`, room, type. Browser calls `startSession({scheduleId, clientRequestId})`.
Trusted backend must verify:
- valid authenticated teacher UID and active teacher role;
- active assignment to schedule's batch/subject;
- schedule, room, start time, valid timezone and one-active-session rule;
- idempotency via `clientRequestId` and transaction/unique constraint;
- server-generated startedAt timestamp and immutable session ownership.
Write active session via transaction, audit event, and publish event. Both clients may subscribe.

### Live monitoring

Query `sessions/{sessionId}` and approved derived presence indicators, not raw untrusted RSSI alone.
Show states `not detected`, `verifying`, `candidate presence`, `temporarily lost`, `under review`, and only `verified` when genuine server-side checks have completed. Show last updated time and offline/stale states.
Do not mark attendance Present just because student appears on website.

### End class / final attendance

Teacher calls `endSession({sessionId, clientRequestId})`; server locks session, merges validated non-overlapping intervals, caps to class duration, checks late policy and configured rules, writes at most one final attendance per student per session and audit entries. Retry is idempotent.
Example policy ONLY if college approves: initial dwell 10–15 seconds triggers an entry candidate; final Present requires >=75% **valid** presence; 50–74% Review; less than 50% Absent. Distinguish Late flag. Do not count mere initial dwell as full attendance.

### Correction requests

Student submits an evidence-aware request to an authenticated, protected collection or callable; teacher sees requests only for their assigned subjects. Approve/reject calls trusted function `reviewCorrection` verifying teacher assignment, pending state, mandatory reason, revision/version and stable idempotency key. Audit before/after, retain original record, update derived percentages. Teacher cannot change arbitrary records from browser without server authorization.

### Notices

Teacher submits assigned subject + batch + title + content + dueAt via trusted function or strictly validated rules. Validate length and date, sanitize content before HTML display, set server author/time. Student listens only to notices for their enrollments. Optional FCM notifications should not be a source of truth; Firestore remains canonical.

## 6. Android BLE scanning (future, NOT in teacher website)

1. Student must be authenticated, enrolled, authorized device and inside an active session time window.
2. Native Android scanner obtains authorized BLE advertisement identifiers, RSSI and timestamps from 3 nodes. Do not broadcast student PII. Handle Android permissions, foreground service limits and no-Bluetooth/no-permission states.
3. Keep 5–10 recent RSSI samples per node; use median / trimmed mean, device-specific calibration, multiple-node agreement, 10–15s entry dwell, loss hysteresis and timeout.
4. Produce intervals as *candidate presence evidence*, not official attendance. Handle walking out, body attenuation, interference, different phone chipsets, replayed signals and intermittent connectivity.
5. Queue minimal events offline, deduplicate with event IDs when online, validate freshness and server-owned session association. BLE RSSI alone cannot reliably prove exact room or prevent relays/spoofing, so confidence limitations and optional manual review are required.
6. Server alone may finalize attendance after class end. Keep algorithms and thresholds configurable/versioned and tested on real ESP32 boards and representative Android models.

## 7. Current Firestore rules vs production rules

Current rules from the user's setup were of the form:

```firestore
match /users/{userId} {
  allow get: if request.auth != null && request.auth.uid == userId;
}
match /{document=**} { allow read, write: if false; }
```

These allow self-profile GET only; **teacher can't list users, read attendance or start sessions with these rules.** Do not replace with `allow read, write: if true` or grant all writes to teachers. Keep deny-by-default, allow narrowly scoped, explicitly authorized reads per teacherAssignments and enrollment, and use trusted callable functions for all official writes. Server SDKs bypass client Firestore rules, so each function must validate both authentication and authorization itself. Test allowed and forbidden operations using Firebase Emulator Suite before deploying rules.

## 8. Recommended secure backend functions

- `startSession(input)` → auth + teacher assignment + timetable + room + uniqueness; create active session + audit, return ID.
- `endSession(input)` → teacher validation + session state + transaction; compute final attendance + audit.
- `submitPresenceEvidence(input)` → student auth + enrollment + active session + device binding + freshness; validate, dedupe, normalize evidence.
- `reviewCorrection(input)` → teacher assignment + request status + reason + audit; update official attendance transactionally.
- `publishNotice(input)` → validate teacher assignment and content; commit notice and optional push trigger.
- `requestDeviceChange(input)` → student auth + active device controls; submit pending request.
- `provisionUser(input)` → admins only, no client-controlled role elevation. Prefer admin-managed invitation/onboarding.

Use server timestamps, stable document IDs, transactions, schema validation (Zod or equivalent), per-operation request IDs, and structured logs without sensitive credentials.

## 9. Firebase configuration & hosting

- Existing Firebase Android app config is in `D:\Smart Attend\app\google-services.json`; do not use this as the website config.
- Register **a second app of type Web** in the **same** Firebase project and copy its Firebase Web SDK `firebaseConfig` object into an appropriate config module. Web config values are public identifiers, not Admin SDK secrets; enforce security through rules and backend.
- Use npm Firebase modular JS SDK, ideally Vite + TypeScript if moving beyond a small static proof of concept. Alternatively retain vanilla JS and use ES modules with Vite.
- Use Firebase Hosting for the website only after tests and permission checks. Public hosting does not replace faculty authorization.
- Cloud Functions deployment may require Blaze plan (billing). Obtain approval and configure billing alerts; never upgrade/deploy automatically.

## 10. Acceptance tests

| Test | Expected |
|---|---|
| Unauthenticated visits private route | Redirect to teacher sign-in |
| Student Firebase login visits teacher portal | Denied by role checks and rules |
| Teacher A requests Teacher B's batch | Denied by rules/functions |
| Teacher starts two sessions for same schedule | One valid active session, idempotent retry |
| Student opens app during active class | Can see enrolled active session, no auto-Present |
| No BLE hardware / permissions denied | Explicit unverified/error, no official attendance |
| Submit same evidence twice | No double count |
| Lost network | Clear stale/offline state; queued evidence eventually deduplicated |
| Teacher ends class | Official server-generated attendance once, audited |
| Student writes finalized attendance directly | Denied |
| Teacher approves correction from unrelated batch | Denied |
| Reset local demo | Only browser demo state resets, Firestore unchanged |

## 11. Immediate build order

1. Preserve original ZIP and commit upgraded frontend to a Git branch.
2. Register Firebase Web App in existing project; set up local dev environment.
3. Implement web Firebase Auth; provision one teacher via trusted admin/manual console, enforce teacher only.
4. Propose, test and approve Firestore teacher read rules, assignments and basic schema; don't deploy blindly.
5. Replace demo student/timetable data with authorized Firestore queries and proper loading/empty/error UI.
6. Implement callable start/end session with server validation; connect student active-session listener.
7. Implement secure correction and notices; implement reports from finalized records.
8. Implement Android BLE scanner against three real ESP32 beacons, test calibration and presence engine.
9. Add backend ingestion/finalization and comprehensive end-to-end tests.
10. Only then launch official attendance.

## 12. Primary docs

- https://firebase.google.com/docs/web/setup
- https://firebase.google.com/docs/auth/web/password-auth
- https://firebase.google.com/docs/firestore/security/get-started
- https://firebase.google.com/docs/firestore/solutions/role-based-access
- https://firebase.google.com/docs/functions/callable
- https://firebase.google.com/docs/firestore/security/test-rules-emulator
- https://firebase.google.com/docs/hosting/quickstart
