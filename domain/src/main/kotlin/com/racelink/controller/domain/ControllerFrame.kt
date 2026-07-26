package com.racelink.controller.domain

/** A normalized controller snapshot. Values are in [-1, 1] unless stated otherwise. */
data class ControllerFrame(
    val sequence: UInt,
    val monotonicTimestampNanos: Long,
    val steering: Float,
    val throttle: Float,
    val brake: Float,
    val handbrake: Float,
    val buttons: UInt,
)

interface ControllerFrameSink {
    suspend fun send(frame: ControllerFrame)
}
