# Firebase Android SDK Setup (Connected; Service Operations Pending)

The offline Android build still works without signing in or reaching Firestore. The Firebase Android client SDK is now packaged, but no production account or database workflow is connected to the student repositories.

## Completed foundation

1. Firebase project SmartAttend and Android app package `com.smartattend.app` were configured by the project owner.
2. `app/google-services.json` is present locally, package-matched, ignored by Git, and not tracked.
3. Email/Password Authentication is enabled and Cloud Firestore exists in the Mumbai region in Production mode, as reported by the project owner.
4. Google Services Gradle plugin `4.5.0` is applied using Kotlin DSL.
5. Firebase BoM `34.19.0` manages the non-KTX `firebase-auth` and `firebase-firestore` main modules.
6. Gradle resource processing, debug compilation, and an API 36 instrumentation initialization test pass.

Firebase BoM `35.0.0` was evaluated first but selected Firebase Authentication `25.0.0`, whose Kotlin metadata `2.4.0` is newer than this project's Kotlin compiler `2.2.0`. BoM `34.19.0` is the newest version verified here that compiles with the current project toolchain; it resolves Authentication `24.2.0` and Firestore `26.6.0`. Upgrade the project Kotlin toolchain and re-run all checks before moving to a newer BoM.

## Intentionally not performed

- No Authentication user was created and no sign-in was attempted.
- No Firestore document was read, created, updated, or deleted.
- `firestore.rules` was not deployed or changed.
- No repository adapter, background synchronization, Cloud Function, or server-side attendance finalization was enabled.
- The existing demo login, Room database, DataStore preferences, and simulated BLE flow remain the active app behavior.

## Next implementation milestone

1. Define the authenticated student session state and map Firebase UIDs to trusted `users/{uid}` profiles.
2. Create test student accounts through a controlled admin workflow; never add credentials to source control.
3. Implement Firebase repository adapters behind the existing repository interfaces while keeping Room as the offline source of truth.
4. Define conflict, retry, pending-operation, and sign-out behavior before enabling writes.
5. Verify authorization with the Firebase Emulator Suite and separate student evidence from teacher-authorized attendance.
6. Only after emulator verification, review and deploy production Security Rules and then test read-only and write flows with dedicated non-production accounts.

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

