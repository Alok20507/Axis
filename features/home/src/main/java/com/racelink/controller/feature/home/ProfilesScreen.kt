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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    var profileList by remember { mutableStateOf(store.customProfileNames.toList()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }

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
            verticalArrangement = Arrangement.spacedBy(18.dp)
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
                "Create, customize, and save custom button placement profiles for your PC games.",
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 14.sp
            )

            Button(
                onClick = { showCreateDialog = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("➕ Create New Profile", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            profileList.forEach { profileName ->
                ProfileOptionCard(
                    title = profileName,
                    subtitle = if (profileName == selectedProfile) "Active Layout Profile" else "Saved Custom Profile",
                    details = "Stores customized button sizes, coordinates, and analog placement.",
                    isSelected = selectedProfile == profileName,
                    onSelect = {
                        selectedProfile = profileName
                        store.currentProfile = profileName
                    },
                    onResetLayout = {
                        store.resetLayout(profileName, "GAMEPAD")
                        store.resetLayout(profileName, "RACING")
                    }
                )
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter a custom profile name (e.g. God of War Layout, GTA V Driving, Forza Wheel).")
                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        singleLine = true,
                        label = { Text("Profile Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProfileName.isNotBlank()) {
                            store.addCustomProfile(newProfileName.trim())
                            selectedProfile = newProfileName.trim()
                            profileList = store.customProfileNames.toList()
                            newProfileName = ""
                            showCreateDialog = false
                        }
                    },
                    enabled = newProfileName.isNotBlank()
                ) {
                    Text("Create Profile")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileOptionCard(
    title: String,
    subtitle: String,
    details: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onResetLayout: () -> Unit
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
            
            if (isSelected) {
                OutlinedButton(
                    onClick = onResetLayout,
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Reset Layout to Default", fontSize = 12.sp)
                }
            }
        }
    }
}
