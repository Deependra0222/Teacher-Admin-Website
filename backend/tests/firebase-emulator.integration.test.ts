import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import path from "node:path";
import { after, before, describe, test } from "node:test";
import { deleteApp, initializeApp, type App } from "firebase-admin/app";
import { getFirestore, type Firestore } from "firebase-admin/firestore";
import request from "supertest";

import { createApp } from "../src/app.js";
import {
  createFirebaseAdminRuntime,
  type FirebaseAdminRuntime,
} from "../src/firebase/admin-runtime.js";

const projectId = process.env.GCLOUD_PROJECT ?? "demo-smartattend";
const authHost = process.env.FIREBASE_AUTH_EMULATOR_HOST ?? "127.0.0.1:9099";
const firestoreHost = process.env.FIRESTORE_EMULATOR_HOST ?? "127.0.0.1:8080";
const password = `Emulator-only-${randomUUID()}-A1!`;
const staticDirectory = path.resolve(import.meta.dirname, "../../teacher-website");

interface EmulatorAccount {
  readonly uid: string;
  readonly email: string;
  readonly idToken: string;
}

let seedApp: App;
let firestore: Firestore;
let runtime: FirebaseAdminRuntime;
let teacher: EmulatorAccount;
let student: EmulatorAccount;
let missingProfile: EmulatorAccount;
let incorrectRole: EmulatorAccount;

async function authRequest(action: string, body: Record<string, unknown>) {
  return fetch(
    `http://${authHost}/identitytoolkit.googleapis.com/v1/accounts:${action}?key=demo-api-key`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    },
  );
}

async function createAccount(email: string): Promise<EmulatorAccount> {
  const response = await authRequest("signUp", {
    email,
    password,
    returnSecureToken: true,
  });
  if (!response.ok) {
    throw new Error(`Auth emulator signup failed: ${await response.text()}`);
  }
  const body = (await response.json()) as {
    localId: string;
    email: string;
    idToken: string;
  };
  return { uid: body.localId, email: body.email, idToken: body.idToken };
}

function authorization(idToken: string) {
  return { Authorization: `Bearer ${idToken}` };
}

function backend() {
  return createApp({
    allowedOrigins: ["http://localhost:3000"],
    staticDirectory,
    teacherAuthorization: runtime.teacherAuthorization,
  });
}

before(async () => {
  process.env.FIREBASE_AUTH_EMULATOR_HOST = authHost;
  process.env.FIRESTORE_EMULATOR_HOST = firestoreHost;

  runtime = createFirebaseAdminRuntime({
    mode: "emulator",
    projectId,
    authEmulatorHost: authHost,
    firestoreEmulatorHost: firestoreHost,
  })!;
  seedApp = initializeApp({ projectId }, `smartattend-integration-seed-${process.pid}`);
  firestore = getFirestore(seedApp);

  [teacher, student, missingProfile, incorrectRole] = await Promise.all([
    createAccount("teacher@smartattend.test"),
    createAccount("student@smartattend.test"),
    createAccount("missing-profile@smartattend.test"),
    createAccount("incorrect-role@smartattend.test"),
  ]);

  await Promise.all([
    firestore.doc(`users/${teacher.uid}`).set({
      role: "teacher",
      name: "Emulator Teacher",
      email: teacher.email,
      department: "Computer Science",
      active: true,
    }),
    firestore.doc(`users/${student.uid}`).set({
      role: "student",
      name: "Emulator Student",
      email: student.email,
      department: "Computer Science",
      batch: "BCA-3A",
    }),
    firestore.doc(`users/${incorrectRole.uid}`).set({
      role: "Teacher",
      name: "Incorrect Role",
      email: incorrectRole.email,
      department: "Computer Science",
    }),
  ]);
});

after(async () => {
  await runtime?.close();
  await deleteApp(seedApp);
});

describe("Firebase emulator teacher authentication", () => {
  test("authenticates and authorizes only a complete teacher profile", async () => {
    const response = await request(backend())
      .get("/api/me")
      .set(authorization(teacher.idToken));

    assert.equal(response.status, 200);
    assert.deepEqual(response.body, {
      teacher: {
        uid: teacher.uid,
        role: "teacher",
        name: "Emulator Teacher",
        email: teacher.email,
        department: "Computer Science",
      },
    });
  });

  test("rejects an incorrect password at Firebase Authentication", async () => {
    const response = await authRequest("signInWithPassword", {
      email: teacher.email,
      password: "incorrect-password",
      returnSecureToken: true,
    });

    assert.equal(response.status, 400);
    const body = (await response.json()) as { error: { message: string } };
    assert.match(body.error.message, /INVALID_LOGIN_CREDENTIALS|INVALID_PASSWORD/);
  });

  test("denies a student account", async () => {
    const response = await request(backend())
      .get("/api/me")
      .set(authorization(student.idToken));

    assert.equal(response.status, 403);
    assert.equal(response.body.error.code, "TEACHER_ACCESS_DENIED");
  });

  test("denies an authenticated account with no Firestore profile", async () => {
    const response = await request(backend())
      .get("/api/me")
      .set(authorization(missingProfile.idToken));

    assert.equal(response.status, 403);
    assert.equal(response.body.error.code, "TEACHER_PROFILE_MISSING");
  });

  test("denies an incorrectly capitalized teacher role", async () => {
    const response = await request(backend())
      .get("/api/me")
      .set(authorization(incorrectRole.idToken));

    assert.equal(response.status, 403);
    assert.equal(response.body.error.code, "TEACHER_ACCESS_DENIED");
  });

  test("rejects missing and invalid ID tokens", async (context) => {
    await context.test("missing token", async () => {
      const response = await request(backend()).get("/api/me");
      assert.equal(response.status, 401);
      assert.equal(response.body.error.code, "AUTHENTICATION_REQUIRED");
    });

    await context.test("invalid token", async () => {
      const response = await request(backend())
        .get("/api/me")
        .set(authorization("not-a-firebase-token"));
      assert.equal(response.status, 401);
      assert.equal(response.body.error.code, "INVALID_TOKEN");
    });
  });

  test("returns an empty authorized assignment result instead of broad access", async () => {
    const response = await request(backend())
      .get("/api/teacher/assignments")
      .set(authorization(teacher.idToken));

    assert.equal(response.status, 200);
    assert.deepEqual(response.body, { assignments: [] });
  });

  test("returns only active assignments bound to the verified teacher UID", async () => {
    await Promise.all([
      firestore.doc("teacherAssignments/teacher-active").set({
        teacherUid: teacher.uid,
        subjectId: "CS301",
        batchId: "BCA-3A",
        academicYear: "2026-27",
        active: true,
      }),
      firestore.doc("teacherAssignments/teacher-inactive").set({
        teacherUid: teacher.uid,
        subjectId: "CS999",
        batchId: "BCA-3A",
        active: false,
      }),
      firestore.doc("teacherAssignments/student-owned").set({
        teacherUid: student.uid,
        subjectId: "CS401",
        batchId: "BCA-4A",
        active: true,
      }),
    ]);

    const response = await request(backend())
      .get("/api/teacher/assignments?teacherUid=someone-else")
      .set(authorization(teacher.idToken));

    assert.equal(response.status, 200);
    assert.deepEqual(response.body, {
      assignments: [
        {
          id: "teacher-active",
          subjectId: "CS301",
          batchId: "BCA-3A",
          academicYear: "2026-27",
        },
      ],
    });
  });

  test("current Firestore rules deny unauthenticated and cross-student reads", async () => {
    const teacherDocumentUrl =
      `http://${firestoreHost}/v1/projects/${projectId}/databases/(default)/documents/users/${teacher.uid}`;
    const studentDocumentUrl =
      `http://${firestoreHost}/v1/projects/${projectId}/databases/(default)/documents/users/${student.uid}`;

    const unauthenticated = await fetch(teacherDocumentUrl);
    assert.equal(unauthenticated.status, 403);

    const crossStudent = await fetch(teacherDocumentUrl, {
      headers: authorization(student.idToken),
    });
    assert.equal(crossStudent.status, 403);

    const ownStudent = await fetch(studentDocumentUrl, {
      headers: authorization(student.idToken),
    });
    assert.equal(ownStudent.status, 200);
  });

  test("continues authorizing the teacher after the backend runtime restarts", async () => {
    await runtime.close();
    runtime = createFirebaseAdminRuntime({
      mode: "emulator",
      projectId,
      authEmulatorHost: authHost,
      firestoreEmulatorHost: firestoreHost,
    })!;

    const response = await request(backend())
      .get("/api/me")
      .set(authorization(teacher.idToken));

    assert.equal(response.status, 200);
    assert.equal(response.body.teacher.uid, teacher.uid);
  });
});
