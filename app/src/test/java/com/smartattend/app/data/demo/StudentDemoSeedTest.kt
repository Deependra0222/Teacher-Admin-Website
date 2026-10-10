package com.smartattend.app.data.demo

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudentDemoSeedTest {
    private val date = LocalDate.of(2026, 10, 10)
    private val now = Instant.parse("2026-10-10T04:30:00Z")

    @Test
    fun seedIsDeterministicAndIdsAreUniqueWithinCollections() {
        val first = StudentDemoSeed.create(date, now)
        val second = StudentDemoSeed.create(date, now.plusSeconds(60))

        assertEquals(first.student.id, second.student.id)
        assertEquals(first.subjects.map { it.code }, second.subjects.map { it.code })
        assertEquals(first.history.map { it.id }, second.history.map { it.id })
        assertEquals(first.notices.map { it.id }, second.notices.map { it.id })
        assertEquals(first.schedule.map { it.id }, second.schedule.map { it.id })
        assertEquals(first.history.size, first.history.map { it.id }.toSet().size)
        assertEquals(first.notices.size, first.notices.map { it.id }.toSet().size)
    }

    @Test
    fun everySeededRecordIsClearlyDemoAndCoversUsefulScenarios() {
        val seed = StudentDemoSeed.create(date, now)

        assertEquals("DEMO", seed.student.sourceMode)
        assertTrue(seed.subjects.all { it.simulated && it.sourceMode == "DEMO" })
        assertTrue(seed.history.all { it.simulated && !it.teacherFinalized && it.sourceMode == "DEMO" })
        assertTrue(seed.schedule.all { it.simulated && it.sourceMode == "DEMO" })
        assertTrue(seed.notices.all { it.simulated && it.sourceMode == "DEMO" })
        assertTrue(seed.history.map { it.dateEpochDay }.distinct().size >= 3)
        assertTrue(seed.notices.map { it.type }.distinct().size >= 3)
        assertTrue(seed.corrections.map { it.status }.containsAll(listOf("APPROVED", "REJECTED")))
    }
}
