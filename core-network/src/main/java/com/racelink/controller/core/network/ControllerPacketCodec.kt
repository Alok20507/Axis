package com.racelink.controller.core.network

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Fixed-width network framing for the real-time control lane.
 * Encodes 46-byte binary control frames supporting dual analog sticks, triggers, and Xbox buttons.
 */
object ControllerPacketCodec {
    const val MAGIC: Int = 0x524C4E4B // RLNK
    const val VERSION: Short = 1
    const val CONTROL_PACKET_BYTES: Int = 46
    private val random = SecureRandom()

    fun encodeControl(frame: WireControlFrame, destination: ByteBuffer) {
        require(destination.remaining() >= CONTROL_PACKET_BYTES) { "Packet buffer is too small" }
        destination.order(ByteOrder.BIG_ENDIAN)
        destination.putInt(MAGIC)
        destination.putShort(VERSION)
        destination.putShort(0)
        destination.putInt(frame.sequence)
        destination.putLong(frame.timestampNanos)
        destination.putFloat(frame.leftStickX)
        destination.putFloat(frame.leftStickY)
        destination.putFloat(frame.rightStickX)
        destination.putFloat(frame.rightStickY)
        destination.putFloat(frame.throttle)
        destination.putFloat(frame.brake)
        destination.putFloat(frame.handbrake)
        destination.putShort(frame.buttons)
    }

    fun decodeControl(source: ByteBuffer): WireControlFrame? {
        if (source.remaining() < 36) return null
        source.order(ByteOrder.BIG_ENDIAN)
        if (source.int != MAGIC || source.short != VERSION) return null
        source.short // reserved
        val sequence = source.int
        val timestampNanos = source.long

        if (source.remaining() >= 26) {
            val lsX = source.float
            val lsY = source.float
            val rsX = source.float
            val rsY = source.float
            val throttle = source.float
            val brake = source.float
            val handbrake = source.float
            val buttons = if (source.remaining() >= 2) source.short else 0
            return WireControlFrame(sequence, timestampNanos, lsX, lsY, rsX, rsY, throttle, brake, handbrake, buttons)
        } else {
            val steering = source.float
            val throttle = source.float
            val brake = source.float
            val handbrake = if (source.remaining() >= 4) source.float else 0f
            val buttons = if (source.remaining() >= 2) source.short else 0
            return WireControlFrame(sequence, timestampNanos, steering, 0f, 0f, 0f, throttle, brake, handbrake, buttons)
        }
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
        if (source.remaining() < 12 + 4 + 36) return null
        source.order(ByteOrder.BIG_ENDIAN)
        val iv = ByteArray(12).also { source.get(it) }
        val cipherLength = source.int
        if (cipherLength !in 36..128 || source.remaining() < cipherLength) return null

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
    val leftStickX: Float = 0f,
    val leftStickY: Float = 0f,
    val rightStickX: Float = 0f,
    val rightStickY: Float = 0f,
    val throttle: Float = 0f,
    val brake: Float = 0f,
    val handbrake: Float = 0f,
    val buttons: Short = 0,
)
