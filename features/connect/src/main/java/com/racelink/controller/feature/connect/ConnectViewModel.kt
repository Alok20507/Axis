package com.racelink.controller.feature.connect

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.racelink.controller.core.network.DiscoveredDesktop
import com.racelink.controller.core.network.WifiDiscoveryManager
import com.racelink.controller.core.network.pairing.PairingClient
import com.racelink.controller.core.storage.PairedDesktopStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.InetAddress

data class ConnectUiState(
    val isDiscovering: Boolean = false,
    val isPairing: Boolean = false,
    val selectedDesktop: DiscoveredDesktop? = null,
    val pinInput: String = "",
    val desktops: List<DiscoveredDesktop> = emptyList(),
    val manualAddress: String = "",
    val errorMessage: String? = null,
    val pairingSuccessSessionKey: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ConnectUiState
        return isDiscovering == other.isDiscovering &&
                isPairing == other.isPairing &&
                selectedDesktop == other.selectedDesktop &&
                pinInput == other.pinInput &&
                desktops == other.desktops &&
                manualAddress == other.manualAddress &&
                errorMessage == other.errorMessage &&
                (pairingSuccessSessionKey?.contentEquals(other.pairingSuccessSessionKey ?: byteArrayOf()) ?: (other.pairingSuccessSessionKey == null))
    }

    override fun hashCode(): Int = 31 * isDiscovering.hashCode() + isPairing.hashCode()
}

class ConnectViewModel(application: Application) : AndroidViewModel(application) {
    private val discoveryManager = WifiDiscoveryManager(application)
    private val pairedStore = PairedDesktopStore(application)

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

    fun selectDesktop(desktop: DiscoveredDesktop, onConnectDirectly: (DiscoveredDesktop, ByteArray) -> Unit) {
        viewModelScope.launch {
            val saved = pairedStore.savedSession.firstOrNull()
            if (saved != null && (saved.hostAddress == desktop.address || saved.sessionKey.size == 32)) {
                // Auto-reconnect using saved paired session key (no PIN prompt required!)
                onConnectDirectly(desktop, saved.sessionKey)
            } else {
                // Prompt for initial 6-digit PIN pairing
                mutableState.update { it.copy(selectedDesktop = desktop, pinInput = "", errorMessage = null) }
            }
        }
    }

    fun dismissPairing() {
        mutableState.update { it.copy(selectedDesktop = null, pinInput = "", errorMessage = null) }
    }

    fun updatePinInput(pin: String) {
        if (pin.length <= 6 && pin.all { it.isDigit() }) {
            mutableState.update { it.copy(pinInput = pin, errorMessage = null) }
        }
    }

    fun pairSelectedDesktop(onSuccess: (DiscoveredDesktop, ByteArray) -> Unit) {
        val desktop = mutableState.value.selectedDesktop ?: return
        val pin = mutableState.value.pinInput.trim()
        if (pin.length != 6) {
            mutableState.update { it.copy(errorMessage = "Enter the 6-digit PIN shown on Axis Desktop.") }
            return
        }

        viewModelScope.launch {
            mutableState.update { it.copy(isPairing = true, errorMessage = null) }
            runCatching { PairingClient.pair(desktop.address, pin) }
                .onSuccess { result ->
                    pairedStore.saveSession(desktop.hostName, desktop.address, result.sessionKey)
                    mutableState.update { it.copy(isPairing = false, selectedDesktop = null) }
                    onSuccess(desktop, result.sessionKey)
                }
                .onFailure { error ->
                    mutableState.update { it.copy(isPairing = false, errorMessage = error.message ?: "Pairing failed. Check PIN and try again.") }
                }
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
                        if (result == null) it.copy(isDiscovering = false, errorMessage = "No compatible Axis desktop responded at $input.")
                        else it.copy(isDiscovering = false, desktops = listOf(result))
                    }
                }
                .onFailure { error -> mutableState.update { it.copy(isDiscovering = false, errorMessage = error.message ?: "The address is invalid or unreachable.") } }
        }
    }
}
