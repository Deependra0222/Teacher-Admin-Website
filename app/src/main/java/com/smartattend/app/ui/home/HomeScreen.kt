package com.smartattend.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartattend.app.core.model.AttendanceResult
import com.smartattend.app.core.model.ClassSession
import com.smartattend.app.core.model.Student
import com.smartattend.app.domain.session.SessionDisplayState
import com.smartattend.app.ui.components.LabeledValue
import com.smartattend.app.ui.components.ScreenHeader
import com.smartattend.app.ui.components.StatusPill
import com.smartattend.app.ui.components.toCampusTime
import com.smartattend.app.ui.theme.SmartNavy
import com.smartattend.app.ui.theme.SmartSuccess
import com.smartattend.app.ui.theme.SmartWarning

@Composable
fun HomeScreen(
    student: Student,
    session: ClassSession,
    sessionState: SessionDisplayState,
    inside: Boolean,
    confirmedMinutes: Long,
    result: AttendanceResult?,
    onEnter: () -> Unit,
    onExit: () -> Unit,
    onFinish: () -> Unit,
) {
    val eligible = sessionState == SessionDisplayState.ACTIVE_ELIGIBLE
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ScreenHeader("Hello, ${student.name.substringBefore(' ')}", "Your current classroom status and demo controls") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SmartNavy), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(session.subjectName, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        StatusPill(sessionState.readableName(), if (eligible) SmartSuccess else SmartWarning, Color.White)
                    }
                    Text("${session.start.toCampusTime()} - ${session.end.toCampusTime()} • ${session.room}", color = Color(0xFFDCE8FF))
                    Text("Batch ${session.batch}", color = Color(0xFFDCE8FF))
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Simulated classroom presence", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Manual controls create untrusted demo evidence only. No Bluetooth scan is running.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    LabeledValue("Detection", if (inside) "Inside (simulated)" else "Outside")
                    LabeledValue("Confirmed so far", "$confirmedMinutes min")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onEnter, enabled = eligible && !inside, modifier = Modifier.weight(1f)) { Text("Enter") }
                        OutlinedButton(onClick = onExit, enabled = eligible && inside, modifier = Modifier.weight(1f)) { Text("Exit") }
                    }
                    OutlinedButton(onClick = onFinish, enabled = eligible, modifier = Modifier.fillMaxWidth()) { Text("Calculate demo result") }
                }
            }
        }
        if (result != null) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Local calculation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        LabeledValue("Result", result.status.name)
                        LabeledValue("Percentage", "%.1f%%".format(result.percentage))
                        LabeledValue("Late flag", if (result.late) "Yes" else "No")
                        Text("Not finalized by a teacher", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

private fun SessionDisplayState.readableName(): String = when (this) {
    SessionDisplayState.NO_CLASS -> "No class"
    SessionDisplayState.UPCOMING -> "Upcoming"
    SessionDisplayState.ACTIVE_ELIGIBLE -> "Active • eligible"
    SessionDisplayState.ACTIVE_INELIGIBLE -> "Active • ineligible"
}
