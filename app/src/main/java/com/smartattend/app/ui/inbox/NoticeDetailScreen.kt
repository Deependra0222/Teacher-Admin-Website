package com.smartattend.app.ui.inbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartattend.app.core.model.StudentNotice
import com.smartattend.app.ui.components.BackHeader
import com.smartattend.app.ui.components.DemoDataPill
import com.smartattend.app.ui.components.LabeledValue
import com.smartattend.app.ui.components.StudentSectionCard
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun NoticeDetailScreen(notice: StudentNotice?, onBack: () -> Unit, onSetRead: (Boolean) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { BackHeader(notice?.title ?: "Notice not found", notice?.type?.name?.replace('_', ' '), onBack) }
        if (notice == null) item { StudentSectionCard("Unavailable") { Text("This notice may have been removed from local demo data.") } } else {
            item { DemoDataPill("DEMO NOTICE") }
            item {
                StudentSectionCard("Details") {
                    Text(notice.body)
                    LabeledValue("Published", DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a").withZone(ZoneId.of("Asia/Kolkata")).format(notice.publishedAt))
                    notice.dueAt?.let { LabeledValue("Due", DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a").withZone(ZoneId.of("Asia/Kolkata")).format(it)) }
                    if (notice.isRead) OutlinedButton(onClick = { onSetRead(false) }, modifier = Modifier.fillMaxWidth()) { Text("Mark unread") }
                    else Button(onClick = { onSetRead(true) }, modifier = Modifier.fillMaxWidth()) { Text("Mark read") }
                }
            }
        }
    }
}
