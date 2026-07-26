package com.racelink.controller.core.storage

import android.content.Context
import android.content.SharedPreferences

class ControllerPreferencesStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("axis_controller_preferences", Context.MODE_PRIVATE)

    var gyroSensitivity: Float
        get() = prefs.getFloat("gyro_sensitivity", 2.0f)
        set(value) = prefs.edit().putFloat("gyro_sensitivity", value).apply()

    var stickSensitivity: Float
        get() = prefs.getFloat("stick_sensitivity", 1.0f)
        set(value) = prefs.edit().putFloat("stick_sensitivity", value).apply()

    var deadzone: Float
        get() = prefs.getFloat("deadzone", 0.05f)
        set(value) = prefs.edit().putFloat("deadzone", value).apply()

    var hapticsEnabled: Boolean
        get() = prefs.getBoolean("haptics_enabled", true)
        set(value) = prefs.edit().putBoolean("haptics_enabled", value).apply()

    var currentProfile: String
        get() = prefs.getString("current_profile", "Universal Gamepad") ?: "Universal Gamepad"
        set(value) = prefs.edit().putString("current_profile", value).apply()
}
