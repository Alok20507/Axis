package com.racelink.controller

import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
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
import com.racelink.controller.feature.home.ProfilesScreen
import com.racelink.controller.feature.home.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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
                    onCustomizeLayout = { navController.navigate("controller/preview/preview") }
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

            composable("profiles") { ProfilesScreen(onBack = { navController.popBackStack() }) }
            composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }) }
        }
    }
}
