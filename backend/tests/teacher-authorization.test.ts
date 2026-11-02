import assert from "node:assert/strict";
import { describe, test } from "node:test";
import request from "supertest";

import {
  TeacherAuthorizationService,
  type TeacherDataStore,
  type TokenVerifier,
  type VerifiedIdentity,
} from "../src/auth/teacher-authorization.js";
import { createApp } from "../src/app.js";

const teacherUid = "teacher-uid-001";
const teacherToken = "valid-teacher-token";

const completeProfile = {
  role: "teacher",
  name: "Dr. Meera Joshi",
  email: "meera@college.edu",
  department: "Computer Science",
  active: true,
};

type TokenBehavior = VerifiedIdentity | { readonly errorCode: string };

class ControlledTokenVerifier implements TokenVerifier {
  constructor(private readonly behaviors: ReadonlyMap<string, TokenBehavior>) {}

  async verifyIdToken(idToken: string, checkRevoked: boolean): Promise<VerifiedIdentity> {
    if (!checkRevoked) {
      throw new Error("revocation checks must be enabled");
    }

    const behavior = this.behaviors.get(idToken);
    if (behavior === undefined) {
      throw Object.assign(new Error("unknown token"), { code: "auth/argument-error" });
    }
    if ("errorCode" in behavior) {
      throw Object.assign(new Error("controlled token failure"), {
        code: behavior.errorCode,
      });
    }
    return behavior;
  }
}

class InMemoryTeacherDataStore implements TeacherDataStore {
  readonly profiles = new Map<string, Record<string, unknown> | null>();
  readonly assignments = new Map<
    string,
    readonly { readonly id: string; readonly data: Record<string, unknown> }[]
  >();
  profileFailure: Error | null = null;

  async getUserProfile(uid: string): Promise<Record<string, unknown> | null> {
    if (this.profileFailure !== null) {
      throw this.profileFailure;
    }
    return this.profiles.get(uid) ?? null;
  }

  async getActiveAssignments(
    uid: string,
  ): Promise<readonly { readonly id: string; readonly data: Record<string, unknown> }[]> {
    return this.assignments.get(uid) ?? [];
  }
}

function createConfiguredTestApp(options?: {
  readonly profile?: Record<string, unknown> | null;
  readonly tokenBehaviors?: ReadonlyMap<string, TokenBehavior>;
  readonly configureStore?: (store: InMemoryTeacherDataStore) => void;
}) {
  const verifier = new ControlledTokenVerifier(
    options?.tokenBehaviors ??
      new Map([[teacherToken, { uid: teacherUid, email: completeProfile.email }]]),
  );
  const store = new InMemoryTeacherDataStore();
  store.profiles.set(teacherUid, options?.profile === undefined ? completeProfile : options.profile);
  options?.configureStore?.(store);

  return createApp({
    allowedOrigins: ["http://localhost:3000"],
    staticDirectory: process.cwd(),
    teacherAuthorization: new TeacherAuthorizationService(verifier, store),
  });
}

function bearer(token = teacherToken): { readonly Authorization: string } {
  return { Authorization: `Bearer ${token}` };
}

describe("teacher authorization endpoints", () => {
  test("returns 503 when Firebase Admin authorization is disabled", async () => {
    const app = createApp({
      allowedOrigins: ["http://localhost:3000"],
      staticDirectory: process.cwd(),
    });

    const response = await request(app).get("/api/me");

    assert.equal(response.status, 503);
    assert.deepEqual(response.body, {
      error: {
        code: "FIREBASE_NOT_CONFIGURED",
        message: "Firebase teacher authentication is not configured.",
      },
    });
  });

  test("rejects a request with no Authorization header", async () => {
    const response = await request(createConfiguredTestApp()).get("/api/me");

    assert.equal(response.status, 401);
    assert.equal(response.body.error.code, "AUTHENTICATION_REQUIRED");
  });

  test("rejects malformed Authorization headers", async (context) => {
    for (const value of ["Basic abc", "Bearer", "Bearer first second"]) {
      await context.test(value, async () => {
        const response = await request(createConfiguredTestApp())
          .get("/api/me")
          .set("Authorization", value);

        assert.equal(response.status, 401);
        assert.equal(response.body.error.code, "INVALID_AUTHORIZATION_HEADER");
      });
    }
  });

  test("rejects an invalid token", async () => {
    const response = await request(createConfiguredTestApp())
      .get("/api/me")
      .set(bearer("invalid-token"));

    assert.equal(response.status, 401);
    assert.equal(response.body.error.code, "INVALID_TOKEN");
  });

  test("distinguishes expired and revoked sessions", async (context) => {
    const tokenBehaviors = new Map<string, TokenBehavior>([
      ["expired", { errorCode: "auth/id-token-expired" }],
      ["revoked", { errorCode: "auth/id-token-revoked" }],
    ]);
    const app = createConfiguredTestApp({ tokenBehaviors });

    await context.test("expired", async () => {
      const response = await request(app).get("/api/me").set(bearer("expired"));
      assert.equal(response.status, 401);
      assert.equal(response.body.error.code, "TOKEN_EXPIRED");
    });

    await context.test("revoked", async () => {
      const response = await request(app).get("/api/me").set(bearer("revoked"));
      assert.equal(response.status, 401);
      assert.equal(response.body.error.code, "TOKEN_REVOKED");
    });
  });

  test("rejects an authenticated account with no users UID profile", async () => {
    const response = await request(createConfiguredTestApp({ profile: null }))
      .get("/api/me")
      .set(bearer());

    assert.equal(response.status, 403);
    assert.equal(response.body.error.code, "TEACHER_PROFILE_MISSING");
  });

  test("rejects missing, blank, and non-string required teacher fields", async (context) => {
    const invalidProfiles: readonly [string, Record<string, unknown>][] = [
      ["missing name", { ...completeProfile, name: undefined }],
      ["blank email", { ...completeProfile, email: "   " }],
      ["numeric department", { ...completeProfile, department: 42 }],
    ];

    for (const [label, profile] of invalidProfiles) {
      await context.test(label, async () => {
        const response = await request(createConfiguredTestApp({ profile }))
          .get("/api/me")
          .set(bearer());

        assert.equal(response.status, 403);
        assert.equal(response.body.error.code, "TEACHER_PROFILE_INCOMPLETE");
      });
    }
  });

  test("rejects student, admin, and incorrectly capitalized roles", async (context) => {
    for (const role of ["student", "admin", "Teacher"]) {
      await context.test(role, async () => {
        const response = await request(
          createConfiguredTestApp({ profile: { ...completeProfile, role } }),
        )
          .get("/api/me")
          .set(bearer());

        assert.equal(response.status, 403);
        assert.equal(response.body.error.code, "TEACHER_ACCESS_DENIED");
      });
    }
  });

  test("rejects an explicitly inactive teacher", async () => {
    const response = await request(
      createConfiguredTestApp({ profile: { ...completeProfile, active: false } }),
    )
      .get("/api/me")
      .set(bearer());

    assert.equal(response.status, 403);
    assert.equal(response.body.error.code, "TEACHER_ACCOUNT_INACTIVE");
  });

  test("returns only the authorized UID-bound teacher profile", async () => {
    const response = await request(createConfiguredTestApp())
      .get("/api/me")
      .set(bearer());

    assert.equal(response.status, 200);
    assert.deepEqual(response.body, {
      teacher: {
        uid: teacherUid,
        role: "teacher",
        name: "Dr. Meera Joshi",
        email: "meera@college.edu",
        department: "Computer Science",
      },
    });
  });

  test("returns an authorized empty assignment list without broad fallback", async () => {
    const response = await request(createConfiguredTestApp())
      .get("/api/teacher/assignments")
      .set(bearer());

    assert.equal(response.status, 200);
    assert.deepEqual(response.body, { assignments: [] });
  });

  test("ignores a browser-supplied teacher UID and returns only verified-UID assignments", async () => {
    const app = createConfiguredTestApp({
      configureStore(store) {
        store.assignments.set(teacherUid, [
          {
            id: "assignment-1",
            data: {
              teacherUid,
              subjectId: "dbms",
              batchId: "te-a",
              active: true,
              academicYear: "2026-27",
            },
          },
        ]);
        store.assignments.set("other-teacher", [
          {
            id: "private-assignment",
            data: {
              teacherUid: "other-teacher",
              subjectId: "secret",
              batchId: "secret",
              active: true,
            },
          },
        ]);
      },
    });

    const response = await request(app)
      .get("/api/teacher/assignments?teacherUid=other-teacher")
      .set(bearer());

    assert.equal(response.status, 200);
    assert.deepEqual(response.body, {
      assignments: [
        {
          id: "assignment-1",
          subjectId: "dbms",
          batchId: "te-a",
          academicYear: "2026-27",
        },
      ],
    });
  });

  test("fails closed when an active assignment is malformed", async () => {
    const app = createConfiguredTestApp({
      configureStore(store) {
        store.assignments.set(teacherUid, [
          {
            id: "bad-assignment",
            data: { teacherUid, subjectId: "dbms", active: true },
          },
        ]);
      },
    });

    const response = await request(app)
      .get("/api/teacher/assignments")
      .set(bearer());

    assert.equal(response.status, 500);
    assert.equal(response.body.error.code, "AUTHORIZATION_DATA_INVALID");
    assert.equal("stack" in response.body, false);
  });

  test("returns a safe 503 when the authorization data service is unavailable", async () => {
    const app = createConfiguredTestApp({
      configureStore(store) {
        store.profileFailure = new Error("private upstream detail");
      },
    });

    const response = await request(app).get("/api/me").set(bearer());

    assert.equal(response.status, 503);
    assert.deepEqual(response.body, {
      error: {
        code: "AUTHORIZATION_SERVICE_UNAVAILABLE",
        message: "Teacher authorization is temporarily unavailable.",
      },
    });
  });
});
