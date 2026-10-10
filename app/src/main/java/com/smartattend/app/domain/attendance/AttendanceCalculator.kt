package com.smartattend.app.domain.attendance

import com.smartattend.app.core.model.AttendanceResult
import com.smartattend.app.core.model.AttendanceStatus
import java.time.Duration

class AttendanceCalculator {
    fun calculate(
        confirmed: Duration,
        scheduled: Duration,
        late: Boolean,
    ): AttendanceResult {
        require(!scheduled.isZero && !scheduled.isNegative) {
            "Scheduled duration must be greater than zero"
        }

        val clampedMillis = confirmed.toMillis().coerceIn(0L, scheduled.toMillis())
        val percentage = clampedMillis.toDouble() / scheduled.toMillis() * 100.0
        val status = when {
            percentage >= 75.0 -> AttendanceStatus.PRESENT
            percentage >= 50.0 -> AttendanceStatus.REVIEW
            else -> AttendanceStatus.ABSENT
        }

        return AttendanceResult(
            confirmed = Duration.ofMillis(clampedMillis),
            percentage = percentage,
            status = status,
            late = late,
        )
    }
}

