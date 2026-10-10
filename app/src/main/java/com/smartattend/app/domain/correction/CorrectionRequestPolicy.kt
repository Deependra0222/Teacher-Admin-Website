package com.smartattend.app.domain.correction

import com.smartattend.app.core.model.AttendanceStatus

object CorrectionRequestPolicy {
    fun validate(
        currentStatus: AttendanceStatus,
        requestedStatus: AttendanceStatus,
        reason: String,
        hasOpenRequest: Boolean,
    ): String? = when {
        reason.trim().length < 10 -> "Explain the correction in at least 10 characters"
        currentStatus == requestedStatus -> "Choose a status different from the recorded status"
        hasOpenRequest -> "A correction request for this attendance record is already pending"
        else -> null
    }
}
