package com.smartattend.app.domain.presence

import java.time.Instant

fun interface AppClock {
    fun now(): Instant
}

object SystemAppClock : AppClock {
    override fun now(): Instant = Instant.now()
}

enum class PresenceSignalType { ENTER, EXIT }

data class PresenceSignal(
    val type: PresenceSignalType,
    val occurredAt: Instant,
    val simulated: Boolean = true,
)

interface PresenceSource {
    val label: String
    fun signal(type: PresenceSignalType): PresenceSignal
}

class SimulatedPresenceSource(
    private val clock: AppClock,
) : PresenceSource {
    override val label: String = "SIMULATION ONLY - NOT VERIFIED BY BLE"

    override fun signal(type: PresenceSignalType): PresenceSignal =
        PresenceSignal(type = type, occurredAt = clock.now(), simulated = true)
}

