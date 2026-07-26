package com.racelink.controller.core.network

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Fixed-width network framing for the real-time control lane.
 * Encodes 38-byte binary control frames with optional AES-256-GCM encryption.
 */
object ControllerPacketCodec {
    const val MAGIC: Int = 0x524C4E4B // RLNK
    const val VERSION: Short = 1
    const val CONTROL_PACKET_BYTES: Int = 38
    private val random = SecureRandom()

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
        destination.putShort(frame.buttons)
    }

    fun decodeControl(source: ByteBuffer): WireControlFrame? {
        if (source.remaining() < CONTROL_PACKET_BYTES) return null
        source.order(ByteOrder.BIG_ENDIAN)
        if (source.int != MAGIC || source.short != VERSION) return null
        source.short // reserved
        val sequence = source.int
        val timestampNanos = source.long
        val steering = source.float
        val throttle = source.float
        val brake = source.float
        val handbrake = source.float
        val buttons = source.short
        if (!steering.isFinite() || !throttle.isFinite() || !brake.isFinite() || !handbrake.isFinite()) return null
        return WireControlFrame(sequence, timestampNanos, steering, throttle, brake, handbrake, buttons)
    }

    fun encodeEncryptedControl(frame: WireControlFrame, sessionKey: ByteArray, destination: ByteBuffer) {
        val rawBuffer = ByteBuffer.allocate(CONTROL_PACKET_BYTES)
        encodeControl(frame, rawBuffer)
        val rawBytes = rawBuffer.array()

        val iv = ByteArray(12).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(sessionKey, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(128, iv))
        val cipherText = cipher.doFinal(rawBytes)

        destination.order(ByteOrder.BIG_ENDIAN)
        destination.put(iv)
        destination.putInt(cipherText.size)
        destination.put(cipherText)
    }

    fun decodeEncryptedControl(source: ByteBuffer, sessionKey: ByteArray): WireControlFrame? {
        if (source.remaining() < 12 + 4 + CONTROL_PACKET_BYTES) return null
        source.order(ByteOrder.BIG_ENDIAN)
        val iv = ByteArray(12).also { source.get(it) }
        val cipherLength = source.int
        if (cipherLength !in CONTROL_PACKET_BYTES..(CONTROL_PACKET_BYTES + 32) || source.remaining() < cipherLength) return null

        val cipherText = ByteArray(cipherLength).also { source.get(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(sessionKey, "AES")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(128, iv))
        val plainText = cipher.doFinal(cipherText)

        return decodeControl(ByteBuffer.wrap(plainText))
    }
}

data class WireControlFrame(
    val sequence: Int,
    val timestampNanos: Long,
    val steering: Float,
    val throttle: Float,
    val brake: Float,
    val handbrake: Float,
    val buttons: Short = 0,
)
