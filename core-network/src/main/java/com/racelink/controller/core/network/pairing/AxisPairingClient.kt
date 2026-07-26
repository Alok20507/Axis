package com.racelink.controller.core.network.pairing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

/** One-time PIN pairing. The returned 256-bit session key is never written by this transport. */
class AxisPairingClient {
    suspend fun pair(host: String, port: Int, code: CharArray): ByteArray = withContext(Dispatchers.IO) {
        require(code.size == 6 && code.all(Char::isDigit)) { "Enter the six-digit code shown in Axis Desktop." }
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), 5_000)
            val input = DataInputStream(socket.getInputStream())
            val output = DataOutputStream(socket.getOutputStream())
            val clientNonce = ByteArray(16).also(SecureRandom()::nextBytes)
            output.writeInt(MAGIC); output.write(clientNonce); output.flush()
            check(input.readInt() == MAGIC) { "Axis Desktop rejected the pairing request." }
            val salt = ByteArray(16).also(input::readFully)
            val iv = ByteArray(12).also(input::readFully)
            val key = derive(code, salt)
            val proof = encrypt(key, iv, clientNonce, "axis-pair-v1".encodeToByteArray())
            output.writeInt(proof.size); output.write(proof); output.flush()
            val resultLength = input.readInt()
            require(resultLength in 17..128) { "Invalid pairing response." }
            val encryptedSession = ByteArray(resultLength).also(input::readFully)
            decrypt(key, iv, clientNonce, encryptedSession)
        }.also { code.fill('\u0000') }
    }

    private fun derive(code: CharArray, salt: ByteArray): ByteArray = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        .generateSecret(PBEKeySpec(code, salt, 150_000, 256)).encoded
    private fun encrypt(key: ByteArray, iv: ByteArray, aad: ByteArray, plain: ByteArray): ByteArray = Cipher.getInstance("AES/GCM/NoPadding").run { init(Cipher.ENCRYPT_MODE, javax.crypto.spec.SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv)); updateAAD(aad); doFinal(plain) }
    private fun decrypt(key: ByteArray, iv: ByteArray, aad: ByteArray, cipherText: ByteArray): ByteArray = Cipher.getInstance("AES/GCM/NoPadding").run { init(Cipher.DECRYPT_MODE, javax.crypto.spec.SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv)); updateAAD(aad); doFinal(cipherText) }
    private companion object { const val MAGIC = 0x41584953 }
}
