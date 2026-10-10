# Firebase Setup (Manual, Not Yet Configured)

The offline Android build does not require Firebase. Complete this checklist only when you are ready to connect a real Firebase project.

## Console steps

1. Create a Firebase project named SmartAttend.
2. Register an Android app with the exact package `com.smartattend.app`.
3. Download `google-services.json` and place it at `app/google-services.json`. The file is ignored by Git in this repository.
4. Enable Authentication > Sign-in method > Email/Password.
5. Create Cloud Firestore in an appropriate region and publish the repository's `firestore.rules`; do not use open test rules.
6. Create two student Authentication users and one teacher Authentication user.
7. In `users/{uid}`, create student profiles with `role: "student"`. Bootstrap the teacher profile manually from a trusted admin environment with `role: "teacher"`. There is deliberately no client-side “make me teacher” path.
8. Add the Google services plugin and Firebase Auth/Firestore main modules only after the configuration file exists. Use the current Firebase BoM and do not use the discontinued `-ktx` artifacts.

As checked on 2026-10-10, the official Android setup documentation lists Firebase BoM `34.19.0` and Google services plugin `4.5.0`. Recheck official release notes before integration because these versions change.

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

