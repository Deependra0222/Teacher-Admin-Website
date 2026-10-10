package com.smartattend.app.data.local

import androidx.room.withTransaction
import com.smartattend.app.core.model.AttendanceStatus
import com.smartattend.app.core.model.BackendMode
import com.smartattend.app.core.model.DeviceRegistrationState
import com.smartattend.app.core.model.ThemePreference
import com.smartattend.app.data.RepositoryResult
import com.smartattend.app.data.StudentRepository
import com.smartattend.app.data.demo.StudentDemoSeed
import com.smartattend.app.data.demo.StudentDemoSeedBundle
import com.smartattend.app.data.local.entity.CorrectionRequestEntity
import com.smartattend.app.data.local.entity.MetadataEntity
import com.smartattend.app.data.local.entity.PendingOperationEntity
import com.smartattend.app.data.local.entity.RegisteredDeviceEntity
import com.smartattend.app.domain.correction.CorrectionRequestPolicy
import com.smartattend.app.domain.sync.SyncStateResolver
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class OfflineStudentRepository(
    private val database: SmartAttendDatabase,
    private val preferences: AppPreferencesRepository,
    private val clock: Clock = Clock.system(ZoneId.of("Asia/Kolkata")),
) : StudentRepository {
    private val dao = database.studentDao()
    private val initializationMutex = Mutex()

    override val student = dao.observeStudent().map { it?.toDomain() }
    override val subjects = dao.observeSubjects().map { rows -> rows.map { it.toDomain() } }
    override val history = dao.observeHistory().map { rows -> rows.map { it.toDomain() } }
    override val schedule = dao.observeSchedule().map { rows -> rows.map { it.toDomain() } }
    override val notices = dao.observeNotices().map { rows -> rows.map { it.toDomain() } }
    override val corrections = dao.observeCorrections().map { rows -> rows.map { it.toDomain() } }
    override val device = dao.observeDevice().map { it?.toDomain() }
    override val settings = preferences.settings
    override val pendingOperationCount = dao.observePendingOperations().map { it.size }
    override val syncState = combine(pendingOperationCount, preferences.seedVersion) { pending, _ ->
        SyncStateResolver.resolve(
            backendMode = BackendMode.UNCONFIGURED,
            isConnected = false,
            pendingOperations = pending,
        )
    }

    override suspend fun initialize() = initializationMutex.withLock {
        if (dao.metadataValue(SEED_VERSION_KEY) == StudentDemoSeed.VERSION.toString()) return
        seed(StudentDemoSeed.create(LocalDate.now(clock), Instant.now(clock)), clearFirst = false)
        preferences.setSeedVersion(StudentDemoSeed.VERSION)
    }

    override suspend fun setNoticeRead(id: String, isRead: Boolean): RepositoryResult {
        val now = Instant.now(clock).toEpochMilli()
        return if (dao.setNoticeRead(id, isRead, now) == 0) {
            RepositoryResult.Error("Notice not found")
        } else {
            dao.upsertPendingOperation(pending("NOTICE_READ", id, "notice-read-$id", isRead.toString(), now))
            RepositoryResult.Success
        }
    }

    override suspend fun markAllNoticesRead(): RepositoryResult {
        val now = Instant.now(clock).toEpochMilli()
        dao.markAllNoticesRead(now)
        dao.upsertPendingOperation(pending("NOTICES_READ_ALL", "all", "notices-read-all", "true", now))
        return RepositoryResult.Success
    }

    override suspend fun submitCorrection(
        attendanceId: String,
        requestedStatus: AttendanceStatus,
        reason: String,
    ): RepositoryResult = database.withTransaction {
        val attendance = dao.findHistory(attendanceId)
            ?: return@withTransaction RepositoryResult.Error("Attendance record not found")
        val validation = CorrectionRequestPolicy.validate(
            currentStatus = AttendanceStatus.valueOf(attendance.status),
            requestedStatus = requestedStatus,
            reason = reason,
            hasOpenRequest = dao.findOpenCorrection(attendanceId) != null,
        )
        if (validation != null) return@withTransaction RepositoryResult.Error(validation)

        val now = Instant.now(clock).toEpochMilli()
        val id = "correction-${UUID.randomUUID()}"
        dao.insertCorrection(
            CorrectionRequestEntity(
                id = id,
                attendanceId = attendanceId,
                subjectCode = attendance.subjectCode,
                currentStatus = attendance.status,
                requestedStatus = requestedStatus.name,
                reason = reason.trim(),
                status = "PENDING",
                createdAtEpochMillis = now,
                teacherResponse = null,
                simulated = true,
                updatedAt = now,
                sourceMode = "DEMO",
            ),
        )
        dao.upsertPendingOperation(pending("CORRECTION_SUBMIT", id, "correction-$attendanceId", reason.trim(), now))
        RepositoryResult.Success
    }

    override suspend fun registerDemoDevice(label: String): RepositoryResult {
        if (label.trim().length < 2) return RepositoryResult.Error("Enter a device label")
        val now = Instant.now(clock).toEpochMilli()
        val installationId = preferences.installationId()
        database.withTransaction {
            dao.upsertDevice(
                RegisteredDeviceEntity(
                    installationId = installationId,
                    label = label.trim(),
                    state = DeviceRegistrationState.PENDING_SERVER_VERIFICATION.name,
                    requestedAtEpochMillis = now,
                    lastCheckedAtEpochMillis = null,
                    simulated = true,
                    updatedAt = now,
                    sourceMode = "DEMO",
                ),
            )
            dao.upsertPendingOperation(pending("DEVICE_REGISTRATION", installationId, "device-registration", label.trim(), now))
        }
        return RepositoryResult.Success
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) = preferences.setNotificationsEnabled(enabled)
    override suspend fun setReminderLeadMinutes(minutes: Int) = preferences.setReminderLeadMinutes(minutes)
    override suspend fun setTheme(theme: ThemePreference) = preferences.setTheme(theme)

    override suspend fun resetDemoData(): RepositoryResult {
        seed(StudentDemoSeed.create(LocalDate.now(clock), Instant.now(clock)), clearFirst = true)
        preferences.setSeedVersion(StudentDemoSeed.VERSION)
        return RepositoryResult.Success
    }

    private suspend fun seed(bundle: StudentDemoSeedBundle, clearFirst: Boolean) = database.withTransaction {
        if (clearFirst) dao.clearAllStudentData()
        dao.upsertStudent(bundle.student)
        dao.upsertSubjects(bundle.subjects)
        dao.upsertHistory(bundle.history)
        dao.upsertSchedule(bundle.schedule)
        dao.upsertNotices(bundle.notices)
        dao.upsertCorrections(bundle.corrections)
        dao.upsertMetadata(MetadataEntity(SEED_VERSION_KEY, StudentDemoSeed.VERSION.toString()))
    }

    private fun pending(kind: String, targetId: String, key: String, payload: String, now: Long) =
        PendingOperationEntity(
            id = "operation-${UUID.randomUUID()}",
            kind = kind,
            targetId = targetId,
            deduplicationKey = key,
            payload = payload,
            state = "PENDING_BACKEND_CONFIGURATION",
            createdAtEpochMillis = now,
        )

    private companion object {
        const val SEED_VERSION_KEY = "demo_seed_version"
    }
}
