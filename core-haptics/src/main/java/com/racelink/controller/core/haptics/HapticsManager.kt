package com.racelink.controller.core.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

class HapticsManager(context: Context) {
    @Suppress("DEPRECATION")
    private val vibrator: Vibrator? = runCatching {
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }.getOrNull()

    fun tick() {
        runCatching {
            val v = vibrator ?: return@runCatching
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(18)
            }
        }
    }

    fun heavyClick() {
        runCatching {
            val v = vibrator ?: return@runCatching
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(45, 255))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(45)
            }
        }
    }

    fun rumble(durationMs: Long = 60, amplitude: Int = 220) {
        runCatching {
            val v = vibrator ?: return@runCatching
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(durationMs)
            }
        }
    }
}
