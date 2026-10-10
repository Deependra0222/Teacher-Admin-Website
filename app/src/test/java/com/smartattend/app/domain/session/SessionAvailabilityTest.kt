package com.smartattend.app.domain.session

import com.smartattend.app.core.model.ClassSession
import com.smartattend.app.core.model.SessionStatus
import com.smartattend.app.core.model.Student
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionAvailabilityTest {
    private val start = Instant.parse("2026-10-10T04:00:00Z")
    private val student = Student("s1", "Aarav", "CS-042", "a@demo", "CSE", "A")
    private val session = ClassSession(
        id = "session",
        subjectId = "subject",
        subjectName = "Operating Systems",
        room = "Lab 204",
        batch = "A",
        start = start,
        end = start.plusSeconds(3_600),
        gracePeriod = Duration.ofMinutes(10),
        status = SessionStatus.ACTIVE,
    )

    @Test
    fun missingSessionIsNoClass() {
        assertEquals(SessionDisplayState.NO_CLASS, SessionAvailability.determine(student, null, start))
    }

    @Test
    fun futureSessionIsUpcoming() {
        assertEquals(
            SessionDisplayState.UPCOMING,
            SessionAvailability.determine(student, session, start.minusSeconds(1)),
        )
    }

    @Test
    fun enrolledStudentInActiveWindowIsEligible() {
        assertEquals(
            SessionDisplayState.ACTIVE_ELIGIBLE,
            SessionAvailability.determine(student, session, start.plusSeconds(1)),
        )
    }

    @Test
    fun wrongBatchStudentIsIneligible() {
        assertEquals(
            SessionDisplayState.ACTIVE_INELIGIBLE,
            SessionAvailability.determine(student.copy(batch = "B"), session, start.plusSeconds(1)),
        )
    }

    @Test
    fun endedWindowIsNoClass() {
        assertEquals(
            SessionDisplayState.NO_CLASS,
            SessionAvailability.determine(student, session, session.end),
        )
    }
}
