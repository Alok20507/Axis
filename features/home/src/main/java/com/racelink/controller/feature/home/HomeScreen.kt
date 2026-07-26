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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
    onCustomizeLayout: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onConnect = onConnect,
        onProfiles = onProfiles,
        onSettings = onSettings,
        onCustomizeLayout = onCustomizeLayout
    )
}

@Composable
private fun HomeScreen(
    state: HomeUiState,
    onConnect: () -> Unit,
    onProfiles: () -> Unit,
    onSettings: () -> Unit,
    onCustomizeLayout: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(
                painter = painterResource(id = R.drawable.ic_axis_logo),
                contentDescription = "Axis Icon",
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
            )
            Text("AXIS", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        }
        Text("Ready to play.", fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.SemiBold)

        Text("A precision controller for your PC.", color = MaterialTheme.colorScheme.secondary, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        StatusCard(state)

        // Primary Action: Connect to PC
        Button(
            onClick = onConnect,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
        ) {
            Text("🎮 Connect to PC", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        // Secondary Action: Test & Customize Layout (Offline Editor)
        OutlinedButton(
            onClick = onCustomizeLayout,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text("✏️ Test & Customize Button Layout", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        // Tertiary Action: Saved Profiles
        Surface(
            onClick = onProfiles,
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("📁 Custom Layout Profiles", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("→", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            }
        }

        Surface(
            onClick = onSettings,
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("⚙️ Controller Preferences", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("→", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun StatusCard(state: HomeUiState) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(state.connectionLabel, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(state.connectionDetail, color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Battery ${state.batteryPercent}%", color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
                Spacer(Modifier.width(20.dp))
                Text(state.latencyMs?.let { "$it ms" } ?: "Latency —", color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
            }
        }
    }
}
