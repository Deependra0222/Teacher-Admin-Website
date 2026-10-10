package com.smartattend.app.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartattend.app.ui.SmartAttendUiState
import com.smartattend.app.ui.components.DemoDataPill
import com.smartattend.app.ui.components.ScreenHeader
import com.smartattend.app.ui.components.StudentSectionCard
import com.smartattend.app.ui.components.SyncPill

@Composable
fun MoreScreen(state: SmartAttendUiState, onProfile: () -> Unit, onCorrections: () -> Unit, onDevice: () -> Unit, onSettings: () -> Unit, onAbout: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader("More", "Profile, requests, device and offline settings") }
        item { DemoDataPill() }
        item { SyncPill(state.syncState, state.pendingOperations) }
        item { MoreRow("Profile", "${state.student.name} • ${state.student.rollNumber}", Icons.Rounded.Person, onProfile) }
        item { MoreRow("Correction requests", "${state.corrections.size} demo request(s)", Icons.Rounded.EditNote, onCorrections) }
        item { MoreRow("Registered device", state.device?.state?.name?.replace('_', ' ') ?: "Not registered", Icons.Rounded.Devices, onDevice) }
        item { MoreRow("Settings", "Notifications, reminders, theme and reset", Icons.Rounded.Settings, onSettings) }
        item { MoreRow("About demo mode", "What is simulated and what remains pending", Icons.Rounded.Info, onAbout) }
    }
}

@Composable
private fun MoreRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    StudentSectionCard(title, Modifier.clickable(onClick = onClick), icon) { Text(subtitle) }
}
