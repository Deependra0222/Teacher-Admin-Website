package com.smartattend.app.data.local

import android.content.Context
import androidx.core.content.edit
import com.smartattend.app.domain.attendance.PresenceSnapshot
import com.smartattend.app.core.model.AttendanceResult
import com.smartattend.app.core.model.AttendanceStatus
import java.time.Duration
import java.time.Instant

data class StoredDemoSession(
    val start: Instant,
    val end: Instant,
)

class DemoPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("smartattend_demo", Context.MODE_PRIVATE)

    val isLoggedIn: Boolean get() = preferences.getBoolean(KEY_LOGGED_IN, false)

    fun setLoggedIn(loggedIn: Boolean) {
        preferences.edit { putBoolean(KEY_LOGGED_IN, loggedIn) }
    }

    fun loadOrCreateSession(now: Instant): StoredDemoSession {
        val storedStart = preferences.getLong(KEY_SESSION_START, Long.MIN_VALUE)
        val storedEnd = preferences.getLong(KEY_SESSION_END, Long.MIN_VALUE)
        if (storedStart != Long.MIN_VALUE && storedEnd != Long.MIN_VALUE) {
            val storedSession = StoredDemoSession(Instant.ofEpochMilli(storedStart), Instant.ofEpochMilli(storedEnd))
            if (
                storedEnd > now.toEpochMilli() ||
                DemoSessionPolicy.retainExpiredSession(
                    snapshot = loadPresence(),
                    hasFinalResult = preferences.getBoolean(KEY_RESULT_PRESENT, false),
                )
            ) {
                return storedSession
            }
        }

        val session = StoredDemoSession(
            start = now.minus(Duration.ofMinutes(15)),
            end = now.plus(Duration.ofMinutes(45)),
        )
        preferences.edit {
            putLong(KEY_SESSION_START, session.start.toEpochMilli())
            putLong(KEY_SESSION_END, session.end.toEpochMilli())
            remove(KEY_INSIDE)
            remove(KEY_OPEN_SINCE)
            remove(KEY_CONFIRMED_MILLIS)
            remove(KEY_FIRST_ENTRY)
            remove(KEY_LAST_EVENT)
        }
        return session
    }

    fun loadPresence(): PresenceSnapshot {
        val openSince = preferences.getLong(KEY_OPEN_SINCE, Long.MIN_VALUE).asInstantOrNull()
        val firstEntry = preferences.getLong(KEY_FIRST_ENTRY, Long.MIN_VALUE).asInstantOrNull()
        val lastEventAt = preferences.getLong(KEY_LAST_EVENT, Long.MIN_VALUE).asInstantOrNull()
        return PresenceSnapshot(
            inside = preferences.getBoolean(KEY_INSIDE, false) && openSince != null,
            openSince = openSince,
            confirmed = Duration.ofMillis(preferences.getLong(KEY_CONFIRMED_MILLIS, 0L).coerceAtLeast(0L)),
            firstEntry = firstEntry,
            lastEventAt = lastEventAt,
        )
    }

    fun savePresence(snapshot: PresenceSnapshot) {
        preferences.edit {
            putBoolean(KEY_INSIDE, snapshot.inside)
            putLong(KEY_OPEN_SINCE, snapshot.openSince?.toEpochMilli() ?: Long.MIN_VALUE)
            putLong(KEY_CONFIRMED_MILLIS, snapshot.confirmed.toMillis())
            putLong(KEY_FIRST_ENTRY, snapshot.firstEntry?.toEpochMilli() ?: Long.MIN_VALUE)
            putLong(KEY_LAST_EVENT, snapshot.lastEventAt?.toEpochMilli() ?: Long.MIN_VALUE)
        }
    }

    fun loadResult(): AttendanceResult? {
        if (!preferences.getBoolean(KEY_RESULT_PRESENT, false)) return null
        val status = runCatching {
            AttendanceStatus.valueOf(preferences.getString(KEY_RESULT_STATUS, null).orEmpty())
        }.getOrNull() ?: return null
        return AttendanceResult(
            confirmed = Duration.ofMillis(preferences.getLong(KEY_RESULT_CONFIRMED_MILLIS, 0L)),
            percentage = Double.fromBits(preferences.getLong(KEY_RESULT_PERCENTAGE_BITS, 0L)),
            status = status,
            late = preferences.getBoolean(KEY_RESULT_LATE, false),
        )
    }

    fun saveResult(result: AttendanceResult) {
        preferences.edit {
            putBoolean(KEY_RESULT_PRESENT, true)
            putLong(KEY_RESULT_CONFIRMED_MILLIS, result.confirmed.toMillis())
            putLong(KEY_RESULT_PERCENTAGE_BITS, result.percentage.toBits())
            putString(KEY_RESULT_STATUS, result.status.name)
            putBoolean(KEY_RESULT_LATE, result.late)
        }
    }

    private fun Long.asInstantOrNull(): Instant? =
        takeUnless { it == Long.MIN_VALUE }?.let(Instant::ofEpochMilli)

    private companion object {
        const val KEY_LOGGED_IN = "logged_in"
        const val KEY_SESSION_START = "session_start"
        const val KEY_SESSION_END = "session_end"
        const val KEY_INSIDE = "inside"
        const val KEY_OPEN_SINCE = "open_since"
        const val KEY_CONFIRMED_MILLIS = "confirmed_millis"
        const val KEY_FIRST_ENTRY = "first_entry"
        const val KEY_LAST_EVENT = "last_event"
        const val KEY_RESULT_PRESENT = "result_present"
        const val KEY_RESULT_CONFIRMED_MILLIS = "result_confirmed_millis"
        const val KEY_RESULT_PERCENTAGE_BITS = "result_percentage_bits"
        const val KEY_RESULT_STATUS = "result_status"
        const val KEY_RESULT_LATE = "result_late"
    }
}

object DemoSessionPolicy {
    fun retainExpiredSession(snapshot: PresenceSnapshot, hasFinalResult: Boolean): Boolean =
        !hasFinalResult && (snapshot.inside || snapshot.confirmed > Duration.ZERO)
}

