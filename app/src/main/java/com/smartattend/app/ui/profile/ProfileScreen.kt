package com.smartattend.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartattend.app.core.model.Student
import com.smartattend.app.ui.components.LabeledValue
import com.smartattend.app.ui.components.ScreenHeader

@Composable
fun ProfileScreen(student: Student, onLogout: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ScreenHeader("Profile", "Local demo student details") }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Text(student.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    LabeledValue("Roll number", student.rollNumber)
                    LabeledValue("Email", student.email)
                    LabeledValue("Department", student.department)
                    LabeledValue("Batch", student.batch)
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Future classroom device", fontWeight = FontWeight.Bold)
                    Text("No ESP32 or BLE device is connected. Real hardware mode will be added only after calibration.")
                    OutlinedButton(onClick = {}, enabled = false) { Text("BLE setup unavailable") }
                }
            }
        }
        item { Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Log out of demo") } }
    }
}
