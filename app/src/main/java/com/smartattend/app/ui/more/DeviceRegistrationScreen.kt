package com.smartattend.app.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import com.smartattend.app.core.model.RegisteredDevice
import com.smartattend.app.ui.components.BackHeader
import com.smartattend.app.ui.components.DemoDataPill
import com.smartattend.app.ui.components.LabeledValue
import com.smartattend.app.ui.components.StudentSectionCard

@Composable
fun DeviceRegistrationScreen(device: RegisteredDevice?, bleDiagnostic: String?, onBack: () -> Unit, onRegister: (String) -> Unit, onSimulateSignals: () -> Unit) {
    var label by rememberSaveable { mutableStateOf(device?.label ?: "My Android phone") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { BackHeader("Device registration", "Local placeholder for future server verification", onBack) }
        item { DemoDataPill("NOT VERIFIED • NO HARDWARE SCAN") }
        item {
            StudentSectionCard("Installation") {
                LabeledValue("ID", device?.installationId ?: "Created when registration is requested")
                LabeledValue("State", device?.state?.name?.replace('_', ' ') ?: "NOT REGISTERED")
                Text("This demo does not call a server, platform attestation service, or Bluetooth scanner.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(label, { label = it }, label = { Text("Device label") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Button(onClick = { onRegister(label) }, modifier = Modifier.fillMaxWidth()) { Text("Save pending registration") }
            }
        }
        item {
            StudentSectionCard("Simulated BLE diagnostics") {
                Text("Uncalibrated sample engine only", fontWeight = FontWeight.Bold)
                Text(bleDiagnostic ?: "No simulated sample has been processed")
                Button(onClick = onSimulateSignals) { Text("Run simulated signal burst") }
            }
        }
    }
}
