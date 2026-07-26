package com.racelink.controller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.racelink.controller.core.ui.RaceLinkTheme
import com.racelink.controller.feature.home.HomeRoute
import com.racelink.controller.feature.home.HomeViewModel
import com.racelink.controller.feature.connect.ConnectRoute
import com.racelink.controller.feature.connect.ConnectViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { RaceLinkTheme { RaceLinkApp() } }
    }
}

@Composable
private fun RaceLinkApp() {
    val navController = rememberNavController()
    Surface(color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeRoute(
                viewModel = viewModel<HomeViewModel>(),
                onConnect = { navController.navigate("connect") },
                onProfiles = { navController.navigate("profiles") },
                onSettings = { navController.navigate("settings") },
            )
        }
        composable("connect") {
            ConnectRoute(
                viewModel = viewModel<ConnectViewModel>(),
                onDesktopSelected = { navController.navigate("pair/${it.address}/${it.pairingPort}/${it.hostName}") },
            )
        }
        composable("profiles") { FoundationScreen("Profiles", "Driving profiles will be stored locally and synchronised only after pairing.") }
        composable("settings") { FoundationScreen("Settings", "Controller preferences will be available once the input pipeline is established.") }
    }
    }
}

@Composable
private fun FoundationScreen(title: String, description: String) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
        Text(description, color = MaterialTheme.colorScheme.secondary, fontSize = 16.sp)
    }
}
