package com.racelink.controller.core.storage

import android.content.Context
import android.content.SharedPreferences

data class ControlElementTransform(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1.0f
)

class ControllerPreferencesStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("axis_controller_preferences", Context.MODE_PRIVATE)

    var gyroSensitivity: Float
        get() = prefs.getFloat("gyro_sensitivity", 2.0f)
        set(value) = prefs.edit().putFloat("gyro_sensitivity", value).apply()

    var stickSensitivity: Float
        get() = prefs.getFloat("stick_sensitivity", 1.0f)
        set(value) = prefs.edit().putFloat("stick_sensitivity", value).apply()

    var deadzone: Float
        get() = prefs.getFloat("deadzone", 0.04f)
        set(value) = prefs.edit().putFloat("deadzone", value).apply()

    var hapticsEnabled: Boolean
        get() = prefs.getBoolean("haptics_enabled", true)
        set(value) = prefs.edit().putBoolean("haptics_enabled", value).apply()

    var currentProfile: String
        get() = prefs.getString("current_profile", "Default Profile") ?: "Default Profile"
        set(value) = prefs.edit().putString("current_profile", value).apply()

    var customProfileNames: Set<String>
        get() = prefs.getStringSet("custom_profile_names", setOf("Default Profile")) ?: setOf("Default Profile")
        set(value) = prefs.edit().putStringSet("custom_profile_names", value).apply()

    fun addCustomProfile(name: String) {
        val set = customProfileNames.toMutableSet()
        set.add(name)
        customProfileNames = set
        currentProfile = name
    }

    fun deleteCustomProfile(name: String) {
        if (name == "Default Profile") return
        val set = customProfileNames.toMutableSet()
        set.remove(name)
        customProfileNames = set
        if (currentProfile == name) {
            currentProfile = "Default Profile"
        }
    }

    fun getTransform(profile: String, mode: String, element: String): ControlElementTransform {
        val x = prefs.getFloat("elem_${profile}_${mode}_${element}_x", 0f)
        val y = prefs.getFloat("elem_${profile}_${mode}_${element}_y", 0f)
        val scale = prefs.getFloat("elem_${profile}_${mode}_${element}_scale", 1.0f)
        return ControlElementTransform(x, y, scale)
    }

    fun setTransform(profile: String, mode: String, element: String, transform: ControlElementTransform) {
        prefs.edit()
            .putFloat("elem_${profile}_${mode}_${element}_x", transform.offsetX)
            .putFloat("elem_${profile}_${mode}_${element}_y", transform.offsetY)
            .putFloat("elem_${profile}_${mode}_${element}_scale", transform.scale)
            .apply()
    }

    fun resetLayout(profile: String, mode: String) {
        val editor = prefs.edit()
        listOf(
            "left_stick", "right_stick", "dpad", "action_buttons", "left_triggers", "right_triggers",
            "wheel", "brake_pedal", "throttle_pedal", "handbrake_button", "wheel_action_buttons"
        ).forEach { element ->
            editor.remove("elem_${profile}_${mode}_${element}_x")
            editor.remove("elem_${profile}_${mode}_${element}_y")
            editor.remove("elem_${profile}_${mode}_${element}_scale")
        }
        editor.apply()
    }
}
