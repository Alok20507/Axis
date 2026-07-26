package com.racelink.controller.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.racelink.controller.core.ui.R

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onConnect: () -> Unit,
    onProfiles: () -> Unit,
    onSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(state, onConnect, onProfiles, onSettings)
}

@Composable
private fun HomeScreen(
    state: HomeUiState,
    onConnect: () -> Unit,
    onProfiles: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(
                painter = painterResource(id = R.drawable.ic_axis_logo),
                contentDescription = "Axis Icon",
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
            )
            Text("AXIS", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        }
        Text("Ready to drive.", fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.SemiBold)

        Text("A precision controller for your PC.", color = MaterialTheme.colorScheme.secondary, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        StatusCard(state)
        Button(
            onClick = onConnect,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
        ) { Text("Connect to PC", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
        SectionCard("Current profile", state.currentProfile, "Tune steering, pedals and haptics", onProfiles)
        SectionCard("Recent games", "No sessions yet", "Your recent driving sessions appear here", onConnect)
        Spacer(Modifier.weight(1f))
        Text("Settings", modifier = Modifier.align(Alignment.End).padding(8.dp), color = MaterialTheme.colorScheme.secondary, fontSize = 15.sp)
        Surface(onClick = onSettings, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Text("Controller preferences", modifier = Modifier.padding(18.dp), fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun StatusCard(state: HomeUiState) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(state.connectionLabel, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
            Text(state.connectionDetail, color = MaterialTheme.colorScheme.secondary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Battery ${state.batteryPercent}%", color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(20.dp))
                Text(state.latencyMs?.let { "$it ms" } ?: "Latency —", color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, value: String, detail: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
            Text(value, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Text(detail, color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
        }
    }
}
