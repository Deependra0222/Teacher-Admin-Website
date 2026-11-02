# SmartAttend Real Teacher Login Readiness Design

## Intent

Prepare Milestone 2 for the owner's first live Firebase teacher login without creating accounts, changing the Android registration, writing production Firestore data, changing rules, deploying, or starting Milestone 3.

## Verified starting point

- The current ignored `app/google-services.json` identifies Firebase project `smartattend-2a5ce`, Android package `com.smartattend.app`, and storage bucket `smartattend-2a5ce.firebasestorage.app`.
- The Android Gradle `applicationId` and namespace are both `com.smartattend.app`.
- The backend's ignored `.env` remains in safe `disabled` mode. It now contains only verified project-wide public values; Web API key, Auth domain, and Web App ID remain absent.
- `smartattend-e9a35` appears only in teacher-side examples, test fixtures, and documentation. It is not active runtime configuration.
- Application Default Credentials are absent, and `gcloud` is not installed/on PATH. No private credential should be added to source.

## Goals

- Correct stale teacher-side project examples to `smartattend-2a5ce` while leaving Android configuration untouched.
- Add a safe local audit command that checks Android, Admin, and Web project IDs without printing API keys, App IDs, tokens, or credentials.
- Preserve the existing modular Web SDK, Admin SDK, ID-token, UID profile, role, session, Demo Mode, and logout behavior.
- Confirm and improve automated coverage for every requested positive and negative authentication path.
- Provide an exact beginner checklist for the values the owner must copy from the existing Firebase Web App and for local Admin credentials.

## Non-goals and safety boundaries

- No new Firebase project or app registration.
- No edit to `app/google-services.json`, Android source/Gradle files, live Firestore rules, Authentication users, or Firestore documents.
- No production login request during preparation.
- No service-account JSON committed or copied into the repository.
- No teacher business data, attendance synchronization, deployment, billing change, or ESP32/BLE work.

## Approach

### Chosen: explicit cross-client audit command

Add a small pure project-consistency check plus a CLI that reads the ignored Android registration and backend environment, prints only non-sensitive project/configuration status, and fails if live ADC Admin/Web IDs do not equal the Android project. Emulator mode remains intentionally isolated on a `demo-*` project and is reported as non-live rather than treated as a mismatch. This catches future production drift without coupling production startup to the Android file.

### Rejected: make backend startup read `app/google-services.json`

This would catch mismatch automatically but incorrectly couples a deployable server to an Android-only file that should not exist on the server.

### Rejected: documentation-only correction

This fixes today's typo but allows the same security-sensitive mismatch to recur silently.

## Configuration contract

Known, verified values:

```text
FIREBASE_PROJECT_ID=smartattend-2a5ce
FIREBASE_WEB_PROJECT_ID=smartattend-2a5ce
FIREBASE_WEB_STORAGE_BUCKET=smartattend-2a5ce.firebasestorage.app
FIREBASE_WEB_MESSAGING_SENDER_ID=1065635060805
```

The owner must copy these exact values from Firebase Console's existing Web App; they will not be guessed:

```text
FIREBASE_WEB_API_KEY
FIREBASE_WEB_AUTH_DOMAIN
FIREBASE_WEB_APP_ID
```

Live local mode is enabled only after all required Web values and local Admin credentials are ready:

```text
FIREBASE_MODE=adc
```

## Authentication/profile contract

The backend verifies the Firebase ID token with revocation checking, takes the UID only from the decoded token, and reads `users/{uid}` directly. It never queries profiles by email and never trusts a frontend role.

Required profile fields are exact lowercase `role: "teacher"` and nonblank string `name`, `email`, and `department`. Optional `active` must be boolean and denies access when `false`.

## Verification

- Unit tests cover configuration absence/mismatch, valid teacher, student, missing/wrong/incomplete profile, invalid/expired/revoked token, Auth/Firestore failure, and logout.
- Auth/Firestore emulator tests cover positive/negative server flows without production writes.
- Android/old teacher/original backup/rules remain unchanged by Git diff.
- The live owner login remains explicitly not run until the owner reports the account/profile and supplies Web config plus ADC locally.
