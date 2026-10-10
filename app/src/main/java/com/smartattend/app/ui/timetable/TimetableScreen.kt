package com.smartattend.app.ui.timetable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartattend.app.core.model.TimetableItem
import com.smartattend.app.ui.components.ScreenHeader
import com.smartattend.app.ui.components.StatusPill
import com.smartattend.app.ui.components.toCampusTime
import com.smartattend.app.ui.theme.SmartSuccess

@Composable
fun TimetableScreen(items: List<TimetableItem>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader("Today's Timetable", "Campus time • Asia/Kolkata") }
        if (items.isEmpty()) item { Text("No lectures scheduled for today.") }
        items(items, key = { it.id }) { lecture ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (lecture.active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
            ) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${lecture.subjectCode} • ${lecture.subjectName}", fontWeight = FontWeight.Bold)
                        Text("${lecture.start.toCampusTime()} - ${lecture.end.toCampusTime()}")
                        Text(lecture.room, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (lecture.active) StatusPill("ACTIVE", SmartSuccess, Color.White)
                }
            }
        }
    }
}

