package com.racelink.controller.feature.connect

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.racelink.controller.core.haptics.HapticsManager
import com.racelink.controller.core.network.ControllerPacketCodec
import com.racelink.controller.core.network.WireControlFrame
import com.racelink.controller.core.sensors.MotionSteeringProducer
import com.racelink.controller.core.storage.ControllerPreferencesStore
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
    private val prefsStore = ControllerPreferencesStore(application)

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
        if (prefsStore.hapticsEnabled) haptics.tick()
    }

    fun toggleGyro(enabled: Boolean) {
        mutableState.update { it.copy(useGyro = enabled) }
        if (enabled) {
            motionProducer.sensitivity = prefsStore.gyroSensitivity
            motionProducer.deadzone = prefsStore.deadzone
            motionProducer.start()
        } else {
            motionProducer.close()
        }
        if (prefsStore.hapticsEnabled) haptics.tick()
    }

    fun recalibrateGyro() {
        motionProducer.recalibrateZero()
        if (prefsStore.hapticsEnabled) haptics.heavyClick()
    }

    fun setLeftStick(x: Float, y: Float) {
        val s = prefsStore.stickSensitivity
        mutableState.update { it.copy(leftStickX = (x * s).coerceIn(-1f, 1f), leftStickY = (y * s).coerceIn(-1f, 1f)) }
    }

    fun setRightStick(x: Float, y: Float) {
        val s = prefsStore.stickSensitivity
        mutableState.update { it.copy(rightStickX = (x * s).coerceIn(-1f, 1f), rightStickY = (y * s).coerceIn(-1f, 1f)) }
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
        if (active && prefsStore.hapticsEnabled) haptics.heavyClick()
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
        if (pressed && prefsStore.hapticsEnabled) haptics.tick()
    }

    private fun startControlLoop() {
        controlLoopJob?.cancel()
        controlLoopJob = viewModelScope.launch(Dispatchers.IO) {
            val targetIp = runCatching { InetAddress.getByName(hostAddressStr) }.getOrNull() ?: return@launch
            DatagramSocket().use { socket ->
                val buffer = ByteBuffer.allocate(256)
                while (isActive) {
                    val s = mutableState.value
                    val motion = motionProducer.state.value

                    // Steering Wheel (Left Stick X) in RACING mode, or touch stick
                    val lsX = if (s.useGyro && s.mode == ControllerMode.RACING) motion.roll else s.leftStickX

                    // Camera Aiming (Right Stick X & Y) in GAMEPAD mode (Sony DualSense Motion Aim)
                    val rsX = if (s.useGyro && s.mode == ControllerMode.GAMEPAD) {
                        (s.rightStickX + motion.roll).coerceIn(-1f, 1f)
                    } else s.rightStickX

                    val rsY = if (s.useGyro && s.mode == ControllerMode.GAMEPAD) {
                        (s.rightStickY + motion.pitch).coerceIn(-1f, 1f)
                    } else s.rightStickY

                    val frame = WireControlFrame(
                        sequence = sequence.getAndIncrement(),
                        timestampNanos = System.nanoTime(),
                        leftStickX = lsX,
                        leftStickY = s.leftStickY,
                        rightStickX = rsX,
                        rightStickY = rsY,
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
