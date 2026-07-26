package com.racelink.controller.core.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

data class MotionControlState(val steering: Float = 0f, val timestampNanos: Long = 0L)

/** Sensor motion producer for Gyro tilt steering (Sony DualSense style motion control). */
class MotionSteeringProducer(context: Context) : SensorEventListener, AutoCloseable {
    private val manager: SensorManager? = runCatching { context.getSystemService(SensorManager::class.java) }.getOrNull()
    private val gyro: Sensor? = manager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val accel: Sensor? = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val mutableState = MutableStateFlow(MotionControlState())
    val state = mutableState.asStateFlow()

    private var filteredRadians = 0f
    private var lastTimestamp = 0L
    private var isListening = false

    fun start() {
        val m = manager ?: return
        if (isListening) return
        runCatching {
            if (gyro != null) {
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
                Sensor.TYPE_GYROSCOPE -> {
                    val dt = if (lastTimestamp == 0L) 0f else ((event.timestamp - lastTimestamp) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    lastTimestamp = event.timestamp
                    filteredRadians = (filteredRadians + event.values[1] * dt).coerceIn(-1.57f, 1.57f)
                    val raw = filteredRadians / 1.2f
                    val steering = if (abs(raw) < 0.03f) 0f else raw.coerceIn(-1f, 1f)
                    mutableState.value = MotionControlState(steering, event.timestamp)
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    val tiltX = (-event.values[0] / 8f).coerceIn(-1f, 1f)
                    val steering = if (abs(tiltX) < 0.05f) 0f else tiltX
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
            lastTimestamp = 0L
            filteredRadians = 0f
        }
    }
}
