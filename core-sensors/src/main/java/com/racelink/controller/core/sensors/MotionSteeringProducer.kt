package com.racelink.controller.core.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.atan2

data class MotionControlState(val steering: Float = 0f, val timestampNanos: Long = 0L)

/** High-precision sensor motion producer for Gyro tilt steering (Sony DualSense style motion control). */
class MotionSteeringProducer(context: Context) : SensorEventListener, AutoCloseable {
    private val manager: SensorManager? = runCatching { context.getSystemService(SensorManager::class.java) }.getOrNull()
    private val gyro: Sensor? = manager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val accel: Sensor? = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val rotVector: Sensor? = manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val mutableState = MutableStateFlow(MotionControlState())
    val state = mutableState.asStateFlow()

    var sensitivity: Float = 2.0f // Default high sensitivity multiplier so small 20° tilt turns 100%
    var deadzone: Float = 0.03f
    private var isListening = false

    fun start() {
        val m = manager ?: return
        if (isListening) return
        runCatching {
            if (rotVector != null) {
                m.registerListener(this, rotVector, SensorManager.SENSOR_DELAY_GAME)
            } else if (accel != null) {
                m.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME)
            } else if (gyro != null) {
                m.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME)
            }
            isListening = true
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        runCatching {
            when (event.sensor.type) {
                Sensor.TYPE_ROTATION_VECTOR -> {
                    // Convert rotation vector to orientation angles (roll for steering)
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    
                    // orientation[2] is roll in radians (landscape steering angle)
                    val rollRadians = orientation[2]
                    val rawSteering = (-rollRadians * sensitivity).coerceIn(-1f, 1f)
                    val steering = if (abs(rawSteering) < deadzone) 0f else rawSteering
                    mutableState.value = MotionControlState(steering, event.timestamp)
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    // Fallback using accelerometer roll angle calculation
                    val ax = event.values[0]
                    val ay = event.values[1]
                    // Roll angle in landscape
                    val roll = atan2(ay.toDouble(), ax.toDouble()).toFloat()
                    val rawSteering = (roll * sensitivity * 0.8f).coerceIn(-1f, 1f)
                    val steering = if (abs(rawSteering) < deadzone) 0f else rawSteering
                    mutableState.value = MotionControlState(steering, event.timestamp)
                }
                Sensor.TYPE_GYROSCOPE -> {
                    val rawSteering = (event.values[1] * sensitivity * 0.5f).coerceIn(-1f, 1f)
                    val steering = if (abs(rawSteering) < deadzone) 0f else rawSteering
                    mutableState.value = MotionControlState(steering, event.timestamp)
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
