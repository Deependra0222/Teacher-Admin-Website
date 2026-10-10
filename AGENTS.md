# SmartAttend Workspace Rules

- Treat the supplied build-kit documents as the product source of truth.
- Work in small, buildable milestones and preserve every verified working feature.
- Prefer beginner-readable Kotlin, Jetpack Compose, Material 3, and focused files with clear names.
- Use test-first development for business logic and bug fixes; report only commands actually run.
- Label all local/sample attendance data as `DEMO` and `NOT VERIFIED BY BLE`.
- Never describe simulated presence as secure physical attendance or proxy-proof evidence.
- Never add passwords, signing keys, service-account keys, or other secrets to source control.
- Do not enable Firebase writes or BLE behavior without the required configuration/hardware and verification.
- Keep Firebase-authorized attendance separate from student-reported presence evidence.
- Update `PROJECT_STATUS.md` after each milestone with exact tests, blockers, and the next action.

