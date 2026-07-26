package com.racelink.controller.feature.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.racelink.controller.core.network.DiscoveredDesktop

@Composable
fun ConnectRoute(viewModel: ConnectViewModel, onDesktopSelected: (DiscoveredDesktop) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.discover() }
    ConnectScreen(state, viewModel::discover, viewModel::updateManualAddress, viewModel::probeManualAddress, onDesktopSelected)
}

@Composable
private fun ConnectScreen(
    state: ConnectUiState,
    onRefresh: () -> Unit,
    onManualAddressChange: (String) -> Unit,
    onManualProbe: () -> Unit,
    onDesktopSelected: (DiscoveredDesktop) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Spacer(Modifier.height(20.dp))
            Text("Connection", fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
            Text("Searching your local network for compatible Axis desktops.", color = MaterialTheme.colorScheme.secondary, fontSize = 16.sp)
            if (state.isDiscovering) Text("Looking for PCs…", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            state.desktops.forEach { desktop ->
                Surface(onClick = { onDesktopSelected(desktop) }, color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(desktop.hostName, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                        Text(desktop.address, color = MaterialTheme.colorScheme.secondary)
                        Text("${desktop.roundTripMillis} ms discovery round trip", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                    }
                }
            }
            if (!state.isDiscovering && state.desktops.isEmpty() && state.errorMessage == null) {
                Text("No compatible PC found. Confirm Axis Desktop is running on the same Wi-Fi network.", color = MaterialTheme.colorScheme.secondary)
            }
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = onRefresh, enabled = !state.isDiscovering, modifier = Modifier.fillMaxWidth()) { Text("Search again") }
            Spacer(Modifier.height(8.dp))
            Text("Manual connection", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text("Enter the IPv4 address shown in Axis Desktop.", color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = state.manualAddress, onValueChange = onManualAddressChange, singleLine = true, label = { Text("PC address") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                Button(onClick = onManualProbe, enabled = !state.isDiscovering, modifier = Modifier.height(56.dp)) { Text("Find") }
            }
        }
    }
}
