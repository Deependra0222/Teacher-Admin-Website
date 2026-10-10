package com.smartattend.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartattend.app.core.model.Student
import com.smartattend.app.ui.components.BackHeader
import com.smartattend.app.ui.components.DemoDataPill
import com.smartattend.app.ui.components.LabeledValue
import com.smartattend.app.ui.components.StudentSectionCard

@Composable
fun ProfileScreen(student: Student, onBack: () -> Unit, onLogout: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { BackHeader("Student profile", "Cached local demo identity", onBack) }
        item { DemoDataPill("NOT A FIREBASE IDENTITY") }
        item {
            StudentSectionCard(student.name) {
                LabeledValue("Roll number", student.rollNumber)
                LabeledValue("Email", student.email)
                LabeledValue("Department", student.department)
                LabeledValue("Batch", student.batch)
            }
        }
        item { StudentSectionCard("Authentication") { Text("This milestone uses demo login only. Firebase Authentication is not configured."); Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Log out of demo") } } }
    }
}
