package com.smartattend.app.core.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate

data class Student(
    val id: String,
    val name: String,
    val rollNumber: String,
    val email: String,
    val department: String,
    val batch: String,
)

data class SubjectAttendance(
    val code: String,
    val name: String,
    val attended: Int,
    val total: Int,
    val requiredPercentage: Int = 75,
    val simulated: Boolean = true,
) {
    val percentage: Double
        get() = if (total == 0) 0.0 else attended.toDouble() / total * 100.0
}

enum class AttendanceStatus { PRESENT, REVIEW, ABSENT }

data class AttendanceResult(
    val confirmed: Duration,
    val percentage: Double,
    val status: AttendanceStatus,
    val late: Boolean,
)

enum class SessionStatus { SCHEDULED, ACTIVE, ENDED }

enum class SessionKind { LECTURE, LAB }

data class ClassSession(
    val id: String,
    val subjectId: String,
    val subjectName: String,
    val room: String,
    val batch: String,
    val start: Instant,
    val end: Instant,
    val gracePeriod: Duration,
    val status: SessionStatus,
)

data class TimetableItem(
    val id: String,
    val subjectCode: String,
    val subjectName: String,
    val room: String,
    val start: Instant,
    val end: Instant,
    val active: Boolean,
    val instructor: String = "Demo Faculty",
    val kind: SessionKind = SessionKind.LECTURE,
    val simulated: Boolean = true,
)

data class AttendanceHistoryItem(
    val id: String,
    val date: LocalDate,
    val subjectCode: String,
    val subjectName: String,
    val status: AttendanceStatus,
    val late: Boolean,
    val percentage: Double,
    val simulated: Boolean,
    val sessionId: String = id,
    val room: String = "Classroom",
    val teacherFinalized: Boolean = false,
)

enum class NoticeType { ANNOUNCEMENT, ASSIGNMENT, ATTENDANCE_ALERT, CORRECTION_UPDATE }

data class StudentNotice(
    val id: String,
    val type: NoticeType,
    val title: String,
    val body: String,
    val publishedAt: Instant,
    val dueAt: Instant? = null,
    val isRead: Boolean = false,
    val simulated: Boolean = true,
)

enum class CorrectionRequestStatus { PENDING, APPROVED, REJECTED, SYNC_FAILED }

data class CorrectionRequest(
    val id: String,
    val attendanceId: String,
    val subjectCode: String,
    val currentStatus: AttendanceStatus,
    val requestedStatus: AttendanceStatus,
    val reason: String,
    val status: CorrectionRequestStatus,
    val createdAt: Instant,
    val teacherResponse: String? = null,
    val simulated: Boolean = true,
)

enum class DeviceRegistrationState { NOT_REGISTERED, PENDING_SERVER_VERIFICATION }

data class RegisteredDevice(
    val installationId: String,
    val label: String,
    val state: DeviceRegistrationState,
    val requestedAt: Instant? = null,
    val lastCheckedAt: Instant? = null,
    val simulated: Boolean = true,
)

enum class BackendMode { DEMO_ONLY, UNCONFIGURED, CONFIGURED }

enum class SyncDisplayState {
    DEMO_ONLY,
    BACKEND_UNCONFIGURED,
    OFFLINE,
    OFFLINE_PENDING,
    SYNCING,
    SYNCHRONIZED,
    FAILED,
}

enum class ThemePreference { SYSTEM, LIGHT, DARK }

data class StudentSettings(
    val notificationsEnabled: Boolean = true,
    val reminderLeadMinutes: Int = 30,
    val theme: ThemePreference = ThemePreference.SYSTEM,
)

