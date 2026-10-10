package com.smartattend.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartattend.app.ui.theme.SmartNavy
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DemoBanner(modifier: Modifier = Modifier, applyStatusInset: Boolean = true) {
    val insetModifier = if (applyStatusInset) modifier.statusBarsPadding() else modifier
    Text(
        text = "DEMO - NOT VERIFIED BY BLE",
        modifier = insetModifier.fillMaxWidth().background(Color(0xFFFFE9B6)).padding(horizontal = 16.dp, vertical = 9.dp),
        color = Color(0xFF6B4700),
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
fun ScreenHeader(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = SmartNavy)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun LabeledValue(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold, color = SmartNavy)
    }
}

@Composable
fun StatusPill(text: String, background: Color, foreground: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = background, shape = RoundedCornerShape(100.dp)) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = foreground, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

fun Instant.toCampusTime(): String = DateTimeFormatter.ofPattern("hh:mm a")
    .withZone(ZoneId.of("Asia/Kolkata"))
    .format(this)
