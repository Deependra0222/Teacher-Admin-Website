"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const model = require("./dashboard-model.js");

const defaults = [{ id: "CS-042", name: "Aarav", percentage: 82, status: "Present" }];

test("restores persisted corrected status", () => {
  const stored = JSON.stringify([{ id: "CS-042", name: "Aarav", percentage: 82, status: "Absent" }]);
  assert.equal(model.restoreStudents(stored, defaults)[0].status, "Absent");
});

test("escapes correction reason before HTML rendering", () => {
  assert.equal(
    model.escapeHtml('<img src=x onerror="alert(1)">'),
    "&lt;img src=x onerror=&quot;alert(1)&quot;&gt;",
  );
});

test("CSV carries demo source and BLE verification disclaimer", () => {
  const csv = model.buildAttendanceCsv(defaults);
  assert.match(csv, /Source/);
  assert.match(csv, /Simulation/);
  assert.match(csv, /NOT VERIFIED BY BLE/);
});
