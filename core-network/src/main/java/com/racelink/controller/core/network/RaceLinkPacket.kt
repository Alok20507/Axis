package com.racelink.controller.core.network

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32

enum class PacketType(val code: UByte) {
    DISCOVERY_REQUEST(1u),
    DISCOVERY_RESPONSE(2u),
    PAIR_HELLO(3u),
    PAIR_CHALLENGE(4u),
    PAIR_COMPLETE(5u),
    HEARTBEAT(10u),
    CONTROL(11u),
    TELEMETRY(12u);

    companion object { fun from(code: UByte) = entries.firstOrNull { it.code == code } }
}

data class RaceLinkPacket(
    val type: PacketType,
    val sequence: UInt,
    val timestampNanos: Long,
    val payload: ByteArray,
)

/** Shared Android/Windows wire format. Header is fixed at 24 bytes and CRC covers header+payload. */
object RaceLinkPacketCodec {
    private const val MAGIC = 0x52434C4B // RCLK
    private const val VERSION: Byte = 1
    private const val HEADER_BYTES = 20
    private const val CRC_BYTES = 4
    const val MAX_PACKET_BYTES = 1_200

    fun encode(packet: RaceLinkPacket, output: ByteBuffer) {
        require(packet.payload.size <= MAX_PACKET_BYTES - HEADER_BYTES - CRC_BYTES)
        val start = output.position()
        require(output.remaining() >= HEADER_BYTES + packet.payload.size + CRC_BYTES)
        output.order(ByteOrder.BIG_ENDIAN)
        output.putInt(MAGIC)
        output.put(VERSION)
        output.put(packet.type.code.toByte())
        output.putShort(packet.payload.size.toShort())
        output.putInt(packet.sequence.toInt())
        output.putLong(packet.timestampNanos)
        output.put(packet.payload)
        val checksum = CRC32().also { crc ->
            val duplicate = output.duplicate().apply { position(start); limit(output.position()) }
            while (duplicate.hasRemaining()) crc.update(duplicate.get().toInt())
        }.value.toInt()
        output.putInt(checksum)
    }

    fun decode(input: ByteBuffer): RaceLinkPacket? {
        input.order(ByteOrder.BIG_ENDIAN)
        if (input.remaining() < HEADER_BYTES + CRC_BYTES) return null
        val frameStart = input.position()
        if (input.int != MAGIC || input.get() != VERSION) return null
        val type = PacketType.from(input.get().toUByte()) ?: return null
        val payloadLength = input.short.toInt() and 0xffff
        if (payloadLength > MAX_PACKET_BYTES - HEADER_BYTES - CRC_BYTES || input.remaining() != payloadLength + CRC_BYTES) return null
        val sequence = input.int.toUInt()
        val timestamp = input.long
        val payload = ByteArray(payloadLength)
        input.get(payload)
        val receivedCrc = input.int
        val crc = CRC32()
        val checked = input.duplicate().apply { position(frameStart); limit(frameStart + HEADER_BYTES + payloadLength) }
        while (checked.hasRemaining()) crc.update(checked.get().toInt())
        if (crc.value.toInt() != receivedCrc) return null
        return RaceLinkPacket(type, sequence, timestamp, payload)
    }
}
