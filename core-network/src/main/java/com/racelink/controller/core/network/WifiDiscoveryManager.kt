package com.racelink.controller.core.network

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.util.concurrent.ThreadLocalRandom

class WifiDiscoveryManager(context: Context) {
    private val wifiManager = context.applicationContext.getSystemService(WifiManager::class.java)

    suspend fun discover(timeoutMillis: Int = 3_000): List<DiscoveredDesktop> = withContext(Dispatchers.IO) {
        require(timeoutMillis in 500..10_000)
        val lock = wifiManager?.createMulticastLock("racelink-discovery")?.apply { setReferenceCounted(false); acquire() }
        try {
            val nonce = ThreadLocalRandom.current().nextLong()
            val sentAt = System.nanoTime()
            DatagramSocket().use { socket ->
                socket.broadcast = true
                socket.soTimeout = 250
                val request = packet(PacketType.DISCOVERY_REQUEST, DiscoveryProtocol.request(nonce))
                discoveryTargets().forEach { target -> socket.send(DatagramPacket(request, request.size, target, DiscoveryProtocol.DISCOVERY_PORT)) }
                val deadline = System.nanoTime() + timeoutMillis * 1_000_000L
                val results = linkedMapOf<String, DiscoveredDesktop>()
                val receiveBuffer = ByteArray(RaceLinkPacketCodec.MAX_PACKET_BYTES)
                while (System.nanoTime() < deadline) {
                    try {
                        val incoming = DatagramPacket(receiveBuffer, receiveBuffer.size)
                        socket.receive(incoming)
                        val parsed = RaceLinkPacketCodec.decode(ByteBuffer.wrap(incoming.data, 0, incoming.length)) ?: continue
                        if (parsed.type != PacketType.DISCOVERY_RESPONSE) continue
                        val response = DiscoveryProtocol.decodeResponse(parsed.payload) ?: continue
                        if (response.nonce != nonce || response.controllerVersion != 1) continue
                        val host = incoming.address.hostAddress ?: continue
                        results[host] = DiscoveredDesktop(response.hostName, host, response.pairingPort, response.controllerVersion, (System.nanoTime() - sentAt) / 1_000_000L)
                    } catch (_: SocketTimeoutException) { /* bounded receive window */ }
                }
                results.values.toList()
            }
        } finally { lock?.release() }
    }

    suspend fun probe(address: InetAddress, timeoutMillis: Int = 1_500): DiscoveredDesktop? = withContext(Dispatchers.IO) {
        val nonce = ThreadLocalRandom.current().nextLong()
        val sentAt = System.nanoTime()
        DatagramSocket().use { socket ->
            socket.soTimeout = timeoutMillis
            val request = packet(PacketType.DISCOVERY_REQUEST, DiscoveryProtocol.request(nonce))
            socket.send(DatagramPacket(request, request.size, address, DiscoveryProtocol.DISCOVERY_PORT))
            val incoming = DatagramPacket(ByteArray(RaceLinkPacketCodec.MAX_PACKET_BYTES), RaceLinkPacketCodec.MAX_PACKET_BYTES)
            try { socket.receive(incoming) } catch (_: SocketTimeoutException) { return@withContext null }
            val parsed = RaceLinkPacketCodec.decode(ByteBuffer.wrap(incoming.data, 0, incoming.length)) ?: return@withContext null
            val response = DiscoveryProtocol.decodeResponse(parsed.payload) ?: return@withContext null
            if (parsed.type != PacketType.DISCOVERY_RESPONSE || response.nonce != nonce || response.controllerVersion != 1) return@withContext null
            val host = incoming.address.hostAddress ?: return@withContext null
            DiscoveredDesktop(response.hostName, host, response.pairingPort, response.controllerVersion, (System.nanoTime() - sentAt) / 1_000_000L)
        }
    }

    private fun packet(type: PacketType, payload: ByteArray): ByteArray {
        val buffer = ByteBuffer.allocate(RaceLinkPacketCodec.MAX_PACKET_BYTES)
        RaceLinkPacketCodec.encode(RaceLinkPacket(type, 0u, System.nanoTime(), payload), buffer)
        return buffer.array().copyOf(buffer.position())
    }

    private fun discoveryTargets(): Set<InetAddress> = buildSet {
        add(InetAddress.getByName("255.255.255.255"))
        NetworkInterface.getNetworkInterfaces().asSequence().filter { it.isUp && !it.isLoopback }.flatMap { it.interfaceAddresses.asSequence() }
            .mapNotNullTo(this) { it.broadcast }
    }
}
