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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.racelink.controller.core.ui.RaceLinkTheme
import com.racelink.controller.feature.connect.ConnectRoute
import com.racelink.controller.feature.connect.ConnectViewModel
import com.racelink.controller.feature.connect.ControllerRoute
import com.racelink.controller.feature.connect.ControllerViewModel
import com.racelink.controller.feature.home.HomeRoute
import com.racelink.controller.feature.home.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import android.util.Base64

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
                    onPairSuccess = { desktop, sessionKey ->
                        val keyB64 = Base64.encodeToString(sessionKey, Base64.NO_WRAP)
                        val encodedAddr = URLEncoder.encode(desktop.address, StandardCharsets.UTF_8.toString())
                        val encodedKey = URLEncoder.encode(keyB64, StandardCharsets.UTF_8.toString())
                        navController.navigate("controller/$encodedAddr/$encodedKey")
                    }
                )
            }

            composable(
                route = "controller/{address}/{key}",
                arguments = listOf(
                    navArgument("address") { type = NavType.StringType },
                    navArgument("key") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val rawAddr = backStackEntry.arguments?.getString("address") ?: ""
                val rawKey = backStackEntry.arguments?.getString("key") ?: ""

                val address = URLDecoder.decode(rawAddr, StandardCharsets.UTF_8.toString())
                val keyB64 = URLDecoder.decode(rawKey, StandardCharsets.UTF_8.toString())
                val sessionKey = runCatching { Base64.decode(keyB64, Base64.NO_WRAP) }.getOrDefault(ByteArray(32))

                ControllerRoute(
                    viewModel = viewModel<ControllerViewModel>(),
                    address = address,
                    sessionKey = sessionKey,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("profiles") { FoundationScreen("Profiles", "Driving profiles and force-feedback presets stored locally.") }
            composable("settings") { FoundationScreen("Settings", "Controller input sensitivity, deadzones, and sensor calibration.") }
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
