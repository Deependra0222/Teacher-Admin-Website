package com.smartattend.app.ui.more

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.smartattend.app.ui.SmartAttendUiState
import com.smartattend.app.ui.components.BackHeader
import com.smartattend.app.ui.components.DemoDataPill
import com.smartattend.app.ui.components.StudentSectionCard
import com.smartattend.app.ui.components.SyncPill

@Composable
fun PrivacyPermissionsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { BackHeader("Privacy & permissions", "Current debug APK boundaries", onBack) }
        item { DemoDataPill("DEMO • LOCAL DATA ONLY") }
        item {
            StudentSectionCard("Permissions in this milestone") {
                PermissionLine("Bluetooth / nearby devices", "Not declared or requested")
                PermissionLine("Location", "Not declared or requested")
                PermissionLine("Notifications", "Not declared or requested")
                PermissionLine("Internet", "Declared by Firebase SDK; no app sign-in or Firestore operation is connected")
                Text(
                    "Changing the notification preference stores a local setting only; it does not grant Android permission or schedule a notification.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item {
            StudentSectionCard("Data stored on this device") {
                Text("Room stores demo profile, attendance, timetable, notices, correction requests, device status and pending operations inside the app sandbox.")
                Text("DataStore stores the installation ID and app preferences. Demo reset is available in Settings.")
            }
        }
        item {
            StudentSectionCard("Future production permission flow") {
                Text("Bluetooth permissions will be requested only when real ESP32 scanning is implemented and ready for physical-device testing. Firebase client configuration is packaged, but account access and data synchronization will be added only with verified authorization and security rules.")
            }
        }
        item {
            Button(
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            "package:${context.packageName}".toUri(),
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Open Android app settings")
            }
        }
    }
}

@Composable
fun OfflineDiagnosticsScreen(
    state: SmartAttendUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { BackHeader("Offline & BLE status", "Local storage and simulator diagnostics", onBack) }
        item { DemoDataPill("SIMULATED • NOT VERIFIED BY BLE") }
        item { SyncPill(state.syncState, state.pendingOperations) }
        item {
            StudentSectionCard("Offline storage") {
                Text("Subjects cached: ${state.subjects.size}")
                Text("Attendance records cached: ${state.history.size}")
                Text("Notices cached: ${state.notices.size}")
                Text("Pending local operations: ${state.pendingOperations}")
                Text("Firebase SDK is configured, but no repository or authentication sync is connected, so pending operations are not uploaded.", style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            StudentSectionCard("Simulated BLE engine") {
                Text(state.bleDecision?.let { "${it.kind}: ${it.reason}" } ?: "No simulated signal sequence has been run in this app session.")
                Text("No Android Bluetooth scan or ESP32 signal is active. RSSI filtering here uses generated samples only.", style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            StudentSectionCard("Error recovery") {
                Text(state.fatalError ?: "No active local database error.")
                Text("If Room or DataStore reports an error, the app replaces normal content with a retryable error screen.", style = MaterialTheme.typography.bodySmall)
                Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Reload offline data") }
            }
        }
    }
}

@Composable
private fun PermissionLine(label: String, value: String) {
    Text(label, style = MaterialTheme.typography.titleSmall)
    Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
