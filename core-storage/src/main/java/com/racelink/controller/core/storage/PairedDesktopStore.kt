package com.racelink.controller.core.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import android.util.Base64

private val Context.dataStore by preferencesDataStore(name = "axis_paired_desktop")

data class SavedDesktopSession(
    val hostName: String,
    val hostAddress: String,
    val sessionKey: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SavedDesktopSession
        return hostName == other.hostName && hostAddress == other.hostAddress && sessionKey.contentEquals(other.sessionKey)
    }

    override fun hashCode(): Int = 31 * (31 * hostName.hashCode() + hostAddress.hashCode()) + sessionKey.contentHashCode()
}

class PairedDesktopStore(private val context: Context) {
    companion object {
        private val KEY_HOST_NAME = stringPreferencesKey("host_name")
        private val KEY_HOST_ADDRESS = stringPreferencesKey("host_address")
        private val KEY_SESSION_KEY_B64 = stringPreferencesKey("session_key_b64")
    }

    val savedSession: Flow<SavedDesktopSession?> = context.dataStore.data.map { prefs ->
        val name = prefs[KEY_HOST_NAME] ?: return@map null
        val addr = prefs[KEY_HOST_ADDRESS] ?: return@map null
        val keyB64 = prefs[KEY_SESSION_KEY_B64] ?: return@map null
        try {
            val keyBytes = Base64.decode(keyB64, Base64.NO_WRAP)
            SavedDesktopSession(name, addr, keyBytes)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveSession(hostName: String, hostAddress: String, sessionKey: ByteArray) {
        val keyB64 = Base64.encodeToString(sessionKey, Base64.NO_WRAP)
        context.dataStore.edit { prefs ->
            prefs[KEY_HOST_NAME] = hostName
            prefs[KEY_HOST_ADDRESS] = hostAddress
            prefs[KEY_SESSION_KEY_B64] = keyB64
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
