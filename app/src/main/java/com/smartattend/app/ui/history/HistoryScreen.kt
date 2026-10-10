package com.smartattend.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartattend.app.core.model.AttendanceHistoryItem
import com.smartattend.app.ui.components.BackHeader
import com.smartattend.app.ui.components.DemoDataPill
import com.smartattend.app.ui.components.StudentSectionCard

@Composable
fun HistoryScreen(
    history: List<AttendanceHistoryItem>,
    subjectFilter: String = "ALL",
    statusFilter: String = "ALL",
    onSubjectFilter: (String) -> Unit = {},
    onStatusFilter: (String) -> Unit = {},
    onCorrection: (String) -> Unit = {},
    onBack: (() -> Unit)? = null,
) {
    val shown = history.filter { (subjectFilter == "ALL" || it.subjectCode == subjectFilter) && (statusFilter == "ALL" || it.status.name == statusFilter) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            if (onBack == null) com.smartattend.app.ui.components.ScreenHeader("History", "Filterable attendance evidence")
            else BackHeader("Attendance history", "DEMO records are not teacher-finalized", onBack)
        }
        item { DemoDataPill("DEMO EVIDENCE") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AssistChip(onClick = { onSubjectFilter("ALL") }, label = { Text("All subjects") })
                history.map { it.subjectCode }.distinct().take(2).forEach { code -> AssistChip(onClick = { onSubjectFilter(code) }, label = { Text(code) }) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("ALL", "PRESENT", "ABSENT").forEach { status -> AssistChip(onClick = { onStatusFilter(status) }, label = { Text(status.lowercase().replaceFirstChar(Char::uppercase)) }) }
            }
        }
        if (shown.isEmpty()) item { StudentSectionCard("No matching records") { Text("Change the filters to see other demo records.") } }
        items(shown, key = { it.id }) { record ->
            StudentSectionCard("${record.subjectCode} • ${record.subjectName}") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(record.date.toString())
                    Text(record.status.name, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Text(formatHistorySummary(record.percentage, record.late))
                Text("${record.room} • NOT VERIFIED BY BLE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                Button(onClick = { onCorrection(record.id) }) { Text("Request correction") }
            }
        }
    }
}

fun formatHistorySummary(percentage: Double, late: Boolean): String =
    "%.1f%% confirmed%s".format(percentage, if (late) " • Late" else "")
