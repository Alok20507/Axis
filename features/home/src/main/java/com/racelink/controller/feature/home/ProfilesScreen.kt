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
fun ProfilesScreen(
    onBack: () -> Unit,
    onCustomizeProfileLayout: (String) -> Unit
) {
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
                Text("Layout Profiles", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                "Create layout profiles and customize button positions & sizes for each profile.",
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 14.sp
            )

            Button(
                onClick = { showCreateDialog = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("➕ Create Custom Profile", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            profileList.forEach { profileName ->
                ProfileOptionCard(
                    title = profileName,
                    isSelected = selectedProfile == profileName,
                    onSelect = {
                        selectedProfile = profileName
                        store.currentProfile = profileName
                    },
                    onCustomize = {
                        selectedProfile = profileName
                        store.currentProfile = profileName
                        onCustomizeProfileLayout(profileName)
                    },
                    onDelete = if (profileName != "Default Profile") {
                        {
                            store.deleteCustomProfile(profileName)
                            selectedProfile = store.currentProfile
                            profileList = store.customProfileNames.toList()
                        }
                    } else null,
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
            title = { Text("Create Custom Layout Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter a name for your custom layout profile (e.g. My Driving Layout, Custom Gamepad).")
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
                            val name = newProfileName.trim()
                            store.addCustomProfile(name)
                            selectedProfile = name
                            profileList = store.customProfileNames.toList()
                            newProfileName = ""
                            showCreateDialog = false
                            onCustomizeProfileLayout(name)
                        }
                    },
                    enabled = newProfileName.isNotBlank()
                ) {
                    Text("Create & Edit Layout")
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
    isSelected: Boolean,
    onSelect: () -> Unit,
    onCustomize: () -> Unit,
    onDelete: (() -> Unit)?,
    onResetLayout: () -> Unit
) {
    Surface(
        onClick = onSelect,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    Text("ACTIVE FOR PC", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onCustomize,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("✏️ Move & Resize Controls", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                if (onDelete != null) {
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.height(42.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🗑️ Delete", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (isSelected) {
                TextButton(
                    onClick = onResetLayout,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Reset Layout Positions", fontSize = 12.sp)
                }
            }
        }
    }
}
