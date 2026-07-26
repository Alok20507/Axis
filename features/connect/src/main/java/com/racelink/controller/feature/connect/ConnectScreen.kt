package com.racelink.controller.feature.connect

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.racelink.controller.core.network.DiscoveredDesktop
import com.racelink.controller.core.ui.R

@Composable
fun ConnectRoute(
    viewModel: ConnectViewModel,
    onPairSuccess: (DiscoveredDesktop, ByteArray) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.discover() }

    ConnectScreen(
        state = state,
        onRefresh = viewModel::discover,
        onSelectDesktop = viewModel::selectDesktop,
        onPinChange = viewModel::updatePinInput,
        onConfirmPairing = { viewModel.pairSelectedDesktop(onPairSuccess) },
        onDismissPairing = viewModel::dismissPairing,
        onManualAddressChange = viewModel::updateManualAddress,
        onManualProbe = viewModel::probeManualAddress
    )
}

@Composable
private fun ConnectScreen(
    state: ConnectUiState,
    onRefresh: () -> Unit,
    onSelectDesktop: (DiscoveredDesktop) -> Unit,
    onPinChange: (String) -> Unit,
    onConfirmPairing: () -> Unit,
    onDismissPairing: () -> Unit,
    onManualAddressChange: (String) -> Unit,
    onManualProbe: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.ic_axis_logo),
                    contentDescription = "Axis Icon",
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                )
                Text("Connection", fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
            }
            Text("Searching your local network for compatible Axis desktops.", color = MaterialTheme.colorScheme.secondary, fontSize = 16.sp)

            if (state.isDiscovering) Text("Looking for PCs…", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)

            state.desktops.forEach { desktop ->
                Surface(
                    onClick = { onSelectDesktop(desktop) },
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
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

            Button(onClick = onRefresh, enabled = !state.isDiscovering, modifier = Modifier.fillMaxWidth()) {
                Text("Search again")
            }

            Spacer(Modifier.height(8.dp))
            Text("Manual connection", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text("Enter the IPv4 address shown in Axis Desktop.", color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.manualAddress,
                    onValueChange = onManualAddressChange,
                    singleLine = true,
                    label = { Text("PC address") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                Button(onClick = onManualProbe, enabled = !state.isDiscovering, modifier = Modifier.height(56.dp)) {
                    Text("Find")
                }
            }
        }
    }

    // 6-Digit PIN Pairing Dialog
    state.selectedDesktop?.let { desktop ->
        AlertDialog(
            onDismissRequest = onDismissPairing,
            title = { Text("Pair with ${desktop.hostName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter the 6-digit PIN code currently displayed on Axis Desktop companion (${desktop.address}).")
                    OutlinedTextField(
                        value = state.pinInput,
                        onValueChange = onPinChange,
                        singleLine = true,
                        label = { Text("6-Digit PIN Code") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (state.isPairing) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Text("Pairing and deriving session key…", fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirmPairing,
                    enabled = state.pinInput.length == 6 && !state.isPairing,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Pair & Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissPairing, enabled = !state.isPairing) {
                    Text("Cancel")
                }
            }
        )
    }
}
