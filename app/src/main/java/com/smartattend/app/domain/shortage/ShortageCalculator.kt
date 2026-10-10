package com.smartattend.app.domain.shortage

sealed interface ShortageProjection {
    data class OnTrack(
        val currentPercentage: Double,
        val targetPercentage: Int,
        val classesCanMiss: Int,
        val classesToAttend: Int = 0,
    ) : ShortageProjection

    data class NeedsClasses(
        val currentPercentage: Double,
        val targetPercentage: Int,
        val classesToAttend: Int,
        val classesCanMiss: Int = 0,
    ) : ShortageProjection

    data class Unattainable(
        val currentPercentage: Double,
        val targetPercentage: Int,
        val reason: String,
    ) : ShortageProjection

    data class Invalid(val reason: String) : ShortageProjection
}

class ShortageCalculator {
    fun calculate(attended: Int, total: Int, targetPercentage: Int): ShortageProjection {
        if (attended < 0 || total < 0) {
            return ShortageProjection.Invalid("Attendance counts cannot be negative")
        }
        if (attended > total) {
            return ShortageProjection.Invalid("Attended classes cannot exceed total classes")
        }
        if (targetPercentage !in 1..100) {
            return ShortageProjection.Invalid("Target percentage must be between 1 and 100")
        }

        val current = if (total == 0) 0.0 else attended.toDouble() / total * 100.0
        if (targetPercentage == 100) {
            return when {
                total == 0 -> ShortageProjection.NeedsClasses(current, targetPercentage, classesToAttend = 1)
                attended == total -> ShortageProjection.OnTrack(current, targetPercentage, classesCanMiss = 0)
                else -> ShortageProjection.Unattainable(
                    currentPercentage = current,
                    targetPercentage = targetPercentage,
                    reason = "A previous absence makes an exact 100% target unreachable",
                )
            }
        }

        return if (attended * 100L >= targetPercentage.toLong() * total) {
            val canMiss = (attended * 100L / targetPercentage - total).coerceAtLeast(0).toInt()
            ShortageProjection.OnTrack(current, targetPercentage, classesCanMiss = canMiss)
        } else {
            val gap = targetPercentage.toLong() * total - attended * 100L
            val denominator = 100 - targetPercentage
            val mustAttend = ((gap + denominator - 1) / denominator).toInt()
            ShortageProjection.NeedsClasses(current, targetPercentage, classesToAttend = mustAttend)
        }
    }
}
