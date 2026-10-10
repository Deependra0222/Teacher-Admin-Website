package com.smartattend.app.ui.attendance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartattend.app.core.model.SubjectAttendance
import com.smartattend.app.ui.components.LabeledValue
import com.smartattend.app.ui.components.ScreenHeader

@Composable
fun AttendanceScreen(subjects: List<SubjectAttendance>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader("My Attendance", "Subject totals from local demo records") }
        if (subjects.isEmpty()) item { Text("No attendance records are available yet.") }
        items(subjects, key = { it.code }) { subject ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("${subject.code} • ${subject.name}", fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = { (subject.percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    LabeledValue("Attended", "${subject.attended} / ${subject.total}")
                    LabeledValue("Percentage", "%.1f%%".format(subject.percentage))
                    if (subject.percentage < 75.0) {
                        Text("Shortage warning: below the 75% example policy", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

