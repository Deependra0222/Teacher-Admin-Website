package com.smartattend.app.ui.attendance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartattend.app.core.model.SubjectAttendance
import com.smartattend.app.ui.components.DemoDataPill
import com.smartattend.app.ui.components.LabeledValue
import com.smartattend.app.ui.components.ScreenHeader
import com.smartattend.app.ui.components.StudentSectionCard

@Composable
fun AttendanceScreen(
    subjects: List<SubjectAttendance>,
    onSubject: (String) -> Unit,
    onCalculator: () -> Unit,
    onHistory: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader("Attendance", "Subject-wise local demo summaries") }
        item { DemoDataPill("DEMO • NOT TEACHER FINALIZED") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCalculator) { Text("Shortage calculator") }
                OutlinedButton(onClick = onHistory) { Text("History") }
            }
        }
        if (subjects.isEmpty()) item { StudentSectionCard("No records") { Text("Attendance records will appear after offline data loads.") } }
        items(subjects, key = { it.code }) { subject ->
            StudentSectionCard("${subject.code} • ${subject.name}", Modifier.clickable { onSubject(subject.code) }) {
                LinearProgressIndicator({ (subject.percentage / 100).toFloat().coerceIn(0f, 1f) }, Modifier.fillMaxWidth())
                LabeledValue("Attended", "${subject.attended} / ${subject.total}")
                LabeledValue("Percentage", "%.1f%%".format(subject.percentage))
                LabeledValue("Demo target", "${subject.requiredPercentage}%")
                if (subject.percentage < subject.requiredPercentage) {
                    Text("Shortage projection available", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                } else Text("On track in demo data", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
