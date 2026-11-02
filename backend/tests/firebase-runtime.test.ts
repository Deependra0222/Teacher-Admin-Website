import assert from "node:assert/strict";
import { describe, test } from "node:test";
import request from "supertest";

import { createApp } from "../src/app.js";
import { loadEnvironment } from "../src/config/environment.js";
import {
  FirebaseAdminTeacherDataStore,
  FirebaseAdminTokenVerifier,
} from "../src/firebase/admin-runtime.js";

const staticDirectory = new URL("../", import.meta.url).pathname;

describe("Firebase runtime configuration", () => {
  test("keeps Firebase disabled by default", () => {
    const config = loadEnvironment({});

    assert.deepEqual(config.firebase, { mode: "disabled" });
    assert.equal(config.firebaseWebConfig, undefined);
  });

  test("rejects an unsupported Firebase mode", () => {
    assert.throws(
      () => loadEnvironment({ FIREBASE_MODE: "production" }),
      /FIREBASE_MODE must be disabled, emulator, or adc/,
    );
  });

  test("creates safe local defaults for the emulator-only project", () => {
    const config = loadEnvironment({
      FIREBASE_MODE: "emulator",
      FIREBASE_PROJECT_ID: "demo-smartattend",
    });

    assert.deepEqual(config.firebase, {
      mode: "emulator",
      projectId: "demo-smartattend",
      authEmulatorHost: "127.0.0.1:9099",
      firestoreEmulatorHost: "127.0.0.1:8080",
    });
    assert.deepEqual(config.firebaseWebConfig, {
      apiKey: "demo-smartattend-local-only",
      authDomain: "demo-smartattend.firebaseapp.com",
      projectId: "demo-smartattend",
      appId: "demo-smartattend-local-only",
      authEmulatorUrl: "http://127.0.0.1:9099",
    });
  });

  test("requires a complete matching Web configuration in ADC mode", () => {
    assert.throws(
      () =>
        loadEnvironment({
          FIREBASE_MODE: "adc",
          FIREBASE_PROJECT_ID: "smartattend-2a5ce",
          FIREBASE_WEB_API_KEY: "public-api-key",
        }),
      /Firebase Web configuration is incomplete/,
    );

    assert.throws(
      () =>
        loadEnvironment({
          FIREBASE_MODE: "adc",
          FIREBASE_PROJECT_ID: "smartattend-2a5ce",
          FIREBASE_WEB_API_KEY: "public-api-key",
          FIREBASE_WEB_AUTH_DOMAIN: "smartattend-2a5ce.firebaseapp.com",
          FIREBASE_WEB_PROJECT_ID: "another-project",
          FIREBASE_WEB_APP_ID: "1:web:example",
        }),
      /must match FIREBASE_PROJECT_ID/,
    );
  });

  test("accepts the public Web configuration separately from Admin credentials", () => {
    const config = loadEnvironment({
      FIREBASE_MODE: "adc",
      FIREBASE_PROJECT_ID: "smartattend-2a5ce",
      FIREBASE_WEB_API_KEY: "public-api-key",
      FIREBASE_WEB_AUTH_DOMAIN: "smartattend-2a5ce.firebaseapp.com",
      FIREBASE_WEB_PROJECT_ID: "smartattend-2a5ce",
      FIREBASE_WEB_STORAGE_BUCKET: "smartattend-2a5ce.firebasestorage.app",
      FIREBASE_WEB_MESSAGING_SENDER_ID: "123456789",
      FIREBASE_WEB_APP_ID: "1:123456789:web:abcdef",
    });

    assert.deepEqual(config.firebase, {
      mode: "adc",
      projectId: "smartattend-2a5ce",
    });
    assert.deepEqual(config.firebaseWebConfig, {
      apiKey: "public-api-key",
      authDomain: "smartattend-2a5ce.firebaseapp.com",
      projectId: "smartattend-2a5ce",
      storageBucket: "smartattend-2a5ce.firebasestorage.app",
      messagingSenderId: "123456789",
      appId: "1:123456789:web:abcdef",
    });
  });
});

describe("public Firebase browser configuration", () => {
  test("returns an explicit disabled payload with no caching", async () => {
    const response = await request(
      createApp({ allowedOrigins: [], staticDirectory }),
    ).get("/firebase-config.js");

    assert.equal(response.status, 200);
    assert.match(response.headers["content-type"], /^application\/javascript/);
    assert.equal(response.headers["cache-control"], "no-store");
    assert.equal(response.text, "window.__SMARTATTEND_FIREBASE__ = {\"enabled\":false};\n");
  });

  test("publishes only the explicit modular Web SDK options", async () => {
    const response = await request(
      createApp({
        allowedOrigins: [],
        staticDirectory,
        firebaseWebConfig: {
          apiKey: "public-api-key",
          authDomain: "smartattend-2a5ce.firebaseapp.com",
          projectId: "smartattend-2a5ce",
          appId: "1:web:<unsafe>",
        },
      }),
    ).get("/firebase-config.js");

    assert.equal(response.status, 200);
    assert.equal(response.headers["cache-control"], "no-store");
    assert.match(response.text, /\"enabled\":true/);
    assert.match(response.text, /public-api-key/);
    assert.equal(response.text.includes("<unsafe>"), false);
    assert.match(response.text, /\\u003cunsafe>/);
    assert.equal(response.text.includes("private_key"), false);
  });
});

describe("Firebase Admin adapters", () => {
  test("verifies ID tokens with revocation checking and exposes only identity fields", async () => {
    const calls: unknown[][] = [];
    const auth = {
      async verifyIdToken(...args: unknown[]) {
        calls.push(args);
        return { uid: "teacher-uid", email: "teacher@example.edu", admin: true };
      },
    };
    const verifier = new FirebaseAdminTokenVerifier(auth);

    const identity = await verifier.verifyIdToken("firebase-id-token", true);

    assert.deepEqual(calls, [["firebase-id-token", true]]);
    assert.deepEqual(identity, {
      uid: "teacher-uid",
      email: "teacher@example.edu",
    });
  });

  test("reads the UID-bound profile and queries only active UID-bound assignments", async () => {
    const operations: string[] = [];
    const firestore = {
      collection(name: string) {
        operations.push(`collection:${name}`);
        if (name === "users") {
          return {
            doc(uid: string) {
              operations.push(`doc:${uid}`);
              return {
                async get() {
                  return { exists: true, data: () => ({ role: "teacher" }) };
                },
              };
            },
          };
        }

        const query = {
          where(field: string, operator: string, value: unknown) {
            operations.push(`where:${field}:${operator}:${String(value)}`);
            return query;
          },
          async get() {
            return {
              docs: [
                {
                  id: "assignment-1",
                  data: () => ({
                    teacherUid: "teacher-uid",
                    subjectId: "CS301",
                    batchId: "BCA-3A",
                    active: true,
                  }),
                },
              ],
            };
          },
        };
        return query;
      },
    };
    const store = new FirebaseAdminTeacherDataStore(firestore);

    const profile = await store.getUserProfile("teacher-uid");
    const assignments = await store.getActiveAssignments("teacher-uid");

    assert.deepEqual(profile, { role: "teacher" });
    assert.deepEqual(assignments, [
      {
        id: "assignment-1",
        data: {
          teacherUid: "teacher-uid",
          subjectId: "CS301",
          batchId: "BCA-3A",
          active: true,
        },
      },
    ]);
    assert.deepEqual(operations, [
      "collection:users",
      "doc:teacher-uid",
      "collection:teacherAssignments",
      "where:teacherUid:==:teacher-uid",
      "where:active:==:true",
    ]);
  });
});
