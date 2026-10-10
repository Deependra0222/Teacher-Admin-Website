package com.smartattend.app.ui.attendance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartattend.app.core.model.AttendanceHistoryItem
import com.smartattend.app.core.model.SubjectAttendance
import com.smartattend.app.ui.components.BackHeader
import com.smartattend.app.ui.components.DemoDataPill
import com.smartattend.app.ui.components.LabeledValue
import com.smartattend.app.ui.components.StudentSectionCard

@Composable
fun SubjectDetailScreen(subject: SubjectAttendance?, history: List<AttendanceHistoryItem>, onBack: () -> Unit, onCalculator: () -> Unit, onCorrection: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { BackHeader(subject?.name ?: "Subject not found", subject?.code, onBack) }
        if (subject == null) item { StudentSectionCard("Unavailable") { Text("This subject may have been removed from local demo data.") } } else {
            item { DemoDataPill("DEMO ATTENDANCE") }
            item {
                StudentSectionCard("Summary") {
                    Text("%.1f%%".format(subject.percentage), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator({ (subject.percentage / 100).toFloat() }, Modifier.fillMaxWidth())
                    LabeledValue("Attended", "${subject.attended} of ${subject.total}")
                    LabeledValue("Target", "${subject.requiredPercentage}%")
                    Button(onClick = onCalculator) { Text("Open shortage calculator") }
                }
            }
            item { Text("Recent records", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(history.filter { it.subjectCode == subject.code }, key = { it.id }) { record ->
                StudentSectionCard(record.date.toString()) {
                    Text(record.status.name, fontWeight = FontWeight.Bold)
                    Text("${record.room} • ${if (record.late) "Late" else "On time"}")
                    Button(onClick = { onCorrection(record.id) }) { Text("Request correction") }
                }
            }
        }
    }
}
