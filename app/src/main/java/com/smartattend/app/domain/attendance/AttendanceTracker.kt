package com.smartattend.app.domain.attendance

import com.smartattend.app.core.model.AttendanceResult
import com.smartattend.app.core.model.ClassSession
import com.smartattend.app.core.model.SessionStatus
import com.smartattend.app.core.model.Student
import com.smartattend.app.domain.presence.AppClock
import com.smartattend.app.domain.presence.PresenceSignal
import com.smartattend.app.domain.presence.PresenceSignalType
import java.time.Duration
import java.time.Instant

data class PresenceSnapshot(
    val inside: Boolean = false,
    val openSince: Instant? = null,
    val confirmed: Duration = Duration.ZERO,
    val firstEntry: Instant? = null,
    val lastEventAt: Instant? = null,
)

data class TrackingOutcome(
    val accepted: Boolean,
    val message: String,
    val snapshot: PresenceSnapshot,
)

class AttendanceTracker(
    private val student: Student,
    private val session: ClassSession,
    private val clock: AppClock,
    private val calculator: AttendanceCalculator = AttendanceCalculator(),
    initialSnapshot: PresenceSnapshot = PresenceSnapshot(),
) {
    private var state = initialSnapshot

    fun snapshot(): PresenceSnapshot = state

    fun record(signal: PresenceSignal): TrackingOutcome {
        val rejection = validate(signal.occurredAt)
        if (rejection != null) return rejected(rejection)

        return when (signal.type) {
            PresenceSignalType.ENTER -> enter(signal.occurredAt)
            PresenceSignalType.EXIT -> exit(signal.occurredAt)
        }
    }

    fun finish(): AttendanceResult {
        if (state.inside) {
            val closeAt = minOf(clock.now(), session.end)
            val startAt = state.openSince ?: closeAt
            val extra = Duration.between(startAt, closeAt).coerceAtLeast(Duration.ZERO)
            state = state.copy(
                inside = false,
                openSince = null,
                confirmed = state.confirmed.plus(extra),
            )
        }

        val late = state.firstEntry?.isAfter(session.start.plus(session.gracePeriod)) == true
        return calculator.calculate(
            confirmed = state.confirmed,
            scheduled = Duration.between(session.start, session.end),
            late = late,
        )
    }

    private fun validate(at: Instant): String? = when {
        student.batch != session.batch -> "Student is not enrolled in this session batch"
        session.status != SessionStatus.ACTIVE -> "Session is not active"
        at.isBefore(session.start) || !at.isBefore(session.end) -> "Event is outside the active session"
        state.lastEventAt?.let(at::isBefore) == true -> "Event time is earlier than the last accepted event"
        else -> null
    }

    private fun enter(at: Instant): TrackingOutcome {
        if (state.inside) return rejected("Entry already recorded")
        state = state.copy(
            inside = true,
            openSince = at,
            firstEntry = state.firstEntry ?: at,
            lastEventAt = at,
        )
        return accepted("Demo classroom entry recorded")
    }

    private fun exit(at: Instant): TrackingOutcome {
        if (!state.inside || state.openSince == null) return rejected("No open entry to close")
        val elapsed = Duration.between(state.openSince, at).coerceAtLeast(Duration.ZERO)
        state = state.copy(
            inside = false,
            openSince = null,
            confirmed = state.confirmed.plus(elapsed),
            lastEventAt = at,
        )
        return accepted("Demo classroom exit recorded")
    }

    private fun accepted(message: String) = TrackingOutcome(true, message, state)

    private fun rejected(message: String) = TrackingOutcome(false, message, state)
}
