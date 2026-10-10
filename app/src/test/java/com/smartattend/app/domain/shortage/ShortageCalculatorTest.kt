package com.smartattend.app.domain.shortage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortageCalculatorTest {
    private val calculator = ShortageCalculator()

    @Test
    fun studentAtNinetyPercentCanMissFourClassesAndRemainAtSeventyFivePercent() {
        val result = calculator.calculate(attended = 18, total = 20, targetPercentage = 75)

        assertTrue(result is ShortageProjection.OnTrack)
        result as ShortageProjection.OnTrack
        assertEquals(4, result.classesCanMiss)
        assertEquals(0, result.classesToAttend)
        assertEquals(90.0, result.currentPercentage, 0.001)
    }

    @Test
    fun studentAtFiftyPercentMustAttendTwentyClassesToReachSeventyFivePercent() {
        val result = calculator.calculate(attended = 10, total = 20, targetPercentage = 75)

        assertTrue(result is ShortageProjection.NeedsClasses)
        result as ShortageProjection.NeedsClasses
        assertEquals(20, result.classesToAttend)
        assertEquals(0, result.classesCanMiss)
    }

    @Test
    fun studentExactlyAtTargetHasNoRequiredOrMissableClasses() {
        val result = calculator.calculate(attended = 15, total = 20, targetPercentage = 75)

        assertEquals(
            ShortageProjection.OnTrack(
                currentPercentage = 75.0,
                targetPercentage = 75,
                classesCanMiss = 0,
                classesToAttend = 0,
            ),
            result,
        )
    }

    @Test
    fun invalidCountsAndTargetsAreRejected() {
        val cases = listOf(
            calculator.calculate(attended = -1, total = 20, targetPercentage = 75),
            calculator.calculate(attended = 21, total = 20, targetPercentage = 75),
            calculator.calculate(attended = 1, total = -1, targetPercentage = 75),
            calculator.calculate(attended = 1, total = 2, targetPercentage = 0),
            calculator.calculate(attended = 1, total = 2, targetPercentage = 101),
        )

        assertTrue(cases.all { it is ShortageProjection.Invalid })
    }

    @Test
    fun imperfectRecordCannotMathematicallyRecoverToOneHundredPercent() {
        val result = calculator.calculate(attended = 9, total = 10, targetPercentage = 100)

        assertTrue(result is ShortageProjection.Unattainable)
    }
}
