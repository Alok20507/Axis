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

/** High-precision sensor motion producer with landscape remapped coordinates for Sony DualSense tilt & aiming. */
class MotionSteeringProducer(context: Context) : SensorEventListener, AutoCloseable {
    private val manager: SensorManager? = runCatching { context.getSystemService(SensorManager::class.java) }.getOrNull()
    private val rotVector: Sensor? = manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val gyro: Sensor? = manager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val accel: Sensor? = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val mutableState = MutableStateFlow(MotionControlState())
    val state = mutableState.asStateFlow()

    var sensitivity: Float = 2.0f // Default high sensitivity multiplier
    var deadzone: Float = 0.02f
    private var isListening = false

    fun start() {
        val m = manager ?: return
        if (isListening) return
        runCatching {
            if (rotVector != null) {
                m.registerListener(this, rotVector, SensorManager.SENSOR_DELAY_GAME)
            } else if (gyro != null) {
                m.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME)
            } else if (accel != null) {
                m.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME)
            }
            isListening = true
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        runCatching {
            when (event.sensor.type) {
                Sensor.TYPE_ROTATION_VECTOR -> {
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

                    // orientation[2] = Roll angle in landscape (Steering / Yaw)
                    // orientation[1] = Pitch angle in landscape (Camera Pitch)
                    val rawRoll = (-orientation[2] * sensitivity * 0.8f).coerceIn(-1f, 1f)
                    val rawPitch = (-orientation[1] * sensitivity * 0.8f).coerceIn(-1f, 1f)

                    val roll = if (abs(rawRoll) < deadzone) 0f else rawRoll
                    val pitch = if (abs(rawPitch) < deadzone) 0f else rawPitch

                    mutableState.value = MotionControlState(roll = roll, pitch = pitch, timestampNanos = event.timestamp)
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    // Fallback landscape tilt calculation: ax is vertical tilt in landscape
                    val ax = event.values[0]
                    val ay = event.values[1]
                    val rawRoll = (ay / 7f * sensitivity).coerceIn(-1f, 1f)
                    val rawPitch = (-ax / 7f * sensitivity).coerceIn(-1f, 1f)

                    val roll = if (abs(rawRoll) < deadzone) 0f else rawRoll
                    val pitch = if (abs(rawPitch) < deadzone) 0f else rawPitch

                    mutableState.value = MotionControlState(roll = roll, pitch = pitch, timestampNanos = event.timestamp)
                }
                Sensor.TYPE_GYROSCOPE -> {
                    val rawRoll = (event.values[1] * sensitivity * 0.4f).coerceIn(-1f, 1f)
                    val rawPitch = (event.values[0] * sensitivity * 0.4f).coerceIn(-1f, 1f)

                    val roll = if (abs(rawRoll) < deadzone) 0f else rawRoll
                    val pitch = if (abs(rawPitch) < deadzone) 0f else rawPitch

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
        }
    }
}
