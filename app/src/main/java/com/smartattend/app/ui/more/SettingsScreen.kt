package com.smartattend.app.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartattend.app.core.model.StudentSettings
import com.smartattend.app.core.model.ThemePreference
import com.smartattend.app.ui.components.BackHeader
import com.smartattend.app.ui.components.DemoDataPill
import com.smartattend.app.ui.components.StudentSectionCard
import com.smartattend.app.ui.components.SyncPill
import com.smartattend.app.core.model.SyncDisplayState

@Composable
fun SettingsScreen(settings: StudentSettings, syncState: SyncDisplayState, pending: Int, onBack: () -> Unit, onNotifications: (Boolean) -> Unit, onReminder: (Int) -> Unit, onTheme: (ThemePreference) -> Unit, onReset: () -> Unit) {
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { BackHeader("Settings", "Preferences persist on this device", onBack) }
        item { DemoDataPill() }
        item { SyncPill(syncState, pending) }
        item {
            StudentSectionCard("Notifications") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Enable local notifications")
                    Switch(settings.notificationsEnabled, onNotifications)
                }
                Text("No Android notification permission or delivery service is enabled in this milestone.", style = MaterialTheme.typography.bodySmall)
                Text("Reminder lead time: ${settings.reminderLeadMinutes} min")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(10, 30, 60).forEach { minutes -> AssistChip(onClick = { onReminder(minutes) }, label = { Text("$minutes min") }) }
                }
            }
        }
        item {
            StudentSectionCard("Appearance") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ThemePreference.entries.forEach { theme -> AssistChip(onClick = { onTheme(theme) }, label = { Text(theme.name.lowercase().replaceFirstChar { it.uppercase() }) }) }
                }
                Text("Selected: ${settings.theme.name}")
            }
        }
        item {
            StudentSectionCard("Demo data") {
                Text("Reset restores seeded records and removes local correction/device/pending-operation changes.")
                OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) { Text("Reset demo data") }
            }
        }
    }
    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text("Reset demo data?") },
        text = { Text("Your local demo notice, correction, device and pending-operation changes will be replaced by the original seed.") },
        confirmButton = { Button(onClick = { confirmReset = false; onReset() }) { Text("Reset") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
    )
}

@Composable
fun AboutDemoScreen(onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { BackHeader("About demo mode", "Honest boundaries for this milestone", onBack) }
        item { DemoDataPill("OFFLINE STUDENT MILESTONE") }
        item { StudentSectionCard("Working now") { Text("Room offline records, DataStore settings, all student navigation, local correction requests, projections, Firebase client SDK initialization, and a platform-independent simulated BLE decision engine.") } }
        item { StudentSectionCard("Not implemented or verified") { Text("Real Firebase accounts, Firestore reads/writes or synchronization, Security Rules emulator tests, Cloud Functions, Android BLE scanning, ESP32 calibration, teacher finalization, real notifications, and physical-device attendance verification.") } }
        item { StudentSectionCard("Safety boundary") { Text("The student app can prepare local evidence and requests. It never finalizes or directly edits official attendance.") } }
    }
}
