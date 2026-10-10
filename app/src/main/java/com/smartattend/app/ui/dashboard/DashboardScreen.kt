package com.smartattend.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartattend.app.ui.SmartAttendUiState
import com.smartattend.app.ui.components.DemoDataPill
import com.smartattend.app.ui.components.StudentSectionCard
import com.smartattend.app.ui.components.SyncPill
import com.smartattend.app.ui.components.toCampusTime

@Composable
fun DashboardScreen(
    state: SmartAttendUiState,
    onAttendance: () -> Unit,
    onSchedule: () -> Unit,
    onInbox: () -> Unit,
    onCalculator: () -> Unit,
    onSimulatePresence: () -> Unit,
    onSimulateLoss: () -> Unit,
    onCalculateSession: () -> Unit,
) {
    val average = state.subjects.takeIf { it.isNotEmpty() }?.map { it.percentage }?.average() ?: 0.0
    val shortageCount = state.subjects.count { it.percentage < it.requiredPercentage }
    val current = state.timetable.firstOrNull { it.active } ?: state.timetable.firstOrNull()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Good day, ${state.student.name.substringBefore(' ')}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Your attendance workspace", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { DemoDataPill(); SyncPill(state.syncState, state.pendingOperations) } }
        item {
            StudentSectionCard("Overall attendance", icon = Icons.Rounded.School) {
                Text("%.1f%%".format(average), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                LinearProgressIndicator({ (average / 100).toFloat() }, Modifier.fillMaxWidth())
                Text(if (shortageCount == 0) "All subjects are at or above their demo target" else "$shortageCount subject(s) below their demo target")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onAttendance) { Text("View subjects") }
                    OutlinedButton(onClick = onCalculator) { Text("Calculator") }
                }
            }
        }
        item {
            StudentSectionCard("Current / next class", icon = Icons.Rounded.CalendarMonth) {
                if (current == null) Text("No demo classes scheduled") else {
                    Text("${current.subjectCode} • ${current.subjectName}", fontWeight = FontWeight.Bold)
                    Text("${current.start.toCampusTime()}–${current.end.toCampusTime()} • ${current.room}")
                    Text("${current.kind.name.lowercase().replaceFirstChar { it.uppercase() }} • ${current.instructor}")
                    OutlinedButton(onClick = onSchedule) { Text("Open schedule") }
                }
            }
        }
        item {
            StudentSectionCard("Simulated classroom presence", icon = Icons.Rounded.BluetoothSearching) {
                Text("SIMULATED — NOT VERIFIED BY BLE", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                Text(if (state.insideClassroom) "Demo state: inside classroom" else "Demo state: outside classroom")
                Text("Confirmed demo time: ${state.confirmedMinutes} min")
                state.bleDecision?.let { Text("Engine: ${it.kind} • ${it.reason}", style = MaterialTheme.typography.bodySmall) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSimulatePresence) { Text("Simulate signals") }
                    OutlinedButton(onClick = onSimulateLoss) { Text("Simulate loss") }
                }
                OutlinedButton(onClick = onCalculateSession, modifier = Modifier.fillMaxWidth()) { Text("Calculate local demo result") }
            }
        }
        item {
            StudentSectionCard("Inbox", icon = Icons.Rounded.Notifications) {
                Text("${state.notices.count { !it.isRead }} unread demo notice(s)")
                Button(onClick = onInbox) { Text("Open inbox") }
            }
        }
    }
}
