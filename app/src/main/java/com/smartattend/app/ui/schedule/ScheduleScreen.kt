package com.smartattend.app.ui.schedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartattend.app.core.model.TimetableItem
import com.smartattend.app.ui.components.DemoDataPill
import com.smartattend.app.ui.components.ScreenHeader
import com.smartattend.app.ui.components.StudentSectionCard
import com.smartattend.app.ui.components.toCampusTime
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun ScheduleScreen(
    items: List<TimetableItem>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onOpenItem: (String) -> Unit,
) {
    val zone = ZoneId.of("Asia/Kolkata")
    val agenda = items.filter { it.start.atZone(zone).toLocalDate() == selectedDate }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader("Schedule", "Calendar and agenda for demo lectures and labs") }
        item { DemoDataPill() }
        item {
            StudentSectionCard("Selected date", icon = Icons.Rounded.CalendarMonth) {
                Text(selectedDate.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { onDateSelected(selectedDate.minusDays(1)) }, label = { Text("Previous") })
                    AssistChip(onClick = { onDateSelected(LocalDate.now(zone)) }, label = { Text("Today") })
                    AssistChip(onClick = { onDateSelected(selectedDate.plusDays(1)) }, label = { Text("Next") })
                }
            }
        }
        if (agenda.isEmpty()) item { StudentSectionCard("No classes") { Text("No demo timetable entries for this date.") } }
        items(agenda, key = { it.id }) { item ->
            StudentSectionCard(item.subjectName, modifier = Modifier.clickable { onOpenItem(item.id) }) {
                Text("${item.subjectCode} • ${item.kind.name}", fontWeight = FontWeight.SemiBold)
                Text("${item.start.toCampusTime()}–${item.end.toCampusTime()} • ${item.room}")
                Text(item.instructor, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (item.active) Text("ACTIVE DEMO SESSION", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}
