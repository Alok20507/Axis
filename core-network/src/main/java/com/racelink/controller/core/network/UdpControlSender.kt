package com.racelink.controller.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger

/** Latest-state UDP sender. Callers schedule this at 120 Hz; no control frame is queued. */
class UdpControlSender(private val address: InetAddress, private val port: Int = 45_102) : AutoCloseable {
    private val socket = DatagramSocket()
    private val sequence = AtomicInteger()
    suspend fun send(steering: Float, throttle: Float, brake: Float, handbrake: Float) = withContext(Dispatchers.IO) {
        val buffer = ByteBuffer.allocate(ControllerPacketCodec.CONTROL_PACKET_BYTES)
        ControllerPacketCodec.encodeControl(WireControlFrame(sequence.getAndIncrement(), System.nanoTime(), steering, throttle, brake, handbrake), buffer)
        socket.send(DatagramPacket(buffer.array(), buffer.position(), address, port))
    }
    override fun close() = socket.close()
}
