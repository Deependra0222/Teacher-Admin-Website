package com.smartattend.app.domain.attendance

import com.smartattend.app.core.model.AttendanceStatus
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Test

class AttendanceCalculatorTest {
    private val calculator = AttendanceCalculator()

    @Test
    fun classifiesRequiredPercentageBoundaries() {
        val scheduled = Duration.ofSeconds(1_000)
        val cases = listOf(
            1_000L to AttendanceStatus.PRESENT,
            750L to AttendanceStatus.PRESENT,
            749L to AttendanceStatus.REVIEW,
            500L to AttendanceStatus.REVIEW,
            499L to AttendanceStatus.ABSENT,
        )

        cases.forEach { (confirmedSeconds, expectedStatus) ->
            val result = calculator.calculate(
                confirmed = Duration.ofSeconds(confirmedSeconds),
                scheduled = scheduled,
                late = false,
            )

            assertEquals(expectedStatus, result.status)
        }
    }

    @Test
    fun clampsConfirmedTimeToScheduledDuration() {
        val result = calculator.calculate(
            confirmed = Duration.ofMinutes(75),
            scheduled = Duration.ofMinutes(60),
            late = false,
        )

        assertEquals(100.0, result.percentage, 0.001)
        assertEquals(Duration.ofMinutes(60), result.confirmed)
    }
}

