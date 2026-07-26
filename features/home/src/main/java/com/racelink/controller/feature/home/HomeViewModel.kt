package com.racelink.controller.feature.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HomeUiState(
    val connectionLabel: String = "No PC connected",
    val connectionDetail: String = "Connect Axis for a live controller.",
    val currentProfile: String = "Road Racing",
    val batteryPercent: Int = 100,
    val latencyMs: Int? = null,
)

class HomeViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(HomeUiState())
    val state = mutableState.asStateFlow()
}
