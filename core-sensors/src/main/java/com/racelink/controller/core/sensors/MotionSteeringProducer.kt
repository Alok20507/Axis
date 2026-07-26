package com.racelink.controller.core.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

data class MotionControlState(
    val roll: Float = 0f,    // Landscape Steering / Camera Yaw (-1.0 to +1.0)
    val pitch: Float = 0f,   // Camera Pitch (-1.0 to +1.0)
    val timestampNanos: Long = 0L
)

/**
 * High-precision sensor motion producer for racing controller input.
 * Uses Sensor.TYPE_GAME_ROTATION_VECTOR (Gyro + Accelerometer fusion without magnetometer drift).
 */
class MotionSteeringProducer(context: Context) : SensorEventListener, AutoCloseable {
    private val manager: SensorManager? = runCatching { context.getSystemService(SensorManager::class.java) }.getOrNull()
    private val gameRotVector: Sensor? = manager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        ?: manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val gyro: Sensor? = manager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val mutableState = MutableStateFlow(MotionControlState())
    val state = mutableState.asStateFlow()

    var sensitivity: Float = 2.0f
    var deadzone: Float = 0.04f // ~2.3 degrees deadzone around calibrated center

    // Low-Pass Filter (Alpha ~ 0.18 for ~8ms ultra-low latency anti-jitter)
    private val lpfAlpha: Float = 0.18f
    private var filteredRoll: Float = 0f
    private var filteredPitch: Float = 0f

    // Dynamic Recalibration Zero-Offset Point
    private var zeroRollOffset: Float = 0f
    private var zeroPitchOffset: Float = 0f
    private var lastRawRoll: Float = 0f
    private var lastRawPitch: Float = 0f

    private var isListening = false

    fun start() {
        val m = manager ?: return
        if (isListening) return
        runCatching {
            if (gameRotVector != null) {
                m.registerListener(this, gameRotVector, SensorManager.SENSOR_DELAY_GAME)
            } else if (gyro != null) {
                m.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME)
            }
            isListening = true
        }
    }

    /**
     * Zero out current phone tilt posture as neutral center (0.0).
     */
    fun recalibrateZero() {
        zeroRollOffset = lastRawRoll
        zeroPitchOffset = lastRawPitch
        filteredRoll = 0f
        filteredPitch = 0f
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        runCatching {
            when (event.sensor.type) {
                Sensor.TYPE_GAME_ROTATION_VECTOR, Sensor.TYPE_ROTATION_VECTOR -> {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                    // Remap coordinate system for Landscape orientation (top of phone is left)
                    val remappedMatrix = FloatArray(9)
                    SensorManager.remapCoordinateSystem(
                        rotationMatrix,
                        SensorManager.AXIS_Y,
                        SensorManager.AXIS_MINUS_X,
                        remappedMatrix
                    )

                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(remappedMatrix, orientation)

                    val currentRawRoll = -orientation[2]
                    val currentRawPitch = -orientation[1]

                    lastRawRoll = currentRawRoll
                    lastRawPitch = currentRawPitch

                    // Apply zero-calibration offset
                    val uncalibratedRoll = (currentRawRoll - zeroRollOffset) * sensitivity * 0.85f
                    val uncalibratedPitch = (currentRawPitch - zeroPitchOffset) * sensitivity * 0.85f

                    val clampedRoll = uncalibratedRoll.coerceIn(-1f, 1f)
                    val clampedPitch = uncalibratedPitch.coerceIn(-1f, 1f)

                    // Low-pass filter (Exponential Moving Average for zero-jitter at 120Hz)
                    filteredRoll += lpfAlpha * (clampedRoll - filteredRoll)
                    filteredPitch += lpfAlpha * (clampedPitch - filteredPitch)

                    // Apply deadzone (~2-3 degrees) around neutral center
                    val finalRoll = if (abs(filteredRoll) < deadzone) 0f else filteredRoll
                    val finalPitch = if (abs(filteredPitch) < deadzone) 0f else filteredPitch

                    mutableState.value = MotionControlState(roll = finalRoll, pitch = finalPitch, timestampNanos = event.timestamp)
                }
                Sensor.TYPE_GYROSCOPE -> {
                    val rawRoll = (event.values[1] * sensitivity * 0.4f).coerceIn(-1f, 1f)
                    val rawPitch = (event.values[0] * sensitivity * 0.4f).coerceIn(-1f, 1f)

                    filteredRoll += lpfAlpha * (rawRoll - filteredRoll)
                    filteredPitch += lpfAlpha * (rawPitch - filteredPitch)

                    val roll = if (abs(filteredRoll) < deadzone) 0f else filteredRoll
                    val pitch = if (abs(filteredPitch) < deadzone) 0f else filteredPitch

                    mutableState.value = MotionControlState(roll = roll, pitch = pitch, timestampNanos = event.timestamp)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun close() {
        val m = manager ?: return
        if (!isListening) return
        runCatching {
            m.unregisterListener(this)
            isListening = false
            filteredRoll = 0f
            filteredPitch = 0f
        }
    }
}
