package com.smartattend.app.ui.attendance

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
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.smartattend.app.domain.shortage.ShortageProjection
import com.smartattend.app.ui.SmartAttendUiState
import com.smartattend.app.ui.components.BackHeader
import com.smartattend.app.ui.components.DemoDataPill
import com.smartattend.app.ui.components.StudentSectionCard

@Composable
fun ShortageCalculatorScreen(state: SmartAttendUiState, onBack: () -> Unit, onAttended: (String) -> Unit, onTotal: (String) -> Unit, onTarget: (String) -> Unit, onCalculate: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { BackHeader("Shortage calculator", "Projection only — does not change attendance", onBack) }
        item { DemoDataPill("DEMO PROJECTION") }
        item {
            StudentSectionCard("Inputs") {
                NumericField("Classes attended", state.calculatorAttended, onAttended)
                NumericField("Total classes", state.calculatorTotal, onTotal)
                NumericField("Required percentage", state.calculatorTarget, onTarget)
                state.calculatorError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = onCalculate, modifier = Modifier.fillMaxWidth()) { Text("Calculate projection") }
            }
        }
        state.calculatorResult?.takeUnless { it is ShortageProjection.Invalid }?.let { result ->
            item {
                StudentSectionCard("Projection") {
                    Text(projectionText(result), style = MaterialTheme.typography.titleMedium)
                    Text("Future attendance can change this result. Only teacher-finalized records are official.")
                }
            }
        }
    }
}

@Composable
private fun NumericField(label: String, value: String, onValue: (String) -> Unit) {
    OutlinedTextField(value, { onValue(it.filter(Char::isDigit)) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
}

private fun projectionText(result: ShortageProjection): String = when (result) {
    is ShortageProjection.OnTrack -> "On track at %.1f%%. You can miss ${result.classesCanMiss} consecutive future class(es) and remain at or above ${result.targetPercentage}%.".format(result.currentPercentage)
    is ShortageProjection.NeedsClasses -> "Currently %.1f%%. Attend ${result.classesToAttend} consecutive future class(es) to reach ${result.targetPercentage}%.".format(result.currentPercentage)
    is ShortageProjection.Unattainable -> result.reason
    is ShortageProjection.Invalid -> result.reason
}
