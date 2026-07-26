package com.racelink.controller.core.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.pow

data class MotionControlState(val steering: Float = 0f, val timestampNanos: Long = 0L)

/** Allocation-free sensor callback; UI observes a sampled state, networking reads the latest value. */
class MotionSteeringProducer(context: Context) : SensorEventListener, AutoCloseable {
    private val manager = context.getSystemService(SensorManager::class.java)
    private val gyro = manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val mutableState = MutableStateFlow(MotionControlState())
    val state = mutableState.asStateFlow()
    private var filteredRadians = 0f
    private var lastTimestamp = 0L

    fun start() { gyro?.let { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST) } }
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_GYROSCOPE) return
        val dt = if (lastTimestamp == 0L) 0f else ((event.timestamp - lastTimestamp) / 1_000_000_000f).coerceIn(0f, .05f)
        lastTimestamp = event.timestamp
        filteredRadians = (filteredRadians + event.values[1] * dt).coerceIn(-3.14159f, 3.14159f)
        val raw = filteredRadians / 1.5708f
        val deadZone = .025f
        val adjusted = if (abs(raw) < deadZone) 0f else ((abs(raw) - deadZone) / (1f - deadZone)).coerceIn(0f, 1f).pow(1.35f) * if (raw < 0) -1 else 1
        mutableState.value = MotionControlState(adjusted, event.timestamp)
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    override fun close() { manager.unregisterListener(this); lastTimestamp = 0L; filteredRadians = 0f }
}
