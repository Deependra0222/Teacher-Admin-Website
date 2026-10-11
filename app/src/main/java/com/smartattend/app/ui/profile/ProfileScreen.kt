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
import com.smartattend.app.domain.auth.AuthSessionMode
import com.smartattend.app.ui.components.BackHeader
import com.smartattend.app.ui.components.DemoDataPill
import com.smartattend.app.ui.components.LabeledValue
import com.smartattend.app.ui.components.StudentSectionCard

@Composable
fun ProfileScreen(
    student: Student,
    authMode: AuthSessionMode,
    onBack: () -> Unit,
    onLogout: () -> Unit,
) {
    val isFirebase = authMode == AuthSessionMode.FIREBASE
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            BackHeader(
                "Student profile",
                if (isFirebase) "Authorized Firebase student profile" else "Cached local demo identity",
                onBack,
            )
        }
        item {
            DemoDataPill(if (isFirebase) "FIREBASE AUTHENTICATED STUDENT" else "DEMO LOCAL IDENTITY")
        }
        item {
            StudentSectionCard(student.name) {
                LabeledValue("Roll number", student.rollNumber)
                LabeledValue("Email", student.email)
                LabeledValue("Department", student.department)
                LabeledValue("Batch", student.batch)
            }
        }
        item {
            StudentSectionCard("Authentication") {
                Text(
                    if (isFirebase) {
                        "Your identity was loaded from your UID-bound Firestore profile. Attendance, timetable, notices, corrections, device and BLE records in this milestone remain DEMO and are not synchronized."
                    } else {
                        "This is an explicit offline Demo Mode session. It is not a Firebase identity and cannot record verified attendance."
                    },
                )
                Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                    Text(if (isFirebase) "Sign out" else "Exit Demo Mode")
                }
            }
        }
    }
}
