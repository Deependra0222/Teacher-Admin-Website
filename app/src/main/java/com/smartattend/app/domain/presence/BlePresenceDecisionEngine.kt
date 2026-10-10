package com.smartattend.app.domain.presence

import java.time.Duration
import java.time.Instant
import java.util.ArrayDeque

data class BleSample(
    val sessionId: String,
    val beaconId: String,
    val rssi: Int,
    val occurredAt: Instant,
    val simulated: Boolean,
)

data class BleDecisionConfig(
    val sessionId: String,
    val allowedBeaconIds: Set<String>,
    val requiredBeaconCount: Int,
    val windowSize: Int,
    val emaAlpha: Double,
    val entryThresholdDbm: Double,
    val exitThresholdDbm: Double,
    val verificationDuration: Duration,
    val signalLossTimeout: Duration,
    val simulationOnly: Boolean = true,
) {
    init {
        require(allowedBeaconIds.isNotEmpty())
        require(requiredBeaconCount in 1..allowedBeaconIds.size)
        require(windowSize > 0)
        require(emaAlpha > 0.0 && emaAlpha <= 1.0)
        require(entryThresholdDbm > exitThresholdDbm)
        require(!verificationDuration.isNegative)
        require(!signalLossTimeout.isNegative && !signalLossTimeout.isZero)
    }
}

enum class BleDecisionKind { OUTSIDE, CANDIDATE, ENTERED, STAYING, EXITED, SIGNAL_LOST, REJECTED }

data class BleDecision(
    val kind: BleDecisionKind,
    val reason: String,
    val qualifyingBeaconCount: Int,
    val simulated: Boolean = true,
    val filteredRssi: Map<String, Double> = emptyMap(),
)

class BlePresenceDecisionEngine(private val config: BleDecisionConfig) {
    private data class BeaconTrack(
        val samples: ArrayDeque<Int> = ArrayDeque(),
        var filteredRssi: Double? = null,
        var lastSeenAt: Instant? = null,
    )

    private val tracks = config.allowedBeaconIds.associateWith { BeaconTrack() }
    private var candidateSince: Instant? = null
    private var weakSince: Instant? = null
    private var lastAcceptedAt: Instant? = null
    private var lastConfidentAt: Instant? = null

    var isInside: Boolean = false
        private set

    fun process(sample: BleSample): BleDecision {
        validationError(sample)?.let { return decision(BleDecisionKind.REJECTED, it, sample.occurredAt) }
        lastAcceptedAt = sample.occurredAt

        val track = tracks.getValue(sample.beaconId)
        track.samples.addLast(sample.rssi)
        while (track.samples.size > config.windowSize) track.samples.removeFirst()
        val median = track.samples.sorted().median()
        track.filteredRssi = track.filteredRssi?.let { previous ->
            config.emaAlpha * median + (1.0 - config.emaAlpha) * previous
        } ?: median
        track.lastSeenAt = sample.occurredAt

        return evaluate(
            at = sample.occurredAt,
            observedConfidentSignal = (track.filteredRssi ?: -127.0) > config.exitThresholdDbm,
        )
    }

    fun advanceTime(at: Instant): BleDecision {
        if (lastAcceptedAt?.let(at::isBefore) == true) {
            return decision(BleDecisionKind.REJECTED, "Time cannot move backwards", at)
        }
        lastAcceptedAt = at
        return evaluate(at = at, observedConfidentSignal = false)
    }

    private fun evaluate(at: Instant, observedConfidentSignal: Boolean): BleDecision {
        val freshTracks = tracks.values.filter { track -> track.isFresh(at) }
        val strongCount = freshTracks.count { (it.filteredRssi ?: -127.0) >= config.entryThresholdDbm }

        if (!isInside) {
            if (strongCount < config.requiredBeaconCount) {
                candidateSince = null
                return decision(BleDecisionKind.OUTSIDE, "Waiting for enough strong classroom signals", at)
            }

            val beganAt = candidateSince ?: at.also { candidateSince = it }
            if (Duration.between(beganAt, at) < config.verificationDuration) {
                return decision(BleDecisionKind.CANDIDATE, "Strong demo signals are within the dwell window", at)
            }

            isInside = true
            candidateSince = null
            weakSince = null
            lastConfidentAt = at
            return decision(BleDecisionKind.ENTERED, "Simulated classroom presence candidate entered", at)
        }

        val exitConfidenceCount = freshTracks.count { (it.filteredRssi ?: -127.0) > config.exitThresholdDbm }
        if (exitConfidenceCount > 0) {
            weakSince = null
            if (observedConfidentSignal) lastConfidentAt = at
            return decision(BleDecisionKind.STAYING, "Simulated classroom signal remains within hysteresis", at)
        }

        val confidentAt = lastConfidentAt ?: at
        if (freshTracks.isEmpty() && Duration.between(confidentAt, at) >= config.signalLossTimeout) {
            leave()
            return decision(BleDecisionKind.SIGNAL_LOST, "All simulated classroom signals timed out", at)
        }

        val weakAt = weakSince ?: at.also { weakSince = it }
        if (Duration.between(weakAt, at) >= config.signalLossTimeout) {
            leave()
            return decision(BleDecisionKind.EXITED, "Simulated signals stayed below the exit threshold", at)
        }

        return decision(BleDecisionKind.STAYING, "Weak simulated signal is inside the loss timeout", at)
    }

    private fun leave() {
        isInside = false
        candidateSince = null
        weakSince = null
        lastConfidentAt = null
    }

    private fun validationError(sample: BleSample): String? = when {
        sample.sessionId != config.sessionId -> "Sample belongs to a different session"
        sample.beaconId !in config.allowedBeaconIds -> "Beacon is not allowlisted for this session"
        sample.rssi !in -127..0 -> "RSSI must be between -127 and 0 dBm"
        config.simulationOnly && !sample.simulated -> "This build accepts simulated BLE samples only"
        lastAcceptedAt?.let(sample.occurredAt::isBefore) == true -> "Sample time is earlier than the last accepted sample"
        else -> null
    }

    private fun BeaconTrack.isFresh(at: Instant): Boolean {
        val seenAt = lastSeenAt ?: return false
        val age = Duration.between(seenAt, at)
        return !age.isNegative && age < config.signalLossTimeout
    }

    private fun decision(kind: BleDecisionKind, reason: String, at: Instant): BleDecision {
        val fresh = tracks.filterValues { it.isFresh(at) }
        return BleDecision(
            kind = kind,
            reason = reason,
            qualifyingBeaconCount = fresh.values.count {
                (it.filteredRssi ?: -127.0) >= config.entryThresholdDbm
            },
            simulated = true,
            filteredRssi = fresh.mapValues { it.value.filteredRssi ?: -127.0 },
        )
    }

    private fun List<Int>.median(): Double {
        val middle = size / 2
        return if (size % 2 == 1) {
            this[middle].toDouble()
        } else {
            (this[middle - 1] + this[middle]) / 2.0
        }
    }
}
