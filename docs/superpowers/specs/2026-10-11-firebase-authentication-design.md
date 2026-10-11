# SmartAttend Firebase Student Authentication Design

## Purpose and success criteria

This milestone replaces the misleading single demo-login action with two explicit and independent paths: production Firebase Email/Password authentication for authorized students, and local Demo Mode entered only by deliberate user action. It preserves the existing Room-backed demo experience while preventing demo state or demo data from being mistaken for a production identity.

The milestone succeeds when Firebase credentials are passed only to `FirebaseAuth.signInWithEmailAndPassword`, the returned UID is authorized through the matching `users/{uid}` Firestore profile, demo login never runs implicitly after a Firebase failure, session restoration distinguishes Firebase from Demo Mode, logout and password reset work, automated tests and `assembleDebug` pass, and emulator results are reported honestly. A successful real login is not claimed unless an actual request using a user-supplied test account succeeds.

## Current defect and root cause

`LoginScreen` currently exposes one button labeled `Continue in demo mode`. Both identifier and password fields are prefilled with demo values, and the button calls `SmartAttendViewModel.login`. That method runs only `LoginValidator`, sets `DemoPreferences.logged_in = true`, and changes `isLoggedIn` in UI state. It never calls Firebase Authentication.

At startup, `SmartAttendViewModel` converts the same saved Boolean directly into `SmartAttendUiState.isLoggedIn`. `SmartAttendApp` treats that Boolean as the entire authentication decision and immediately opens the student navigation graph. There is no authentication repository, no Firebase-versus-demo session type, no Firestore profile authorization, and no Firebase session restoration. Firebase SDK initialization therefore cannot make the existing login form authenticate.

## Architectural approach

The single Android module keeps its existing `core`, `domain`, `data`, and `ui` structure. Authentication is added behind an `AuthenticationRepository` contract so Compose and the ViewModel do not depend directly on Firebase classes.

The implementation contains these focused units:

- `AuthenticationRepository`: suspend operations for real login, session restoration, logout, and password reset.
- `FirebaseAuthenticationRepository`: the only production class that calls Firebase Auth and reads `users/{uid}` from Firestore.
- Authentication domain results and session modes: explicit, exhaustively handled states for signed out, demo, authenticating/restoring, authorized Firebase student, and recoverable errors.
- `AuthenticationSessionStore`: DataStore-backed persistence for the explicit local Demo Mode marker only. It never stores passwords, Firebase tokens, roles, or profile documents.
- `AuthenticationCoordinator`: testable orchestration for login, profile authorization, demo entry/exit, restoration precedence, logout, and password reset.
- `SmartAttendViewModel`: adapts coordinator state to existing app UI state and keeps student data workflows unchanged.

Direct Firebase calls from composables or the ViewModel are rejected because they couple UI state to callback APIs and make restoration/error/authorization behavior difficult to test. Full attendance synchronization is also excluded; this milestone authenticates the profile but does not convert local demo attendance into official data.

## Authentication and profile authorization

Real login accepts an email and password only from the Firebase form. It validates nonblank, syntactically plausible email and nonblank password locally, clears any Demo Mode marker, sets an authenticating state, and calls `FirebaseAuth.signInWithEmailAndPassword(email, password)`.

After sign-in, the repository obtains the Firebase UID from the returned/current user and reads exactly `users/{uid}`. A profile is authorized only when all conditions hold:

- The Firestore document exists at the authenticated UID.
- The document field `role` is exactly `student`, case-sensitive.
- `name`, `email`, `rollNumber`, `department`, and `batch` are nonblank strings.
- The app constructs `Student.id` from the authenticated UID, never from a client-controlled profile field.
- If a profile email is present, it is displayed as stored by the trusted administrator; it does not determine authorization.

Missing, malformed, unreadable, teacher, admin, or any other role profile never enters the student navigation graph. Missing/malformed/wrong-role profiles produce an Account Not Configured or Not Authorized result and sign out the Firebase user. The Android client exposes no method to create or modify `users/{uid}`, roles, sessions, finalized attendance, or audit records.

Firestore rules remain unchanged and deny client profile writes. The existing `users/{userId}` read rule already restricts a student to the document matching `request.auth.uid`.

## Session model and restoration precedence

Firebase Auth remains the secure owner of Firebase credential/token persistence. DataStore stores only whether the user explicitly chose Demo Mode.

Startup restoration follows this order:

1. Enter a restoring state so neither demo nor stale profile content flashes as authenticated, and wait for the first Firebase `AuthStateListener` callback so restoration does not race Firebase initialization.
2. If the resolved Firebase current user exists, load and authorize `users/{uid}`. A valid student becomes the Firebase session regardless of an old demo marker; the demo marker is cleared.
3. If no Firebase user exists and the persisted Demo Mode marker is true, restore Demo Mode with the local demo student.
4. Otherwise show the signed-out login screen.

A Firebase login attempt always clears the Demo Mode marker before the request. Authentication or profile failure returns to signed out with a specific error and never enters Demo Mode. A failed restoration caused by a transient network error remains signed out with a retryable message; it does not display demo identity. An unauthorized/missing profile signs out locally to prevent later navigation bypass.

Demo logout clears only the Demo Mode marker. Firebase logout calls `FirebaseAuth.signOut`, clears the demo marker defensively, removes the real student from UI state, and returns to signed out. No logout operation deletes Room demo records or app preferences unrelated to authentication.

## User interface

The login screen keeps the existing Material 3 visual identity but removes prefilled credentials and separates actions:

- Email field.
- Password field with hidden text.
- Primary `Sign in with Firebase` button.
- `Forgot password?` action using the entered email.
- Visually separate secondary `Continue in Demo Mode` action with explicit local/demo explanation.
- Progress state disables repeated submissions.
- Inline errors distinguish validation, invalid credentials, network failure, rate limiting, account-not-configured, unauthorized-role, and unexpected service failure.

The app shell receives the active session mode. Demo Mode retains `DEMO - NOT VERIFIED BY BLE`. An authorized Firebase profile uses the real student name and profile fields, but all still-local attendance/timetable/notices remain visibly labeled demo until a later synchronization milestone. This prevents a real identity from making local sample attendance appear official.

The Profile screen displays `Firebase authenticated student` for a real session and `DEMO LOCAL IDENTITY` for Demo Mode. Its action is `Sign out` for Firebase and `Exit Demo Mode` for demo, providing an explicit path from demo back to real login.

## Password reset and error handling

Password reset validates the email field and calls `FirebaseAuth.sendPasswordResetEmail`. Success shows a neutral confirmation. Error mapping avoids displaying raw exception internals or credentials.

Authentication errors map to stable user messages:

- Invalid email: `Enter a valid email address.`
- Invalid credentials, wrong password, disabled/deleted/nonexistent account: `Incorrect email or password.` except explicitly disabled accounts, which state that the account is disabled.
- Network failure: `No internet connection. Check your connection and try again.`
- Too many attempts: `Too many attempts. Wait a while and try again.`
- Missing profile: `Account Not Configured. Ask your administrator to create your student profile.`
- Wrong role: `This account is not authorized for the student app.`
- Malformed profile: `Account Not Configured. The student profile is incomplete.`
- Firestore permission denied: `Your student profile could not be authorized.`
- Unexpected failures: a generic retry message without sensitive technical content.

Errors never trigger Demo Mode. Passwords are never logged, persisted, placed in test fixtures, committed, or included in status reports.

## Existing data behavior

Room, existing DAOs/entities, DataStore settings, demo seeding, correction workflows, simulated BLE logic, and current navigation destinations remain intact. This milestone replaces only authentication/session ownership and the student profile displayed for an authorized Firebase session.

The local repository continues to emit seeded demo subjects, attendance, history, schedule, notices, corrections, and device state. Those records retain `simulated`/demo provenance. No Firestore attendance, timetable, notice, correction, device, or BLE synchronization is added.

## Test strategy

Pure JVM tests use a fake `AuthenticationRepository` and fake session store. They cover:

- Valid Firebase credentials plus a valid `student` profile enter the Firebase student session.
- Invalid credentials remain signed out with no demo fallback.
- Network failures remain signed out with a retryable error.
- Missing, malformed, teacher, admin, and unknown-role profiles are rejected and signed out.
- Demo Mode is entered only through the explicit demo action.
- Starting a Firebase login after Demo Mode clears the demo marker and cannot be overridden by it.
- Startup prioritizes a valid Firebase current user over a stale demo marker.
- Startup restores Demo Mode only when no Firebase current user exists.
- Firebase and Demo logout both return to signed out and clear the appropriate state.
- Password reset validates email and maps success/failure without exposing account secrets.
- Firebase profile mapping always uses the authenticated UID and rejects missing required fields.

Android instrumentation tests verify Firebase SDK initialization and Compose/login integration where stable. Existing Room/repository tests remain green. Completion requires fresh successful runs of `testDebugUnitTest`, `assembleDebug`, `lintDebug`, and emulator instrumentation when an emulator is available.

Real-service verification uses no hardcoded credential. The user enters the existing test account directly into the emulator. Verification records only whether authentication succeeded, whether the expected UID-backed authorized profile loaded, and which public error category appeared. Incorrect-password testing also requires user-supplied test input or a controlled Firebase Auth Emulator; the project must not fabricate or store the real password.

## Firebase configuration required from the project owner

- Email/Password sign-in remains enabled.
- The test Authentication user already exists.
- Firestore contains `users/{authenticationUid}` with exactly `role: "student"` and nonblank `name`, `email`, `rollNumber`, `department`, and `batch` fields.
- Existing production security rules remain restrictive; no public read/write rule is introduced.
- The project owner supplies credentials only through the running app UI, never source control or chat history.

## Explicit non-goals

- Creating Firebase Authentication accounts from the student app.
- Creating or modifying Firestore student profiles from the Android client.
- Synchronizing attendance, subjects, timetable, notices, corrections, device registrations, or BLE evidence.
- Deploying or weakening Firestore rules.
- Adding teacher/admin navigation or client-side role elevation.
- Claiming production attendance security, server finalization, ESP32 verification, or physical-device verification.

## Acceptance checklist

- Real and demo login actions are distinct and cannot call each other's implementation.
- Firebase login never falls back to Demo Mode.
- Authorized Firebase login uses the Firebase UID and trusted Firestore student profile.
- Missing or unauthorized profiles cannot reach student navigation.
- Old demo state cannot override a Firebase login attempt or restored Firebase session.
- Logout, Demo Mode exit, password reset, and restart restoration behave as specified.
- Existing offline demo data and features remain available and visibly labeled.
- Firestore rules are unchanged and no new client write capability exists.
- Automated tests, lint, APK assembly, and available emulator checks are recorded with exact outcomes.
- Real Firebase login is reported as successful only after an actual request succeeds.
