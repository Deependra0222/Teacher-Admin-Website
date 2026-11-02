// Example of how you would update the UI when a student leaves the room
function updateStudentStatus(studentId, status, presenceText) {
  const row = document.getElementById(studentId);
  if (row) {
    const statusCell = row.cells[2];
    const presenceCell = row.cells[4];
    
    // Update Status
    if (status === 'Outside') {
      statusCell.innerHTML = '<span class="badge badge-warning">Outside</span>';
    } else if (status === 'Verified') {
      statusCell.innerHTML = '<span class="badge badge-success">Verified</span>';
    }
    
    // Update Presence
    presenceCell.innerText = presenceText;
  }
}

// Simulate Step 7 & 8 (Student leaves classroom)
setTimeout(() => {
  updateStudentStatus('student-101', 'Outside', '52 / 60');
}, 5000);