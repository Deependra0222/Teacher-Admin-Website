package com.smartattend.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StudentRoutesTest {
    @Test
    fun nestedRouteIdsRoundTripWithoutLosingReservedCharacters() {
        val id = "demo id/42"

        assertEquals(id, StudentRoutes.subjectId(StudentRoutes.subject(id)))
        assertEquals(id, StudentRoutes.noticeId(StudentRoutes.notice(id)))
        assertEquals(id, StudentRoutes.scheduleId(StudentRoutes.scheduleDetail(id)))
        assertEquals(id, StudentRoutes.correctionId(StudentRoutes.correctionDetail(id)))
        assertEquals(id, StudentRoutes.attendanceId(StudentRoutes.correctionForm(id)))
    }

    @Test
    fun malformedOrWrongRoutesReturnNull() {
        assertNull(StudentRoutes.subjectId("subject"))
        assertNull(StudentRoutes.noticeId("schedule/demo"))
        assertNull(StudentRoutes.attendanceId("correction/new/"))
        assertNull(StudentRoutes.scheduleId("schedule-detail/%"))
    }
}
