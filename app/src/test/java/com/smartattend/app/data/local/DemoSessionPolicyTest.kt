package com.smartattend.app.data.local

import com.smartattend.app.domain.attendance.PresenceSnapshot
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoSessionPolicyTest {
    private val now = Instant.parse("2026-10-10T05:30:00Z")

    @Test
    fun expiredSessionIsRetainedWhenConfirmedEvidenceExists() {
        val snapshot = PresenceSnapshot(confirmed = Duration.ofMinutes(20))

        assertTrue(DemoSessionPolicy.retainExpiredSession(snapshot, hasFinalResult = false))
    }

    @Test
    fun expiredSessionIsRetainedWhenIntervalIsStillOpen() {
        val snapshot = PresenceSnapshot(inside = true, openSince = now.minusSeconds(60))

        assertTrue(DemoSessionPolicy.retainExpiredSession(snapshot, hasFinalResult = false))
    }

    @Test
    fun expiredSessionWithoutEvidenceCanRotate() {
        assertFalse(DemoSessionPolicy.retainExpiredSession(PresenceSnapshot(), hasFinalResult = false))
    }

    @Test
    fun finalizedExpiredSessionCanRotateToANewDemo() {
        val snapshot = PresenceSnapshot(confirmed = Duration.ofMinutes(45))

        assertFalse(DemoSessionPolicy.retainExpiredSession(snapshot, hasFinalResult = true))
    }
}

