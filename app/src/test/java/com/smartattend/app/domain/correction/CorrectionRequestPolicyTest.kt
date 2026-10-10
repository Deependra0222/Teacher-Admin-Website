package com.smartattend.app.domain.correction

import com.smartattend.app.core.model.AttendanceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CorrectionRequestPolicyTest {
    @Test
    fun reasonMustContainAtLeastTenNonWhitespaceCharacters() {
        val error = CorrectionRequestPolicy.validate(
            currentStatus = AttendanceStatus.ABSENT,
            requestedStatus = AttendanceStatus.PRESENT,
            reason = " too short ",
            hasOpenRequest = false,
        )

        assertEquals("Explain the correction in at least 10 characters", error)
    }

    @Test
    fun requestedStatusMustDifferFromRecordedStatus() {
        val error = CorrectionRequestPolicy.validate(
            currentStatus = AttendanceStatus.REVIEW,
            requestedStatus = AttendanceStatus.REVIEW,
            reason = "I attended the complete laboratory session",
            hasOpenRequest = false,
        )

        assertEquals("Choose a status different from the recorded status", error)
    }

    @Test
    fun duplicateOpenRequestIsRejected() {
        val error = CorrectionRequestPolicy.validate(
            currentStatus = AttendanceStatus.ABSENT,
            requestedStatus = AttendanceStatus.PRESENT,
            reason = "The teacher recorded my signed lab sheet",
            hasOpenRequest = true,
        )

        assertEquals("A correction request for this attendance record is already pending", error)
    }

    @Test
    fun validCorrectionRequestIsAccepted() {
        val error = CorrectionRequestPolicy.validate(
            currentStatus = AttendanceStatus.ABSENT,
            requestedStatus = AttendanceStatus.PRESENT,
            reason = "The teacher recorded my signed lab sheet",
            hasOpenRequest = false,
        )

        assertNull(error)
    }
}
