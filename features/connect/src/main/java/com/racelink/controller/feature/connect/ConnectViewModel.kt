package com.racelink.controller.feature.connect

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.racelink.controller.core.network.DiscoveredDesktop
import com.racelink.controller.core.network.WifiDiscoveryManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.InetAddress

data class ConnectUiState(
    val isDiscovering: Boolean = false,
    val desktops: List<DiscoveredDesktop> = emptyList(),
    val manualAddress: String = "",
    val errorMessage: String? = null,
)

class ConnectViewModel(application: Application) : AndroidViewModel(application) {
    private val discoveryManager = WifiDiscoveryManager(application)
    private val mutableState = MutableStateFlow(ConnectUiState())
    val state = mutableState.asStateFlow()
    private var discoveryJob: Job? = null

    fun discover() {
        discoveryJob?.cancel()
        discoveryJob = viewModelScope.launch {
            mutableState.update { it.copy(isDiscovering = true, errorMessage = null, desktops = emptyList()) }
            runCatching { discoveryManager.discover() }
                .onSuccess { results -> mutableState.update { it.copy(isDiscovering = false, desktops = results) } }
                .onFailure { error -> mutableState.update { it.copy(isDiscovering = false, errorMessage = error.message ?: "Discovery could not start.") } }
        }
    }

    fun updateManualAddress(address: String) = mutableState.update { it.copy(manualAddress = address, errorMessage = null) }

    fun probeManualAddress() {
        val input = mutableState.value.manualAddress.trim()
        if (input.isBlank()) {
            mutableState.update { it.copy(errorMessage = "Enter a PC address.") }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(isDiscovering = true, errorMessage = null) }
            runCatching { discoveryManager.probe(InetAddress.getByName(input)) }
                .onSuccess { result ->
                    mutableState.update {
                        if (result == null) it.copy(isDiscovering = false, errorMessage = "No compatible RaceLink desktop responded at $input.")
                        else it.copy(isDiscovering = false, desktops = listOf(result))
                    }
                }
                .onFailure { error -> mutableState.update { it.copy(isDiscovering = false, errorMessage = error.message ?: "The address is invalid or unreachable.") } }
        }
    }
}
