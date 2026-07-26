package com.racelink.controller.core.network

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.util.concurrent.ThreadLocalRandom

class WifiDiscoveryManager(context: Context) {
    private val wifiManager = context.applicationContext.getSystemService(WifiManager::class.java)

    suspend fun discover(timeoutMillis: Int = 3_500): List<DiscoveredDesktop> = withContext(Dispatchers.IO) {
        require(timeoutMillis in 500..10_000)
        val lock = wifiManager?.createMulticastLock("racelink-discovery")?.apply { setReferenceCounted(false); acquire() }
        try {
            val nonce = ThreadLocalRandom.current().nextLong()
            val sentAt = System.nanoTime()
            DatagramSocket().use { socket ->
                socket.broadcast = true
                socket.soTimeout = 300
                val request = packet(PacketType.DISCOVERY_REQUEST, DiscoveryProtocol.request(nonce))
                discoveryTargets().forEach { target ->
                    runCatching { socket.send(DatagramPacket(request, request.size, target, DiscoveryProtocol.DISCOVERY_PORT)) }
                }
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
                        if (response.nonce != nonce) continue
                        val host = incoming.address.hostAddress ?: continue
                        results[host] = DiscoveredDesktop(response.hostName, host, response.pairingPort, response.controllerVersion, (System.nanoTime() - sentAt) / 1_000_000L)
                    } catch (_: SocketTimeoutException) { /* bounded receive window */ }
                }
                results.values.toList()
            }
        } finally { lock?.release() }
    }

    suspend fun probe(address: InetAddress, timeoutMillis: Int = 2_000): DiscoveredDesktop? = withContext(Dispatchers.IO) {
        val nonce = ThreadLocalRandom.current().nextLong()
        val sentAt = System.nanoTime()

        // 1. Try UDP Probe
        val udpResult = runCatching {
            DatagramSocket().use { socket ->
                socket.soTimeout = timeoutMillis
                val request = packet(PacketType.DISCOVERY_REQUEST, DiscoveryProtocol.request(nonce))
                socket.send(DatagramPacket(request, request.size, address, DiscoveryProtocol.DISCOVERY_PORT))
                val incoming = DatagramPacket(ByteArray(RaceLinkPacketCodec.MAX_PACKET_BYTES), RaceLinkPacketCodec.MAX_PACKET_BYTES)
                socket.receive(incoming)
                val parsed = RaceLinkPacketCodec.decode(ByteBuffer.wrap(incoming.data, 0, incoming.length)) ?: return@use null
                val response = DiscoveryProtocol.decodeResponse(parsed.payload) ?: return@use null
                if (parsed.type != PacketType.DISCOVERY_RESPONSE || response.nonce != nonce) return@use null
                val host = incoming.address.hostAddress ?: return@use null
                DiscoveredDesktop(response.hostName, host, response.pairingPort, response.controllerVersion, (System.nanoTime() - sentAt) / 1_000_000L)
            }
        }.getOrNull()

        if (udpResult != null) return@withContext udpResult

        // 2. Fallback: Direct TCP Probe on Pairing Port 45101 (bypasses UDP router filters)
        runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(address, 45101), 1_500)
                val hostStr = address.hostAddress ?: ""
                DiscoveredDesktop("Axis PC ($hostStr)", hostStr, 45101, 1, (System.nanoTime() - sentAt) / 1_000_000L)
            }
        }.getOrNull()
    }

    private fun packet(type: PacketType, payload: ByteArray): ByteArray {
        val buffer = ByteBuffer.allocate(RaceLinkPacketCodec.MAX_PACKET_BYTES)
        RaceLinkPacketCodec.encode(RaceLinkPacket(type, 0u, System.nanoTime(), payload), buffer)
        return buffer.array().copyOf(buffer.position())
    }

    private fun discoveryTargets(): Set<InetAddress> = buildSet {
        add(InetAddress.getByName("255.255.255.255"))
        runCatching {
            NetworkInterface.getNetworkInterfaces().asSequence().filter { it.isUp && !it.isLoopback }.flatMap { it.interfaceAddresses.asSequence() }
                .mapNotNullTo(this) { it.broadcast }
        }
    }
}
