package com.smartattend.app.data.local

import com.smartattend.app.core.model.AttendanceHistoryItem
import com.smartattend.app.core.model.AttendanceStatus
import com.smartattend.app.core.model.CorrectionRequest
import com.smartattend.app.core.model.CorrectionRequestStatus
import com.smartattend.app.core.model.DeviceRegistrationState
import com.smartattend.app.core.model.NoticeType
import com.smartattend.app.core.model.RegisteredDevice
import com.smartattend.app.core.model.SessionKind
import com.smartattend.app.core.model.Student
import com.smartattend.app.core.model.StudentNotice
import com.smartattend.app.core.model.SubjectAttendance
import com.smartattend.app.core.model.TimetableItem
import com.smartattend.app.data.local.entity.AttendanceHistoryEntity
import com.smartattend.app.data.local.entity.CorrectionRequestEntity
import com.smartattend.app.data.local.entity.NoticeEntity
import com.smartattend.app.data.local.entity.RegisteredDeviceEntity
import com.smartattend.app.data.local.entity.ScheduleEntity
import com.smartattend.app.data.local.entity.StudentEntity
import com.smartattend.app.data.local.entity.SubjectEntity
import java.time.Instant
import java.time.LocalDate

fun StudentEntity.toDomain() = Student(id, name, rollNumber, email, department, batch)

fun SubjectEntity.toDomain() = SubjectAttendance(code, name, attended, total, requiredPercentage, simulated)

fun AttendanceHistoryEntity.toDomain() = AttendanceHistoryItem(
    id = id,
    date = LocalDate.ofEpochDay(dateEpochDay),
    subjectCode = subjectCode,
    subjectName = subjectName,
    status = AttendanceStatus.valueOf(status),
    late = late,
    percentage = percentage,
    simulated = simulated,
    sessionId = sessionId,
    room = room,
    teacherFinalized = teacherFinalized,
)

fun ScheduleEntity.toDomain() = TimetableItem(
    id = id,
    subjectCode = subjectCode,
    subjectName = subjectName,
    room = room,
    start = Instant.ofEpochMilli(startEpochMillis),
    end = Instant.ofEpochMilli(endEpochMillis),
    active = active,
    instructor = instructor,
    kind = SessionKind.valueOf(kind),
    simulated = simulated,
)

fun NoticeEntity.toDomain() = StudentNotice(
    id = id,
    type = NoticeType.valueOf(type),
    title = title,
    body = body,
    publishedAt = Instant.ofEpochMilli(publishedAtEpochMillis),
    dueAt = dueAtEpochMillis?.let(Instant::ofEpochMilli),
    isRead = isRead,
    simulated = simulated,
)

fun CorrectionRequestEntity.toDomain() = CorrectionRequest(
    id = id,
    attendanceId = attendanceId,
    subjectCode = subjectCode,
    currentStatus = AttendanceStatus.valueOf(currentStatus),
    requestedStatus = AttendanceStatus.valueOf(requestedStatus),
    reason = reason,
    status = CorrectionRequestStatus.valueOf(status),
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    teacherResponse = teacherResponse,
    simulated = simulated,
)

fun RegisteredDeviceEntity.toDomain() = RegisteredDevice(
    installationId = installationId,
    label = label,
    state = DeviceRegistrationState.valueOf(state),
    requestedAt = requestedAtEpochMillis?.let(Instant::ofEpochMilli),
    lastCheckedAt = lastCheckedAtEpochMillis?.let(Instant::ofEpochMilli),
    simulated = simulated,
)
