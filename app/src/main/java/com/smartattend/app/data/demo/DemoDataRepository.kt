package com.smartattend.app.data.demo

import com.smartattend.app.core.model.AttendanceHistoryItem
import com.smartattend.app.core.model.AttendanceStatus
import com.smartattend.app.core.model.ClassSession
import com.smartattend.app.core.model.SessionStatus
import com.smartattend.app.core.model.Student
import com.smartattend.app.core.model.SubjectAttendance
import com.smartattend.app.core.model.TimetableItem
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DemoDataRepository(
    private val campusZone: ZoneId = ZoneId.of("Asia/Kolkata"),
) {
    val student = Student(
        id = "student-demo-042",
        name = "Aarav Sharma",
        rollNumber = "CS-042",
        email = "aarav.sharma@demo.smartattend",
        department = "Computer Science",
        batch = "CSE-2026-A",
    )

    fun activeSession(start: Instant, end: Instant, now: Instant = Instant.now()) = ClassSession(
        id = "session-os-demo",
        subjectId = "cs301",
        subjectName = "Operating Systems",
        room = "Lab 204",
        batch = student.batch,
        start = start,
        end = end,
        gracePeriod = Duration.ofMinutes(10),
        status = if (now.isBefore(end)) SessionStatus.ACTIVE else SessionStatus.ENDED,
    )

    fun timetable(session: ClassSession): List<TimetableItem> = listOf(
        TimetableItem(
            id = session.id,
            subjectCode = "CS301",
            subjectName = session.subjectName,
            room = session.room,
            start = session.start,
            end = session.end,
            active = true,
        ),
        TimetableItem(
            id = "session-db-demo",
            subjectCode = "CS305",
            subjectName = "Database Systems",
            room = "Room 118",
            start = session.end.plus(Duration.ofMinutes(20)),
            end = session.end.plus(Duration.ofMinutes(80)),
            active = false,
        ),
        TimetableItem(
            id = "session-net-demo",
            subjectCode = "CS307",
            subjectName = "Computer Networks",
            room = "Room 312",
            start = session.end.plus(Duration.ofMinutes(100)),
            end = session.end.plus(Duration.ofMinutes(160)),
            active = false,
        ),
    )

    val subjectAttendance = listOf(
        SubjectAttendance("CS301", "Operating Systems", 22, 26),
        SubjectAttendance("CS305", "Database Systems", 18, 25),
        SubjectAttendance("CS307", "Computer Networks", 24, 27),
        SubjectAttendance("CS309", "Software Engineering", 16, 20),
    )

    val history: List<AttendanceHistoryItem>
        get() {
            val today = LocalDate.now(campusZone)
            return listOf(
                AttendanceHistoryItem("h1", today.minusDays(1), "CS301", "Operating Systems", AttendanceStatus.PRESENT, false, 91.0, true),
                AttendanceHistoryItem("h2", today.minusDays(2), "CS305", "Database Systems", AttendanceStatus.REVIEW, true, 68.0, true),
                AttendanceHistoryItem("h3", today.minusDays(3), "CS307", "Computer Networks", AttendanceStatus.PRESENT, false, 87.0, true),
                AttendanceHistoryItem("h4", today.minusDays(5), "CS309", "Software Engineering", AttendanceStatus.ABSENT, false, 42.0, true),
            )
        }
}

