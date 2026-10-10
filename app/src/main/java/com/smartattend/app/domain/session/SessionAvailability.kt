package com.smartattend.app.domain.session

import com.smartattend.app.core.model.ClassSession
import com.smartattend.app.core.model.SessionStatus
import com.smartattend.app.core.model.Student
import java.time.Instant

enum class SessionDisplayState {
    NO_CLASS,
    UPCOMING,
    ACTIVE_ELIGIBLE,
    ACTIVE_INELIGIBLE,
}

object SessionAvailability {
    fun determine(
        student: Student,
        session: ClassSession?,
        now: Instant,
    ): SessionDisplayState {
        if (session == null || session.status == SessionStatus.ENDED || !now.isBefore(session.end)) {
            return SessionDisplayState.NO_CLASS
        }
        if (now.isBefore(session.start) || session.status == SessionStatus.SCHEDULED) {
            return SessionDisplayState.UPCOMING
        }
        return if (student.batch == session.batch) {
            SessionDisplayState.ACTIVE_ELIGIBLE
        } else {
            SessionDisplayState.ACTIVE_INELIGIBLE
        }
    }
}

