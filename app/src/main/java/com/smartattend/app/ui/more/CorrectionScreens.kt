package com.smartattend.app.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartattend.app.core.model.AttendanceHistoryItem
import com.smartattend.app.core.model.AttendanceStatus
import com.smartattend.app.core.model.CorrectionRequest
import com.smartattend.app.ui.components.BackHeader
import com.smartattend.app.ui.components.DemoDataPill
import com.smartattend.app.ui.components.LabeledValue
import com.smartattend.app.ui.components.StudentSectionCard

@Composable
fun CorrectionListScreen(requests: List<CorrectionRequest>, onBack: () -> Unit, onOpen: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { BackHeader("Correction requests", "Submitted evidence cannot directly edit attendance", onBack) }
        item { DemoDataPill("DEMO REQUESTS") }
        if (requests.isEmpty()) item { StudentSectionCard("No requests") { Text("Open an attendance history record to submit a correction.") } }
        items(requests, key = { it.id }) { request ->
            StudentSectionCard("${request.subjectCode} • ${request.status.name}", Modifier.clickable { onOpen(request.id) }) {
                Text("${request.currentStatus.name} → ${request.requestedStatus.name}", fontWeight = FontWeight.Bold)
                Text(request.reason, maxLines = 2)
                if (request.status.name == "PENDING") Text("Pending backend configuration", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun CorrectionFormScreen(record: AttendanceHistoryItem?, onBack: () -> Unit, onSubmit: (AttendanceStatus, String) -> Unit) {
    var requested by rememberSaveable(record?.id) { mutableStateOf(if (record?.status == AttendanceStatus.PRESENT) AttendanceStatus.ABSENT else AttendanceStatus.PRESENT) }
    var reason by rememberSaveable(record?.id) { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val localError = reason.trim().takeIf { it.isNotEmpty() && it.length < 10 }?.let { "Use at least 10 characters" }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { BackHeader("Request correction", record?.let { "${it.subjectCode} • ${it.date}" }, onBack) }
        if (record == null) item { StudentSectionCard("Record not found") { Text("Return to history and choose another record.") } } else {
            item { DemoDataPill("LOCAL PENDING REQUEST") }
            item {
                StudentSectionCard("Recorded evidence") {
                    LabeledValue("Current status", record.status.name)
                    LabeledValue("Source", "DEMO • NOT VERIFIED BY BLE")
                }
            }
            item {
                StudentSectionCard("Requested change") {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("Requested: ${requested.name}") }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        AttendanceStatus.entries.filter { it != record.status }.forEach { status ->
                            DropdownMenuItem(text = { Text(status.name) }, onClick = { requested = status; expanded = false })
                        }
                    }
                    OutlinedTextField(reason, { reason = it }, label = { Text("Mandatory reason") }, supportingText = { Text(localError ?: "At least 10 non-space characters") }, isError = localError != null, minLines = 3, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { onSubmit(requested, reason) }, enabled = reason.trim().length >= 10 && requested != record.status, modifier = Modifier.fillMaxWidth()) { Text("Save pending request") }
                }
            }
        }
    }
}

@Composable
fun CorrectionDetailScreen(request: CorrectionRequest?, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { BackHeader("Correction detail", request?.subjectCode, onBack) }
        if (request == null) item { StudentSectionCard("Request not found") { Text("It may have been removed during a demo reset.") } } else {
            item { DemoDataPill("DEMO REQUEST") }
            item {
                StudentSectionCard(request.status.name) {
                    LabeledValue("Recorded", request.currentStatus.name)
                    LabeledValue("Requested", request.requestedStatus.name)
                    Text(request.reason)
                    request.teacherResponse?.let { Text("Seeded response: $it", fontWeight = FontWeight.Bold) }
                    if (request.status.name == "PENDING") Text("Stored locally; no teacher or backend has received this request.", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
