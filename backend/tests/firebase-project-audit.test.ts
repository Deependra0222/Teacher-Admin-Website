import assert from "node:assert/strict";
import { describe, test } from "node:test";

import {
  auditFirebaseProject,
  formatFirebaseProjectAudit,
} from "../src/config/firebase-project-audit.js";

const android = {
  projectId: "smartattend-2a5ce",
  projectNumber: "1065635060805",
  packages: ["com.smartattend.app"],
};

const matchingWebAppId = "1:1065635060805:web:abc123";

describe("Firebase cross-client project audit", () => {
  test("reports disabled mode as safe but not ready for live login", () => {
    const result = auditFirebaseProject({
      android,
      firebase: { mode: "disabled" },
    });

    assert.equal(result.status, "disabled");
    assert.equal(result.liveProjectConfigReady, false);
    assert.equal(result.androidProjectId, "smartattend-2a5ce");
  });

  test("keeps emulator mode isolated from the production Android project", () => {
    const result = auditFirebaseProject({
      android,
      firebase: {
        mode: "emulator",
        projectId: "demo-smartattend",
        authEmulatorHost: "127.0.0.1:9099",
        firestoreEmulatorHost: "127.0.0.1:8080",
      },
      webProjectId: "demo-smartattend",
    });

    assert.equal(result.status, "emulator");
    assert.equal(result.liveProjectConfigReady, false);
    assert.equal(result.backendProjectId, "demo-smartattend");
  });

  test("accepts one matching Android, Admin, and Web project in ADC mode", () => {
    const result = auditFirebaseProject({
      android,
      firebase: { mode: "adc", projectId: "smartattend-2a5ce" },
      webProjectId: "smartattend-2a5ce",
      webAppId: matchingWebAppId,
      messagingSenderId: "1065635060805",
    });

    assert.equal(result.status, "ready");
    assert.equal(result.liveProjectConfigReady, true);
    assert.equal(result.backendProjectId, "smartattend-2a5ce");
    assert.equal(result.webProjectId, "smartattend-2a5ce");
  });

  test("rejects a live Admin project that differs from Android", () => {
    assert.throws(
      () =>
        auditFirebaseProject({
          android,
          firebase: { mode: "adc", projectId: "wrong-project" },
          webProjectId: "wrong-project",
          webAppId: matchingWebAppId,
        }),
      /FIREBASE_PROJECT_ID.*smartattend-2a5ce/,
    );
  });

  test("rejects a live Web project that differs from Android", () => {
    assert.throws(
      () =>
        auditFirebaseProject({
          android,
          firebase: { mode: "adc", projectId: "smartattend-2a5ce" },
          webProjectId: "wrong-project",
          webAppId: matchingWebAppId,
        }),
      /FIREBASE_WEB_PROJECT_ID.*smartattend-2a5ce/,
    );
  });

  test("rejects missing Android project metadata", () => {
    assert.throws(
      () =>
        auditFirebaseProject({
          android: { projectId: " ", packages: ["com.smartattend.app"] },
          firebase: { mode: "disabled" },
        }),
      /Android Firebase project ID is missing/,
    );
  });

  test("rejects a live Web App ID from a different Firebase project number", () => {
    assert.throws(
      () =>
        auditFirebaseProject({
          android,
          firebase: { mode: "adc", projectId: "smartattend-2a5ce" },
          webProjectId: "smartattend-2a5ce",
          webAppId: "1:999999999999:web:wrongproject",
        }),
      /FIREBASE_WEB_APP_ID.*1065635060805/,
    );
  });

  test("rejects a messaging sender ID from a different Firebase project", () => {
    assert.throws(
      () =>
        auditFirebaseProject({
          android,
          firebase: { mode: "adc", projectId: "smartattend-2a5ce" },
          webProjectId: "smartattend-2a5ce",
          webAppId: matchingWebAppId,
          messagingSenderId: "999999999999",
        }),
      /FIREBASE_WEB_MESSAGING_SENDER_ID.*1065635060805/,
    );
  });

  test("formats a secret-free status report", () => {
    const result = auditFirebaseProject({
      android,
      firebase: { mode: "adc", projectId: "smartattend-2a5ce" },
      webProjectId: "smartattend-2a5ce",
      webAppId: matchingWebAppId,
    });
    const report = formatFirebaseProjectAudit(result);

    assert.match(report, /smartattend-2a5ce/);
    assert.match(report, /LIVE PROJECT CONFIG READY: YES/);
    assert.doesNotMatch(report, /api.?key|app.?id|token|credential/i);
  });
});
