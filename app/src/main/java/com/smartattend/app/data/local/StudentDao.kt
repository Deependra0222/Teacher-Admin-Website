package com.smartattend.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.smartattend.app.data.local.entity.AttendanceHistoryEntity
import com.smartattend.app.data.local.entity.CorrectionRequestEntity
import com.smartattend.app.data.local.entity.MetadataEntity
import com.smartattend.app.data.local.entity.NoticeEntity
import com.smartattend.app.data.local.entity.PendingOperationEntity
import com.smartattend.app.data.local.entity.RegisteredDeviceEntity
import com.smartattend.app.data.local.entity.ScheduleEntity
import com.smartattend.app.data.local.entity.StudentEntity
import com.smartattend.app.data.local.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students LIMIT 1") fun observeStudent(): Flow<StudentEntity?>
    @Query("SELECT * FROM subjects ORDER BY code") fun observeSubjects(): Flow<List<SubjectEntity>>
    @Query("SELECT * FROM attendance_history ORDER BY dateEpochDay DESC, id") fun observeHistory(): Flow<List<AttendanceHistoryEntity>>
    @Query("SELECT * FROM schedule ORDER BY startEpochMillis") fun observeSchedule(): Flow<List<ScheduleEntity>>
    @Query("SELECT * FROM notices ORDER BY publishedAtEpochMillis DESC") fun observeNotices(): Flow<List<NoticeEntity>>
    @Query("SELECT * FROM correction_requests ORDER BY createdAtEpochMillis DESC") fun observeCorrections(): Flow<List<CorrectionRequestEntity>>
    @Query("SELECT * FROM registered_devices LIMIT 1") fun observeDevice(): Flow<RegisteredDeviceEntity?>
    @Query("SELECT * FROM pending_operations ORDER BY createdAtEpochMillis") fun observePendingOperations(): Flow<List<PendingOperationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertStudent(value: StudentEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertSubjects(values: List<SubjectEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertHistory(values: List<AttendanceHistoryEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertSchedule(values: List<ScheduleEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertNotices(values: List<NoticeEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertCorrection(value: CorrectionRequestEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertCorrections(values: List<CorrectionRequestEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertDevice(value: RegisteredDeviceEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertPendingOperation(value: PendingOperationEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertMetadata(value: MetadataEntity)

    @Query("UPDATE notices SET isRead = :isRead, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setNoticeRead(id: String, isRead: Boolean, updatedAt: Long): Int

    @Query("UPDATE notices SET isRead = 1, updatedAt = :updatedAt")
    suspend fun markAllNoticesRead(updatedAt: Long)

    @Query("SELECT * FROM correction_requests WHERE attendanceId = :attendanceId AND status IN ('PENDING', 'SYNC_FAILED') LIMIT 1")
    suspend fun findOpenCorrection(attendanceId: String): CorrectionRequestEntity?

    @Query("SELECT * FROM attendance_history WHERE id = :id LIMIT 1")
    suspend fun findHistory(id: String): AttendanceHistoryEntity?

    @Query("SELECT value FROM metadata WHERE `key` = :key LIMIT 1") suspend fun metadataValue(key: String): String?
    @Query("SELECT COUNT(*) FROM pending_operations") suspend fun pendingOperationCount(): Int

    @Query("DELETE FROM students") suspend fun deleteStudents()
    @Query("DELETE FROM subjects") suspend fun deleteSubjects()
    @Query("DELETE FROM attendance_history") suspend fun deleteHistory()
    @Query("DELETE FROM schedule") suspend fun deleteSchedule()
    @Query("DELETE FROM notices") suspend fun deleteNotices()
    @Query("DELETE FROM correction_requests") suspend fun deleteCorrections()
    @Query("DELETE FROM registered_devices") suspend fun deleteDevices()
    @Query("DELETE FROM pending_operations") suspend fun deletePendingOperations()
    @Query("DELETE FROM metadata") suspend fun deleteMetadata()

    @Transaction
    suspend fun clearAllStudentData() {
        deleteStudents()
        deleteSubjects()
        deleteHistory()
        deleteSchedule()
        deleteNotices()
        deleteCorrections()
        deleteDevices()
        deletePendingOperations()
        deleteMetadata()
    }
}
