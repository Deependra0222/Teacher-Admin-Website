(function (root, factory) {
  const api = factory();
  if (typeof module === "object" && module.exports) module.exports = api;
  root.DashboardModel = api;
})(typeof globalThis !== "undefined" ? globalThis : this, function () {
  "use strict";

  function escapeHtml(value) {
    return String(value)
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#39;");
  }

  function restoreStudents(storedJson, defaults) {
    if (!storedJson) return defaults.map((student) => ({ ...student }));
    try {
      const parsed = JSON.parse(storedJson);
      if (!Array.isArray(parsed)) throw new Error("Stored students are not an array");
      const byId = new Map(parsed.filter(isStudent).map((student) => [student.id, student]));
      return defaults.map((fallback) => ({ ...fallback, ...(byId.get(fallback.id) || {}) }));
    } catch (_) {
      return defaults.map((student) => ({ ...student }));
    }
  }

  function isStudent(value) {
    return value && typeof value.id === "string" && typeof value.name === "string" &&
      typeof value.percentage === "number" && typeof value.status === "string";
  }

  function buildAttendanceCsv(students) {
    const rows = [
      ["Student ID", "Student", "Percentage", "Status", "Source", "Verification"],
      ...students.map((student) => [
        student.id,
        student.name,
        student.percentage,
        student.status,
        "Simulation",
        "NOT VERIFIED BY BLE",
      ]),
    ];
    return rows.map((row) => row.map(csvCell).join(",")).join("\n");
  }

  function csvCell(value) {
    return `"${String(value).replaceAll('"', '""')}"`;
  }

  return { escapeHtml, restoreStudents, buildAttendanceCsv };
});

