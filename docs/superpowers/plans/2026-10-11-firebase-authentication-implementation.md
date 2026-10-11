# SmartAttend Firebase Student Authentication Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add secure Firebase Email/Password student authentication with UID-bound Firestore profile authorization while keeping explicit offline Demo Mode separate and preserving all existing student features.

**Architecture:** A Firebase-free domain contract models authentication results, user-facing error categories, session modes, and profile validation. A Firebase repository is the sole SDK boundary; a coordinator owns Firebase-versus-demo precedence and a DataStore session store persists only explicit Demo Mode. `SmartAttendViewModel` and Compose adapt those states without synchronizing or writing production student data.

**Tech Stack:** Kotlin 2.2.0, coroutines 1.10.1, Firebase Auth 24.2.0, Firestore 26.6.0, Preferences DataStore 1.2.1, Jetpack Compose/Material 3, JUnit 4, Android instrumentation.

**Spec:** `docs/superpowers/specs/2026-10-11-firebase-authentication-design.md`

## Global Constraints

- Keep `applicationId` and Firebase Android registration at `com.smartattend.app`.
- Use Firebase main modules, never deprecated Firebase `-ktx` modules or Analytics.
- Never persist, log, hardcode, or commit an email/password credential or Firebase token.
- Read the authorized profile only from `users/{authenticatedUid}` and require `role == "student"` plus all five nonblank profile fields.
- Add no Firestore writes and do not modify or deploy `firestore.rules`.
- Firebase failure must remain signed out; it must never fall back to Demo Mode.
- Firebase session restoration has precedence over any saved Demo Mode marker.
- Existing Room demo data, DataStore settings, simulated BLE, navigation, and demo provenance remain intact.
- A successful real Firebase login may be claimed only after an actual request with a user-supplied account succeeds.

## Review Focus

- Firebase signs in but the profile read is denied or fails: remain outside navigation, sign out locally, and show an authorization/network category.
- Firebase SDK callback/restoration races app startup: wait for the first auth-state callback before consulting Demo Mode.
- Room's demo student Flow emits after Firebase authorization: it must not overwrite the authenticated student profile.
- A stale legacy/demo session exists while a Firebase user is restored: Firebase wins and the demo marker is cleared.
- Rapid repeated login/reset taps: busy UI disables duplicate actions and no request causes implicit Demo Mode.

---

### Task 1: Authentication contracts, validation, and profile authorization mapping

**Files:**
- Create: `app/src/main/java/com/smartattend/app/domain/auth/AuthenticationModels.kt`
- Create: `app/src/main/java/com/smartattend/app/data/firebase/StudentProfileMapper.kt`
- Modify: `app/src/main/java/com/smartattend/app/domain/auth/LoginValidator.kt`
- Create: `app/src/test/java/com/smartattend/app/domain/auth/FirebaseLoginValidatorTest.kt`
- Create: `app/src/test/java/com/smartattend/app/data/firebase/StudentProfileMapperTest.kt`

**Interfaces:**
- Produces: `AuthSessionMode`, `AuthError`, `AuthenticationResult`, `PasswordResetResult`, `AuthenticationRepository`, `AuthenticationSessionStore`, `LoginValidator.validateFirebase(email, password)`, and `StudentProfileMapper.map(uid, data): AuthenticationResult`.
- Consumes: existing `Student` domain model.

- [ ] **Step 1: Write failing validation and profile-mapping tests**

Tests assert valid email acceptance; blank/invalid email and blank password rejection; UID-owned valid student mapping; missing document/field, blank field, teacher/admin/unknown role rejection; and ignoring any client document `id` in favor of the authenticated UID.

- [ ] **Step 2: Run the focused tests and verify RED**

Run: `build-smartattend.bat testDebugUnitTest --tests "*FirebaseLoginValidatorTest" --tests "*StudentProfileMapperTest" --no-daemon`

Expected: compilation failure because the new contracts and mapper do not exist.

- [ ] **Step 3: Implement the minimal domain contracts and mapper**

`AuthenticationResult` distinguishes `Authorized(Student)`, `NoSession`, and `Failure(AuthError)`. `AuthError` provides stable categories, not raw exception text. `StudentProfileMapper` requires `role`, `name`, `email`, `rollNumber`, `department`, and `batch` and always assigns `Student.id = uid`.

- [ ] **Step 4: Run the focused tests and full JVM suite**

Expected: focused tests and all existing tests pass.

- [ ] **Step 5: Commit Task 1**

Commit message: `feat: define Firebase student authentication contract`

### Task 2: Demo/Firebase session precedence coordinator

**Files:**
- Create: `app/src/main/java/com/smartattend/app/domain/auth/AuthenticationCoordinator.kt`
- Modify: `app/src/main/java/com/smartattend/app/data/local/AppPreferencesRepository.kt`
- Create: `app/src/test/java/com/smartattend/app/domain/auth/AuthenticationCoordinatorTest.kt`

**Interfaces:**
- Consumes: Task 1 `AuthenticationRepository`, `AuthenticationSessionStore`, `AuthenticationResult`, `PasswordResetResult`, `AuthSessionMode`.
- Produces: `AuthenticationCoordinator.restore()`, `signIn(email,password)`, `enterDemoMode()`, `logout()`, and `sendPasswordReset(email)` returning explicit `AuthenticationState`/action results.

- [ ] **Step 1: Write failing coordinator tests**

Tests assert explicit Demo Mode only; failed real login never falls back; real login clears demo; Firebase restoration beats stale demo; no Firebase user restores demo only when marked; missing/wrong-role/network failures remain signed out; logout signs out Firebase and clears demo; reset validation and success/error mapping; and repeated state transitions do not retain a real profile after logout.

- [ ] **Step 2: Run the focused suite and verify RED**

Run: `build-smartattend.bat testDebugUnitTest --tests "*AuthenticationCoordinatorTest" --no-daemon`

Expected: compilation failure because the coordinator does not exist.

- [ ] **Step 3: Implement coordinator and DataStore session store**

`AppPreferencesRepository` implements `AuthenticationSessionStore` with `authentication_demo_mode`. Passwords/tokens/roles are never stored. `enterDemoMode` signs out any Firebase state before setting the marker. `signIn` clears the marker before calling the repository.

- [ ] **Step 4: Run focused and full JVM suites**

Expected: all coordinator and existing tests pass.

- [ ] **Step 5: Commit Task 2**

Commit message: `feat: separate Firebase and demo sessions`

### Task 3: Firebase Auth and Firestore repository adapter

**Files:**
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/smartattend/app/data/firebase/FirebaseAuthenticationRepository.kt`
- Create: `app/src/main/java/com/smartattend/app/data/firebase/FirebaseErrorMapper.kt`
- Create: `app/src/test/java/com/smartattend/app/data/firebase/FirebaseErrorMapperTest.kt`
- Modify: `app/src/androidTest/java/com/smartattend/app/firebase/FirebaseSdkInitializationTest.kt`

**Interfaces:**
- Consumes: Task 1 `AuthenticationRepository`, results/errors, and `StudentProfileMapper`.
- Produces: production repository using `FirebaseAuth.signInWithEmailAndPassword`, first `AuthStateListener` restoration callback, `users/{uid}.get()`, `FirebaseAuth.signOut`, and `sendPasswordResetEmail`.

- [ ] **Step 1: Write failing error-mapping tests**

Tests map invalid email/credentials, disabled account, network failure, too-many-requests, Firestore permission denied/unavailable, and unknown errors to stable `AuthError` values without raw secrets.

- [ ] **Step 2: Run the focused test and verify RED**

Expected: compilation failure because `FirebaseErrorMapper` does not exist.

- [ ] **Step 3: Implement the Firebase adapter**

Add `kotlinx-coroutines-play-services:1.10.1` for cancellable `Task.await()`. Await the first `AuthStateListener` callback during restore. After sign-in/restore, read only `users/{uid}` and invoke the mapper. Sign out on missing/malformed/unauthorized/unreadable profile. Perform no Firestore write.

- [ ] **Step 4: Verify focused tests, compile, and SDK instrumentation**

Run focused JVM tests, `compileDebugKotlin`, and the existing Firebase initialization instrumentation test on an available emulator.

- [ ] **Step 5: Commit Task 3**

Commit message: `feat: authenticate and authorize Firebase students`

### Task 4: ViewModel, login UI, profile UI, and navigation integration

**Files:**
- Modify: `app/src/main/java/com/smartattend/app/ui/SmartAttendViewModel.kt`
- Modify: `app/src/main/java/com/smartattend/app/ui/SmartAttendApp.kt`
- Modify: `app/src/main/java/com/smartattend/app/ui/login/LoginScreen.kt`
- Modify: `app/src/main/java/com/smartattend/app/ui/profile/ProfileScreen.kt`
- Modify: `app/src/main/java/com/smartattend/app/ui/components/CommonComponents.kt`
- Create: `app/src/androidTest/java/com/smartattend/app/ui/login/LoginScreenTest.kt`

**Interfaces:**
- Consumes: Tasks 1–3 coordinator, session states, real student profile, existing offline repository flows.
- Produces: separate `firebaseLogin`, `enterDemoMode`, `sendPasswordReset`, and `logout` UI actions; mode-aware banner/profile; restoration/busy/error UI; real profile protected from Room demo Flow overwrites.

- [ ] **Step 1: Write failing Compose tests for separate actions**

Tests assert empty credential fields, distinct Firebase and Demo buttons, only the selected callback fires, busy state disables both, password reset uses the entered email, and error text is rendered without entering demo.

- [ ] **Step 2: Run the focused instrumentation test and verify RED**

Expected: compile/test failure because the current LoginScreen has a single demo callback and prefilled values.

- [ ] **Step 3: Integrate coordinator and rebuild the login/profile surfaces**

Start in restoring mode; run restoration once; expose authentication loading/error/message state; ignore Room student emissions while in Firebase mode; use a mode-aware banner; and label real identity separately while keeping all offline data visibly demo.

- [ ] **Step 4: Run focused instrumentation and full JVM suites**

Expected: new Compose tests, coordinator tests, and all regression tests pass.

- [ ] **Step 5: Commit Task 4**

Commit message: `feat: add separate Firebase and demo login flows`

### Task 5: Full verification, real-service boundary, and documentation

**Files:**
- Modify: `README.md`
- Modify: `docs/FIREBASE_SETUP.md`
- Modify: `PROJECT_PLAN.md`
- Modify: `PROJECT_STATUS.md`
- Modify: `TEST_REPORT.md`

**Interfaces:**
- Consumes: completed authentication flow and actual verification outputs.
- Produces: exact build/test record, required `users/{uid}` schema, beginner login instructions, APK path/hash, and explicit real-login verified/not-verified status.

- [ ] **Step 1: Run complete verification**

Run: `build-smartattend.bat testDebugUnitTest assembleDebug lintDebug --no-daemon`, then `connectedDebugAndroidTest` on the Pixel 7 AVD when available. Install and cold-launch the APK; confirm separate login buttons and explicit Demo Mode exit.

- [ ] **Step 2: Exercise safe authentication failures**

On emulator, confirm local invalid-email handling without network and a controlled failed Firebase request only when it cannot affect a real account. Never invent or store the user's real password.

- [ ] **Step 3: Verify a real account only with user-entered credentials**

If credentials can be entered directly into the emulator by the project owner and `users/{uid}` is configured, observe successful real login/profile rendering and restart restoration without recording secrets. Otherwise report this exact external verification blocker and do not claim success.

- [ ] **Step 4: Update status and setup documentation from evidence**

Record tests, warnings, APK size/hash, emulator results, Firestore profile fields, unchanged rules, and real-service verification boundary.

- [ ] **Step 5: Final diff/security review and commit**

Confirm no credentials, Analytics/KTX dependencies, Firestore writes, or rule changes; run `git diff --check`; commit with `docs: record Firebase authentication verification`.
