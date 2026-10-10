package com.smartattend.app.data

import com.smartattend.app.core.model.AttendanceHistoryItem
import com.smartattend.app.core.model.AttendanceStatus
import com.smartattend.app.core.model.CorrectionRequest
import com.smartattend.app.core.model.RegisteredDevice
import com.smartattend.app.core.model.Student
import com.smartattend.app.core.model.StudentNotice
import com.smartattend.app.core.model.StudentSettings
import com.smartattend.app.core.model.SubjectAttendance
import com.smartattend.app.core.model.SyncDisplayState
import com.smartattend.app.core.model.ThemePreference
import com.smartattend.app.core.model.TimetableItem
import kotlinx.coroutines.flow.Flow

sealed interface RepositoryResult {
    data object Success : RepositoryResult
    data class Error(val message: String) : RepositoryResult
}

interface StudentRepository {
    val student: Flow<Student?>
    val subjects: Flow<List<SubjectAttendance>>
    val history: Flow<List<AttendanceHistoryItem>>
    val schedule: Flow<List<TimetableItem>>
    val notices: Flow<List<StudentNotice>>
    val corrections: Flow<List<CorrectionRequest>>
    val device: Flow<RegisteredDevice?>
    val settings: Flow<StudentSettings>
    val pendingOperationCount: Flow<Int>
    val syncState: Flow<SyncDisplayState>

    suspend fun initialize()
    suspend fun setNoticeRead(id: String, isRead: Boolean): RepositoryResult
    suspend fun markAllNoticesRead(): RepositoryResult
    suspend fun submitCorrection(attendanceId: String, requestedStatus: AttendanceStatus, reason: String): RepositoryResult
    suspend fun registerDemoDevice(label: String): RepositoryResult
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setReminderLeadMinutes(minutes: Int)
    suspend fun setTheme(theme: ThemePreference)
    suspend fun resetDemoData(): RepositoryResult
}
