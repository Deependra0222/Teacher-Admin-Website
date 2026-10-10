package com.smartattend.app.domain.sync

import com.smartattend.app.core.model.BackendMode
import com.smartattend.app.core.model.SyncDisplayState
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncStateResolverTest {
    @Test
    fun demoAndUnconfiguredBackendsNeverClaimSynchronization() {
        assertEquals(
            SyncDisplayState.DEMO_ONLY,
            SyncStateResolver.resolve(BackendMode.DEMO_ONLY, isConnected = true, pendingOperations = 0),
        )
        assertEquals(
            SyncDisplayState.BACKEND_UNCONFIGURED,
            SyncStateResolver.resolve(BackendMode.UNCONFIGURED, isConnected = true, pendingOperations = 0),
        )
    }

    @Test
    fun offlineStateDistinguishesPendingChanges() {
        assertEquals(
            SyncDisplayState.OFFLINE,
            SyncStateResolver.resolve(BackendMode.CONFIGURED, isConnected = false, pendingOperations = 0),
        )
        assertEquals(
            SyncDisplayState.OFFLINE_PENDING,
            SyncStateResolver.resolve(BackendMode.CONFIGURED, isConnected = false, pendingOperations = 2),
        )
    }

    @Test
    fun configuredBackendCanReportSyncingSynchronizedAndFailed() {
        assertEquals(
            SyncDisplayState.SYNCING,
            SyncStateResolver.resolve(
                BackendMode.CONFIGURED,
                isConnected = true,
                pendingOperations = 2,
                isSyncing = true,
            ),
        )
        assertEquals(
            SyncDisplayState.SYNCHRONIZED,
            SyncStateResolver.resolve(BackendMode.CONFIGURED, isConnected = true, pendingOperations = 0),
        )
        assertEquals(
            SyncDisplayState.FAILED,
            SyncStateResolver.resolve(
                BackendMode.CONFIGURED,
                isConnected = true,
                pendingOperations = 1,
                lastError = "Permission denied",
            ),
        )
    }
}
