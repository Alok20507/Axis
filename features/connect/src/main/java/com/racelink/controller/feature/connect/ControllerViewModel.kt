package com.racelink.controller.feature.connect

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.racelink.controller.core.haptics.HapticsManager
import com.racelink.controller.core.network.ControllerPacketCodec
import com.racelink.controller.core.network.WireControlFrame
import com.racelink.controller.core.sensors.MotionSteeringProducer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger

enum class ControllerMode {
    GAMEPAD, RACING
}

data class ControllerUiState(
    val hostAddress: String = "",
    val isConnected: Boolean = true,
    val mode: ControllerMode = ControllerMode.GAMEPAD,
    val useGyro: Boolean = false,
    val leftStickX: Float = 0f,
    val leftStickY: Float = 0f,
    val rightStickX: Float = 0f,
    val rightStickY: Float = 0f,
    val throttle: Float = 0f,
    val brake: Float = 0f,
    val handbrake: Float = 0f,
    val buttonFlags: Short = 0,
    val latencyMs: Long = 4L,
)

class ControllerViewModel(application: Application) : AndroidViewModel(application) {
    private val haptics = HapticsManager(application)
    private val motionProducer = MotionSteeringProducer(application)

    private val mutableState = MutableStateFlow(ControllerUiState())
    val state = mutableState.asStateFlow()

    private var hostAddressStr: String = ""
    private var sessionKey: ByteArray = ByteArray(32)
    private val sequence = AtomicInteger(1)
    private var controlLoopJob: Job? = null

    fun startSession(address: String, key: ByteArray) {
        this.hostAddressStr = address
        this.sessionKey = key
        mutableState.update { it.copy(hostAddress = address, isConnected = true) }
        startControlLoop()
    }

    fun setMode(newMode: ControllerMode) {
        mutableState.update { it.copy(mode = newMode) }
        haptics.tick()
    }

    fun toggleGyro(enabled: Boolean) {
        mutableState.update { it.copy(useGyro = enabled) }
        if (enabled) {
            motionProducer.start()
        } else {
            motionProducer.close()
        }
        haptics.tick()
    }

    fun setLeftStick(x: Float, y: Float) {
        mutableState.update { it.copy(leftStickX = x.coerceIn(-1f, 1f), leftStickY = y.coerceIn(-1f, 1f)) }
    }

    fun setRightStick(x: Float, y: Float) {
        mutableState.update { it.copy(rightStickX = x.coerceIn(-1f, 1f), rightStickY = y.coerceIn(-1f, 1f)) }
    }

    fun setSteering(value: Float) {
        if (!mutableState.value.useGyro) {
            val clamped = value.coerceIn(-1f, 1f)
            mutableState.update { it.copy(leftStickX = clamped) }
        }
    }

    fun setThrottle(value: Float) {
        mutableState.update { it.copy(throttle = value.coerceIn(0f, 1f)) }
    }

    fun setBrake(value: Float) {
        mutableState.update { it.copy(brake = value.coerceIn(0f, 1f)) }
    }

    fun setHandbrake(active: Boolean) {
        mutableState.update { it.copy(handbrake = if (active) 1f else 0f) }
        if (active) haptics.heavyClick()
    }

    fun setButtonFlag(flag: Short, pressed: Boolean) {
        mutableState.update { current ->
            val newFlags = if (pressed) {
                (current.buttonFlags.toInt() or flag.toInt()).toShort()
            } else {
                (current.buttonFlags.toInt() and flag.toInt().inv()).toShort()
            }
            current.copy(buttonFlags = newFlags)
        }
        if (pressed) haptics.tick()
    }

    private fun startControlLoop() {
        controlLoopJob?.cancel()
        controlLoopJob = viewModelScope.launch(Dispatchers.IO) {
            val targetIp = runCatching { InetAddress.getByName(hostAddressStr) }.getOrNull() ?: return@launch
            DatagramSocket().use { socket ->
                val buffer = ByteBuffer.allocate(256)
                while (isActive) {
                    val s = mutableState.value
                    val lsX = if (s.useGyro) motionProducer.state.value.steering else s.leftStickX

                    val frame = WireControlFrame(
                        sequence = sequence.getAndIncrement(),
                        timestampNanos = System.nanoTime(),
                        leftStickX = lsX,
                        leftStickY = s.leftStickY,
                        rightStickX = s.rightStickX,
                        rightStickY = s.rightStickY,
                        throttle = s.throttle,
                        brake = s.brake,
                        handbrake = s.handbrake,
                        buttons = s.buttonFlags,
                    )

                    buffer.clear()
                    ControllerPacketCodec.encodeControl(frame, buffer)

                    val packetData = buffer.array()
                    val packetLen = buffer.position()
                    val dgram = DatagramPacket(packetData, packetLen, targetIp, 45102)
                    runCatching { socket.send(dgram) }

                    delay(8) // ~120 Hz loop (8.3 ms interval)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        motionProducer.close()
        controlLoopJob?.cancel()
    }
}
