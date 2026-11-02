import assert from "node:assert/strict";
import { after, before, describe, test } from "node:test";
import { mkdtemp, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import request from "supertest";

import { createApp } from "../src/app.js";
import { loadEnvironment } from "../src/config/environment.js";

const allowedOrigin = "http://localhost:4173";
const fixedTime = "2026-11-02T12:00:00.000Z";

let staticDirectory = "";

before(async () => {
  staticDirectory = await mkdtemp(path.join(tmpdir(), "smartattend-static-"));
  await writeFile(
    path.join(staticDirectory, "index.html"),
    "<!doctype html><title>Static fixture</title><h1>Teacher portal fixture</h1>",
    "utf8",
  );
});

after(async () => {
  await rm(staticDirectory, { recursive: true, force: true });
});

function buildApp(jsonLimit = "100kb") {
  return createApp({
    allowedOrigins: [allowedOrigin],
    jsonLimit,
    now: () => new Date(fixedTime),
    staticDirectory,
  });
}

describe("teacher API foundation", () => {
  test("reports non-sensitive health information without requiring an Origin header", async () => {
    const response = await request(buildApp()).get("/api/health");

    assert.equal(response.status, 200);
    assert.deepEqual(response.body, {
      status: "ok",
      service: "smartattend-teacher-api",
      timestamp: fixedTime,
    });
  });

  test("allows a configured browser origin", async () => {
    const response = await request(buildApp())
      .get("/api/health")
      .set("Origin", allowedOrigin);

    assert.equal(response.status, 200);
    assert.equal(response.headers["access-control-allow-origin"], allowedOrigin);
  });

  test("rejects an unconfigured browser origin without exposing internals", async () => {
    const response = await request(buildApp())
      .get("/api/health")
      .set("Origin", "https://untrusted.example");

    assert.equal(response.status, 403);
    assert.deepEqual(response.body, {
      error: {
        code: "ORIGIN_NOT_ALLOWED",
        message: "This browser origin is not allowed.",
      },
    });
    assert.equal("stack" in response.body, false);
  });

  test("returns JSON 404 for an unknown API route instead of frontend HTML", async () => {
    const response = await request(buildApp()).get("/api/not-implemented");

    assert.equal(response.status, 404);
    assert.match(response.headers["content-type"], /^application\/json/);
    assert.deepEqual(response.body, {
      error: {
        code: "API_ROUTE_NOT_FOUND",
        message: "The requested API route does not exist.",
      },
    });
  });

  test("returns a safe error for malformed JSON", async () => {
    const response = await request(buildApp())
      .post("/api/not-implemented")
      .set("Content-Type", "application/json")
      .send('{"broken"');

    assert.equal(response.status, 400);
    assert.deepEqual(response.body, {
      error: {
        code: "INVALID_JSON",
        message: "The request body must contain valid JSON.",
      },
    });
    assert.equal("stack" in response.body, false);
  });

  test("returns a safe error when JSON exceeds the configured limit", async () => {
    const response = await request(buildApp("1kb"))
      .post("/api/not-implemented")
      .send({ content: "x".repeat(2_048) });

    assert.equal(response.status, 413);
    assert.deepEqual(response.body, {
      error: {
        code: "PAYLOAD_TOO_LARGE",
        message: "The request body is too large.",
      },
    });
    assert.equal("stack" in response.body, false);
  });

  test("serves the website from the explicit static directory", async () => {
    const response = await request(buildApp()).get("/");

    assert.equal(response.status, 200);
    assert.match(response.headers["content-type"], /^text\/html/);
    assert.match(response.text, /Teacher portal fixture/);
  });
});

describe("environment configuration", () => {
  test("provides local-only defaults without Firebase credentials", () => {
    const config = loadEnvironment({});

    assert.equal(config.port, 3000);
    assert.deepEqual(config.allowedOrigins, ["http://localhost:3000"]);
    assert.equal(config.jsonLimit, "100kb");
    assert.equal(path.basename(config.staticDirectory), "teacher-website");
  });

  test("parses explicit port, origins, JSON limit, and static directory", () => {
    const config = loadEnvironment({
      PORT: "4100",
      ALLOWED_ORIGINS: "https://faculty.example, http://localhost:4100 ",
      JSON_LIMIT: "64kb",
      STATIC_DIR: "../custom-teacher-site",
    });

    assert.equal(config.port, 4100);
    assert.deepEqual(config.allowedOrigins, [
      "https://faculty.example",
      "http://localhost:4100",
    ]);
    assert.equal(config.jsonLimit, "64kb");
    assert.equal(path.basename(config.staticDirectory), "custom-teacher-site");
  });

  test("rejects an invalid server port before startup", () => {
    assert.throws(
      () => loadEnvironment({ PORT: "70000" }),
      /PORT must be an integer from 1 to 65535/,
    );
  });
});
