package com.smartattend.app.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartattend.app.core.model.TimetableItem
import com.smartattend.app.ui.components.BackHeader
import com.smartattend.app.ui.components.DemoDataPill
import com.smartattend.app.ui.components.LabeledValue
import com.smartattend.app.ui.components.StudentSectionCard
import com.smartattend.app.ui.components.toCampusTime

@Composable
fun ScheduleDetailScreen(item: TimetableItem?, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { BackHeader(item?.subjectName ?: "Class not found", item?.subjectCode, onBack) }
        if (item == null) item { StudentSectionCard("Unavailable") { Text("This schedule item no longer exists locally.") } } else {
            item { DemoDataPill("DEMO TIMETABLE") }
            item {
                StudentSectionCard("Class details") {
                    LabeledValue("Type", item.kind.name)
                    LabeledValue("Time", "${item.start.toCampusTime()}–${item.end.toCampusTime()}")
                    LabeledValue("Room", item.room)
                    LabeledValue("Instructor", item.instructor)
                    Text(if (item.active) "Active demo session" else "Scheduled demo session", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
