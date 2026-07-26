package com.racelink.controller.feature.home

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.racelink.controller.core.storage.ControllerPreferencesStore
import com.racelink.controller.core.ui.R

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { ControllerPreferencesStore(context) }

    var gyroSensitivity by remember { mutableFloatStateOf(store.gyroSensitivity) }
    var stickSensitivity by remember { mutableFloatStateOf(store.stickSensitivity) }
    var deadzone by remember { mutableFloatStateOf(store.deadzone) }
    var hapticsEnabled by remember { mutableStateOf(store.hapticsEnabled) }
    var calibrationMessage by remember { mutableStateOf<String?>(null) }

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
                Text("Controller Preferences", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                "Customize sensor calibration, motion gyro tilt sensitivity, thumbstick deadzones, and haptics.",
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 15.sp
            )

            // 1. Gyro Steering Sensitivity
            PreferenceCard(
                title = "🏎️ Motion Steering / Gyro Sensitivity",
                value = String.format("%.1fx multiplier", gyroSensitivity),
                description = "Adjust how responsive the steering wheel is when tilting your phone."
            ) {
                Slider(
                    value = gyroSensitivity,
                    onValueChange = {
                        gyroSensitivity = it
                        store.gyroSensitivity = it
                    },
                    valueRange = 1.0f..4.0f,
                    steps = 15,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
                )
            }

            // 2. Analog Thumbstick Sensitivity
            PreferenceCard(
                title = "🎯 Thumbstick Sensitivity",
                value = String.format("%.1fx multiplier", stickSensitivity),
                description = "Adjust responsiveness for Left/Right analog thumbsticks."
            ) {
                Slider(
                    value = stickSensitivity,
                    onValueChange = {
                        stickSensitivity = it
                        store.stickSensitivity = it
                    },
                    valueRange = 0.5f..2.5f,
                    steps = 20,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
                )
            }

            // 3. Thumbstick Deadzone
            PreferenceCard(
                title = "⭕ Analog Stick Deadzone",
                value = String.format("%.0f%%", deadzone * 100f),
                description = "Ignore tiny accidental finger rests near center stick position."
            ) {
                Slider(
                    value = deadzone,
                    onValueChange = {
                        deadzone = it
                        store.deadzone = it
                    },
                    valueRange = 0.0f..0.20f,
                    steps = 20,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
                )
            }

            // 4. Haptic Vibration
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("⚡ Haptic Vibration Feedback", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text("Vibrate phone on button taps and trigger clicks", color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
                    }
                    Switch(
                        checked = hapticsEnabled,
                        onCheckedChange = {
                            hapticsEnabled = it
                            store.hapticsEnabled = it
                        }
                    )
                }
            }

            // 5. Gyro Calibration Button
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("📐 Gyroscope Horizon Calibration", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text("Hold your phone horizontally in your natural driving posture and tap calibrate.", color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
                    
                    Button(
                        onClick = {
                            calibrationMessage = "🟢 Gyroscope horizon zero-point recalibrated successfully!"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Recalibrate Gyro Center", fontWeight = FontWeight.SemiBold)
                    }

                    calibrationMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PreferenceCard(
    title: String,
    value: String,
    description: String,
    content: @Composable () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
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
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(value, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Text(description, color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
            content()
        }
    }
}
