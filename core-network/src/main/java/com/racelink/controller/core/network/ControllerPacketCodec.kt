package com.racelink.controller.core.network

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Fixed-width network framing for the real-time control lane. This deliberately has no JSON,
 * reflection, or per-field object graph. Authentication and encryption wrap this payload later.
 */
object ControllerPacketCodec {
    const val MAGIC: Int = 0x524C4E4B // RLNK
    const val VERSION: Short = 1
    const val CONTROL_PACKET_BYTES: Int = 36

    fun encodeControl(frame: WireControlFrame, destination: ByteBuffer) {
        require(destination.remaining() >= CONTROL_PACKET_BYTES) { "Packet buffer is too small" }
        destination.order(ByteOrder.BIG_ENDIAN)
        destination.putInt(MAGIC)
        destination.putShort(VERSION)
        destination.putShort(0)
        destination.putInt(frame.sequence)
        destination.putLong(frame.timestampNanos)
        destination.putFloat(frame.steering)
        destination.putFloat(frame.throttle)
        destination.putFloat(frame.brake)
        destination.putFloat(frame.handbrake)
    }

    fun decodeControl(source: ByteBuffer): WireControlFrame? {
        if (source.remaining() != CONTROL_PACKET_BYTES) return null
        source.order(ByteOrder.BIG_ENDIAN)
        if (source.int != MAGIC || source.short != VERSION) return null
        source.short // reserved
        val sequence = source.int
        val timestampNanos = source.long
        val steering = source.float
        val throttle = source.float
        val brake = source.float
        val handbrake = source.float
        if (!steering.isFinite() || !throttle.isFinite() || !brake.isFinite() || !handbrake.isFinite()) return null
        return WireControlFrame(sequence, timestampNanos, steering, throttle, brake, handbrake)
    }
}

data class WireControlFrame(
    val sequence: Int,
    val timestampNanos: Long,
    val steering: Float,
    val throttle: Float,
    val brake: Float,
    val handbrake: Float,
)
