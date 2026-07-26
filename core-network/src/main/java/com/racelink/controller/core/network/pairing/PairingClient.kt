package com.racelink.controller.core.network.pairing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class PairingResult(
    val sessionKey: ByteArray,
    val hostAddress: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PairingResult
        return sessionKey.contentEquals(other.sessionKey) && hostAddress == other.hostAddress
    }

    override fun hashCode(): Int = 31 * sessionKey.contentHashCode() + hostAddress.hashCode()
}

object PairingClient {
    const val PAIRING_PORT = 45101
    private const val MAGIC = 0x41584953 // AXIS

    suspend fun pair(hostAddress: String, pinCode: String, timeoutMillis: Int = 10_000): PairingResult = withContext(Dispatchers.IO) {
        val random = SecureRandom()
        val clientNonce = ByteArray(16).also { random.nextBytes(it) }

        Socket().use { socket ->
            socket.connect(InetSocketAddress(hostAddress, PAIRING_PORT), timeoutMillis)
            socket.soTimeout = timeoutMillis
            val output = DataOutputStream(socket.getOutputStream())
            val input = DataInputStream(socket.getInputStream())

            // 1. Send Magic & Client Nonce
            output.writeInt(MAGIC)
            output.write(clientNonce)
            output.flush()

            // 2. Read Server Response Header (Magic + Salt 16B + IV 12B)
            val serverMagic = input.readInt()
            if (serverMagic != MAGIC) throw IllegalStateException("Invalid server magic: $serverMagic")

            val salt = ByteArray(16).also { input.readFully(it) }
            val iv = ByteArray(12).also { input.readFully(it) }

            // 3. Derive Key via PBKDF2-SHA256 (150,000 iterations)
            val derivedKey = deriveKey(pinCode, salt)

            // 4. Encrypt and Send Proof ("axis-pair-v1")
            val proofPlain = "axis-pair-v1".toByteArray(StandardCharsets.UTF_8)
            val encryptedProof = aesGcmEncrypt(derivedKey, iv, clientNonce, proofPlain)

            output.writeInt(encryptedProof.size)
            output.write(encryptedProof)
            output.flush()

            // 5. Read Encrypted Session Token
            val sessionLength = input.readInt()
            if (sessionLength !in 16..256) throw IllegalStateException("Invalid session payload length: $sessionLength")

            val encryptedSession = ByteArray(sessionLength).also { input.readFully(it) }
            val sessionKey = aesGcmDecrypt(derivedKey, iv, clientNonce, encryptedSession)

            PairingResult(sessionKey, hostAddress)
        }
    }

    private fun deriveKey(pinCode: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pinCode.toCharArray(), salt, 150_000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    fun aesGcmEncrypt(keyBytes: ByteArray, iv: ByteArray, aad: ByteArray, plainText: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key = SecretKeySpec(keyBytes, "AES")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        cipher.updateAAD(aad)
        return cipher.doFinal(plainText)
    }

    fun aesGcmDecrypt(keyBytes: ByteArray, iv: ByteArray, aad: ByteArray, cipherText: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key = SecretKeySpec(keyBytes, "AES")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        cipher.updateAAD(aad)
        return cipher.doFinal(cipherText)
    }
}
