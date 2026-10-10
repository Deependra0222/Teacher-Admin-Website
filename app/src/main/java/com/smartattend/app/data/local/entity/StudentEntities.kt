package com.smartattend.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val rollNumber: String,
    val email: String,
    val department: String,
    val batch: String,
    val updatedAt: Long,
    val sourceMode: String,
)

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val code: String,
    val name: String,
    val attended: Int,
    val total: Int,
    val requiredPercentage: Int,
    val simulated: Boolean,
    val updatedAt: Long,
    val sourceMode: String,
)

@Entity(tableName = "attendance_history", indices = [Index("subjectCode"), Index("sessionId")])
data class AttendanceHistoryEntity(
    @PrimaryKey val id: String,
    val dateEpochDay: Long,
    val subjectCode: String,
    val subjectName: String,
    val status: String,
    val late: Boolean,
    val percentage: Double,
    val simulated: Boolean,
    val sessionId: String,
    val room: String,
    val teacherFinalized: Boolean,
    val updatedAt: Long,
    val sourceMode: String,
)

@Entity(tableName = "schedule", indices = [Index("startEpochMillis"), Index("subjectCode")])
data class ScheduleEntity(
    @PrimaryKey val id: String,
    val subjectCode: String,
    val subjectName: String,
    val room: String,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val active: Boolean,
    val instructor: String,
    val kind: String,
    val simulated: Boolean,
    val updatedAt: Long,
    val sourceMode: String,
)

@Entity(tableName = "notices", indices = [Index("publishedAtEpochMillis"), Index("isRead")])
data class NoticeEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val body: String,
    val publishedAtEpochMillis: Long,
    val dueAtEpochMillis: Long?,
    val isRead: Boolean,
    val simulated: Boolean,
    val updatedAt: Long,
    val sourceMode: String,
)

@Entity(tableName = "correction_requests", indices = [Index("attendanceId"), Index("status")])
data class CorrectionRequestEntity(
    @PrimaryKey val id: String,
    val attendanceId: String,
    val subjectCode: String,
    val currentStatus: String,
    val requestedStatus: String,
    val reason: String,
    val status: String,
    val createdAtEpochMillis: Long,
    val teacherResponse: String?,
    val simulated: Boolean,
    val updatedAt: Long,
    val sourceMode: String,
)

@Entity(tableName = "registered_devices")
data class RegisteredDeviceEntity(
    @PrimaryKey val installationId: String,
    val label: String,
    val state: String,
    val requestedAtEpochMillis: Long?,
    val lastCheckedAtEpochMillis: Long?,
    val simulated: Boolean,
    val updatedAt: Long,
    val sourceMode: String,
)

@Entity(tableName = "pending_operations", indices = [Index(value = ["deduplicationKey"], unique = true)])
data class PendingOperationEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val targetId: String,
    val deduplicationKey: String,
    val payload: String,
    val state: String,
    val createdAtEpochMillis: Long,
    val lastError: String? = null,
)

@Entity(tableName = "metadata")
data class MetadataEntity(
    @PrimaryKey val key: String,
    val value: String,
)
