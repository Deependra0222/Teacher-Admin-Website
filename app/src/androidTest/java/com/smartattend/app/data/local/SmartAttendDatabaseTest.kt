package com.smartattend.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartattend.app.data.local.entity.AttendanceHistoryEntity
import com.smartattend.app.data.local.entity.CorrectionRequestEntity
import com.smartattend.app.data.local.entity.NoticeEntity
import com.smartattend.app.data.local.entity.StudentEntity
import com.smartattend.app.core.model.AttendanceStatus
import com.smartattend.app.data.RepositoryResult
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmartAttendDatabaseTest {
    private lateinit var database: SmartAttendDatabase
    private lateinit var dao: StudentDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SmartAttendDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.studentDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun recordsFlowAndMutableOfflineStateSurviveRepositoryReads() = runTest {
        dao.upsertStudent(
            StudentEntity("student", "Demo Student", "CSE-01", "demo@example.test", "CSE", "2026-A", 1L, "DEMO"),
        )
        dao.upsertHistory(
            listOf(AttendanceHistoryEntity("attendance", 1L, "CS301", "Operating Systems", "ABSENT", false, 0.0, true, "session", "Lab", false, 1L, "DEMO")),
        )
        dao.upsertNotices(
            listOf(NoticeEntity("notice", "ANNOUNCEMENT", "Hello", "Body", 1L, null, false, true, 1L, "DEMO")),
        )
        dao.setNoticeRead("notice", true, 2L)
        dao.insertCorrection(
            CorrectionRequestEntity("request", "attendance", "CS301", "ABSENT", "PRESENT", "Medical documentation attached", "PENDING", 3L, null, true, 3L, "DEMO"),
        )

        assertEquals("Demo Student", dao.observeStudent().first()?.name)
        assertEquals(true, dao.observeNotices().first().single().isRead)
        assertEquals("request", dao.findOpenCorrection("attendance")?.id)

        dao.clearAllStudentData()

        assertNull(dao.observeStudent().first())
        assertEquals(emptyList<NoticeEntity>(), dao.observeNotices().first())
        assertNull(dao.findOpenCorrection("attendance"))
    }

    @Test
    fun repositorySeedIsIdempotentAndMutationsRemainPendingUntilBackendExists() = runTest {
        val repository = OfflineStudentRepository(
            database = database,
            preferences = AppPreferencesRepository(ApplicationProvider.getApplicationContext()),
            clock = Clock.fixed(Instant.parse("2026-10-10T04:30:00Z"), ZoneOffset.UTC),
        )

        repository.initialize()
        repository.setNoticeRead("notice-welcome", true)
        repository.initialize()
        val first = repository.submitCorrection(
            attendanceId = "history-se-1",
            requestedStatus = AttendanceStatus.PRESENT,
            reason = "I attended and can provide supporting records",
        )
        val duplicate = repository.submitCorrection(
            attendanceId = "history-se-1",
            requestedStatus = AttendanceStatus.PRESENT,
            reason = "A second request should never be inserted",
        )

        assertEquals(4, repository.subjects.first().size)
        assertEquals(true, repository.notices.first().first { it.id == "notice-welcome" }.isRead)
        assertEquals(RepositoryResult.Success, first)
        assertEquals(true, duplicate is RepositoryResult.Error)
        assertEquals(1, repository.corrections.first().count { it.attendanceId == "history-se-1" })
        assertEquals(2, repository.pendingOperationCount.first())

        repository.resetDemoData()
        assertEquals(false, repository.notices.first().first { it.id == "notice-welcome" }.isRead)
        assertEquals(0, repository.pendingOperationCount.first())
    }
}
