package com.smartattend.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartattend.app.core.model.AttendanceHistoryItem
import com.smartattend.app.core.model.AttendanceResult
import com.smartattend.app.core.model.AttendanceStatus
import com.smartattend.app.core.model.ClassSession
import com.smartattend.app.core.model.CorrectionRequest
import com.smartattend.app.core.model.RegisteredDevice
import com.smartattend.app.core.model.Student
import com.smartattend.app.core.model.StudentNotice
import com.smartattend.app.core.model.StudentSettings
import com.smartattend.app.core.model.SubjectAttendance
import com.smartattend.app.core.model.SyncDisplayState
import com.smartattend.app.core.model.ThemePreference
import com.smartattend.app.core.model.TimetableItem
import com.smartattend.app.data.RepositoryResult
import com.smartattend.app.data.StudentRepository
import com.smartattend.app.data.demo.DemoDataRepository
import com.smartattend.app.data.firebase.FirebaseAuthenticationRepository
import com.smartattend.app.data.local.AppPreferencesRepository
import com.smartattend.app.data.local.DemoPreferences
import com.smartattend.app.data.local.OfflineStudentRepository
import com.smartattend.app.data.local.SmartAttendDatabase
import com.smartattend.app.domain.attendance.AttendanceTracker
import com.smartattend.app.domain.auth.AuthError
import com.smartattend.app.domain.auth.AuthSessionMode
import com.smartattend.app.domain.auth.AuthenticationCoordinator
import com.smartattend.app.domain.auth.AuthenticationState
import com.smartattend.app.domain.auth.PasswordResetResult
import com.smartattend.app.domain.auth.userMessage
import com.smartattend.app.domain.presence.BleDecision
import com.smartattend.app.domain.presence.BleDecisionConfig
import com.smartattend.app.domain.presence.BleDecisionKind
import com.smartattend.app.domain.presence.BlePresenceDecisionEngine
import com.smartattend.app.domain.presence.BleSample
import com.smartattend.app.domain.presence.PresenceSignalType
import com.smartattend.app.domain.presence.SimulatedPresenceSource
import com.smartattend.app.domain.presence.SystemAppClock
import com.smartattend.app.domain.session.SessionAvailability
import com.smartattend.app.domain.session.SessionDisplayState
import com.smartattend.app.domain.shortage.ShortageCalculator
import com.smartattend.app.domain.shortage.ShortageProjection
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class SmartAttendUiState(
    val authMode: AuthSessionMode = AuthSessionMode.RESTORING,
    val isAuthenticating: Boolean = false,
    val isLoading: Boolean = true,
    val fatalError: String? = null,
    val loginError: String? = null,
    val message: String? = null,
    val student: Student,
    val session: ClassSession,
    val sessionState: SessionDisplayState,
    val timetable: List<TimetableItem> = emptyList(),
    val subjects: List<SubjectAttendance> = emptyList(),
    val history: List<AttendanceHistoryItem> = emptyList(),
    val notices: List<StudentNotice> = emptyList(),
    val corrections: List<CorrectionRequest> = emptyList(),
    val device: RegisteredDevice? = null,
    val settings: StudentSettings = StudentSettings(),
    val pendingOperations: Int = 0,
    val syncState: SyncDisplayState = SyncDisplayState.BACKEND_UNCONFIGURED,
    val insideClassroom: Boolean = false,
    val confirmedMinutes: Long = 0,
    val finalResult: AttendanceResult? = null,
    val bleDecision: BleDecision? = null,
    val selectedScheduleDate: LocalDate = LocalDate.now(ZoneId.of("Asia/Kolkata")),
    val noticeFilter: String = "ALL",
    val historySubjectFilter: String = "ALL",
    val historyStatusFilter: String = "ALL",
    val calculatorAttended: String = "18",
    val calculatorTotal: String = "25",
    val calculatorTarget: String = "75",
    val calculatorResult: ShortageProjection? = null,
    val calculatorError: String? = null,
) {
    val isLoggedIn: Boolean
        get() = authMode == AuthSessionMode.DEMO || authMode == AuthSessionMode.FIREBASE
}

class SmartAttendViewModel(application: Application) : AndroidViewModel(application) {
    private val legacyRepository = DemoDataRepository()
    private val preferences = DemoPreferences(application)
    private val appPreferences = AppPreferencesRepository(application)
    private val authentication = AuthenticationCoordinator(
        repository = FirebaseAuthenticationRepository(),
        sessionStore = appPreferences,
        demoStudent = legacyRepository.student,
    )
    private val repository: StudentRepository = OfflineStudentRepository(
        SmartAttendDatabase.getInstance(application),
        appPreferences,
    )
    private val clock = SystemAppClock
    private val source = SimulatedPresenceSource(clock)
    private val startupNow = clock.now()
    private val storedSession = preferences.loadOrCreateSession(startupNow)
    private val session = legacyRepository.activeSession(storedSession.start, storedSession.end, startupNow)
    private val initialSnapshot = preferences.loadPresence()
    private val tracker = AttendanceTracker(legacyRepository.student, session, clock, initialSnapshot = initialSnapshot)
    private val restoredResult = preferences.loadResult() ?: if (
        !startupNow.isBefore(session.end) && (initialSnapshot.inside || initialSnapshot.confirmed.toMillis() > 0L)
    ) {
        tracker.finish().also {
            preferences.savePresence(tracker.snapshot())
            preferences.saveResult(it)
        }
    } else null
    private val bleEngine = BlePresenceDecisionEngine(
        BleDecisionConfig(
            sessionId = session.id,
            allowedBeaconIds = setOf("ESP32-DEMO-A", "ESP32-DEMO-B"),
            requiredBeaconCount = 2,
            windowSize = 3,
            emaAlpha = 0.5,
            entryThresholdDbm = -70.0,
            exitThresholdDbm = -85.0,
            verificationDuration = Duration.ofSeconds(10),
            signalLossTimeout = Duration.ofSeconds(20),
            simulationOnly = true,
        ),
    )
    private var bleCursor: Instant = startupNow
    private var repositoryObservationJob: Job? = null

    var uiState by mutableStateOf(baseState().copy(finalResult = restoredResult))
        private set

    init {
        initializeRepository()
        observeRepository()
        restoreAuthentication()
    }

    fun firebaseLogin(email: String, password: String) {
        if (uiState.isAuthenticating) return
        uiState = uiState.copy(isAuthenticating = true, loginError = null, message = null)
        viewModelScope.launch {
            applyAuthenticationState(
                runCatching { authentication.signIn(email, password) }
                    .getOrElse { AuthenticationState(AuthSessionMode.SIGNED_OUT, error = AuthError.UNKNOWN) },
                successMessage = "Firebase account authenticated. Attendance data is still DEMO and not synchronized.",
            )
        }
    }

    fun enterDemoMode() {
        if (uiState.isAuthenticating) return
        uiState = uiState.copy(isAuthenticating = true, loginError = null, message = null)
        viewModelScope.launch {
            applyAuthenticationState(
                runCatching { authentication.enterDemoMode() }
                    .getOrElse { AuthenticationState(AuthSessionMode.SIGNED_OUT, error = AuthError.UNKNOWN) },
                successMessage = "Entered DEMO mode with offline sample data",
            )
        }
    }

    fun sendPasswordReset(email: String) {
        if (uiState.isAuthenticating) return
        uiState = uiState.copy(isAuthenticating = true, loginError = null, message = null)
        viewModelScope.launch {
            uiState = when (val result = runCatching { authentication.sendPasswordReset(email) }
                .getOrElse { PasswordResetResult.Failure(AuthError.UNKNOWN) }) {
                PasswordResetResult.Sent -> uiState.copy(
                    isAuthenticating = false,
                    loginError = null,
                    message = "Password reset email requested. Check your inbox if the address is registered.",
                )
                is PasswordResetResult.Failure -> uiState.copy(
                    isAuthenticating = false,
                    loginError = result.error.userMessage(),
                )
            }
        }
    }

    fun logout() {
        if (uiState.isAuthenticating) return
        uiState = uiState.copy(isAuthenticating = true, message = null)
        viewModelScope.launch {
            applyAuthenticationState(
                runCatching { authentication.logout() }
                    .getOrElse { AuthenticationState(AuthSessionMode.SIGNED_OUT, error = AuthError.UNKNOWN) },
            )
        }
    }

    fun enterClassroom() = record(PresenceSignalType.ENTER)
    fun exitClassroom() = record(PresenceSignalType.EXIT)

    fun finishDemoSession() {
        val result = tracker.finish()
        preferences.savePresence(tracker.snapshot())
        preferences.saveResult(result)
        refreshTrackerState("Demo result calculated locally; teacher verification is still required", result)
    }

    fun simulateBlePresence() {
        val base = maxOf(clock.now(), bleCursor.plusSeconds(1))
        val decisions = listOf(
            bleEngine.process(sample("ESP32-DEMO-A", -58, base)),
            bleEngine.process(sample("ESP32-DEMO-B", -61, base.plusMillis(100))),
            bleEngine.process(sample("ESP32-DEMO-A", -59, base.plusSeconds(11))),
            bleEngine.process(sample("ESP32-DEMO-B", -62, base.plusSeconds(11).plusMillis(100))),
        )
        val decision = decisions.firstOrNull { it.kind == BleDecisionKind.ENTERED } ?: decisions.last()
        bleCursor = base.plusSeconds(11).plusMillis(100)
        if (decision.kind == BleDecisionKind.ENTERED && !tracker.snapshot().inside) record(PresenceSignalType.ENTER)
        uiState = uiState.copy(bleDecision = decision, message = "SIMULATED BLE: ${decision.reason}. Not verified attendance.")
    }

    fun simulateBleSignalLoss() {
        bleCursor = maxOf(clock.now(), bleCursor).plusSeconds(21)
        val decision = bleEngine.advanceTime(bleCursor)
        if (decision.kind in setOf(BleDecisionKind.SIGNAL_LOST, BleDecisionKind.EXITED) && tracker.snapshot().inside) {
            record(PresenceSignalType.EXIT)
        }
        uiState = uiState.copy(bleDecision = decision, message = "SIMULATED BLE: ${decision.reason}")
    }

    fun setScheduleDate(date: LocalDate) { uiState = uiState.copy(selectedScheduleDate = date) }
    fun setNoticeFilter(filter: String) { uiState = uiState.copy(noticeFilter = filter) }
    fun setHistorySubjectFilter(filter: String) { uiState = uiState.copy(historySubjectFilter = filter) }
    fun setHistoryStatusFilter(filter: String) { uiState = uiState.copy(historyStatusFilter = filter) }

    fun setCalculatorAttended(value: String) { uiState = uiState.copy(calculatorAttended = value, calculatorError = null) }
    fun setCalculatorTotal(value: String) { uiState = uiState.copy(calculatorTotal = value, calculatorError = null) }
    fun setCalculatorTarget(value: String) { uiState = uiState.copy(calculatorTarget = value, calculatorError = null) }
    fun calculateShortage() {
        val attended = uiState.calculatorAttended.toIntOrNull()
        val total = uiState.calculatorTotal.toIntOrNull()
        val target = uiState.calculatorTarget.toIntOrNull()
        if (attended == null || total == null || target == null) {
            uiState = uiState.copy(calculatorResult = null, calculatorError = "Enter whole numbers in all fields")
            return
        }
        val result = ShortageCalculator().calculate(attended, total, target)
        uiState = uiState.copy(calculatorResult = result, calculatorError = (result as? ShortageProjection.Invalid)?.reason)
    }

    fun setNoticeRead(id: String, read: Boolean) = repositoryAction(
        if (read) "Notice marked read offline" else "Notice marked unread offline",
    ) { repository.setNoticeRead(id, read) }

    fun markAllNoticesRead() = repositoryAction("All notices marked read offline") { repository.markAllNoticesRead() }

    fun submitCorrection(attendanceId: String, status: AttendanceStatus, reason: String) =
        repositoryAction("Correction saved locally and pending backend configuration") {
            repository.submitCorrection(attendanceId, status, reason)
        }

    fun registerDevice(label: String) = repositoryAction("Device saved as PENDING SERVER VERIFICATION") {
        repository.registerDemoDevice(label)
    }

    fun setNotificationsEnabled(enabled: Boolean) = launchPreference("Notification preference saved offline") {
        repository.setNotificationsEnabled(enabled)
    }

    fun setReminderMinutes(minutes: Int) = launchPreference("Reminder preference saved offline") {
        repository.setReminderLeadMinutes(minutes)
    }

    fun setTheme(theme: ThemePreference) = launchPreference("Theme preference saved offline") { repository.setTheme(theme) }
    fun resetDemoData() = repositoryAction("Demo records reset to their original state") { repository.resetDemoData() }
    fun retryOfflineData() {
        observeRepository()
        initializeRepository()
    }
    fun clearMessage() { uiState = uiState.copy(message = null) }

    private fun initializeRepository() {
        uiState = uiState.copy(isLoading = true, fatalError = null)
        viewModelScope.launch {
            runCatching { repository.initialize() }
                .onFailure { uiState = uiState.copy(isLoading = false, fatalError = it.message ?: "Offline data could not be opened") }
        }
    }

    private fun observeRepository() {
        repositoryObservationJob?.cancel()
        repositoryObservationJob = viewModelScope.launch {
            launch {
                repository.student.catch { repositoryFailure(it) }.collect { value ->
                    if (value != null && uiState.authMode != AuthSessionMode.FIREBASE) {
                        uiState = uiState.copy(student = value, isLoading = false, fatalError = null)
                    } else if (value != null) {
                        uiState = uiState.copy(isLoading = false, fatalError = null)
                    }
                }
            }
            launch { repository.subjects.catch { repositoryFailure(it) }.collect { uiState = uiState.copy(subjects = it) } }
            launch { repository.history.catch { repositoryFailure(it) }.collect { uiState = uiState.copy(history = it) } }
            launch { repository.schedule.catch { repositoryFailure(it) }.collect { uiState = uiState.copy(timetable = it) } }
            launch { repository.notices.catch { repositoryFailure(it) }.collect { uiState = uiState.copy(notices = it) } }
            launch { repository.corrections.catch { repositoryFailure(it) }.collect { uiState = uiState.copy(corrections = it) } }
            launch { repository.device.catch { repositoryFailure(it) }.collect { uiState = uiState.copy(device = it) } }
            launch { repository.settings.catch { repositoryFailure(it) }.collect { uiState = uiState.copy(settings = it) } }
            launch { repository.pendingOperationCount.catch { repositoryFailure(it) }.collect { uiState = uiState.copy(pendingOperations = it) } }
            launch { repository.syncState.catch { repositoryFailure(it) }.collect { uiState = uiState.copy(syncState = it) } }
        }
    }

    private fun repositoryFailure(error: Throwable) {
        uiState = uiState.copy(
            isLoading = false,
            fatalError = error.message ?: "Offline data stream failed",
        )
    }

    private fun repositoryAction(successMessage: String, action: suspend () -> RepositoryResult) {
        viewModelScope.launch {
            uiState = when (val result = runCatching { action() }.getOrElse { RepositoryResult.Error(it.message ?: "Offline operation failed") }) {
                RepositoryResult.Success -> uiState.copy(message = successMessage)
                is RepositoryResult.Error -> uiState.copy(message = result.message)
            }
        }
    }

    private fun launchPreference(successMessage: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { action() }
                .onSuccess { uiState = uiState.copy(message = successMessage) }
                .onFailure { uiState = uiState.copy(message = it.message ?: "Preference could not be saved") }
        }
    }

    private fun record(type: PresenceSignalType) {
        val outcome = tracker.record(source.signal(type))
        if (outcome.accepted) preferences.savePresence(outcome.snapshot)
        refreshTrackerState(outcome.message)
    }

    private fun refreshTrackerState(message: String, result: AttendanceResult? = uiState.finalResult) {
        val snapshot = tracker.snapshot()
        uiState = uiState.copy(
            insideClassroom = snapshot.inside,
            confirmedMinutes = snapshot.confirmed.toMinutes(),
            finalResult = result,
            message = message,
        )
    }

    private fun sample(beacon: String, rssi: Int, at: Instant) = BleSample(session.id, beacon, rssi, at, simulated = true)

    private fun restoreAuthentication() {
        viewModelScope.launch {
            applyAuthenticationState(
                runCatching { authentication.restore() }
                    .getOrElse { AuthenticationState(AuthSessionMode.SIGNED_OUT, error = AuthError.UNKNOWN) },
            )
        }
    }

    private fun applyAuthenticationState(state: AuthenticationState, successMessage: String? = null) {
        val student = when (state.mode) {
            AuthSessionMode.FIREBASE,
            AuthSessionMode.DEMO,
            -> state.student ?: legacyRepository.student
            AuthSessionMode.RESTORING,
            AuthSessionMode.SIGNED_OUT,
            -> legacyRepository.student
        }
        uiState = uiState.copy(
            authMode = state.mode,
            isAuthenticating = false,
            loginError = state.error?.userMessage(),
            message = if (state.error == null) successMessage else null,
            student = student,
        )
    }

    private fun baseState(): SmartAttendUiState {
        val snapshot = tracker.snapshot()
        return SmartAttendUiState(
            authMode = AuthSessionMode.RESTORING,
            student = legacyRepository.student,
            session = session,
            sessionState = SessionAvailability.determine(legacyRepository.student, session, clock.now()),
            insideClassroom = snapshot.inside,
            confirmedMinutes = snapshot.confirmed.toMinutes(),
        )
    }
}
