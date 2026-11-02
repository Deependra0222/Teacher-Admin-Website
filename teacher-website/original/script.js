document.addEventListener('DOMContentLoaded', () => {
    const menuToggle = document.getElementById('menuToggle');
    const sidebar = document.getElementById('sidebar');
    const batchSelect = document.getElementById('batchSelect');
    const studentListSection = document.getElementById('studentListSection');
    const attendanceFilter = document.getElementById('attendanceFilter');

    // Sidebar collapse action
    menuToggle.addEventListener('click', () => {
        sidebar.classList.toggle('collapsed');
    });

    // Show student list when a batch is selected
    batchSelect.addEventListener('change', () => {
        if (batchSelect.value) {
            studentListSection.style.display = 'block';
        } else {
            studentListSection.style.display = 'none';
        }
    });
document.addEventListener('DOMContentLoaded', () => {
    const batchSelect = document.getElementById('batchSelect');
    const studentListSection = document.getElementById('studentListSection');
    const attendanceFilter = document.getElementById('attendanceFilter');

    // Show list when batch is selected
    if(batchSelect) {
        batchSelect.addEventListener('change', () => {
            if (batchSelect.value== 'batchA') {
                studentListSection.style.display = 'block';
            } else {
                studentListSection.style.display = 'none';
            }
        });
    }

    // Filter students
    if(attendanceFilter) {
        attendanceFilter.addEventListener('change', function() {
            const filterValue = parseInt(this.value);
            const rows = document.querySelectorAll('.student-row');

            rows.forEach(row => {
                const attendance = parseInt(row.getAttribute('data-attendance'));

                if (filterValue === 100) {
                    row.style.display = '';
                } else if (attendance < filterValue) {
                    row.style.display = '';
                } else {
                    row.style.display = 'none';
                }
            });
        });
    }
});
});