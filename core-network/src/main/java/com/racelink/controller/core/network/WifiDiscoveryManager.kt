package com.racelink.controller.core.network

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

class WifiDiscoveryManager(private val context: Context) {
    private val wifiManager = context.applicationContext.getSystemService(WifiManager::class.java)

    suspend fun discover(timeoutMillis: Int = 2_500): List<DiscoveredDesktop> = withContext(Dispatchers.IO) {
        val lock = wifiManager?.createMulticastLock("racelink-discovery")?.apply { setReferenceCounted(false); acquire() }
        try {
            val nonce = ThreadLocalRandom.current().nextLong()
            val sentAt = System.nanoTime()
            val targets = discoveryTargets()

            // 1. Concurrent UDP Broadcast Scan
            val udpTask = async {
                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    socket.soTimeout = 400
                    val request = packet(PacketType.DISCOVERY_REQUEST, DiscoveryProtocol.request(nonce))
                    targets.forEach { target ->
                        runCatching { socket.send(DatagramPacket(request, request.size, target, DiscoveryProtocol.DISCOVERY_PORT)) }
                    }
                    val deadline = System.nanoTime() + 1_500 * 1_000_000L
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
            }

            // 2. Concurrent Parallel TCP Subnet Sweep (runs simultaneously with UDP)
            val tcpTask = async {
                val activeIps = getActiveLocalIps()
                val tcpCandidates = activeIps.flatMap { ip ->
                    val prefix = ip.substringBeforeLast(".")
                    (1..254).map { "$prefix.$it" }
                }.distinct()

                tcpCandidates.chunked(32).flatMap { chunk ->
                    chunk.map { candidate ->
                        async {
                            runCatching {
                                Socket().use { socket ->
                                    socket.connect(InetSocketAddress(candidate, 45101), 300)
                                    DiscoveredDesktop("Axis PC ($candidate)", candidate, 45101, 1, (System.nanoTime() - sentAt) / 1_000_000L)
                                }
                            }.getOrNull()
                        }
                    }.awaitAll().filterNotNull()
                }
            }

            val udpResults = udpTask.await()
            if (udpResults.isNotEmpty()) return@withContext udpResults

            val tcpResults = tcpTask.await()
            tcpResults
        } finally { lock?.release() }
    }

    suspend fun probe(address: InetAddress, timeoutMillis: Int = 1_500): DiscoveredDesktop? = withContext(Dispatchers.IO) {
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

        // 2. Fallback: Direct TCP Probe on Pairing Port 45101
        runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(address, 45101), 1_200)
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

    private fun getActiveLocalIps(): List<String> = buildList {
        runCatching {
            NetworkInterface.getNetworkInterfaces().asSequence().filter { it.isUp && !it.isLoopback }.flatMap { it.interfaceAddresses.asSequence() }
                .mapNotNullTo(this) { it.address.hostAddress?.takeIf { addr -> !addr.contains(":") && addr != "127.0.0.1" } }
        }
    }

    private fun discoveryTargets(): Set<InetAddress> = buildSet {
        add(InetAddress.getByName("255.255.255.255"))
        val ips = getActiveLocalIps()
        for (ip in ips) {
            val prefix = ip.substringBeforeLast(".")
            runCatching { add(InetAddress.getByName("$prefix.255")) }
        }
        runCatching {
            NetworkInterface.getNetworkInterfaces().asSequence().filter { it.isUp && !it.isLoopback }.flatMap { it.interfaceAddresses.asSequence() }
                .mapNotNullTo(this) { it.broadcast }
        }
    }
}
