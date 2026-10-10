package com.smartattend.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartattend.app.core.model.AttendanceHistoryItem
import com.smartattend.app.ui.components.ScreenHeader

@Composable
fun HistoryScreen(history: List<AttendanceHistoryItem>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader("History", "Recent demo evidence and finalized examples") }
        if (history.isEmpty()) item { Text("History will appear after a session is completed.") }
        items(history, key = { it.id }) { record ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${record.subjectCode} • ${record.subjectName}", fontWeight = FontWeight.Bold)
                        Text(record.status.name, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Text(record.date.toString(), style = MaterialTheme.typography.bodySmall)
                    Text(formatHistorySummary(record.percentage, record.late), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (record.simulated) Text("DEMO EVIDENCE", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

fun formatHistorySummary(percentage: Double, late: Boolean): String =
    "%.1f%% confirmed%s".format(percentage, if (late) " • Late" else "")

