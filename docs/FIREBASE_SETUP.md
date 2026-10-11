# Firebase Student Authentication Setup (Implemented; Real Account Verification Pending)

The offline Demo Mode still works without signing in or reaching Firestore. The Android app now performs Firebase Email/Password sign-in and a read-only authorization lookup at `users/{authenticatedUid}`. Attendance, timetable, notices, corrections, device registration, and BLE evidence are still local demo data and are not synchronized.

## Completed foundation

1. Firebase project SmartAttend and Android app package `com.smartattend.app` were configured by the project owner.
2. `app/google-services.json` is present locally, package-matched, ignored by Git, and not tracked.
3. Email/Password Authentication is enabled and Cloud Firestore exists in the Mumbai region in Production mode, as reported by the project owner.
4. Google Services Gradle plugin `4.5.0` is applied using Kotlin DSL.
5. Firebase BoM `34.19.0` manages the non-KTX `firebase-auth` and `firebase-firestore` main modules.
6. Gradle resource processing, debug compilation, and an API 36 instrumentation initialization test pass.
7. Real login calls `FirebaseAuth.signInWithEmailAndPassword`; password reset calls `sendPasswordResetEmail`; Firebase session restoration waits for the first Auth state callback.
8. Successful Auth is permitted into the student app only after the matching Firestore profile is read from the server and validated. Cached profiles cannot authorize offline; missing, incomplete, unreadable, or non-student profiles are signed out.
9. Password-reset completion remains neutral for nonexistent or disabled accounts so the UI does not reveal whether an email is registered.
10. Demo Mode is a separate explicit action backed by a DataStore marker. A failed Firebase request never falls back to demo.

Firebase BoM `35.0.0` was evaluated first but selected Firebase Authentication `25.0.0`, whose Kotlin metadata `2.4.0` is newer than this project's Kotlin compiler `2.2.0`. BoM `34.19.0` is the newest version verified here that compiles with the current project toolchain; it resolves Authentication `24.2.0` and Firestore `26.6.0`. Upgrade the project Kotlin toolchain and re-run all checks before moving to a newer BoM.

## Required student profile

For an Authentication user with UID `<uid>`, a trusted administrator must create `users/<uid>` with these nonblank string fields:

```text
role: "student"
name: "Student Name"
email: "student@example.edu"
rollNumber: "CS-042"
department: "Computer Science"
batch: "CSE-2026-A"
```

The document ID must be the Firebase Authentication UID. The app ignores any `id` field inside the document and uses the authenticated UID. The role check is case-sensitive. Do not let a student client create or modify this profile.

## How to test the account

1. In Firebase Console, confirm Email/Password sign-in remains enabled.
2. Copy the test user's UID from Authentication.
3. In Firestore, create `users/{uid}` with the exact required fields above. Keep the existing restrictive rules.
4. Run the Android app, enter the account email/password directly in the app, and tap **Sign in with Firebase**.
5. A configured student sees their real profile plus a banner stating that attendance data is still demo/not synchronized. A missing profile shows **Account Not Configured**. A teacher/admin/wrong role is rejected.
6. Use **Profile > Sign out** to end the Firebase session. Use **Continue in Demo Mode** only when intentionally reviewing sample data.

Never put the password in source files, terminal commands, documentation, screenshots, or chat messages.

## Intentionally not performed

- No real account credential was supplied to this development session, so a successful registered-user login and authorized production profile read were not claimed.
- A controlled sign-in attempt for an intentionally nonexistent address verified the real Auth failure path and returned the non-enumerating `Incorrect email or password` UI without entering Demo Mode.
- No Firestore document was created, updated, or deleted by the app or this milestone.
- `firestore.rules` was not deployed or changed.
- No background synchronization, Cloud Function, or server-side attendance finalization was enabled.
- Room, DataStore preferences, explicit Demo Mode, and simulated BLE remain available.

## Next backend implementation milestone

1. Have the project owner enter the existing test account directly in the emulator and confirm the UID-bound profile loads; do not record its password.
2. Build/review the Teacher Admin Website as the next product milestone.
3. Define conflict, retry, and pending-operation behavior before enabling synchronized reads or writes.
4. Verify authorization with the Firebase Emulator Suite and separate student evidence from teacher-authorized attendance.
5. Only after emulator verification, review and deploy production Security Rules and then test narrowly scoped flows with dedicated non-production accounts.

## Shared collection contract

| Collection | Important fields | Writer |
|---|---|---|
| `users` | `name`, `email`, `rollNumber`, `department`, `batch`, `role` | Trusted admin |
| `subjects` | `code`, `name`, `teacherIds` | Teacher/admin |
| `enrollments` | Document ID `${studentId}_${subjectId}`; fields `studentId`, `subjectId`, `batch`, `active` | Teacher/admin |
| `timetable` | `subjectId`, `batch`, `room`, `startTime`, `endTime` | Teacher/admin |
| `sessions` | `subjectId`, `teacherId`, `batch`, `room`, `startAt`, `endAt`, `status` | Authorized teacher |
| `presenceEvents` | `studentId`, `sessionId`, `type`, `occurredAt`, `source`, `simulated` | Student evidence only |
| `attendance` | `studentId`, `sessionId`, `confirmedSeconds`, `percentage`, `status`, `late`, `finalizedBy` | Authorized teacher |
| `auditLogs` | `actorId`, `attendanceId`, `sessionId`, `beforeStatus`, `afterStatus`, `reason`, `createdAt` | Authorized teacher |

Store timestamps as UTC Firestore timestamps. Convert to Asia/Kolkata only for display. Student evidence is untrusted input; finalized attendance must remain teacher-controlled.

Attendance corrections must use one Firestore transaction or atomic batch. Generate an audit ID, update the attendance document with `auditLogId` and the corrected `status`, and create `auditLogs/{auditLogId}` with the same attendance/session IDs, the previous and new status, the teacher's UID, a reason of at least five characters, and a server timestamp. The included rules reject a correction if its matching audit record is absent.

## Verification checklist

- Student A can read only Student A's private profile and attendance.
- Student B cannot read Student A's private records.
- A student cannot update `role`, a session, or finalized attendance.
- An authorized teacher can start/stop only their session.
- A correction writes both attendance and an audit record with a reason.
- Network errors produce explicit retry/error UI and do not overwrite local demo state.

