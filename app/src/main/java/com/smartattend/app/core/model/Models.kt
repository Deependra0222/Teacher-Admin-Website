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
)

