package com.racelink.controller.core.network

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

data class DiscoveredDesktop(
    val hostName: String,
    val address: String,
    val pairingPort: Int,
    val controllerVersion: Int,
    val roundTripMillis: Long,
)

object DiscoveryProtocol {
    const val DISCOVERY_PORT = 45_100
    private const val MAX_NAME_BYTES = 64

    fun request(nonce: Long): ByteArray = ByteBuffer.allocate(8).putLong(nonce).array()

    fun response(hostName: String, pairingPort: Int, controllerVersion: Int, nonce: Long): ByteArray {
        val name = hostName.toByteArray(StandardCharsets.UTF_8)
        require(name.size in 1..MAX_NAME_BYTES)
        return ByteBuffer.allocate(8 + 2 + 2 + 1 + name.size)
            .putLong(nonce).putShort(pairingPort.toShort()).putShort(controllerVersion.toShort())
            .put(name.size.toByte()).put(name).array()
    }

    fun decodeResponse(payload: ByteArray): DiscoveryResponse? = runCatching {
        val buffer = ByteBuffer.wrap(payload)
        val nonce = buffer.long
        val port = buffer.short.toInt() and 0xffff
        val version = buffer.short.toInt() and 0xffff
        val nameLength = buffer.get().toInt() and 0xff
        if (nameLength !in 1..MAX_NAME_BYTES || buffer.remaining() != nameLength) return null
        DiscoveryResponse(nonce, port, version, String(ByteArray(nameLength).also(buffer::get), StandardCharsets.UTF_8))
    }.getOrNull()
}

data class DiscoveryResponse(val nonce: Long, val pairingPort: Int, val controllerVersion: Int, val hostName: String)
