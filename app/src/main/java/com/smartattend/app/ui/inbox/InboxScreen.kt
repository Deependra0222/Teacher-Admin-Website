package com.smartattend.app.ui.inbox

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartattend.app.core.model.StudentNotice
import com.smartattend.app.ui.components.DemoDataPill
import com.smartattend.app.ui.components.ScreenHeader
import com.smartattend.app.ui.components.StudentSectionCard
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun InboxScreen(
    notices: List<StudentNotice>,
    filter: String,
    onFilter: (String) -> Unit,
    onOpen: (String) -> Unit,
    onMarkAllRead: () -> Unit,
) {
    val shown = notices.filter { filter == "ALL" || it.type.name == filter }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader("Inbox", "Announcements, assignments and attendance updates") }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { DemoDataPill(); Button(onClick = onMarkAllRead) { Text("Mark all read") } } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("ALL", "ASSIGNMENT", "ATTENDANCE_ALERT").forEach { value ->
                    AssistChip(onClick = { onFilter(value) }, label = { Text(if (value == "ALL") "All" else value.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }) })
                }
            }
        }
        if (shown.isEmpty()) item { StudentSectionCard("Nothing here") { Text("No notices match this filter.") } }
        items(shown, key = { it.id }) { notice ->
            StudentSectionCard(notice.title, modifier = Modifier.clickable { onOpen(notice.id) }, icon = if (notice.isRead) Icons.Rounded.MarkEmailRead else null) {
                Text(notice.type.name.replace('_', ' '), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(notice.body, maxLines = 2)
                Text(DateTimeFormatter.ofPattern("dd MMM • hh:mm a").withZone(ZoneId.of("Asia/Kolkata")).format(notice.publishedAt), style = MaterialTheme.typography.labelMedium)
                if (!notice.isRead) Text("UNREAD", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
