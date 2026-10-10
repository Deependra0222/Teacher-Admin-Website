package com.smartattend.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.smartattend.app.core.model.AttendanceHistoryItem
import com.smartattend.app.core.model.AttendanceResult
import com.smartattend.app.core.model.ClassSession
import com.smartattend.app.core.model.Student
import com.smartattend.app.core.model.SubjectAttendance
import com.smartattend.app.core.model.TimetableItem
import com.smartattend.app.data.demo.DemoDataRepository
import com.smartattend.app.data.local.DemoPreferences
import com.smartattend.app.domain.attendance.AttendanceTracker
import com.smartattend.app.domain.auth.LoginValidator
import com.smartattend.app.domain.presence.PresenceSignalType
import com.smartattend.app.domain.presence.SimulatedPresenceSource
import com.smartattend.app.domain.presence.SystemAppClock
import com.smartattend.app.domain.session.SessionAvailability
import com.smartattend.app.domain.session.SessionDisplayState

enum class AppDestination(val label: String, val shortLabel: String) {
    HOME("Home", "H"),
    ATTENDANCE("Attendance", "A"),
    HISTORY("History", "R"),
    TIMETABLE("Timetable", "T"),
    PROFILE("Profile", "P"),
}

data class SmartAttendUiState(
    val isLoggedIn: Boolean,
    val isLoading: Boolean = false,
    val loginError: String? = null,
    val message: String? = null,
    val destination: AppDestination = AppDestination.HOME,
    val student: Student,
    val session: ClassSession,
    val sessionState: SessionDisplayState,
    val timetable: List<TimetableItem>,
    val subjects: List<SubjectAttendance>,
    val history: List<AttendanceHistoryItem>,
    val insideClassroom: Boolean,
    val confirmedMinutes: Long,
    val finalResult: AttendanceResult? = null,
)

class SmartAttendViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DemoDataRepository()
    private val preferences = DemoPreferences(application)
    private val clock = SystemAppClock
    private val source = SimulatedPresenceSource(clock)
    private val startupNow = clock.now()
    private val storedSession = preferences.loadOrCreateSession(startupNow)
    private val session = repository.activeSession(storedSession.start, storedSession.end, startupNow)
    private val initialSnapshot = preferences.loadPresence()
    private val tracker = AttendanceTracker(
        student = repository.student,
        session = session,
        clock = clock,
        initialSnapshot = initialSnapshot,
    )
    private val restoredResult = preferences.loadResult() ?: if (
        !startupNow.isBefore(session.end) &&
        (initialSnapshot.inside || initialSnapshot.confirmed.toMillis() > 0L)
    ) {
        tracker.finish().also {
            preferences.savePresence(tracker.snapshot())
            preferences.saveResult(it)
        }
    } else {
        null
    }

    var uiState by mutableStateOf(buildState(isLoggedIn = preferences.isLoggedIn).copy(finalResult = restoredResult))
        private set

    fun login(identifier: String, password: String) {
        val error = LoginValidator.validate(identifier, password)
        if (error != null) {
            uiState = uiState.copy(loginError = error)
            return
        }
        preferences.setLoggedIn(true)
        uiState = buildState(isLoggedIn = true).copy(message = "Signed in with local demo data")
    }

    fun logout() {
        preferences.setLoggedIn(false)
        uiState = buildState(isLoggedIn = false)
    }

    fun select(destination: AppDestination) {
        uiState = uiState.copy(destination = destination, message = null)
    }

    fun enterClassroom() = record(PresenceSignalType.ENTER)

    fun exitClassroom() = record(PresenceSignalType.EXIT)

    fun finishDemoSession() {
        val result = tracker.finish()
        preferences.savePresence(tracker.snapshot())
        preferences.saveResult(result)
        uiState = buildState(isLoggedIn = true).copy(
            finalResult = result,
            message = "Demo result calculated locally; teacher verification is still required",
        )
    }

    fun clearMessage() {
        uiState = uiState.copy(message = null)
    }

    private fun record(type: PresenceSignalType) {
        val outcome = tracker.record(source.signal(type))
        if (outcome.accepted) preferences.savePresence(outcome.snapshot)
        uiState = buildState(isLoggedIn = true).copy(message = outcome.message)
    }

    private fun buildState(isLoggedIn: Boolean): SmartAttendUiState {
        val snapshot = tracker.snapshot()
        return SmartAttendUiState(
            isLoggedIn = isLoggedIn,
            student = repository.student,
            session = session,
            sessionState = SessionAvailability.determine(repository.student, session, clock.now()),
            timetable = repository.timetable(session),
            subjects = repository.subjectAttendance,
            history = repository.history,
            insideClassroom = snapshot.inside,
            confirmedMinutes = snapshot.confirmed.toMinutes(),
        )
    }
}
