package com.smartattend.app.data.demo

import com.smartattend.app.data.local.entity.AttendanceHistoryEntity
import com.smartattend.app.data.local.entity.CorrectionRequestEntity
import com.smartattend.app.data.local.entity.NoticeEntity
import com.smartattend.app.data.local.entity.ScheduleEntity
import com.smartattend.app.data.local.entity.StudentEntity
import com.smartattend.app.data.local.entity.SubjectEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class StudentDemoSeedBundle(
    val student: StudentEntity,
    val subjects: List<SubjectEntity>,
    val history: List<AttendanceHistoryEntity>,
    val schedule: List<ScheduleEntity>,
    val notices: List<NoticeEntity>,
    val corrections: List<CorrectionRequestEntity>,
)

object StudentDemoSeed {
    const val VERSION = 1
    private const val SOURCE = "DEMO"

    fun create(anchorDate: LocalDate, now: Instant): StudentDemoSeedBundle {
        val zone = ZoneId.of("Asia/Kolkata")
        fun at(hour: Int, minute: Int = 0): Long = anchorDate.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
        val updated = now.toEpochMilli()
        return StudentDemoSeedBundle(
            student = StudentEntity(
                id = "student-demo-042",
                name = "Aarav Sharma",
                rollNumber = "CS-042",
                email = "aarav.sharma@demo.smartattend",
                department = "Computer Science",
                batch = "CSE-2026-A",
                updatedAt = updated,
                sourceMode = SOURCE,
            ),
            subjects = listOf(
                SubjectEntity("CS301", "Operating Systems", 22, 26, 75, true, updated, SOURCE),
                SubjectEntity("CS305", "Database Systems", 18, 25, 75, true, updated, SOURCE),
                SubjectEntity("CS307", "Computer Networks", 24, 27, 75, true, updated, SOURCE),
                SubjectEntity("CS309", "Software Engineering", 16, 23, 75, true, updated, SOURCE),
            ),
            history = listOf(
                history("history-os-1", anchorDate.minusDays(1), "CS301", "Operating Systems", "PRESENT", 91.0, "Lab 204", updated),
                history("history-db-1", anchorDate.minusDays(2), "CS305", "Database Systems", "REVIEW", 68.0, "Room 118", updated, late = true),
                history("history-net-1", anchorDate.minusDays(3), "CS307", "Computer Networks", "PRESENT", 87.0, "Room 312", updated),
                history("history-se-1", anchorDate.minusDays(5), "CS309", "Software Engineering", "ABSENT", 42.0, "Room 210", updated),
                history("history-os-2", anchorDate.minusDays(8), "CS301", "Operating Systems", "ABSENT", 0.0, "Lab 204", updated),
            ),
            schedule = listOf(
                ScheduleEntity("schedule-os", "CS301", "Operating Systems", "Lab 204", at(10), at(11), true, "Dr. Meera Rao", "LAB", true, updated, SOURCE),
                ScheduleEntity("schedule-db", "CS305", "Database Systems", "Room 118", at(12), at(13), false, "Prof. Arjun Iyer", "LECTURE", true, updated, SOURCE),
                ScheduleEntity("schedule-net-next", "CS307", "Computer Networks", "Room 312", anchorDate.plusDays(1).atTime(9, 30).atZone(zone).toInstant().toEpochMilli(), anchorDate.plusDays(1).atTime(10, 30).atZone(zone).toInstant().toEpochMilli(), false, "Dr. Kavya Shah", "LECTURE", true, updated, SOURCE),
                ScheduleEntity("schedule-se-next", "CS309", "Software Engineering", "Room 210", anchorDate.plusDays(2).atTime(14, 0).atZone(zone).toInstant().toEpochMilli(), anchorDate.plusDays(2).atTime(15, 0).atZone(zone).toInstant().toEpochMilli(), false, "Prof. Rohan Sen", "LECTURE", true, updated, SOURCE),
            ),
            notices = listOf(
                NoticeEntity("notice-welcome", "ANNOUNCEMENT", "Welcome to SmartAttend", "This offline milestone uses realistic demo records. No attendance is verified by BLE or a teacher.", now.minusSeconds(3_600).toEpochMilli(), null, false, true, updated, SOURCE),
                NoticeEntity("notice-db-assignment", "ASSIGNMENT", "Database normalization worksheet", "Submit the worksheet through your institution's approved channel.", now.minusSeconds(7_200).toEpochMilli(), now.plusSeconds(172_800).toEpochMilli(), false, true, updated, SOURCE),
                NoticeEntity("notice-shortage", "ATTENDANCE_ALERT", "Software Engineering attendance below target", "Your demo projection is below 75%. Open the shortage calculator to explore recovery scenarios.", now.minusSeconds(10_800).toEpochMilli(), null, false, true, updated, SOURCE),
                NoticeEntity("notice-correction", "CORRECTION_UPDATE", "Previous correction reviewed", "A seeded example request was approved. This is demo data, not a real teacher action.", now.minusSeconds(86_400).toEpochMilli(), null, true, true, updated, SOURCE),
            ),
            corrections = listOf(
                CorrectionRequestEntity("correction-seeded-approved", "history-db-older", "CS305", "ABSENT", "PRESENT", "Medical leave record was entered incorrectly", "APPROVED", now.minusSeconds(259_200).toEpochMilli(), "Seeded demo response only", true, updated, SOURCE),
                CorrectionRequestEntity("correction-seeded-rejected", "history-net-older", "CS307", "ABSENT", "PRESENT", "I attended but the demo record did not update", "REJECTED", now.minusSeconds(518_400).toEpochMilli(), "Seeded demo response only", true, updated, SOURCE),
            ),
        )
    }

    private fun history(
        id: String,
        date: LocalDate,
        code: String,
        name: String,
        status: String,
        percentage: Double,
        room: String,
        updated: Long,
        late: Boolean = false,
    ) = AttendanceHistoryEntity(
        id = id,
        dateEpochDay = date.toEpochDay(),
        subjectCode = code,
        subjectName = name,
        status = status,
        late = late,
        percentage = percentage,
        simulated = true,
        sessionId = "session-$id",
        room = room,
        teacherFinalized = false,
        updatedAt = updated,
        sourceMode = SOURCE,
    )
}
