"use strict";

const defaultStudents = [
  { id: "CS-042", name: "Aarav Sharma", percentage: 82, status: "Present" },
  { id: "CS-055", name: "Diya Patel", percentage: 67, status: "Review" },
  { id: "CS-061", name: "Kabir Singh", percentage: 43, status: "Absent" },
];
let students = DashboardModel.restoreStudents(
  localStorage.getItem("smartattend-demo-students"),
  defaultStudents,
);
const evidence = [
  { student: "Aarav Sharma", event: "Enter", time: "10:02 AM", source: "Simulation" },
  { student: "Diya Patel", event: "Enter", time: "10:12 AM", source: "Simulation" },
  { student: "Aarav Sharma", event: "Exit", time: "10:51 AM", source: "Simulation" },
];
let selectedStudent = null;
let sessionActive = false;
let audits = JSON.parse(localStorage.getItem("smartattend-demo-audits") || "[]");

const byId = (id) => document.getElementById(id);

function render() {
  byId("session-status").textContent = sessionActive ? "Active (demo)" : "Not started";
  byId("start-button").disabled = sessionActive;
  byId("stop-button").disabled = !sessionActive;
  byId("evidence-body").innerHTML = evidence.map((item) => `<tr><td>${DashboardModel.escapeHtml(item.student)}</td><td>${DashboardModel.escapeHtml(item.event)}</td><td>${DashboardModel.escapeHtml(item.time)}</td><td><span class="badge">${DashboardModel.escapeHtml(item.source)}</span></td></tr>`).join("");
  byId("attendance-body").innerHTML = students.map((student) => `<tr><td>${DashboardModel.escapeHtml(student.name)}<br><small>${DashboardModel.escapeHtml(student.id)}</small></td><td>${DashboardModel.escapeHtml(student.percentage)}%</td><td><span class="badge">${DashboardModel.escapeHtml(student.status)}</span></td><td><button class="secondary correct" data-id="${DashboardModel.escapeHtml(student.id)}">Correct</button></td></tr>`).join("");
  byId("audit-list").innerHTML = audits.length ? audits.map((audit) => `<li><strong>${DashboardModel.escapeHtml(audit.student)}</strong>: ${DashboardModel.escapeHtml(audit.before)} → ${DashboardModel.escapeHtml(audit.after)}. ${DashboardModel.escapeHtml(audit.reason)} <small>(${DashboardModel.escapeHtml(audit.at)})</small></li>`).join("") : "<li>No corrections recorded.</li>";
  document.querySelectorAll(".correct").forEach((button) => button.addEventListener("click", () => openCorrection(button.dataset.id)));
}

function openCorrection(id) {
  selectedStudent = students.find((student) => student.id === id);
  byId("correction-student").textContent = `${selectedStudent.name} • current status ${selectedStudent.status}`;
  byId("correction-status").value = selectedStudent.status;
  byId("correction-reason").value = "";
  byId("correction-error").textContent = "";
  byId("correction-dialog").showModal();
}

byId("login-button").addEventListener("click", () => {
  const email = byId("email").value.trim();
  const password = byId("password").value;
  if (!email || password.length < 4) {
    byId("login-error").textContent = "Enter an email and a demo password of at least 4 characters.";
    return;
  }
  byId("login-panel").hidden = true;
  byId("dashboard").hidden = false;
  render();
});
byId("logout-button").addEventListener("click", () => { byId("dashboard").hidden = true; byId("login-panel").hidden = false; });
byId("start-button").addEventListener("click", () => { sessionActive = true; render(); });
byId("stop-button").addEventListener("click", () => { sessionActive = false; render(); });
byId("save-correction").addEventListener("click", (event) => {
  event.preventDefault();
  const reason = byId("correction-reason").value.trim();
  if (reason.length < 5) { byId("correction-error").textContent = "A reason of at least 5 characters is required."; return; }
  const before = selectedStudent.status;
  selectedStudent.status = byId("correction-status").value;
  audits.unshift({ student: selectedStudent.name, before, after: selectedStudent.status, reason, at: new Date().toLocaleString() });
  localStorage.setItem("smartattend-demo-students", JSON.stringify(students));
  localStorage.setItem("smartattend-demo-audits", JSON.stringify(audits));
  byId("correction-dialog").close();
  render();
});
byId("csv-button").addEventListener("click", () => {
  const csv = DashboardModel.buildAttendanceCsv(students);
  const link = document.createElement("a");
  link.href = URL.createObjectURL(new Blob([csv], { type: "text/csv" }));
  link.download = "smartattend-demo-attendance.csv";
  link.click();
  URL.revokeObjectURL(link.href);
});

