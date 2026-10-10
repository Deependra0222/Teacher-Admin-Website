package com.smartattend.app.domain.sync

import com.smartattend.app.core.model.BackendMode
import com.smartattend.app.core.model.SyncDisplayState

object SyncStateResolver {
    fun resolve(
        backendMode: BackendMode,
        isConnected: Boolean,
        pendingOperations: Int,
        isSyncing: Boolean = false,
        lastError: String? = null,
    ): SyncDisplayState {
        if (backendMode == BackendMode.DEMO_ONLY) return SyncDisplayState.DEMO_ONLY
        if (backendMode == BackendMode.UNCONFIGURED) return SyncDisplayState.BACKEND_UNCONFIGURED
        if (lastError != null) return SyncDisplayState.FAILED
        if (!isConnected) {
            return if (pendingOperations > 0) {
                SyncDisplayState.OFFLINE_PENDING
            } else {
                SyncDisplayState.OFFLINE
            }
        }
        if (isSyncing || pendingOperations > 0) return SyncDisplayState.SYNCING
        return SyncDisplayState.SYNCHRONIZED
    }
}
