import assert from "node:assert/strict";
import { describe, test } from "node:test";

import {
  PortalAuthError,
  createBackendClient,
  createTeacherSessionController,
  firebaseErrorMessage,
} from "../assets/teacher-auth-core.js";

class MemoryStorage {
  values = new Map();

  getItem(key) {
    return this.values.get(key) ?? null;
  }

  setItem(key, value) {
    this.values.set(key, String(value));
  }

  removeItem(key) {
    this.values.delete(key);
  }
}

function fakeGateway(overrides = {}) {
  const calls = [];
  const user = { uid: "teacher-uid" };
  return {
    calls,
    user,
    gateway: {
      async signIn(email, password) {
        calls.push(["signIn", email, password]);
        if (overrides.signInError) throw overrides.signInError;
        return user;
      },
      async currentUser() {
        calls.push(["currentUser"]);
        return overrides.currentUser === undefined ? null : overrides.currentUser;
      },
      async signOut() {
        calls.push(["signOut"]);
      },
      async sendPasswordReset(email) {
        calls.push(["sendPasswordReset", email]);
      },
    },
  };
}

describe("teacher session separation", () => {
  test("enters Demo Mode only through the explicit action and signs Firebase out", async () => {
    const storage = new MemoryStorage();
    const firebase = fakeGateway();
    const controller = createTeacherSessionController({
      storage,
      firebaseGateway: firebase.gateway,
      backendClient: { authorize: async () => assert.fail("must not authorize") },
    });

    const session = await controller.enterDemo();

    assert.deepEqual(session, { mode: "demo" });
    assert.equal(storage.getItem("smartattend_teacher_demo_session"), "explicit");
    assert.deepEqual(firebase.calls, [["signOut"]]);
  });

  test("a real login clears an old Demo marker before authenticating", async () => {
    const storage = new MemoryStorage();
    storage.setItem("smartattend_teacher_demo_session", "explicit");
    const firebase = fakeGateway();
    const controller = createTeacherSessionController({
      storage,
      firebaseGateway: firebase.gateway,
      backendClient: {
        async authorize(user) {
          assert.equal(user, firebase.user);
          return { uid: "teacher-uid", role: "teacher", name: "Dr Rao" };
        },
      },
    });

    const session = await controller.signIn("teacher@example.edu", "secret");

    assert.equal(storage.getItem("smartattend_teacher_demo_session"), null);
    assert.equal(session.mode, "real");
    assert.equal(session.teacher.uid, "teacher-uid");
  });

  test("never falls back to Demo Mode when Firebase credentials fail", async () => {
    const storage = new MemoryStorage();
    storage.setItem("smartattend_teacher_demo_session", "explicit");
    const firebase = fakeGateway({
      signInError: Object.assign(new Error("wrong password"), {
        code: "auth/invalid-credential",
      }),
    });
    const controller = createTeacherSessionController({
      storage,
      firebaseGateway: firebase.gateway,
      backendClient: { authorize: async () => assert.fail("must not authorize") },
    });

    await assert.rejects(
      controller.signIn("teacher@example.edu", "wrong"),
      /email or password is incorrect/i,
    );
    assert.equal(storage.getItem("smartattend_teacher_demo_session"), null);
  });

  test("signs Firebase out when the backend denies the teacher profile", async () => {
    const storage = new MemoryStorage();
    const firebase = fakeGateway();
    const controller = createTeacherSessionController({
      storage,
      firebaseGateway: firebase.gateway,
      backendClient: {
        async authorize() {
          throw new PortalAuthError(
            "TEACHER_ACCESS_DENIED",
            "This account is not authorized for the teacher portal.",
          );
        },
      },
    });

    await assert.rejects(
      controller.signIn("student@example.edu", "secret"),
      /not authorized/,
    );
    assert.deepEqual(firebase.calls.at(-1), ["signOut"]);
    assert.equal(storage.getItem("smartattend_teacher_demo_session"), null);
  });

  test("restores an authorized Firebase teacher session", async () => {
    const storage = new MemoryStorage();
    const firebase = fakeGateway({ currentUser: { uid: "restored-uid" } });
    const controller = createTeacherSessionController({
      storage,
      firebaseGateway: firebase.gateway,
      backendClient: {
        async authorize(user) {
          return { uid: user.uid, role: "teacher", name: "Restored Teacher" };
        },
      },
    });

    const session = await controller.restore();

    assert.equal(session.mode, "real");
    assert.equal(session.teacher.uid, "restored-uid");
  });

  test("logout clears either session mode and invokes Firebase sign-out", async () => {
    const storage = new MemoryStorage();
    storage.setItem("smartattend_teacher_demo_session", "explicit");
    const firebase = fakeGateway();
    const controller = createTeacherSessionController({
      storage,
      firebaseGateway: firebase.gateway,
      backendClient: { authorize: async () => ({}) },
    });

    await controller.logout();

    assert.equal(storage.getItem("smartattend_teacher_demo_session"), null);
    assert.deepEqual(firebase.calls, [["signOut"]]);
  });

  test("delegates password reset to Firebase without changing the session mode", async () => {
    const storage = new MemoryStorage();
    const firebase = fakeGateway();
    const controller = createTeacherSessionController({
      storage,
      firebaseGateway: firebase.gateway,
      backendClient: { authorize: async () => ({}) },
    });

    await controller.sendPasswordReset("teacher@example.edu");

    assert.deepEqual(firebase.calls, [
      ["sendPasswordReset", "teacher@example.edu"],
    ]);
    assert.equal(storage.getItem("smartattend_teacher_demo_session"), null);
  });
});

describe("protected backend client", () => {
  test("sends a short-lived ID token only in the Authorization header", async () => {
    const calls = [];
    const client = createBackendClient(async (url, options) => {
      calls.push([url, options]);
      return new Response(
        JSON.stringify({
          teacher: {
            uid: "teacher-uid",
            role: "teacher",
            name: "Dr Rao",
            email: "teacher@example.edu",
            department: "Computer Science",
          },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    });
    const user = { getIdToken: async () => "short-lived-id-token" };

    const teacher = await client.authorize(user);

    assert.equal(teacher.uid, "teacher-uid");
    assert.equal(calls[0][0], "/api/me");
    assert.deepEqual(calls[0][1], {
      headers: { Authorization: "Bearer short-lived-id-token" },
    });
  });

  test("propagates safe role/profile authorization errors from the backend", async () => {
    const client = createBackendClient(async () =>
      new Response(
        JSON.stringify({
          error: {
            code: "TEACHER_PROFILE_MISSING",
            message: "No authorized teacher profile exists for this account.",
          },
        }),
        { status: 403, headers: { "Content-Type": "application/json" } },
      ),
    );

    await assert.rejects(
      client.authorize({ getIdToken: async () => "token" }),
      (error) =>
        error instanceof PortalAuthError &&
        error.code === "TEACHER_PROFILE_MISSING" &&
        /profile exists/.test(error.message),
    );
  });
});

describe("Firebase error messages", () => {
  test("distinguishes credential, network, throttling, and disabled-provider failures", () => {
    assert.match(
      firebaseErrorMessage({ code: "auth/invalid-credential" }),
      /email or password is incorrect/i,
    );
    assert.match(
      firebaseErrorMessage({ code: "auth/network-request-failed" }),
      /network/i,
    );
    assert.match(
      firebaseErrorMessage({ code: "auth/too-many-requests" }),
      /too many/i,
    );
    assert.match(
      firebaseErrorMessage({ code: "auth/operation-not-allowed" }),
      /not enabled/i,
    );
  });
});
