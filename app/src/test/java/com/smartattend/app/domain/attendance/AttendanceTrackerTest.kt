package com.smartattend.app.domain.attendance

import com.smartattend.app.core.model.AttendanceStatus
import com.smartattend.app.core.model.ClassSession
import com.smartattend.app.core.model.SessionStatus
import com.smartattend.app.core.model.Student
import com.smartattend.app.domain.presence.AppClock
import com.smartattend.app.domain.presence.PresenceSignal
import com.smartattend.app.domain.presence.PresenceSignalType
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceTrackerTest {
    private val start = Instant.parse("2026-10-10T04:00:00Z")
    private val end = start.plus(Duration.ofMinutes(60))
    private val student = Student(
        id = "student-1",
        name = "Aarav Sharma",
        rollNumber = "CS-042",
        email = "aarav@demo.smartattend",
        department = "Computer Science",
        batch = "CSE-2026-A",
    )
    private val session = ClassSession(
        id = "session-1",
        subjectId = "cs301",
        subjectName = "Operating Systems",
        room = "Lab 204",
        batch = "CSE-2026-A",
        start = start,
        end = end,
        gracePeriod = Duration.ofMinutes(10),
        status = SessionStatus.ACTIVE,
    )

    @Test
    fun reentryAccumulatesWithoutCountingTimeBetweenIntervals() {
        val clock = FakeClock(start.plusSeconds(60))
        val tracker = AttendanceTracker(student, session, clock)

        assertTrue(tracker.record(PresenceSignal(PresenceSignalType.ENTER, clock.now())).accepted)
        clock.current = start.plusSeconds(660)
        assertTrue(tracker.record(PresenceSignal(PresenceSignalType.EXIT, clock.now())).accepted)
        clock.current = start.plusSeconds(1_200)
        assertTrue(tracker.record(PresenceSignal(PresenceSignalType.ENTER, clock.now())).accepted)
        clock.current = start.plusSeconds(2_100)
        assertTrue(tracker.record(PresenceSignal(PresenceSignalType.EXIT, clock.now())).accepted)

        assertEquals(Duration.ofMinutes(25), tracker.snapshot().confirmed)
    }

    @Test
    fun repeatedEnterAndExitAreRejectedWithoutChangingDuration() {
        val clock = FakeClock(start.plusSeconds(60))
        val tracker = AttendanceTracker(student, session, clock)

        assertTrue(tracker.record(PresenceSignal(PresenceSignalType.ENTER, clock.now())).accepted)
        clock.current = start.plusSeconds(120)
        assertFalse(tracker.record(PresenceSignal(PresenceSignalType.ENTER, clock.now())).accepted)
        clock.current = start.plusSeconds(180)
        assertTrue(tracker.record(PresenceSignal(PresenceSignalType.EXIT, clock.now())).accepted)
        clock.current = start.plusSeconds(240)
        assertFalse(tracker.record(PresenceSignal(PresenceSignalType.EXIT, clock.now())).accepted)

        assertEquals(Duration.ofMinutes(2), tracker.snapshot().confirmed)
    }

    @Test
    fun arrivalAfterGracePeriodIsMarkedLate() {
        val clock = FakeClock(start.plus(Duration.ofMinutes(11)))
        val tracker = AttendanceTracker(student, session, clock)

        tracker.record(PresenceSignal(PresenceSignalType.ENTER, clock.now()))
        clock.current = end
        val result = tracker.finish()

        assertTrue(result.late)
    }

    @Test
    fun earlyExitProducesAbsentResultWhenBelowHalf() {
        val clock = FakeClock(start)
        val tracker = AttendanceTracker(student, session, clock)

        tracker.record(PresenceSignal(PresenceSignalType.ENTER, clock.now()))
        clock.current = start.plus(Duration.ofMinutes(20))
        tracker.record(PresenceSignal(PresenceSignalType.EXIT, clock.now()))
        val result = tracker.finish()

        assertEquals(AttendanceStatus.ABSENT, result.status)
        assertEquals(33.333, result.percentage, 0.01)
    }

    @Test
    fun wrongBatchCannotCreateEvidence() {
        val clock = FakeClock(start.plusSeconds(30))
        val wrongBatchStudent = student.copy(batch = "CSE-2026-B")
        val tracker = AttendanceTracker(wrongBatchStudent, session, clock)

        val outcome = tracker.record(PresenceSignal(PresenceSignalType.ENTER, clock.now()))

        assertFalse(outcome.accepted)
        assertFalse(tracker.snapshot().inside)
        assertEquals(Duration.ZERO, tracker.snapshot().confirmed)
    }

    @Test
    fun inactiveSessionCannotCreateEvidence() {
        val clock = FakeClock(start.plusSeconds(30))
        val tracker = AttendanceTracker(
            student,
            session.copy(status = SessionStatus.ENDED),
            clock,
        )

        val outcome = tracker.record(PresenceSignal(PresenceSignalType.ENTER, clock.now()))

        assertFalse(outcome.accepted)
        assertFalse(tracker.snapshot().inside)
    }

    @Test
    fun finishingAfterSessionEndClosesOpenIntervalAtScheduledEnd() {
        val clock = FakeClock(start.plus(Duration.ofMinutes(15)))
        val tracker = AttendanceTracker(student, session, clock)
        tracker.record(PresenceSignal(PresenceSignalType.ENTER, clock.now()))
        clock.current = end.plus(Duration.ofMinutes(30))

        val result = tracker.finish()

        assertEquals(Duration.ofMinutes(45), result.confirmed)
        assertEquals(AttendanceStatus.PRESENT, result.status)
        assertFalse(tracker.snapshot().inside)
    }

    @Test
    fun eventBeforeStartIsRejected() {
        val clock = FakeClock(start.minusSeconds(1))
        val tracker = AttendanceTracker(student, session, clock)

        val outcome = tracker.record(PresenceSignal(PresenceSignalType.ENTER, clock.now()))

        assertFalse(outcome.accepted)
        assertEquals(Duration.ZERO, tracker.snapshot().confirmed)
    }

    @Test
    fun outOfOrderReentryIsRejectedWithoutDoubleCounting() {
        val clock = FakeClock(start)
        val tracker = AttendanceTracker(student, session, clock)
        tracker.record(PresenceSignal(PresenceSignalType.ENTER, start))
        tracker.record(PresenceSignal(PresenceSignalType.EXIT, start.plus(Duration.ofMinutes(30))))

        val backwardEntry = tracker.record(
            PresenceSignal(PresenceSignalType.ENTER, start.plus(Duration.ofMinutes(15))),
        )

        assertFalse(backwardEntry.accepted)
        assertEquals(Duration.ofMinutes(30), tracker.snapshot().confirmed)
        assertFalse(tracker.snapshot().inside)
    }

    @Test
    fun exitBeforeItsEntryIsRejectedWithoutClosingInterval() {
        val clock = FakeClock(start.plus(Duration.ofMinutes(20)))
        val tracker = AttendanceTracker(student, session, clock)
        tracker.record(PresenceSignal(PresenceSignalType.ENTER, clock.now()))

        val backwardExit = tracker.record(
            PresenceSignal(PresenceSignalType.EXIT, start.plus(Duration.ofMinutes(10))),
        )

        assertFalse(backwardExit.accepted)
        assertTrue(tracker.snapshot().inside)
        assertEquals(Duration.ZERO, tracker.snapshot().confirmed)
    }

    private class FakeClock(var current: Instant) : AppClock {
        override fun now(): Instant = current
    }
}
