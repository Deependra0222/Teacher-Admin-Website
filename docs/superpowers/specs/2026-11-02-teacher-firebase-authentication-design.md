# SmartAttend Teacher Firebase Authentication Design

## Intent

Add real Firebase Email/Password authentication to the existing SmartAttend Teacher Website and authorize teachers through the trusted Express backend. The browser proves identity with a Firebase ID token; the server independently verifies that token, reads `users/{uid}`, requires the exact teacher role, and scopes assignment results to the authenticated UID.

This milestone establishes identity and read-only authorization only. It does not connect official attendance, student lists, sessions, corrections, notices, reports, BLE evidence, or production writes.

## Current constraints

- The project is already on the completed Milestone 1 foundation.
- `teacher-website/original/` is a protected byte-for-byte backup and must not change.
- The improved HTML/CSS layout must remain visually unchanged.
- The Android application and `firestore.rules` must remain unchanged.
- The original Milestone 2 assumption named `smartattend-e9a35`. The later cross-client audit corrected the existing project to `smartattend-2a5ce`, as verified from the ignored Android `google-services.json`; no Web App configuration is committed to the workspace.
- Firebase Web configuration is public application metadata, but it will be supplied at runtime from ignored backend environment values rather than committed into the website.
- Firebase Admin credentials are private. The implementation supports Application Default Credentials and the Firebase Emulator Suite; credentials are never embedded in browser or source files.
- The present Firestore rules are not used as teacher API authorization. Firebase Admin bypasses rules, so every server operation must validate UID, role, and assignment scope itself.

## Chosen architecture

### Browser

The unchanged `ERP.html` form uses Firebase's modular Web SDK version 13.1.0 to call Email/Password sign-in. Firebase uses session persistence, so credentials are never stored and the browser session ends when the tab/session ends. The signed-in user's short-lived ID token is sent only in the `Authorization: Bearer <token>` header to the same-origin Express API.

Public Firebase Web App settings are delivered by `/firebase-config.js` from environment variables. If configuration is missing, real login fails closed with a clear setup message while explicit Demo Mode remains available.

### Server

The server supports three explicit Firebase modes:

- `disabled` — default; health/static demo work, protected endpoints return a safe 503.
- `emulator` — Admin Auth and Firestore connect only to local emulator hosts with a demo project ID.
- `adc` — Admin SDK uses `applicationDefault()` and an explicit existing-project ID.

For each protected request the server:

1. Parses one Bearer token.
2. Calls Firebase Admin `verifyIdToken(token, true)` so revoked tokens are rejected as well as invalid/expired tokens.
3. uses only the verified token UID.
4. Reads `users/{uid}` from Firestore.
5. Requires `role` to be the exact string `teacher`, `active` not to be `false`, and nonblank string fields `name`, `email`, and `department`.
6. Returns only a safe teacher view.

`GET /api/teacher/assignments` queries `teacherAssignments` with both `teacherUid == authenticatedUid` and `active == true`. No assignments returns an authorized empty array. Missing or malformed assignment data never expands access.

### Demo separation and page protection

Demo Mode is entered only through the existing explicit demo link and stores a `demo` marker in `sessionStorage`, not `localStorage`. Firebase login clears the demo marker. Login failure never enters Demo Mode.

Every non-login teacher page loads the auth guard. Demo Mode keeps the existing local demo screens and labels. Firebase mode first verifies `/api/me`; until authorization succeeds the page's demo content is concealed. After real authorization, the shell remains visually consistent but local demo records are not rendered as real data. Logout uses Firebase `signOut`, clears the session mode, and returns to `ERP.html`.

## Teacher document contract

Expected path:

```text
users/{Firebase Authentication UID}
```

Required fields:

```text
role: "teacher"       // exact lowercase string
name: string           // nonblank
email: string          // nonblank
department: string     // nonblank
```

Optional authorization field:

```text
active: boolean        // omitted or true permits login; false denies it
```

Teacher roles are provisioned manually by an authorized administrator. No browser endpoint can create an account, assign a role, or modify this document.

## Teacher assignment contract

Collection:

```text
teacherAssignments/{assignmentId}
```

Required fields for an active result:

```text
teacherUid: string     // must match authenticated UID
subjectId: string      // nonblank
batchId: string        // nonblank
active: true
```

Optional returned field:

```text
academicYear: string
```

## Error behavior

- Missing/malformed Authorization header: HTTP 401.
- Invalid, expired, or revoked token: HTTP 401 with a stable safe code.
- Missing, incomplete, inactive, student, admin, or other-role profile: HTTP 403.
- Firebase mode disabled: HTTP 503.
- Admin/Firestore availability failure: HTTP 503 without internal details.
- Empty assignments: HTTP 200 with `assignments: []`.
- Frontend wrong password and network failures show distinct messages and never fall back to Demo Mode.

## Testing

- Unit/integration tests exercise the real Express middleware and routes using controlled boundary fakes for Firebase token verification and Firestore.
- Frontend controller tests exercise successful login, backend denial, wrong-password mapping, network failure, explicit demo mode, logout, and restoration without loading the remote SDK.
- Firebase Emulator Suite tests create disposable teacher/student Auth users and Firestore profiles, then verify real Admin token/profile/assignment behavior, invalid credentials, missing profiles, empty assignments, and backend reinitialization.
- The existing Android JVM suite, older teacher-dashboard tests, website syntax/navigation checks, original-folder hashes, build, and localhost smoke tests remain regression gates.

## Explicitly unverified in this milestone

- Production teacher sign-in cannot be claimed until the owner registers a Firebase Web App, supplies its runtime configuration, provisions a teacher profile/assignments, supplies local Admin credentials, and enters the password directly.
- No production Firestore write, rules deployment, Hosting deployment, billing change, session/attendance action, or BLE operation is permitted.

## Approaches considered

1. **Chosen: Web Auth plus Express/Admin ID-token verification.** Matches the requested flow, centralizes authorization, and keeps privileged Firestore access out of the browser.
2. **Rejected: direct browser Firestore teacher queries.** Existing rules are not assignment-scoped enough, and UI checks cannot secure data.
3. **Deferred: server session cookies.** They can be appropriate later but add cookie, CSRF, and session lifecycle work beyond this ID-token milestone.

