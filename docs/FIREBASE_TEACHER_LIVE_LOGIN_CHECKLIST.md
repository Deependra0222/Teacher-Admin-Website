# SmartAttend First Real Teacher Login Checklist

Use this checklist only for the existing Firebase project `smartattend-2a5ce`. It prepares one local, read-only authorization test. It does not create accounts, change rules, write attendance, deploy, or start Milestone 3.

## 1. Confirm the existing Firebase Web App

1. Open Firebase Console and select the project whose **Project ID** is exactly `smartattend-2a5ce`.
2. Open **Project settings** (gear icon) → **General** → **Your apps**.
3. Select the existing Teacher Web App. If there is no Web App, stop and tell the developer; do not create another Firebase project.
4. In **SDK setup and configuration**, select **Config** and keep that page open.
5. Do not paste any Firebase Admin/service-account credential into the website. The Web App block contains public application identifiers only.

## 2. Fill the ignored local backend environment

Open `D:\Smart Attend\backend\.env`. It is ignored by Git. The verified project ID, Web project ID, storage bucket, and messaging sender ID are already prepared. Copy only the three still-missing values from the existing Teacher Web App's `firebaseConfig` block, then change the mode:

```dotenv
FIREBASE_MODE=adc
FIREBASE_PROJECT_ID=smartattend-2a5ce
FIREBASE_WEB_API_KEY=<copy apiKey exactly>
FIREBASE_WEB_AUTH_DOMAIN=<copy authDomain exactly>
FIREBASE_WEB_PROJECT_ID=smartattend-2a5ce
FIREBASE_WEB_STORAGE_BUCKET=smartattend-2a5ce.firebasestorage.app
FIREBASE_WEB_MESSAGING_SENDER_ID=1065635060805
FIREBASE_WEB_APP_ID=<copy appId exactly>
```

Do not put quotes around values unless the Console value itself contains them. Do not commit `.env`, send it in chat, or copy any service-account JSON into this repository.

Required by the current runtime: API key, Auth domain, Web project ID, and Web App ID. The audit also checks that the numeric project portion of the Web App ID and the messaging sender ID match Android project number `1065635060805`.

The missing values can be obtained in either read-only way:

1. Firebase Console → **Project settings** → **General** → **Your apps** → select the existing Teacher Web App → **SDK setup and configuration** → **Config**; or
2. use the local Firebase CLI:

```powershell
cd "D:\Smart Attend\backend"
.\node_modules\.bin\firebase.cmd login
.\node_modules\.bin\firebase.cmd apps:list WEB --project smartattend-2a5ce
.\node_modules\.bin\firebase.cmd apps:sdkconfig WEB <existing-web-app-id> --project smartattend-2a5ce
```

The local Firebase CLI is currently not signed in, and the in-app Firebase Console opened at a Google sign-in page, so these values could not be read automatically. Do not use the Android App ID from `google-services.json`; a Firebase Web App ID must contain `:web:`.

## 3. Configure local Firebase Admin identity

The backend uses Firebase Admin `applicationDefault()` plus the explicit `smartattend-2a5ce` project ID. On this inspected machine, Google Application Default Credentials are currently absent and `gcloud` is not installed/on `PATH`.

Choose one supported local ADC method. No credential belongs in the repository.

### Option A — service-account impersonation (preferred when IAM access permits)

1. Install the official Google Cloud CLI outside the SmartAttend repository.
2. Identify an existing project service account that has the necessary Firebase Authentication read and Firestore read access.
3. Ensure your Google account has permission to impersonate it.
4. Run:

```powershell
gcloud auth application-default login --impersonate-service-account=SERVICE_ACCOUNT_EMAIL
```

Node.js supports impersonated ADC and no private-key file is downloaded.

### Option B — local service-account key (simpler, but protect it carefully)

1. In Firebase Console → **Project settings** → **Service accounts**, manually generate a Firebase Admin private key only if you accept the security responsibility.
2. Save the JSON outside `D:\Smart Attend`, for example in a private user-only directory.
3. In the same PowerShell window that will run the backend:

```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS = "C:\path-outside-repo\firebase-admin.json"
```

Do not add the JSON file or its contents to `.env`, source control, the website, or chat. Revoke/delete the key in Google Cloud IAM when it is no longer needed.

`firebase login` authenticates only the Firebase CLI and is not Admin ADC. Also, plain end-user credentials from `gcloud auth application-default login` can require a custom Desktop OAuth client for Firebase Authentication; do not assume that command alone is sufficient for this backend's revoked-token checks.

## 4. Confirm the teacher account and profile

In Firebase Authentication, the Email/Password account must belong to `smartattend-2a5ce`. Copy its UID. In Cloud Firestore, create or verify exactly this document path:

```text
users/{Firebase Authentication UID}
```

The backend performs a direct document read using the verified token UID. It does not search by email.

Required fields:

| Field | Firestore type | Value rule |
|---|---|---|
| `role` | string | exactly `teacher` in lowercase |
| `name` | string | nonblank teacher name |
| `email` | string | nonblank registered teacher email |
| `department` | string | nonblank department, for example `EXTC` |

Optional field:

| Field | Firestore type | Value rule |
|---|---|---|
| `active` | boolean | omit or set `true`; `false` denies login |

Example only—replace the name/email with the real teacher's values:

```json
{
  "role": "teacher",
  "name": "Teacher Name",
  "email": "registered.teacher@example.edu",
  "department": "EXTC",
  "active": true
}
```

## 5. Check project consistency before starting

In PowerShell:

```powershell
cd "D:\Smart Attend\backend"
npm run verify:firebase-project
```

Continue only if it prints all three project IDs as `smartattend-2a5ce` and ends with:

```text
LIVE PROJECT CONFIG READY: YES
```

This confirms only the Android/Admin/Web project configuration. ADC from section 3 must also exist. If it reports `Backend mode: disabled`, `.env` is still disabled or was not loaded. If it reports a mismatch, correct `.env`; do not change the Android file.

## 6. Start and perform the first login

```powershell
cd "D:\Smart Attend\backend"
npm run dev
```

Wait for both messages:

```text
SmartAttend teacher server listening at http://localhost:3000
Firebase teacher authentication mode: adc
```

Open `http://localhost:3000/ERP.html`. Enter the Firebase Authentication email and password, then use the real sign-in button. Do not use **Continue in Demo Mode** for this test.

A successful test displays the real authorized teacher name/department and no demo student rows. Logout must return to the login page; re-opening a protected page must require authentication again.

## 7. Diagnose safe error messages

| Symptom/message | Check |
|---|---|
| Firebase Web sign-in is not configured | `FIREBASE_MODE` and required Web values; restart the backend after editing `.env`. |
| Email or password is incorrect | Authentication account/email/password and Email/Password provider. Never hardcode the password. |
| No authorized teacher profile exists | Document path must be `users/{the signed-in Auth UID}`. |
| Teacher profile is incomplete | Check exact field names/types and nonblank values in section 4. |
| Not authorized for teacher portal | `role` is not exactly lowercase string `teacher`. |
| Teacher account is inactive | Remove `active` or set boolean `true`. |
| Teacher authorization is temporarily unavailable | Backend terminal, ADC presence, IAM access, network, Firestore availability, and matching project ID. |
| Backend unavailable | Confirm `npm run dev` is still running at port 3000. |

Do not weaken Firestore rules to fix backend authorization. Firebase Admin performs trusted server reads and the backend still enforces UID, role, active state, and assignment scope itself.
