package com.racelink.controller.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.racelink.controller.core.storage.ControllerPreferencesStore

@Composable
fun ProfilesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { ControllerPreferencesStore(context) }
    var selectedProfile by remember { mutableStateOf(store.currentProfile) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(10.dp))
            
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("← Back", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("Controller Profiles", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                "Select or switch active controller mapping preset for your PC games.",
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 15.sp
            )

            // Profile Cards
            ProfileOptionCard(
                title = "🎮 Universal Xbox & PlayStation Gamepad",
                subtitle = "Ideal for God of War, GTA V, Solo Leveling, Cyberpunk 2077",
                details = "Dual analog thumbsticks, △ ◯ ✕ ▢ action buttons, D-pad, adaptive triggers (L2/R2) & shoulder bumpers (L1/R1).",
                isSelected = selectedProfile == "Universal Gamepad",
                onSelect = {
                    selectedProfile = "Universal Gamepad"
                    store.currentProfile = "Universal Gamepad"
                }
            )

            ProfileOptionCard(
                title = "🏎️ Road Racing Wheel & Motion Gyro",
                subtitle = "Ideal for Forza Horizon, Need for Speed, Assetto Corsa",
                details = "360° continuous steering wheel, Motion Gyroscope tilt steering, analog brake & accelerator pedals, paddle shifters.",
                isSelected = selectedProfile == "Road Racing",
                onSelect = {
                    selectedProfile = "Road Racing"
                    store.currentProfile = "Road Racing"
                }
            )

            ProfileOptionCard(
                title = "🖱️ PC Remote Touchpad & Mouse",
                subtitle = "Ideal for Windows Desktop control, Strategy, & Web Navigation",
                details = "Multi-touch trackpad, sub-pixel cursor movement, Left & Right mouse click buttons, volume control & launcher shortcuts.",
                isSelected = selectedProfile == "Touchpad Mouse",
                onSelect = {
                    selectedProfile = "Touchpad Mouse"
                    store.currentProfile = "Touchpad Mouse"
                }
            )
        }
    }
}

@Composable
private fun ProfileOptionCard(
    title: String,
    subtitle: String,
    details: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        onClick = onSelect,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                if (isSelected) {
                    Text("ACTIVE", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
            }
            Text(
                subtitle,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.secondary
            )
            Text(
                details,
                fontSize = 13.sp,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.secondary
            )
        }
    }
}
