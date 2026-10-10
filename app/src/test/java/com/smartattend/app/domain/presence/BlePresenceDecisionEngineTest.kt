package com.smartattend.app.domain.presence

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlePresenceDecisionEngineTest {
    private val start = Instant.parse("2026-10-10T10:00:00Z")

    @Test
    fun twoAllowedBeaconsMustRemainStrongForFullDwellBeforeEntry() {
        val engine = engine(requiredBeacons = 2, dwellSeconds = 10)

        assertEquals(BleDecisionKind.OUTSIDE, engine.process(sample("a", -60, start)).kind)
        assertEquals(BleDecisionKind.CANDIDATE, engine.process(sample("b", -62, start)).kind)
        assertEquals(
            BleDecisionKind.CANDIDATE,
            engine.process(sample("a", -61, start.plusSeconds(9))).kind,
        )
        val entered = engine.process(sample("b", -63, start.plusSeconds(10)))

        assertEquals(BleDecisionKind.ENTERED, entered.kind)
        assertEquals(2, entered.qualifyingBeaconCount)
        assertTrue(entered.simulated)
    }

    @Test
    fun insufficientBeaconCountNeverStartsEntryCandidate() {
        val engine = engine(requiredBeacons = 2)

        val decision = engine.process(sample("a", -55, start))

        assertEquals(BleDecisionKind.OUTSIDE, decision.kind)
        assertEquals(1, decision.qualifyingBeaconCount)
    }

    @Test
    fun medianAndEmaPreventSingleStrongOutlierFromCreatingCandidate() {
        val engine = engine(requiredBeacons = 1, windowSize = 3, emaAlpha = 0.5)

        engine.process(sample("a", -90, start))
        engine.process(sample("a", -20, start.plusSeconds(1)))
        val decision = engine.process(sample("a", -90, start.plusSeconds(2)))

        assertEquals(BleDecisionKind.OUTSIDE, decision.kind)
    }

    @Test
    fun hysteresisKeepsStudentInsideWhenSignalFallsBelowEntryButAboveExit() {
        val engine = engine(requiredBeacons = 1, dwellSeconds = 0, entryThreshold = -70.0, exitThreshold = -85.0)
        engine.process(sample("a", -60, start))

        val decision = engine.process(sample("a", -78, start.plusSeconds(1)))

        assertEquals(BleDecisionKind.STAYING, decision.kind)
    }

    @Test
    fun signalLossTimeoutClosesPresence() {
        val engine = engine(requiredBeacons = 1, dwellSeconds = 0, lossSeconds = 5)
        assertEquals(BleDecisionKind.ENTERED, engine.process(sample("a", -60, start)).kind)

        val beforeTimeout = engine.advanceTime(start.plusSeconds(4))
        val atTimeout = engine.advanceTime(start.plusSeconds(5))

        assertEquals(BleDecisionKind.STAYING, beforeTimeout.kind)
        assertEquals(BleDecisionKind.SIGNAL_LOST, atTimeout.kind)
    }

    @Test
    fun repeatedStrongSamplesReportStayingAfterEntry() {
        val engine = engine(requiredBeacons = 1, dwellSeconds = 0)
        engine.process(sample("a", -60, start))

        assertEquals(
            BleDecisionKind.STAYING,
            engine.process(sample("a", -61, start.plusSeconds(1))).kind,
        )
    }

    @Test
    fun invalidSamplesAreRejectedWithoutChangingPresence() {
        val engine = engine(requiredBeacons = 1, dwellSeconds = 0)

        assertEquals(BleDecisionKind.REJECTED, engine.process(sample("unknown", -60, start)).kind)
        assertEquals(BleDecisionKind.REJECTED, engine.process(sample("a", 1, start)).kind)
        assertEquals(
            BleDecisionKind.REJECTED,
            engine.process(sample("a", -60, start, sessionId = "other-session")).kind,
        )
        assertEquals(BleDecisionKind.ENTERED, engine.process(sample("a", -60, start.plusSeconds(5))).kind)
        assertEquals(
            BleDecisionKind.REJECTED,
            engine.process(sample("a", -60, start.plusSeconds(4))).kind,
        )
        assertTrue(engine.isInside)
    }

    private fun engine(
        requiredBeacons: Int,
        dwellSeconds: Long = 10,
        lossSeconds: Long = 30,
        windowSize: Int = 1,
        emaAlpha: Double = 1.0,
        entryThreshold: Double = -70.0,
        exitThreshold: Double = -85.0,
    ) = BlePresenceDecisionEngine(
        BleDecisionConfig(
            sessionId = "session-1",
            allowedBeaconIds = setOf("a", "b", "c"),
            requiredBeaconCount = requiredBeacons,
            windowSize = windowSize,
            emaAlpha = emaAlpha,
            entryThresholdDbm = entryThreshold,
            exitThresholdDbm = exitThreshold,
            verificationDuration = Duration.ofSeconds(dwellSeconds),
            signalLossTimeout = Duration.ofSeconds(lossSeconds),
        ),
    )

    private fun sample(
        beaconId: String,
        rssi: Int,
        at: Instant,
        sessionId: String = "session-1",
    ) = BleSample(
        sessionId = sessionId,
        beaconId = beaconId,
        rssi = rssi,
        occurredAt = at,
        simulated = true,
    )
}
